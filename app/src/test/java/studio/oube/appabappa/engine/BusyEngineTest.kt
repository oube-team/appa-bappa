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
                BusyStatus(VERY_BUSY, FAST_SIGNALING, merge, approaching = true),
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
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT", turned = true),
                BusyStatus(RECOVERING, MANEUVER, "TURN_LEFT", turned = true),
                BusyStatus(FREE)
            ),
            turnLeftAtIntersection().statuses
        )
    }

    @Test
    fun `바쁜 상황 동안 스티어링 휠을 90도 이상 돌린 적이 있으면 RECOVERING까지 turned를 유지한다`() {
        val turned = turnLeftAtIntersection().statuses.filter { it.state != FREE }
        assertEquals(listOf(true, true), turned.map { it.turned })

        // 좌회전 안내 지점을 스티어링 휠을 돌리지 않고 지나간다.
        val straight = Ride().apply {
            drive(4.0) { t -> driving(30.0, nextManeuver = Maneuver("TURN_LEFT", 30 - 8 * t)) }
            drive(8.0, driving(30.0))
        }
        val passed = straight.statuses.filter { it.state == BUSY || it.state == RECOVERING }
        assertEquals(listOf(BUSY, RECOVERING), passed.map { it.state })
        assertEquals(listOf(false, false), passed.map { it.turned })
        assertEquals(listOf("TURN_LEFT", "TURN_LEFT"), passed.map { it.turnTypeName })
    }

    @Test
    fun `RECOVERING 중에 안내 없이 다시 바빠지면 지나온 안내와 turned를 이어받지 않는다`() {
        // 합류 지점을 지나 방향지시등을 끈 뒤, 3초 뒤에 차로를 변경하려고 오른쪽 방향지시등을 켠다.
        val merged = Ride().apply {
            val merge = { meters: Double -> Maneuver("TURN_DEVIDE_MERGE", meters) }
            drive(12.0) { t -> driving(90.0, TurnSignal.LEFT, nextManeuver = merge(300 - 25 * t)) }
            drive(3.0, driving(90.0))
            drive(2.0, driving(90.0, TurnSignal.RIGHT))
        }
        assertEquals(BusyStatus(VERY_BUSY, FAST_SIGNALING), merged.statuses.last())

        // 좌회전을 마친 뒤, 2초 뒤에 오른쪽 방향지시등을 켠다.
        val turned = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(4.0) { t ->
                driving(
                    15.0,
                    TurnSignal.LEFT,
                    steeringAngleDeg = 180.0,
                    nextManeuver = left(15 - 4 * t)
                )
            }
            drive(2.0, driving(30.0))
            drive(2.0, driving(30.0, TurnSignal.RIGHT))
        }
        assertEquals(BusyStatus(BUSY, SIGNALING), turned.statuses.last())
    }

    @Test
    fun `안내 지점 앞에 방향지시등을 켠 채 정차하면 끝났다고 알리지 않고 다시 출발하면 같은 안내로 알린다`() {
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t -> driving(30.0, TurnSignal.LEFT, nextManeuver = left(45 - 8 * t)) }
            // 급제동으로 판단하지 않도록 4초에 걸쳐 정지한다.
            drive(4.0) { t ->
                driving(30 - 7.5 * t, TurnSignal.LEFT, nextManeuver = left(29 - 4.2 * t))
            }
            drive(2.0, driving(0.0, TurnSignal.LEFT, nextManeuver = left(12.0)))
            drive(2.0) { t ->
                driving(
                    20.0,
                    TurnSignal.LEFT,
                    steeringAngleDeg = 180.0,
                    nextManeuver = left(12 - 5.5 * t)
                )
            }
        }

        assertEquals(
            listOf(
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT"),
                BusyStatus(RECOVERING),
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT", turned = true)
            ),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점을 지나 정차했다가 회전을 마치면 지나온 안내로 회전했다고 알린다`() {
        // 교차로에 들어가 마주 오는 차를 기다렸다가 좌회전하는 경우다.
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t -> driving(20.0, TurnSignal.LEFT, nextManeuver = left(12 - 5.5 * t)) }
            drive(2.0) { t -> driving(20 - 10 * t, TurnSignal.LEFT) }
            drive(3.0, driving(0.0, TurnSignal.LEFT))
            drive(2.0, driving(15.0, TurnSignal.LEFT, steeringAngleDeg = 200.0))
            drive(1.0, driving(30.0))
        }

        assertEquals(
            listOf(
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT"),
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT"),
                BusyStatus(RECOVERING),
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT", turned = true),
                BusyStatus(RECOVERING, SIGNALING, "TURN_LEFT", turned = true)
            ),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점을 지나 RECOVERING이 끝날 때까지 정차했다가 회전을 마쳐도 지나온 안내로 회전했다고 알린다`() {
        // 좌회전하다가 건너편 횡단보도 앞에서 보행자를 기다리는 경우다.
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t -> driving(20.0, TurnSignal.LEFT, nextManeuver = left(12 - 5.5 * t)) }
            drive(2.0) { t -> driving(20 - 10 * t, TurnSignal.LEFT, steeringAngleDeg = 200.0) }
            drive(8.0, driving(0.0, TurnSignal.LEFT, steeringAngleDeg = 200.0))
            drive(2.0, driving(15.0, TurnSignal.LEFT, steeringAngleDeg = 200.0))
            drive(1.0, driving(30.0))
        }

        assertEquals(
            listOf(
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT"),
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT", turned = true),
                BusyStatus(RECOVERING),
                BusyStatus(FREE),
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT", turned = true),
                BusyStatus(RECOVERING, SIGNALING, "TURN_LEFT", turned = true)
            ),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점을 지나 정차한 뒤 50m 넘게 달리면 지나온 안내로 알리지 않는다`() {
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t -> driving(20.0, TurnSignal.LEFT, nextManeuver = left(12 - 5.5 * t)) }
            drive(2.0) { t -> driving(20 - 10 * t, TurnSignal.LEFT) }
            drive(8.0, driving(0.0))
            drive(7.0, driving(30.0))
            drive(1.0, driving(30.0, TurnSignal.RIGHT))
        }

        assertEquals(BusyStatus(BUSY, SIGNALING), ride.statuses.last())
    }

    @Test
    fun `안내 없이 방향지시등을 켠 채 정차하면 방향지시등을 껐다고 알리지 않는다`() {
        val ride = Ride()
        ride.drive(2.0, driving(30.0, TurnSignal.RIGHT))
        ride.drive(4.0) { t -> driving(30 - 7.5 * t, TurnSignal.RIGHT) }
        ride.drive(1.0, driving(0.0, TurnSignal.RIGHT))

        assertEquals(listOf(BusyStatus(BUSY, SIGNALING), BusyStatus(RECOVERING)), ride.statuses)
    }

    @Test
    fun `안내 지점을 지나기 전에 안내가 사라지면 지났다고 알리지 않는다`() {
        val ride = Ride()
        ride.drive(1.0) { t -> driving(30.0, nextManeuver = Maneuver("TURN_LEFT", 45 - 8 * t)) }
        ride.drive(2.0, driving(30.0))

        assertEquals(
            listOf(BusyStatus(BUSY, MANEUVER, "TURN_LEFT"), BusyStatus(RECOVERING)),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점을 지난 뒤 50m 넘게 달리면 방향지시등을 켠 채여도 지나온 안내로 알리지 않는다`() {
        val ride = Ride().apply {
            val merge = { meters: Double -> Maneuver("TURN_DEVIDE_MERGE", meters) }
            drive(12.0) { t -> driving(90.0, TurnSignal.RIGHT, nextManeuver = merge(300 - 25 * t)) }
            drive(4.0, driving(90.0, TurnSignal.RIGHT))
        }

        assertEquals(
            listOf(
                BusyStatus(VERY_BUSY, FAST_SIGNALING, "TURN_DEVIDE_MERGE", approaching = true),
                BusyStatus(VERY_BUSY, FAST_SIGNALING, "TURN_DEVIDE_MERGE"),
                BusyStatus(VERY_BUSY, FAST_SIGNALING)
            ),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점을 지나 50m 넘게 달린 판단에서 바쁜 상황이 끝나도 지나온 안내로 끝났다고 알린다`() {
        // 합류 지점을 지나 50m쯤 달린 뒤 방향지시등을 끄는 경우다. 방향지시등을 끄는 판단에서 지나온 거리가 50m를 넘어
        // 기억이 초기화돼도, 방금 끝난 바쁜 상황은 합류로 알린다.
        val engine = BusyEngine()
        val exit = Maneuver("TURN_EXIT_IC_R", 5_000.0)
        val signaling = driving(90.0, TurnSignal.LEFT, nextManeuver = exit)
        engine.update(
            driving(90.0, TurnSignal.LEFT, nextManeuver = Maneuver("TURN_DEVIDE_MERGE", 10.0)),
            0
        )
        engine.update(signaling, 250)
        engine.update(signaling, 1_250)
        engine.update(signaling, 2_250)

        assertEquals(
            BusyStatus(RECOVERING, FAST_SIGNALING, "TURN_DEVIDE_MERGE"),
            engine.update(driving(90.0, nextManeuver = exit), 2_300)
        )
    }

    @Test
    fun `회전을 마치는 판단에서 다음 안내가 가까워져도 지나온 안내로 회전했다고 알린다`() {
        val engine = BusyEngine()
        engine.update(
            driving(
                20.0,
                TurnSignal.LEFT,
                steeringAngleDeg = 180.0,
                nextManeuver = Maneuver("TURN_LEFT", 5.0)
            ),
            0
        )
        engine.update(driving(20.0, TurnSignal.LEFT, steeringAngleDeg = 180.0), 250)

        assertEquals(
            BusyStatus(RECOVERING, SIGNALING, "TURN_LEFT", turned = true),
            engine.update(driving(30.0, nextManeuver = Maneuver("TURN_RIGHT", 100.0)), 500)
        )
    }

    @Test
    fun `안내 지점을 지나 회전하는 도중에 주차하면 다시 출발해 켠 방향지시등은 지나온 안내로 알리지 않는다`() {
        // 좌회전해서 주차장에 들어가자마자 스티어링 휠을 돌린 채 정차해 기어를 P로 변경하고, 다시 출발하는 경우다.
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t ->
                driving(
                    20.0,
                    TurnSignal.LEFT,
                    steeringAngleDeg = 180.0,
                    nextManeuver = left(10 - 5.5 * t)
                )
            }
            drive(2.0) { t -> driving(20 - 10 * t, TurnSignal.LEFT, steeringAngleDeg = 180.0) }
            drive(
                1.0,
                driving(0.0, TurnSignal.LEFT, steeringAngleDeg = 180.0, gear = Gear.PARK)
            )
            drive(1.0, driving(20.0, TurnSignal.RIGHT))
        }

        assertEquals(BusyStatus(BUSY, SIGNALING), ride.statuses.last())
    }

    @Test
    fun `바쁜 상황이 끝나면 turned를 초기화한다`() {
        val ride = turnLeftAtIntersection()
        ride.drive(2.0, driving(50.0, TurnSignal.RIGHT))

        assertEquals(false, ride.statuses.last().turned)
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
            BusyStatus(BUSY, TURNING, turned = true),
            BusyEngine().update(driving(20.0, steeringAngleDeg = 120.0), 0)
        )
    }

    @Test
    fun `방향지시등을 켜고 스티어링 휠을 크게 돌리면 이유로 SIGNALING을 고른다`() {
        val turning = driving(20.0, TurnSignal.LEFT, steeringAngleDeg = 120.0)

        assertEquals(BusyStatus(BUSY, SIGNALING, turned = true), BusyEngine().update(turning, 0))
    }

    @Test
    fun `안내 지점을 지나는 중이면 방향지시등이 켜져 있어도 이유로 MANEUVER를 고른다`() {
        val passing =
            driving(20.0, TurnSignal.RIGHT, nextManeuver = Maneuver("TURN_RIGHT", 30.0))

        assertEquals(BusyStatus(BUSY, MANEUVER, "TURN_RIGHT"), BusyEngine().update(passing, 0))
    }

    @Test
    fun `안내 지점에 닿기 전에 방향지시등을 켜면 approaching이 true이고, 5초 안으로 남으면 false가 된다`() {
        val engine = BusyEngine()
        val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
        // 시속 50km에서 5초 동안 달리는 거리는 약 69m다.
        val ahead = driving(50.0, TurnSignal.LEFT, nextManeuver = left(200.0))
        val within5Seconds = driving(50.0, TurnSignal.LEFT, nextManeuver = left(65.0))

        assertEquals(
            BusyStatus(BUSY, SIGNALING, "TURN_LEFT", approaching = true),
            engine.update(ahead, 0)
        )
        assertEquals(BusyStatus(BUSY, MANEUVER, "TURN_LEFT"), engine.update(within5Seconds, 250))
    }

    @Test
    fun `시속 60km 이상에서 방향지시등을 켜도 안내 지점까지 5초 안으로 남으면 approaching이 false다`() {
        val merge = { meters: Double -> Maneuver("TURN_DEVIDE_MERGE", meters) }
        // 시속 90km에서 5초 동안 달리는 거리는 125m다.
        val ahead = driving(90.0, TurnSignal.LEFT, nextManeuver = merge(300.0))
        val within5Seconds = driving(90.0, TurnSignal.LEFT, nextManeuver = merge(100.0))

        assertEquals(
            BusyStatus(VERY_BUSY, FAST_SIGNALING, "TURN_DEVIDE_MERGE", approaching = true),
            BusyEngine().update(ahead, 0)
        )
        assertEquals(
            BusyStatus(VERY_BUSY, FAST_SIGNALING, "TURN_DEVIDE_MERGE"),
            BusyEngine().update(within5Seconds, 0)
        )
    }

    @Test
    fun `안내 지점에 닿기 전에 스티어링 휠을 크게 돌려도 approaching이 true다`() {
        val turning =
            driving(20.0, steeringAngleDeg = 120.0, nextManeuver = Maneuver("TURN_LEFT", 80.0))

        assertEquals(
            BusyStatus(BUSY, TURNING, "TURN_LEFT", turned = true, approaching = true),
            BusyEngine().update(turning, 0)
        )
    }

    @Test
    fun `안내 지점을 지난 뒤에도 방향지시등을 켜 두면 approaching이 false다`() {
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(4.0) { t -> driving(30.0, TurnSignal.LEFT, nextManeuver = left(30 - 8 * t)) }
            drive(1.0, driving(30.0, TurnSignal.LEFT))
        }

        assertEquals(
            listOf(
                BusyStatus(BUSY, MANEUVER, "TURN_LEFT"),
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT")
            ),
            ride.statuses
        )
    }

    @Test
    fun `안내 지점에 닿기 전에 방향지시등을 끄면 끝났다고 알리지 않는다`() {
        // 좌회전 지점 200m 앞에서 차로를 변경하려고 방향지시등을 켰다가 끈 경우다.
        val ride = Ride().apply {
            val left = { meters: Double -> Maneuver("TURN_LEFT", meters) }
            drive(2.0) { t -> driving(50.0, TurnSignal.LEFT, nextManeuver = left(200 - 14 * t)) }
            drive(2.0) { t -> driving(50.0, nextManeuver = left(172 - 14 * t)) }
        }

        assertEquals(
            listOf(
                BusyStatus(BUSY, SIGNALING, "TURN_LEFT", approaching = true),
                BusyStatus(RECOVERING)
            ),
            ride.statuses
        )
    }

    @Test
    fun `SOON에서는 이유로 다가오는 안내를 반환한다`() {
        val approaching = driving(50.0, nextManeuver = Maneuver("TURN_RIGHT", 100.0))

        assertEquals(
            BusyStatus(SOON, UPCOMING_MANEUVER, "TURN_RIGHT"),
            BusyEngine().update(approaching, 0)
        )
    }

    // 90 km/h로 달리다 합류 안내 300m 앞에서 왼쪽 방향지시등을 켜고(오른쪽 진입로에서 왼쪽 본선으로 들어간다), 합류
    // 지점을 지난 뒤 끈다.
    private fun mergeOntoHighway() = Ride().apply {
        val merge = { meters: Double -> Maneuver("TURN_DEVIDE_MERGE", meters) }
        val nextExit = Maneuver("TURN_EXIT_IC_R", 5_000.0)
        drive(4.0) { t -> driving(90.0, nextManeuver = merge(400 - 25 * t)) }
        drive(12.0) { t ->
            driving(90.0, TurnSignal.LEFT, nextManeuver = merge(300 - 25 * t))
        }
        drive(2.0, driving(90.0, TurnSignal.LEFT, nextManeuver = nextExit))
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
