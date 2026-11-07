package studio.oube.appabappa.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import studio.oube.appabappa.engine.BusyCause.FAST_SIGNALING
import studio.oube.appabappa.engine.BusyCause.HARD_BRAKING
import studio.oube.appabappa.engine.BusyCause.MANEUVER
import studio.oube.appabappa.engine.BusyCause.REVERSING
import studio.oube.appabappa.engine.BusyCause.SIGNALING
import studio.oube.appabappa.engine.BusyCause.TURNING
import studio.oube.appabappa.engine.BusyCause.UPCOMING_MANEUVER
import studio.oube.appabappa.engine.BusyState.BUSY
import studio.oube.appabappa.engine.BusyState.FREE
import studio.oube.appabappa.engine.BusyState.RECOVERING
import studio.oube.appabappa.engine.BusyState.SOON
import studio.oube.appabappa.engine.BusyState.VERY_BUSY

class BusyEngineTest {
    @Test
    fun `고속도로에 합류하면 SOON, VERY_BUSY, RECOVERING을 거쳐 FREE로 돌아온다`() {
        assertEquals(listOf(FREE, SOON, VERY_BUSY, RECOVERING, FREE), mergeOntoHighway().states)
    }

    @Test
    fun `교차로에서 좌회전할 때 신호를 기다리는 동안에는 FREE를 유지하고 출발하면 BUSY가 된다`() {
        assertEquals(listOf(FREE, BUSY, RECOVERING, FREE), turnLeftAtIntersection().states)
    }

    @Test
    fun `안내 지점 앞에서 신호를 기다리는 동안에는 FREE를 유지한다`() {
        val ride = Ride()
        ride.drive(10.0, driving(0.0, TurnSignal.LEFT, nextManeuver = Maneuver("TURN_LEFT", 30.0)))

        assertEquals(listOf(FREE), ride.states)
    }

    @Test
    fun `급제동하면 VERY_BUSY와 RECOVERING을 거쳐 FREE로 돌아온다`() {
        val ride = Ride()
        ride.drive(2.0, driving(50.0))
        ride.drive(1.0) { t -> driving(50.0 - 15.0 * t) }
        ride.drive(12.0, driving(35.0))

        assertEquals(listOf(FREE, VERY_BUSY, RECOVERING, FREE), ride.states)
    }

    @Test
    fun `정체 구간에서 가다 서다를 반복하는 동안에는 FREE를 유지한다`() {
        val ride = Ride()
        repeat(5) {
            ride.drive(2.0) { t -> driving(5.0 * t) }
            ride.drive(2.0) { t -> driving(10.0 - 5.0 * t) }
        }

        assertEquals(listOf(FREE), ride.states)
    }

    @Test
    fun `방향지시등을 1초만 켜도 BUSY와 RECOVERING을 거쳐 FREE로 돌아온다`() {
        val ride = Ride()
        ride.drive(2.0, driving(50.0))
        ride.drive(1.0, driving(50.0, TurnSignal.RIGHT))
        ride.drive(8.0, driving(50.0))

        assertEquals(listOf(FREE, BUSY, RECOVERING, FREE), ride.states)
    }

    @Test
    fun `기어를 R로 변경하면 정차 중이어도 BUSY가 되고 P로 변경하면 바로 FREE가 된다`() {
        val ride = Ride()
        ride.drive(3.0, driving(0.0, gear = Gear.REVERSE))
        ride.drive(1.0, driving(0.0, gear = Gear.PARK))

        assertEquals(listOf(BUSY, FREE), ride.states)
    }

    @Test
    fun `RECOVERING 중에 방향지시등을 다시 켜면 FREE를 거치지 않고 BUSY가 된다`() {
        val ride = Ride()
        ride.drive(2.0, driving(50.0, TurnSignal.RIGHT))
        ride.drive(3.0, driving(50.0))
        ride.drive(2.0, driving(50.0, TurnSignal.RIGHT))
        ride.drive(8.0, driving(50.0))

        assertEquals(listOf(BUSY, RECOVERING, BUSY, RECOVERING, FREE), ride.states)
    }

