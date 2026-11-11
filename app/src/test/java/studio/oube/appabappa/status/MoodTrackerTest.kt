package studio.oube.appabappa.status

import org.junit.Assert.assertEquals
import org.junit.Test
import studio.oube.appabappa.engine.BusyState

class MoodTrackerTest {
    private val tracker = MoodTracker(thanksMillis = 5_000)

    @Test
    fun `BUSY와 VERY_BUSY는 같은 BUSY 화면으로 보여 준다`() {
        assertEquals(Mood.BUSY, tracker.update(BusyState.BUSY, 0))
        assertEquals(Mood.BUSY, tracker.update(BusyState.VERY_BUSY, 250))
    }

    @Test
    fun `바쁜 상황이 끝나면 thanksMillis 동안 THANKS를 보여 준 뒤 FREE로 돌아온다`() {
        tracker.update(BusyState.RECOVERING, 0)

        assertEquals(Mood.THANKS, tracker.update(BusyState.FREE, 1_000))
        assertEquals(Mood.THANKS, tracker.update(BusyState.FREE, 5_999))
        assertEquals(Mood.FREE, tracker.update(BusyState.FREE, 6_000))
    }

    @Test
    fun `SOON에서 FREE로 돌아오면 조명이 초록색이 되지 않으므로 THANKS 없이 FREE를 보여 준다`() {
        tracker.update(BusyState.SOON, 0)

        assertEquals(Mood.FREE, tracker.update(BusyState.FREE, 250))
    }

    @Test
    fun `THANKS를 보여 주는 동안 SOON을 거쳐 FREE가 되면 THANKS를 다시 보여 주지 않는다`() {
        tracker.update(BusyState.BUSY, 0)
        assertEquals(Mood.THANKS, tracker.update(BusyState.FREE, 1_000))

        assertEquals(Mood.SOON, tracker.update(BusyState.SOON, 2_000))
        assertEquals(Mood.FREE, tracker.update(BusyState.FREE, 3_000))
    }

    @Test
    fun `THANKS를 보여 주는 동안 다시 바빠지면 BUSY를 보여 주고 그 뒤 FREE가 되면 THANKS를 새로 시작한다`() {
        tracker.update(BusyState.BUSY, 0)
        tracker.update(BusyState.FREE, 1_000)

        assertEquals(Mood.BUSY, tracker.update(BusyState.BUSY, 2_000))
        assertEquals(Mood.RECOVERING, tracker.update(BusyState.RECOVERING, 3_000))
        assertEquals(Mood.THANKS, tracker.update(BusyState.FREE, 8_500))
    }
}
