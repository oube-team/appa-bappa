package studio.oube.appabappa.ui.drive

import org.junit.Assert.assertEquals
import org.junit.Test
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveHint
import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.status.DriveProgress

class DriveTextsTest {
    private fun signals(
        kmh: Double = 0.0,
        gear: Gear = Gear.DRIVE,
        turn: TurnSignal = TurnSignal.OFF,
        steering: Double = 0.0,
        maneuverMeters: Double? = null
    ) = VehicleSignals(
        speedMps = kmh / 3.6,
        gear = gear,
        turnSignal = turn,
        steeringAngleDeg = steering,
        nextManeuver = maneuverMeters?.let { Maneuver("TURN_LEFT", it) }
    )

    private fun busy(cause: BusyCause, state: BusyState = BusyState.BUSY) =
        DriveProgress(BusyStatus(state, cause), 5_000)

    @Test
    fun `BUSY와 VERY_BUSY의 근거마다 판단 기준의 숫자로 문장을 만든다`() {
        assertEquals(
            "1초 사이에 차속이 초속 3.5m 이상 줄었어요",
            ruleText(busy(BusyCause.HARD_BRAKING, BusyState.VERY_BUSY), signals(40.0))
        )
        assertEquals(
            "시속 60km 이상에서 방향지시등을 켰어요",
            ruleText(busy(BusyCause.FAST_SIGNALING, BusyState.VERY_BUSY), signals(90.0))
        )
        assertEquals("기어가 R이에요", ruleText(busy(BusyCause.REVERSING), signals(gear = Gear.REVERSE)))
        assertEquals("주행 중에 방향지시등을 켰어요", ruleText(busy(BusyCause.SIGNALING), signals(30.0)))
        assertEquals(
            "주행 중에 스티어링 휠을 90도 이상 돌렸어요",
            ruleText(busy(BusyCause.TURNING), signals(30.0, steering = 120.0))
        )
        assertEquals(
            "차가 전방 충돌 경고를 보냈어요",
            ruleText(busy(BusyCause.FORWARD_COLLISION_WARNING, BusyState.VERY_BUSY), signals(60.0))
        )
        assertEquals(
            "차가 후측방 경고를 보냈어요",
            ruleText(busy(BusyCause.BLIND_SPOT_WARNING, BusyState.VERY_BUSY), signals(50.0))
        )
    }

    @Test
    fun `안내 지점을 지나는 중이면 50m 안인지 5초 안인지 알려 준다`() {
        assertEquals(
            "안내 지점 50m 안으로 들어왔어요",
            ruleText(busy(BusyCause.MANEUVER), signals(30.0, maneuverMeters = 40.0))
        )
        assertEquals(
            "안내 지점까지 5초 안에 도착해요",
            ruleText(busy(BusyCause.MANEUVER), signals(90.0, maneuverMeters = 110.0))
        )
        // 안내 지점을 지나 안내가 끝난 뒤에도 바쁜 상황이 끝나기 전까지는 50m 안으로 들어온 것으로 알린다.
        assertEquals("안내 지점 50m 안으로 들어왔어요", ruleText(busy(BusyCause.MANEUVER), signals(30.0)))
    }

    @Test
    fun `RECOVERING이면 엔진이 RECOVERING을 유지하는 시간을 알려 준다`() {
        val progress =
            DriveProgress(BusyStatus(BusyState.RECOVERING, BusyCause.HARD_BRAKING), 10_000)
        assertEquals("다시 바빠질 수 있어서 10초 동안 주황색을 유지해요", ruleText(progress, signals(40.0)))
    }

    @Test
    fun `바쁘지 않을 때는 왜 바쁘지 않은지 알려 준다`() {
        val free = DriveProgress(BusyStatus(BusyState.FREE), 5_000)
        assertEquals("기어가 P라서 바쁘지 않아요", ruleText(free, signals(gear = Gear.PARK)))
        assertEquals(
            "시속 5km 이하에서는 방향지시등을 켜도 바쁘지 않아요",
            ruleText(free, signals(turn = TurnSignal.LEFT))
        )
        assertEquals(
            "시속 5km 이하에서는 스티어링 휠을 돌려도 바쁘지 않아요",
            ruleText(free, signals(steering = 180.0))
        )
        assertEquals(
            "시속 5km 이하에서는 안내 지점이 가까워도 바쁘지 않아요",
            ruleText(free, signals(maneuverMeters = 30.0))
        )
        assertEquals("바쁜 상황이 아니에요", ruleText(free, signals(maneuverMeters = 120.0)))
        assertEquals("바쁜 상황이 아니에요", ruleText(free, signals(50.0)))
        assertEquals("바쁜 상황이 아니에요", ruleText(null, signals()))
    }

    @Test
    fun `SOON이면 안내 지점까지 15초 안에 도착한다고 알려 준다`() {
        val soon = DriveProgress(BusyStatus(BusyState.SOON, BusyCause.UPCOMING_MANEUVER), 5_000)
        assertEquals("안내 지점까지 15초 안에 도착해요", ruleText(soon, signals(50.0, maneuverMeters = 150.0)))
    }

    @Test
    fun `알림 문장의 숫자는 가상의 차의 기준 값으로 만든다`() {
        assertEquals("시속 15km 이상에서 급제동을 시연할 수 있어요", DriveHint.HARD_BRAKE_TOO_SLOW.text())
        assertEquals("도심 도로라 시속 50km까지 달릴 수 있어요", DriveHint.CITY_SPEED_LIMIT.text())
        assertEquals(
            "차로를 변경하려면 시속 10km 이상으로 달려 보세요",
            DriveHint.LANE_CHANGE_TOO_SLOW.text()
        )
        assertEquals(
            "왼쪽 방향지시등을 교차로 30m 전부터 켜지 않아서\n좌회전하지 않고 직진해요",
            DriveHint.LEFT_TURN_NEEDS_SIGNAL.text()
        )
    }

    @Test
    fun `모든 알림에 문장이 있고 안내 이름은 버튼과 앞유리 표시가 함께 사용한다`() {
        for (hint in DriveHint.entries) assertEquals(true, hint.text().isNotBlank())
        assertEquals("합류", DriveGuide.MERGE.label())
        assertEquals("좌회전", DriveGuide.LEFT_TURN.label())
    }
}
