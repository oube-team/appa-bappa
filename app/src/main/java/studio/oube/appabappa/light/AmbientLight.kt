package studio.oube.appabappa.light

import ai.pleos.playground.vehicle.api.Light
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener
import android.content.SharedPreferences
import android.os.Handler
import android.os.SystemClock
import androidx.core.content.edit
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.sdkCall

private const val ORIGINAL_COLOR_KEY = "original_color"
private const val CHANGE_FROM_KEY = "change_from_color"
private const val CHANGE_TO_KEY = "change_to_color"
private const val GREEN_MILLIS = 3_000L

// 초록색으로 다 바뀐 뒤 GREEN_MILLIS 동안 켜 두도록, 변경하는 시간(FADE_MILLIS)만큼 더 기다렸다가 원래 색으로 되돌린다.
// 앱 화면도 이 시간 동안 고마움을 전하는 문구를 보여 준다(MoodTracker).
internal const val GREEN_SHOW_MILLIS = FADE_MILLIS + GREEN_MILLIS

private const val STOP_TIMEOUT_MILLIS = 2_000L

// 요청이 끝났다는 응답을 수신해도 색은 조금 뒤에 바뀌어서(에뮬레이터 실측 0.05~1.25초), 이 시간만큼 기다린 뒤
// 색을 다시 조회해 확인한다.
private const val VERIFY_DELAY_MILLIS = 1_500L

// 색이 바뀌지 않았거나 요청이 실패하면 이 횟수까지 재요청한다. 그래도 바뀌지 않으면 다음 상태 변화 때 같은 색이라도
// 요청한다.
private const val MAX_RETRIES = 2

// 차가 요청받은 색 대신 비슷한 색을 켜면 알림의 색만으로는 누가 변경했는지 구별할 수 없어서(실차 확인 못 함),
// 앱이 색 변경을 요청한 뒤 이 시간 안에 수신한 알림은 앱이 변경한 결과로 판단한다.
private const val OWN_CHANGE_WINDOW_MILLIS = 3_000L
private val stopToken = Any()
private val verifyToken = Any()

