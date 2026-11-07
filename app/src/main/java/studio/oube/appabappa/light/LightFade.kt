package studio.oube.appabappa.light

import android.os.Handler

// 조명 색이 한 번에 바뀌면 운전자의 시선을 끌 수 있어서 2초에 걸쳐 변경한다(판단).
internal const val FADE_MILLIS = 2_000L

// SDK에 전환 시간을 설정하는 매개변수가 없어서, 앱이 색 변경을 이 횟수만큼 나눠 차례로 요청한다(판단, 요청 간격은 약 0.29초).
private const val FADE_STEPS = 8

internal class LightFade(private val handler: Handler) {
    private val token = Any()

    // from 다음 색부터 to까지 FADE_MILLIS에 걸쳐 차례로 send를 호출한다. to로 호출할 때만 last가 true다.
    fun start(from: Int, to: Int, send: (color: Int, last: Boolean) -> Unit) {
        cancel()
        fadePath(from, to).drop(1).forEachIndexed { index, color ->
            val last = index == FADE_STEPS - 1
            val delay = FADE_MILLIS * index / (FADE_STEPS - 1)
            handler.postDelayed({ send(color, last) }, token, delay)
        }
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(token)
    }
}

// from에서 to로 서서히 변경하는 동안 조명에 켜지는 색이다. from부터 to까지 RGB 값을 같은 간격으로 나눈 FADE_STEPS + 1개다.
internal fun fadePath(from: Int, to: Int) =
    (0..FADE_STEPS).map { LightPlan.blend(from, to, it.toDouble() / FADE_STEPS) }
