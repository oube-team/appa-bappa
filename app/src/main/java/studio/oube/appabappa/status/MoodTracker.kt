package studio.oube.appabappa.status

import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.light.GREEN_SHOW_MILLIS
import studio.oube.appabappa.light.LightPlan

// 조명은 바쁜 상황이 끝나면 초록색으로 바뀌었다가 원래 색으로 돌아가므로(AmbientLight.show), 화면도 조명이 원래 색으로
// 돌아가기 시작할 때까지 THANKS를 보여 준다.
class MoodTracker(private val thanksMillis: Long = GREEN_SHOW_MILLIS) {
    private var previous = BusyState.FREE
    private var thanksUntilMillis = 0L

    fun update(state: BusyState, nowMillis: Long): Mood {
        if (LightPlan.showsGreen(previous, state)) thanksUntilMillis = nowMillis + thanksMillis
        if (state != BusyState.FREE) thanksUntilMillis = 0L
        previous = state
        return when (state) {
            BusyState.SOON -> Mood.SOON
            BusyState.BUSY, BusyState.VERY_BUSY -> Mood.BUSY
            BusyState.RECOVERING -> Mood.RECOVERING
            BusyState.FREE -> if (nowMillis < thanksUntilMillis) Mood.THANKS else Mood.FREE
        }
    }
}
