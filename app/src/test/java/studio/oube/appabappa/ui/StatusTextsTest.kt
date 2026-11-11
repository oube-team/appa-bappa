package studio.oube.appabappa.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.ManeuverTypes
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.Reason

class StatusTextsTest {
    @Test
    fun `받침이 없으면 뒤의 조사를, 있으면 앞의 조사를 붙인다`() {
        assertEquals("아빠가", withJosa("아빠", "이", "가"))
        assertEquals("운전자랑", withJosa("운전자", "이랑", "랑"))
        assertEquals("삼촌이", withJosa("삼촌", "이", "가"))
    }

    @Test
    fun `BUSY 문구는 호칭과 바쁜 이유를 함께 보여 준다`() {
        val copy = copyOf(Mood.BUSY, Reason.MERGE, who = "엄마")

        assertEquals(listOf("엄마가 운전에", "집중하고 있어요"), copy.title)
        assertEquals("합류나 분기 지점을 지나고 있어요", copy.reason)
    }

    @Test
    fun `SOON과 RECOVERING 문구는 그 상태에 맞는 바쁜 이유를 보여 준다`() {
        val soon = copyOf(Mood.SOON, Reason.LEFT_TURN)
        assertEquals(listOf("아빠가 곧 운전에", "집중해야 해요"), soon.title)
        assertEquals("좌회전할 지점이 가까워요", soon.reason)

        val recovering = copyOf(Mood.RECOVERING, Reason.LEFT_TURN)
        assertEquals("방금 좌회전했어요", recovering.reason)

        val passed = copyOf(Mood.RECOVERING, Reason.PASSED_LEFT_TURN)
        assertEquals("방금 좌회전할 지점을 지났어요", passed.reason)
    }

    @Test
    fun `BUSY에서 안내 지점에 닿기 전이면 SOON 문장으로 알린다`() {
        val left = copyOf(Mood.BUSY, Reason.LEFT_TURN, approaching = true)
        assertEquals(listOf("아빠가 운전에", "집중하고 있어요"), left.title)
        assertEquals("좌회전할 지점이 가까워요", left.reason)
        assertEquals("바쁜 상황이 끝나면 초록색으로 알려 드릴게요", left.sub)

        val merge = copyOf(Mood.BUSY, Reason.MERGE, approaching = true)
        assertEquals("합류나 분기 지점이 가까워요", merge.reason)
    }

    @Test
    fun `SOON 문장이 없는 이유는 BUSY 문장으로 알린다`() {
        val copy = copyOf(Mood.BUSY, Reason.TURN_SIGNAL, approaching = true)

        assertEquals("방향지시등을 켜고 있어요", copy.reason)
    }

    @Test
    fun `SOON과 RECOVERING 문구는 안내 지점에 닿기 전인지와 관계없이 그 상태의 문장을 보여 준다`() {
        val soon = copyOf(Mood.SOON, Reason.LEFT_TURN, approaching = true)
        assertEquals("좌회전할 지점이 가까워요", soon.reason)

        val recovering = copyOf(Mood.RECOVERING, Reason.LEFT_TURN, approaching = true)
        assertEquals("방금 좌회전했어요", recovering.reason)
    }

    @Test
    fun `FREE와 THANKS 문구에는 바쁜 이유가 없다`() {
        assertEquals(null, copyOf(Mood.FREE, Reason.MERGE).reason)
        assertEquals(null, copyOf(Mood.THANKS, Reason.MERGE).reason)
    }

    @Test
    fun `실내 조명으로 알리기를 끄면 FREE 문구는 화면으로 알린다고 안내한다`() {
        assertEquals(
            "운전에 집중해야 할 때는 조명 색으로 알려 드려요",
            copyOf(Mood.FREE, null).sub
        )
        assertEquals(
            "운전에 집중해야 할 때는 화면으로 알려 드려요",
            copyOf(Mood.FREE, null, lightEnabled = false).sub
        )
    }

    @Test
    fun `엔진이 SOON으로 판단하는 모든 안내에 SOON 문장이 있다`() {
        for (turnType in ManeuverTypes.demanding) {
            val reason =
                Reason.of(BusyStatus(BusyState.SOON, BusyCause.UPCOMING_MANEUVER, turnType))
            assertNotNull(turnType, reason?.lines()?.soon)
        }
    }

    private fun copyOf(
        mood: Mood,
        reason: Reason?,
        approaching: Boolean = false,
        who: String = "아빠",
        lightEnabled: Boolean = true
    ) = statusCopy(mood, reason, approaching, who, lightEnabled)
}