    @Test
    fun `급제동으로 정지한 순간에는 차속이 0이어도 VERY_BUSY가 된다`() {
        val engine = BusyEngine()
        engine.update(driving(20.0), 0)

        assertEquals(VERY_BUSY, engine.update(driving(0.0), 1_000).state)
    }

    @Test
    fun `방향지시등을 켜지 않아도 스티어링 휠을 90도 이상 돌리면 BUSY가 된다`() {
        val engine = BusyEngine()

        assertEquals(FREE, engine.update(driving(20.0, steeringAngleDeg = 30.0), 0).state)
        assertEquals(BUSY, engine.update(driving(20.0, steeringAngleDeg = -120.0), 250).state)
    }

    @Test
    fun `안내 지점까지 50m 또는 5초 남으면 방향지시등을 켜지 않아도 BUSY가 된다`() {
        val within50Meters = driving(20.0, nextManeuver = Maneuver("TURN_RIGHT", 50.0))
        val within5Seconds = driving(80.0, nextManeuver = Maneuver("TURN_RIGHT", 110.0))

        assertEquals(BUSY, BusyEngine().update(within50Meters, 0).state)
        assertEquals(BUSY, BusyEngine().update(within5Seconds, 0).state)
    }

    @Test
    fun `직진 안내 지점에 가까워져도 FREE를 유지한다`() {
        val ride = Ride()
        ride.drive(10.0) { t ->
            driving(50.0, nextManeuver = Maneuver("TURN_STRAIGHT", 150 - 14 * t))
        }

        assertEquals(listOf(FREE), ride.states)
    }

    @Test
    fun `BUSY가 끝나면 RECOVERING이 5초 동안 유지된다`() {
        val engine = BusyEngine()
        engine.update(driving(50.0, TurnSignal.LEFT), 0)
        engine.update(driving(50.0), 250)

        assertEquals(RECOVERING, engine.update(driving(50.0), 5_000).state)
        assertEquals(FREE, engine.update(driving(50.0), 5_250).state)
    }

    @Test
    fun `VERY_BUSY를 거친 뒤에는 RECOVERING이 10초 동안 유지된다`() {
        val engine = BusyEngine()
        engine.update(driving(70.0, TurnSignal.RIGHT), 0)
        engine.update(driving(70.0), 250)

        assertEquals(RECOVERING, engine.update(driving(70.0), 10_000).state)
        assertEquals(FREE, engine.update(driving(70.0), 10_250).state)
    }

    @Test
    fun `RECOVERING이 끝날 때 다음 안내 지점이 가까우면 SOON이 된다`() {
        val ride = Ride()
        ride.drive(1.0, driving(50.0, TurnSignal.RIGHT))
        ride.drive(6.0) { t ->
            driving(50.0, nextManeuver = Maneuver("TURN_RIGHT", 200 - 14 * t))
        }

        assertEquals(listOf(BUSY, RECOVERING, SOON), ride.states)
    }

    @Test
    fun `경로 안내가 끝나면 SOON에서 바로 FREE로 돌아온다`() {
        val ride = Ride()
        ride.drive(1.0, driving(50.0, nextManeuver = Maneuver("TURN_RIGHT", 100.0)))
        ride.drive(1.0, driving(50.0))

        assertEquals(listOf(SOON, FREE), ride.states)
    }

    @Test
    fun `고속도로에 합류하면 SOON부터 RECOVERING까지 합류 안내를 이유와 함께 반환한다`() {
        val merge = "TURN_DEVIDE_MERGE"

        assertEquals(
            listOf(
                BusyStatus(FREE),
                BusyStatus(SOON, UPCOMING_MANEUVER, merge),
                BusyStatus(VERY_BUSY, FAST_SIGNALING, merge),
                BusyStatus(RECOVERING, FAST_SIGNALING, merge),
                BusyStatus(FREE)
            ),
            mergeOntoHighway().statuses
        )
    }

