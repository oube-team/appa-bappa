package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import android.car.VehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal

class SignalMappingTest {
    @Test
    fun `기어 값 P, R, N은 각각 PARK, REVERSE, NEUTRAL로 처리한다`() {
        assertEquals(Gear.PARK, gearOf(VehicleGear.GEAR_PARK))
        assertEquals(Gear.REVERSE, gearOf(VehicleGear.GEAR_REVERSE))
        assertEquals(Gear.NEUTRAL, gearOf(VehicleGear.GEAR_NEUTRAL))
    }

    @Test
    fun `D, 수동 변속 단수, 아직 수신하지 못한 기어 값은 DRIVE로 처리한다`() {
        assertEquals(Gear.DRIVE, gearOf(VehicleGear.GEAR_DRIVE))
        assertEquals(Gear.DRIVE, gearOf(VehicleGear.GEAR_FIRST))
        assertEquals(Gear.DRIVE, gearOf(null))
    }

    @Test
    fun `방향지시등 값은 그대로 사용하고, 값이 없으면 OFF로 처리한다`() {
        assertEquals(TurnSignal.LEFT, turnSignalOf(TurnSignalValue.LEFT))
        assertEquals(TurnSignal.RIGHT, turnSignalOf(TurnSignalValue.RIGHT))
        assertEquals(TurnSignal.OFF, turnSignalOf(null))
    }

    @Test
    fun `경로 안내 중이면 가장 가까운 안내를 다음 안내로 사용한다`() {
        val guidance =
            listOf(
                TBTInfo(TurnType.TURN_LEFT, 80.0, ""),
                TBTInfo(TurnType.TURN_RIGHT, 380.0, "")
            )

        assertEquals(
            Maneuver("TURN_LEFT", 80.0),
            nextManeuverOf(true, guidance)
        )
    }

    @Test
    fun `경로 안내 중이 아니거나 안내 목록이 비어 있으면 다음 안내가 없다`() {
        val guidance = listOf(TBTInfo(TurnType.TURN_LEFT, 80.0, ""))

        assertNull(nextManeuverOf(false, guidance))
        assertNull(nextManeuverOf(true, emptyList()))
    }
}