class AmbientLight(
    private val light: Light,
    private val handler: Handler,
    private val savedColors: SharedPreferences,
    private val level: () -> Double
) {
    // 원래 색을 알아야 되돌릴 수 있으므로, 원래 색을 확인하기 전에는 조명 색을 변경하지 않는다.
    private var cabin: Cabin? = null
    private var shownColor: Int? = null
    private var wantedColor: Int? = null
    private var lastRequestAt = 0L
    private var stopping = false

    private val probe = LightProbe(light, handler)
    private val fade = LightFade(handler)
    private val restoreOriginal = Runnable { setColor(null, gradually = true) }

    private val colorListener =
        object : AmbientLightBrightnessColorListener {
            override fun onBrightnessColorUpdated(argbColor: Int?) {
                handler.post { confirmColor() }
            }

            // 리스너를 등록한 직후에 지금 켜져 있지 않은 예전 색의 알림을 수신한 적이 있어서(2026-11-11 에뮬레이터), 알림의
            // 색을 사용하지 않고 조명 색을 다시 조회해 확인한다. 앱이 색 변경을 요청한 직후에 수신한 알림은 앱이 변경한
            // 결과로 판단해 조명 색을 조회하지 않는다.
            private fun confirmColor() {
                val (area) = cabin ?: return
                if (stopping || since(lastRequestAt) < OWN_CHANGE_WINDOW_MILLIS) return
                light.getAmbientLightBrightnessColor(
                    area = area,
                    onComplete = { color ->
                        if (color != null) handler.post { onColorChanged(color or OPAQUE) }
                    },
                    onFailed = { AppLog.w("ambient light color read failed", it) }
                )
            }

            override fun onFailed(e: Exception) {
                AppLog.w("ambient light listener failed", e)
            }
        }

    fun start() {
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { _, areas ->
                // 에뮬레이터에서는 areas에 차 안 전체 영역 하나만 포함되어 있어서 첫 번째 영역을 사용한다.
                handler.post {
                    val area = areas.firstOrNull()
                    if (!stopping && area != null) {
                        light.getAmbientLightBrightnessColor(
                            area = area,
                            onComplete = { color ->
                                handler.post { rememberOriginal(area, color) }
                            },
                            onFailed = { AppLog.w("ambient light color read failed", it) }
                        )
                    }
                }
            },
            onFailed = { AppLog.w("ambient light capability failed", it) }
        )
    }

    fun show(previous: BusyState, next: BusyState) {
        handler.removeCallbacks(restoreOriginal)
        if (LightPlan.showsGreen(previous, next)) {
            setColor(LightPlan.GREEN, gradually = true)
            handler.postDelayed(restoreOriginal, GREEN_SHOW_MILLIS)
        } else {
            setColor(LightPlan.colorFor(next), gradually = true)
        }
    }

    // level 값이 바뀌면 켜 둔 앱의 색을 새 밝기로 변경한다. 원래 색은 밝기를 낮추지 않아서, 켜 둔 동안에는 변경하지 않는다.
    fun refresh() {
        wantedColor?.let { setColor(it, gradually = true) }
    }

    // 조명을 원래 색으로 되돌리고, 그 요청의 응답을 수신한 뒤 onStopped를 호출한다.
    // 응답을 수신하지 못해도 서비스 종료가 늦어지지 않도록, 2초가 지나면 onStopped를 호출한다.
    fun stop(onStopped: () -> Unit) {
        stopping = true
        handler.removeCallbacks(restoreOriginal)
        handler.removeCallbacksAndMessages(verifyToken)
        fade.cancel()
        probe.cancel()
        cabin?.let { (area) ->
            sdkCall("ambient light listener unregister") {
                light.unregisterAmbientLightBrightnessColorListener(area, colorListener)
            }
        }
        var stopped = false
        val finish =
            Runnable {
                if (!stopped) {
                    stopped = true
                    handler.removeCallbacksAndMessages(stopToken)
                    onStopped()
                }
            }
        // SDK 연결을 해제하기 전에 끝나도록, 서서히 변경하지 않고 한 번에 원래 색으로 되돌린다.
        val requested = setColor(null, gradually = false) { handler.post(finish) }
        if (requested) {
            handler.postDelayed(finish, stopToken, STOP_TIMEOUT_MILLIS)
        } else {
            finish.run()
        }
    }

    private fun rememberOriginal(area: SeatArea, color: Int?) {
        if (stopping) return
        // SDK 문서에는 알파 값을 0xFF로 고정한다고 명시되어 있지만 에뮬레이터에서는 0이 반환되어서, 조회한 색의 알파 값을 0xFF로 설정한다.
        val current = color?.let { it or OPAQUE }
        // 앱이 조명 색을 변경한 채로 종료됐을 수 있어서, 시작할 때 조회한 색이 앱이 켠 색이면 그 색 대신 지난번에 저장한
        // 원래 색을 사용한다(LightRecord.atStart).
        val saved = savedColors.lightRecord()
        val record = saved.atStart(current)
        if (record != saved) savedColors.save(record)
        val original = record.original
        AppLog.i("ambient light original color ${hex(original)}, now ${hex(current)}")
        if (original != null) {
            cabin = Cabin(area, original)
            shownColor = current
            // 운전 중에 사용자가 차 설정에서 조명 색을 변경하면 그 색을 다음에 되돌릴 원래 색으로 삼도록(onColorChanged),
            // 조명 색 변경 리스너를 등록한다.
            sdkCall("ambient light listener register") {
                light.registerAmbientLightBrightnessColorListener(area, colorListener)
            }
            // 원래 색을 조회하는 동안 상태가 바뀌었거나 지난번에 앱이 켠 색이 남아 있을 수 있어서, 조명 색을 지금 상태에 맞게 변경한다.
            setColor(wantedColor, gradually = true)
        }
    }

    // 켜져 있던 색(shownColor)이나 원래 색과 같은 색은 바뀐 것이 없어서 무시한다.
    // 색 변경을 요청한 직후이거나 밝기를 낮춘 앱의 색(주황색, 연한 주황색, 초록색)이면 앱이 켠 것이어서 무시한다.
    // 그 밖의 색은 사용자가 변경한 색으로 판단해 다음에 되돌릴 원래 색으로 삼는다. 마지막으로 요청한 색과만
    // 비교하면, 사용자가 다른 색으로 변경했다가 앱이 요청했던 색으로 되돌린 경우를 놓친다.
    // 앱이 요청한 직후에 사용자가 색을 변경하거나, 밝기를 낮춘 앱의 색과 똑같은 색을 고르면 알아채지 못한다.
    // 바쁜 상황 중에 사용자가 변경했어도 그 색을 바로 덮지 않고, 다음 상태 변화 때 앱의 색을 요청한다.
    private fun onColorChanged(color: Int) {
        val current = cabin ?: return
        val unchanged =
            shownColor?.let { LightPlan.sameColor(it, color) } == true ||
                LightPlan.sameColor(current.originalColor, color)
        val byApp = since(lastRequestAt) < OWN_CHANGE_WINDOW_MILLIS || LightPlan.isOwnColor(color)
        if (stopping || unchanged || byApp) return
        AppLog.i("ambient light changed outside the app to ${hex(color)}")
        cabin = current.copy(originalColor = color)
        savedColors.save(savedColors.lightRecord().withUserColor(color))
        shownColor = color
    }

    // color가 null이면 조명을 원래 색으로 되돌린다. gradually가 true면 FADE_MILLIS에 걸쳐 변경한다.
    // 요청을 보냈으면 true를 반환한다. 한 번에 변경할 때는 요청의 응답을 수신하면 onAnswered를 호출한다.
    private fun setColor(color: Int?, gradually: Boolean, onAnswered: () -> Unit = {}): Boolean {
        wantedColor = color
        val (area, originalColor) = cabin ?: return false
        // 앱의 색만 밝기를 낮추고, 사용자가 고른 원래 색은 그대로 되돌린다.
        val next = color?.let { LightPlan.dimmed(it, level()) } ?: originalColor
        fade.cancel()
        val from = shownColor
        val unchanged = from != null && LightPlan.sameColor(from, next)
        if (!unchanged) {
            val record = savedColors.lightRecord().withChange(from ?: next, next)
            savedColors.save(record, beforeRequest = true)
            if (gradually && from != null) {
                fade.start(from, next) { step, last ->
                    request(area, step, attempt = 0, verifyAfter = last)
                }
            } else {
                request(area, next, attempt = 0, onAnswered = onAnswered)
            }
        }
        return !unchanged
    }

    // 서서히 변경하는 도중의 색(verifyAfter가 false)은 곧 다음 색으로 바뀌어서 다시 조회해 확인하지 않는다.
    private fun request(
        area: SeatArea,
        color: Int,
        attempt: Int,
        verifyAfter: Boolean = true,
        onAnswered: () -> Unit = {}
    ) {
        shownColor = color
        handler.removeCallbacksAndMessages(verifyToken)
        val requestedAt = SystemClock.elapsedRealtime()
        lastRequestAt = requestedAt
        AppLog.i("ambient light set ${hex(color)}")
        light.setAmbientLightBrightnessColor(
            area = area,
            value = color,
            onComplete = { _, _ ->
                AppLog.i("ambient light set ${hex(color)} answered after ${since(requestedAt)} ms")
                onAnswered()
                if (verifyAfter) {
                    handler.postDelayed({
                        verify(area, color, attempt)
                    }, verifyToken, VERIFY_DELAY_MILLIS)
                }
            },
            onFailed = {
                AppLog.w("ambient light set ${hex(color)} failed", it)
                onAnswered()
                handler.post { retryOrGiveUp(area, color, attempt, current = null) }
            }
        )
        if (AppLog.isMeasuring && !stopping) {
            probe.cancel()
            probe.start(area, color, requestedAt) { shownColor == color }
        }
    }

    // set의 완료 응답은 요청을 접수했다는 뜻이어서(SDK 소스), 요청 뒤 조명 색을 다시 조회해 바뀌지 않았으면 재요청한다.
    // 가상 콘솔 탭이 뒤에 있을 때처럼 요청은 끝났다는데 색이 그대로인 경우가 있었다(2026-11-03 에뮬레이터).
    private fun verify(area: SeatArea, color: Int, attempt: Int) {
        if (stopping || shownColor != color) return
        light.getAmbientLightBrightnessColor(
            area = area,
            onComplete = { current ->
                handler.post {
                    val applied = current != null && LightPlan.sameColor(current, color)
                    if (!applied) retryOrGiveUp(area, color, attempt, current)
                }
            },
            onFailed = { AppLog.w("ambient light verify failed", it) }
        )
    }

    private fun retryOrGiveUp(area: SeatArea, color: Int, attempt: Int, current: Int?) {
        if (stopping || shownColor != color) return
        val message = "ambient light ${hex(color)} not applied, now ${hex(current)}"
        if (attempt < MAX_RETRIES) {
            AppLog.w("$message, retry ${attempt + 1}")
            request(area, color, attempt + 1)
        } else {
            AppLog.w("$message, gave up after ${attempt + 1} tries")
            // 다음 상태 변화 때 같은 색이라도 다시 요청하도록, 실제로 켜져 있는 색을 shownColor에 설정한다.
            shownColor = current?.let { it or OPAQUE }
        }
    }

    private data class Cabin(val area: SeatArea, val originalColor: Int)
}

