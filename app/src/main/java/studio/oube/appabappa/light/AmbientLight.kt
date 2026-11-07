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

private const val OPAQUE = 0xFF000000.toInt()
private const val ORIGINAL_COLOR_KEY = "original_color"
private const val GREEN_MILLIS = 3_000L
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
    private val savedColors: SharedPreferences
) {
    // 원래 색을 알아야 되돌릴 수 있으므로, 원래 색을 확인하기 전에는 조명 색을 변경하지 않는다.
    private var cabin: Cabin? = null
    private var shownColor: Int? = null
    private var wantedColor: Int? = null
    private var lastRequestAt = 0L
    private var stopping = false

    private val probe = LightProbe(light, handler)
    private val restoreOriginal = Runnable { setColor(null) }

    private val colorListener =
        object : AmbientLightBrightnessColorListener {
            override fun onBrightnessColorUpdated(argbColor: Int?) {
                if (argbColor != null) handler.post { onColorChanged(argbColor or OPAQUE) }
            }

            override fun onFailed(e: Exception) {
                AppLog.w("ambient light listener failed", e)
            }
        }

    fun start() {
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { _, areas ->
                // 에뮬레이터에서는 areas에 차 안 전체 영역 하나만 포함되어 있어서 첫 번째 영역을 사용한다.
                handler.post { if (!stopping) areas.firstOrNull()?.let { readOriginalColor(it) } }
            },
            onFailed = { AppLog.w("ambient light capability failed", it) }
        )
    }

    fun show(previous: BusyState, next: BusyState) {
        handler.removeCallbacks(restoreOriginal)
        if (LightPlan.showsGreen(previous, next)) {
            setColor(LightPlan.GREEN)
            handler.postDelayed(restoreOriginal, GREEN_MILLIS)
        } else {
            setColor(LightPlan.colorFor(next))
        }
    }

    // 조명을 원래 색으로 되돌리고, 그 요청의 응답을 수신한 뒤 onStopped를 호출한다.
    // 응답을 수신하지 못해도 서비스 종료가 늦어지지 않도록, 2초가 지나면 onStopped를 호출한다.
    fun stop(onStopped: () -> Unit) {
        stopping = true
        handler.removeCallbacks(restoreOriginal)
        handler.removeCallbacksAndMessages(verifyToken)
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
        val requested = setColor(null) { handler.post(finish) }
        if (requested) {
            handler.postDelayed(finish, stopToken, STOP_TIMEOUT_MILLIS)
        } else {
            finish.run()
        }
    }

    private fun readOriginalColor(area: SeatArea) {
        light.getAmbientLightBrightnessColor(
            area = area,
            onComplete = { color -> handler.post { rememberOriginal(area, color) } },
            onFailed = { AppLog.w("ambient light color read failed", it) }
        )
    }

    private fun rememberOriginal(area: SeatArea, color: Int?) {
        if (stopping) return
        // SDK 문서에는 알파 값을 0xFF로 고정한다고 명시되어 있지만 에뮬레이터에서는 0이 반환되어서, 조회한 색의 알파 값을 0xFF로 설정한다.
        val current = color?.let { it or OPAQUE }
        // 앱이 주황이나 초록을 켜 둔 채 종료됐다면 시작할 때 조회한 색이 원래 색이 아니므로, 지난번에 저장한 원래 색을 사용한다.
        val original = if (current != null && !LightPlan.isOwnColor(current)) {
            savedColors.edit { putInt(ORIGINAL_COLOR_KEY, current) }
            current
        } else {
            savedColors.getInt(ORIGINAL_COLOR_KEY, 0).takeIf {
                savedColors.contains(ORIGINAL_COLOR_KEY)
            }
        }
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
            setColor(wantedColor)
        }
    }

    // 켜져 있던 색(shownColor)이나 원래 색과 같은 알림은 바뀐 것이 없어서 무시한다.
    // 색 변경을 요청한 직후의 알림이거나 앱의 색(주황, 연한 주황, 초록)이면 앱이 변경한 것이어서 무시한다.
    // 그 밖의 색은 사용자가 변경한 색으로 판단해 다음에 되돌릴 원래 색으로 삼는다. 마지막으로 요청한 색과만
    // 비교하면, 사용자가 다른 색으로 변경했다가 앱이 요청했던 색으로 되돌린 경우를 놓친다.
    // 앱이 색 변경을 요청한 직후에 사용자가 변경하거나 앱의 색과 똑같은 색을 고르면 알아채지 못한다.
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
        savedColors.edit { putInt(ORIGINAL_COLOR_KEY, color) }
        shownColor = color
    }

    // color가 null이면 조명을 원래 색으로 되돌린다. 요청을 보냈으면 true를 반환한다.
    // 응답을 수신하면 onAnswered를 호출한다.
    private fun setColor(color: Int?, onAnswered: () -> Unit = {}): Boolean {
        wantedColor = color
        val (area, originalColor) = cabin ?: return false
        val next = color ?: originalColor
        val unchanged = shownColor?.let { LightPlan.sameColor(it, next) } == true
        if (!unchanged) request(area, next, attempt = 0, onAnswered = onAnswered)
        return !unchanged
    }

    private fun request(area: SeatArea, color: Int, attempt: Int, onAnswered: () -> Unit = {}) {
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
                handler.postDelayed({
                    verify(area, color, attempt)
                }, verifyToken, VERIFY_DELAY_MILLIS)
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
