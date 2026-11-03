package studio.oube.appabappa.light

import ai.pleos.playground.vehicle.api.Light
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import android.content.SharedPreferences
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import studio.oube.appabappa.engine.BusyState

private const val TAG = "AppaBappa"
private const val OPAQUE = 0xFF000000.toInt()
private const val ORIGINAL_COLOR_KEY = "original_color"
private const val GREEN_MILLIS = 3_000L
private const val PROBE_INTERVAL_MILLIS = 250L
private const val PROBE_TIMEOUT_MILLIS = 10_000L
private val probeToken = Any()

class AmbientLight(
    private val light: Light,
    private val handler: Handler,
    private val savedColors: SharedPreferences
) {
    // 원래 색을 알아야 되돌릴 수 있으므로, 원래 색을 확인하기 전에는 조명 색을 변경하지 않는다.
    private var cabin: Cabin? = null
    private var shownColor: Int? = null
    private var wantedColor: Int? = null

    private val restoreOriginal = Runnable { setColor(null) }

    fun start() {
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { _, areas ->
                // 에뮬레이터에서는 areas에 차 안 전체 영역 하나만 포함되어 있어서 첫 번째 영역을 사용한다.
                handler.post { areas.firstOrNull()?.let { readOriginalColor(it) } }
            },
            onFailed = { Log.w(TAG, "ambient light capability failed", it) }
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

    fun stop() {
        handler.removeCallbacks(restoreOriginal)
        handler.removeCallbacksAndMessages(probeToken)
        setColor(null)
    }

    private fun readOriginalColor(area: SeatArea) {
        light.getAmbientLightBrightnessColor(
            area = area,
            onComplete = { color -> handler.post { rememberOriginal(area, color) } },
            onFailed = { Log.w(TAG, "ambient light color read failed", it) }
        )
    }

    private fun rememberOriginal(area: SeatArea, color: Int?) {
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
        Log.i(TAG, "ambient light original color ${hex(original)}, now ${hex(current)}")
        if (original != null) {
            cabin = Cabin(area, original)
            shownColor = current
            // 원래 색을 조회하는 동안 상태가 바뀌었거나 지난번에 앱이 켠 색이 남아 있을 수 있어서, 조명 색을 지금 상태에 맞게 변경한다.
            setColor(wantedColor)
        }
    }

    // color가 null이면 조명을 원래 색으로 되돌린다.
    private fun setColor(color: Int?) {
        wantedColor = color
        val (area, originalColor) = cabin ?: return
        val next = color ?: originalColor
        if (shownColor?.let { LightPlan.sameColor(it, next) } == true) return
        shownColor = next
        val requestedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "ambient light set ${hex(next)}")
        light.setAmbientLightBrightnessColor(
            area = area,
            value = next,
            onComplete = { _, _ ->
                Log.i(TAG, "ambient light set ${hex(next)} answered after ${since(requestedAt)} ms")
            },
            onFailed = {
                Log.w(TAG, "ambient light set ${hex(next)} failed", it)
                // 변경에 실패하면 같은 색을 다시 요청할 수 있도록 shownColor를 초기화한다.
                handler.post { if (shownColor == next) shownColor = null }
            }
        )
        // 반영 시간 측정은 조명 색을 반복해서 조회하므로, 디버그 로그를 활성화했을 때만 실행한다.
        // 활성화 명령: adb shell setprop log.tag.AppaBappa DEBUG
        if (Log.isLoggable(TAG, Log.DEBUG)) probe(area, next, requestedAt)
    }

    // 요청이 완료되어도 색이 바로 변경되지 않아서, 변경된 색이 반환될 때까지 반복해서 조회하며 실제 반영 시간을 측정한다.
    private fun probe(area: SeatArea, color: Int, requestedAt: Long) {
        light.getAmbientLightBrightnessColor(
            area = area,
            onComplete = { current ->
                handler.post { judgeProbe(area, color, requestedAt, current) }
            },
            onFailed = { Log.w(TAG, "ambient light probe failed", it) }
        )
    }

    private fun judgeProbe(area: SeatArea, color: Int, requestedAt: Long, current: Int?) {
        val waited = since(requestedAt)
        when {
            current != null && LightPlan.sameColor(current, color) ->
                Log.d(TAG, "ambient light ${hex(color)} applied after $waited ms")

            waited >= PROBE_TIMEOUT_MILLIS ->
                Log.d(
                    TAG,
                    "ambient light ${hex(color)} not applied after $waited ms, now ${hex(current)}"
                )

            shownColor == color ->
                handler.postDelayed(
                    { probe(area, color, requestedAt) },
                    probeToken,
                    PROBE_INTERVAL_MILLIS
                )
        }
    }

    private data class Cabin(val area: SeatArea, val originalColor: Int)
}

private fun since(startedAt: Long) = SystemClock.elapsedRealtime() - startedAt

private fun hex(color: Int?) = color?.let { "#%06X".format(it and LightPlan.RGB_MASK) } ?: "null"