private fun since(startedAt: Long) = SystemClock.elapsedRealtime() - startedAt

private fun SharedPreferences.lightRecord() = LightRecord(
    original = savedColor(ORIGINAL_COLOR_KEY),
    changeFrom = savedColor(CHANGE_FROM_KEY),
    changeTo = savedColor(CHANGE_TO_KEY)
)

private fun SharedPreferences.savedColor(key: String) = getInt(key, 0).takeIf { contains(key) }

// 색을 변경하는 도중에 앱이 종료돼도 다음에 시작할 때 조명에 남은 색을 앱이 켠 색으로 판단하도록(LightRecord.atStart),
// 색 변경을 요청하기 전에 저장할 때는(beforeRequest) 파일 저장이 요청보다 늦어지지 않게 apply 대신 commit을 사용한다.
private fun SharedPreferences.save(record: LightRecord, beforeRequest: Boolean = false) =
    edit(commit = beforeRequest) {
        putColor(ORIGINAL_COLOR_KEY, record.original)
        putColor(CHANGE_FROM_KEY, record.changeFrom)
        putColor(CHANGE_TO_KEY, record.changeTo)
    }

private fun SharedPreferences.Editor.putColor(key: String, color: Int?) {
    if (color != null) putInt(key, color) else remove(key)
}
