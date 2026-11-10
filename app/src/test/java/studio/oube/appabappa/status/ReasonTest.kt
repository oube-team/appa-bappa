package studio.oube.appabappa.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.ManeuverTypes

class ReasonTest {
    @Test
    fun `FREE에는 바쁜 이유가 없다`() {
        assertNull(Reason.of(BusyStatus(BusyState.FREE)))
    }

    @Test
    fun `경고와 급제동과 후진은 근처 안내와 관계없이 그 근거를 알린다`() {
        val merge = "TURN_DEVIDE_MERGE"
        assertEquals(
            Reason.FORWARD_COLLISION_WARNING,
            Reason.of(busy(BusyCause.FORWARD_COLLISION_WARNING, merge))
        )
        assertEquals(
            Reason.BLIND_SPOT_WARNING,
            Reason.of(busy(BusyCause.BLIND_SPOT_WARNING, merge))
        )
        assertEquals(Reason.HARD_BRAKING, Reason.of(busy(BusyCause.HARD_BRAKING, merge)))
        assertEquals(Reason.REVERSING, Reason.of(busy(BusyCause.REVERSING, merge)))
    }

    @Test
    fun `근처에 안내가 없으면 방향지시등 조작은 방향지시등으로, 스티어링 휠 조작은 회전으로 알린다`() {
        assertEquals(Reason.TURN_SIGNAL, Reason.of(busy(BusyCause.SIGNALING, null)))
        assertEquals(Reason.TURN_SIGNAL, Reason.of(busy(BusyCause.FAST_SIGNALING, null)))
        assertEquals(Reason.TURNING, Reason.of(busy(BusyCause.TURNING, null)))
    }

    @Test
    fun `근처에 안내가 있으면 방향지시등 조작과 스티어링 휠 조작도 안내의 종류로 알린다`() {
        assertEquals(Reason.MERGE, Reason.of(busy(BusyCause.FAST_SIGNALING, "TURN_DEVIDE_MERGE")))
        assertEquals(Reason.LEFT_TURN, Reason.of(busy(BusyCause.TURNING, "TURN_LEFT")))
        assertEquals(Reason.HIGHWAY_EXIT, Reason.of(busy(BusyCause.MANEUVER, "TURN_EXIT_IC_R")))
    }

    @Test
    fun `회전 안내 지점을 지난 뒤에는 스티어링 휠을 돌린 적이 있을 때만 회전했다고 알린다`() {
        fun after(turnType: String, turned: Boolean) =
            Reason.of(BusyStatus(BusyState.RECOVERING, BusyCause.MANEUVER, turnType, turned))

        assertEquals(Reason.LEFT_TURN, after("TURN_LEFT", turned = true))
        assertEquals(Reason.PASSED_LEFT_TURN, after("TURN_LEFT", turned = false))
        assertEquals(Reason.PASSED_RIGHT_TURN, after("TURN_RIGHT", turned = false))
        assertEquals(Reason.PASSED_U_TURN, after("TURN_UTURN", turned = false))
        // 지점을 지나는 것으로 알리는 안내는 스티어링 휠과 관계없이 그대로 알린다.
        assertEquals(Reason.MERGE, after("TURN_DEVIDE_MERGE", turned = false))
        // 지점을 지나기 전(BUSY)에는 회전 안내를 그대로 알린다.
        assertEquals(Reason.LEFT_TURN, Reason.of(busy(BusyCause.MANEUVER, "TURN_LEFT")))
    }

    @Test
    fun `안내 지점에 닿기 전에 방향지시등이나 스티어링 휠을 조작했으면 isApproaching이 true다`() {
        fun ahead(cause: BusyCause, state: BusyState = BusyState.BUSY) =
            BusyStatus(state, cause, "TURN_LEFT", approaching = true)

        assertTrue(Reason.isApproaching(ahead(BusyCause.SIGNALING)))
        assertTrue(Reason.isApproaching(ahead(BusyCause.TURNING)))
        assertTrue(Reason.isApproaching(ahead(BusyCause.FAST_SIGNALING, BusyState.VERY_BUSY)))
        assertFalse(Reason.isApproaching(busy(BusyCause.SIGNALING, "TURN_LEFT")))
        assertFalse(Reason.isApproaching(busy(BusyCause.SIGNALING, null)))
    }

    @Test
    fun `경고와 급제동과 후진은 안내 지점에 닿기 전이어도 isApproaching이 false다`() {
        val causes =
            listOf(
                BusyCause.FORWARD_COLLISION_WARNING,
                BusyCause.BLIND_SPOT_WARNING,
                BusyCause.HARD_BRAKING,
                BusyCause.REVERSING
            )
        for (cause in causes) {
            val status = BusyStatus(BusyState.VERY_BUSY, cause, "TURN_LEFT", approaching = true)
            assertFalse(cause.name, Reason.isApproaching(status))
        }
    }

    @Test
    fun `갈림길 안내는 갈림길로, 졸음쉼터 안내는 휴게소와 구분해 졸음쉼터로 알린다`() {
        assertEquals(Reason.JUNCTION, Reason.of(busy(BusyCause.MANEUVER, "TURN_KEEP_R")))
        assertEquals(Reason.JUNCTION, Reason.of(busy(BusyCause.SIGNALING, "TURN_KEEP_L")))
        assertEquals(
            Reason.REST_AREA,
            Reason.of(busy(BusyCause.MANEUVER, "TURN_ENTER_SERVICEAREA"))
        )
        assertEquals(Reason.DROWSY_REST_AREA, Reason.of(busy(BusyCause.MANEUVER, "TURN_RESTAREA")))
    }

    @Test
    fun `ManeuverTypes의 demanding 안내마다 바쁜 이유를 반환한다`() {
        for (turnType in ManeuverTypes.demanding) {
            val soon = BusyStatus(BusyState.SOON, BusyCause.UPCOMING_MANEUVER, turnType)
            assertNotNull(turnType, Reason.of(soon))
        }
    }

    private fun busy(cause: BusyCause, turnType: String?) =
        BusyStatus(BusyState.BUSY, cause, turnType)
}
