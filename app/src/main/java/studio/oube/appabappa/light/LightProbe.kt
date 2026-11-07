package studio.oube.appabappa.light

import ai.pleos.playground.vehicle.api.Light
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import android.os.Handler
import android.os.SystemClock
import studio.oube.appabappa.AppLog

private const val PROBE_INTERVAL_MILLIS = 250L
private const val PROBE_TIMEOUT_MILLIS = 10_000L

// 요청이 완료되어도 색이 바로 변경되지 않아서, 변경된 색이 반환될 때까지 반복해서 조회하며 실제 반영 시간을 측정한다.
// 조명 색을 반복해서 조회하므로, 로그 단계를 DEBUG로 설정했을 때만 사용한다(AppLog.isMeasuring).
internal class LightProbe(private val light: Light, private val handler: Handler) {
    private val token = Any()

    // 다른 색을 요청해서 isCurrent가 false가 되면 측정을 중단한다.
    fun start(area: SeatArea, color: Int, requestedAt: Long, isCurrent: () -> Boolean) {
        light.getAmbientLightBrightnessColor(
            area = area,
            onComplete = { current ->
                handler.post { judge(area, color, requestedAt, current, isCurrent) }
            },
            onFailed = { AppLog.w("ambient light probe failed", it) }
        )
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(token)
    }

    private fun judge(
        area: SeatArea,
        color: Int,
        requestedAt: Long,
        current: Int?,
        isCurrent: () -> Boolean
    ) {
        val waited = SystemClock.elapsedRealtime() - requestedAt
        when {
            current != null && LightPlan.sameColor(current, color) ->
                AppLog.d("ambient light ${hex(color)} applied after $waited ms")

            waited >= PROBE_TIMEOUT_MILLIS ->
                AppLog.d(
                    "ambient light ${hex(color)} not applied after $waited ms, now ${hex(current)}"
                )

            isCurrent() ->
                handler.postDelayed(
                    { start(area, color, requestedAt, isCurrent) },
                    token,
                    PROBE_INTERVAL_MILLIS
                )
        }
    }
}
