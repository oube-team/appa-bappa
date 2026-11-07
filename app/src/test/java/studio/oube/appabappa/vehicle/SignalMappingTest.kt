package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import android.car.VehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `안내 모드가 ROUTE_GUIDING일 때만 경로 안내 중으로 판단한다`() {
        assertTrue(isRouteGuiding(NaviMode.ROUTE_GUIDING))
        assertFalse(isRouteGuiding(NaviMode.SAFE_GUIDING))
        assertFalse(isRouteGuiding(NaviMode.SIMULATION_GUIDING))
        assertFalse(isRouteGuiding(NaviMode.NOT_WORKING))
        assertFalse(isRouteGuiding(null))
    }

    @Test
    fun `안내 목록을 순서 그대로 엔진의 안내 목록으로 변환한다`() {
        val guidance =
            listOf(
                TBTInfo(TurnType.TURN_LEFT, 80.0, ""),
                TBTInfo(TurnType.TURN_RIGHT, 380.0, "")
            )

        assertEquals(
            listOf(Maneuver("TURN_LEFT", 80.0), Maneuver("TURN_RIGHT", 380.0)),
            maneuversOf(guidance)
        )
    }

    @Test
    fun `안내 목록이 비어 있으면 빈 목록을 반환한다`() {
        assertEquals(emptyList<Maneuver>(), maneuversOf(emptyList()))
    }

    @Test
    fun `조향각 범위를 알면 범위의 끝을 540도로 간주해 스티어링 휠 각도로 변환한다`() {
        assertEquals(90.0, wheelAngleOf(6.0, 36.0), 1e-9)
        assertEquals(-540.0, wheelAngleOf(-36.0, 36.0), 1e-9)
    }

    @Test
    fun `조향각 범위가 스티어링 휠 각도 범위와 같으면 수신한 값을 그대로 반환한다`() {
        assertEquals(120.0, wheelAngleOf(120.0, 540.0), 1e-9)
    }

    @Test
    fun `조향각 범위를 모르면 수신한 값을 그대로 반환한다`() {
        assertEquals(30.0, wheelAngleOf(30.0, null), 1e-9)
    }

    @Test
    fun `조향각 범위의 끝은 최솟값과 최댓값 가운데 절댓값이 큰 쪽이다`() {
        assertEquals(36.0, fullLockOf(-36f, 35f) ?: 0.0, 1e-9)
    }

    @Test
    fun `조향각 범위가 0이면 범위를 모르는 것으로 판단한다`() {
        assertNull(fullLockOf(0f, 0f))
    }
}