    @Test
    fun `교차로에서 좌회전하면 BUSY와 RECOVERING 동안 이유로 좌회전 안내를 반환한다`() {
        assertEquals(
            listOf(
                BusyStatus(FREE),
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT"),
                BusyStatus(RECOVERING, MANEUVER, "TURN_LEFT"),
                BusyStatus(FREE)
            ),
            turnLeftAtIntersection().statuses
        )
    }

    @Test
    fun `급제동하면 이유로 HARD_BRAKING을 반환한다`() {
        val engine = BusyEngine()
        engine.update(driving(50.0), 0)

        assertEquals(BusyStatus(VERY_BUSY, HARD_BRAKING), engine.update(driving(30.0), 1_000))
    }

    @Test
    fun `기어가 R이면 이유로 REVERSING을 반환한다`() {
        assertEquals(
            BusyStatus(BUSY, REVERSING),
            BusyEngine().update(driving(0.0, gear = Gear.REVERSE), 0)
        )
    }

    @Test
    fun `방향지시등만 켜면 이유로 SIGNALING을 반환한다`() {
        assertEquals(
            BusyStatus(BUSY, SIGNALING),
            BusyEngine().update(driving(50.0, TurnSignal.RIGHT), 0)
        )
    }

    @Test
    fun `스티어링 휠만 크게 돌리면 이유로 TURNING을 반환한다`() {
        assertEquals(
            BusyStatus(BUSY, TURNING),
            BusyEngine().update(driving(20.0, steeringAngleDeg = 120.0), 0)
        )
    }

    @Test
    fun `방향지시등을 켜고 스티어링 휠을 크게 돌리면 이유로 SIGNALING을 고른다`() {
        val turning = driving(20.0, TurnSignal.LEFT, steeringAngleDeg = 120.0)

        assertEquals(BusyStatus(BUSY, SIGNALING), BusyEngine().update(turning, 0))
    }

    @Test
    fun `안내 지점을 지나는 중이면 방향지시등이 켜져 있어도 이유로 MANEUVER를 고른다`() {
        val passing =
            driving(20.0, TurnSignal.RIGHT, nextManeuver = Maneuver("TURN_RIGHT", 30.0))

        assertEquals(BusyStatus(BUSY, MANEUVER, "TURN_RIGHT"), BusyEngine().update(passing, 0))
    }

    @Test
    fun `SOON에서는 이유로 다가오는 안내를 반환한다`() {
        val approaching = driving(50.0, nextManeuver = Maneuver("TURN_RIGHT", 100.0))

        assertEquals(
            BusyStatus(SOON, UPCOMING_MANEUVER, "TURN_RIGHT"),
            BusyEngine().update(approaching, 0)
        )
    }

    // 90 km/h로 달리다 합류 안내 300m 앞에서 오른쪽 방향지시등을 켜고, 합류 지점을 지난 뒤 끈다.
    private fun mergeOntoHighway() = Ride().apply {
        val merge = { meters: Double -> Maneuver("TURN_DEVIDE_MERGE", meters) }
        val nextExit = Maneuver("TURN_EXIT_IC_R", 5_000.0)
        drive(4.0) { t -> driving(90.0, nextManeuver = merge(400 - 25 * t)) }
        drive(12.0) { t ->
            driving(90.0, TurnSignal.RIGHT, nextManeuver = merge(300 - 25 * t))
        }
        drive(2.0, driving(90.0, TurnSignal.RIGHT, nextManeuver = nextExit))
        drive(12.0, driving(90.0, nextManeuver = nextExit))
    }

    // 좌회전 안내 15m 앞에서 신호를 기다리다 출발해 스티어링 휠을 180도 돌리고, 회전이 끝나면 30 km/h로 달린다.
    private fun turnLeftAtIntersection() = Ride().apply {
        val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
        drive(5.0, driving(0.0, TurnSignal.LEFT, nextManeuver = left(15.0)))
        drive(4.0) { t ->
            driving(
                15.0,
                TurnSignal.LEFT,
                steeringAngleDeg = 180.0,
                nextManeuver = left(15 - 4 * t)
            )
        }
        drive(8.0, driving(30.0, nextManeuver = Maneuver("TURN_RIGHT", 800.0)))
    }
}
