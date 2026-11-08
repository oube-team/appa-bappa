package studio.oube.appabappa.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import studio.oube.appabappa.engine.BusyCause.BLIND_SPOT_WARNING
import studio.oube.appabappa.engine.BusyCause.FORWARD_COLLISION_WARNING
import studio.oube.appabappa.engine.BusyState.FREE
import studio.oube.appabappa.engine.BusyState.RECOVERING
import studio.oube.appabappa.engine.BusyState.VERY_BUSY

class SafetyWarningTest {
    @Test
    fun `전방 충돌 경고가 켜지면 VERY_BUSY가 되고 이유로 FORWARD_COLLISION_WARNING을 반환한다`() {
        val warned = driving(60.0).copy(safetyWarning = SafetyWarning.FORWARD_COLLISION)

        assertEquals(
            BusyStatus(VERY_BUSY, FORWARD_COLLISION_WARNING),
            BusyEngine().update(warned, 0)
        )
    }

    @Test
    fun `주행 중 방향지시등과 후측방 경고가 함께 켜져 있으면 VERY_BUSY가 되고 이유로 BLIND_SPOT_WARNING을 고른다`() {
        val warned = driving(50.0, TurnSignal.RIGHT).copy(safetyWarning = SafetyWarning.BLIND_SPOT)

        assertEquals(BusyStatus(VERY_BUSY, BLIND_SPOT_WARNING), BusyEngine().update(warned, 0))
    }

    @Test
    fun `전방 충돌 경고와 급제동이 함께 참이면 VERY_BUSY가 되고 이유로 FORWARD_COLLISION_WARNING을 고른다`() {
        val engine = BusyEngine()
        engine.update(driving(50.0), 0)
        val braking = driving(30.0).copy(safetyWarning = SafetyWarning.FORWARD_COLLISION)

        assertEquals(
            BusyStatus(VERY_BUSY, FORWARD_COLLISION_WARNING),
            engine.update(braking, 1_000)
        )
    }

    @Test
    fun `경고가 꺼지면 RECOVERING을 10초 동안 유지한 뒤 FREE로 돌아온다`() {
        val engine = BusyEngine()
        engine.update(driving(60.0).copy(safetyWarning = SafetyWarning.FORWARD_COLLISION), 0)
        engine.update(driving(60.0), 250)

        assertEquals(RECOVERING, engine.update(driving(60.0), 10_000).state)
        assertEquals(FREE, engine.update(driving(60.0), 10_250).state)
    }

    @Test
    fun `기어가 P이면 경고가 켜져 있어도 FREE를 유지한다`() {
        val parked =
            driving(0.0, gear = Gear.PARK).copy(safetyWarning = SafetyWarning.FORWARD_COLLISION)

        assertEquals(BusyStatus(FREE), BusyEngine().update(parked, 0))
    }
}
