package studio.oube.appabappa.demo

import kotlin.math.PI
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal

class DriveSimTest {
    private val sim = DriveSim()
    private var now = 0L

    // 0.05초마다 움직인다.
    private fun run(seconds: Double) {
        repeat((seconds * 20).toInt()) {
            now += 50
            sim.step(0.05, now)
        }
    }

    private fun accelerateTo(kmh: Double) {
        sim.pressAccel(true, now)
        while (sim.speedKmh < kmh) run(0.05)
        sim.pressAccel(false, now)
    }

    @Test
    fun `기어 D에서 가속 페달을 누르면 차속이 1초에 초속 2_8m씩 늘고 시속 100km를 넘지 않는다`() {
        sim.pressAccel(true, now)
        run(1.0)
        assertEquals(2.8, sim.speedMps, 0.001)

        run(20.0)
        assertEquals(100.0, sim.speedKmh, 0.001)
    }

    @Test
    fun `기어가 D가 아니면 가속 페달을 눌러도 움직이지 않고 이유를 알려 준다`() {
        sim.chooseGear(Gear.PARK, now)
        sim.pressAccel(true, now)
        run(1.0)
        assertEquals(0.0, sim.speedMps, 0.0)
        assertEquals(DriveHint.SHIFT_TO_DRIVE, sim.hintAt(now))

        sim.pressAccel(false, now)
        sim.chooseGear(Gear.NEUTRAL, now)
        sim.pressAccel(true, now)
        assertEquals(DriveHint.NEUTRAL_GEAR, sim.hintAt(now))

        sim.chooseGear(Gear.REVERSE, now)
        sim.pressAccel(true, now)
        assertEquals(DriveHint.REVERSE_STAYS, sim.hintAt(now))
    }

    @Test
    fun `알림은 2_2초 동안만 표시한다`() {
        sim.chooseGear(Gear.PARK, now)
        sim.pressAccel(true, now)
        run(2.1)
        assertEquals(DriveHint.SHIFT_TO_DRIVE, sim.hintAt(now))

        run(0.2)
        assertNull(sim.hintAt(now))
    }

    @Test
    fun `시속 5km보다 빠르면 기어를 P와 R로 변경할 수 없다`() {
        accelerateTo(30.0)
        sim.chooseGear(Gear.PARK, now)
        assertEquals(Gear.DRIVE, sim.gear)
        assertEquals(DriveHint.STOP_BEFORE_PARK, sim.hintAt(now))

        sim.chooseGear(Gear.REVERSE, now)
        assertEquals(DriveHint.STOP_BEFORE_REVERSE, sim.hintAt(now))
    }

    @Test
    fun `브레이크 페달을 누르면 차속이 1초에 초속 2_5m씩 줄어서 엔진의 급제동 기준에 못 미친다`() {
        accelerateTo(60.0)
        val before = sim.speedMps
        sim.pressBrake(true)
        run(1.0)
        assertEquals(before - 2.5, sim.speedMps, 0.001)
    }

    @Test
    fun `급제동 버튼을 누르면 앞차가 먼저 브레이크등을 켜고 0_3초 뒤부터 차속이 1초에 초속 7m씩 줄어든다`() {
        accelerateTo(60.0)
        val before = sim.speedMps
        sim.hardBrake(now)
        assertTrue(sim.leadBraking(now))

        // 0.3초가 되는 프레임은 그 앞 0.05초 동안 감속한 것으로 계산한다.
        run(0.25)
        assertEquals(before, sim.speedMps, 0.001)
        run(1.0)
        assertEquals(before - 7.0, sim.speedMps, 0.01)
    }

    @Test
    fun `시속 15km보다 느리면 급제동을 시연하지 않는다`() {
        accelerateTo(10.0)
        sim.hardBrake(now)
        assertEquals(DriveHint.HARD_BRAKE_TOO_SLOW, sim.hintAt(now))
        assertFalse(sim.isHardBraking(now + 500))
    }

    @Test
    fun `급제동 버튼을 누르면 엔진이 VERY_BUSY로 판단하고 브레이크 페달을 누르면 VERY_BUSY로 판단하지 않는다`() {
        assertEquals(
            BusyCause.HARD_BRAKING,
            veryBusyCauseAfter(0L) { car, at ->
                car.hardBrake(at)
            }
        )
        assertNull(veryBusyCauseAfter(0L) { car, _ -> car.pressBrake(true) })
    }

    @Test
    fun `서비스의 확인이 0_3초 늦어져도 브레이크 페달을 급제동으로 판단하지 않는다`() {
        assertNull(veryBusyCauseAfter(lateOnceMillis = 300L) { car, _ -> car.pressBrake(true) })
    }

    @Test
    fun `방향지시등은 스티어링 휠을 켠 쪽으로 60도 이상 돌린 뒤 손을 뗀 스티어링 휠이 10도 안으로 돌아오면 꺼진다`() {
        sim.toggleTurn(TurnSignal.LEFT)
        sim.grabWheel()
        sim.turnWheel(-Math.toRadians(90.0))
        run(0.1)
        assertEquals(90.0, sim.steeringDeg, 0.001)
        sim.releaseWheel()
        run(0.5)

        assertEquals(0.0, sim.steeringDeg, 0.0)
        assertEquals(TurnSignal.OFF, sim.turn)
    }

    @Test
    fun `스티어링 휠을 시계 방향으로 돌리면 steeringDeg가 음수(오른쪽)가 된다`() {
        sim.grabWheel()
        sim.turnWheel(Math.toRadians(30.0))
        assertEquals(-30.0, sim.steeringDeg, 0.001)
    }

    @Test
    fun `방향지시등을 켜고 30m를 달리면 그쪽 차로로 변경하고 변경이 끝나면 방향지시등을 끈다`() {
        accelerateTo(50.0)
        sim.toggleTurn(TurnSignal.RIGHT)
        run(1.0)
        assertEquals(0.0, sim.lateralMeters, 0.001)

        run(10.0)
        assertEquals(3.5, sim.lateralMeters, 0.001)
        assertEquals(TurnSignal.OFF, sim.turn)
    }

    @Test
    fun `가장 바깥 차로에서는 방향지시등을 다시 켜도 변경하지 않는다`() {
        accelerateTo(50.0)
        sim.toggleTurn(TurnSignal.RIGHT)
        run(10.0)
        sim.toggleTurn(TurnSignal.RIGHT)
        run(10.0)
        assertEquals(3.5, sim.lateralMeters, 0.001)
        assertEquals(TurnSignal.RIGHT, sim.turn)
    }

    @Test
    fun `차로를 변경하다가 차가 정지하면 옆으로도 움직이지 않는다`() {
        accelerateTo(30.0)
        sim.toggleTurn(TurnSignal.RIGHT)
        while (sim.lateralMeters < 0.1) run(0.05)
        sim.pressBrake(true)
        run(6.0)
        assertEquals(0.0, sim.speedMps, 0.0)
        val stoppedAt = sim.lateralMeters
        assertTrue(stoppedAt < 3.5)

        run(2.0)
        assertEquals(stoppedAt, sim.lateralMeters, 0.0)
    }

    @Test
    fun `좌회전 교차로 앞 실선 구간에서는 차로를 옮기지 않고 이유를 알려 준다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(40.0)
        // 안내 지점 60m 앞에서 방향지시등을 켜면, 30m를 달린 뒤(지점 30m 앞) 실선 구간에 들어와 있다.
        while (sim.guideMeters > 60) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 10) run(0.05)

        assertEquals(0.0, sim.lateralMeters, 0.001)
        assertEquals(DriveHint.SOLID_LINE, sim.hint)
    }

    @Test
    fun `실선 구간 안에서 차선을 넘게 되면 실선 구간 전이라도 차로를 옮기지 않는다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(45.0)
        // 교차로 79.5m 앞에서 켜면 30m 뒤(49.5m 앞)에 변경을 시작하는데, 시속 45km에서는 차선을 넘을 때까지 약
        // 26m(계산값)를 더 달려서 실선 구간(36m 앞부터) 안에서 넘게 된다.
        while (sim.guideMeters > 75) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 10) run(0.05)

        assertEquals(0.0, sim.lateralMeters, 0.001)
        assertEquals(DriveHint.SOLID_LINE, sim.hint)
    }

    @Test
    fun `실선 구간 전에 차선을 넘을 수 있으면 1차로로 변경하고 좌회전할 때까지 방향지시등을 켜 둔다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(45.0)
        while (sim.guideMeters > 130) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 40) run(0.05)

        assertEquals(-3.5, sim.lateralMeters, 0.001)
        assertEquals(TurnSignal.LEFT, sim.turn)
    }

    @Test
    fun `교차로를 지나는 동안에는 차로를 변경하지 않는다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(40.0)
        // 안내 지점 20m 앞에서 켜면 신호 30m를 채웠을 때 교차로 안에 있다.
        while (sim.guideMeters > 20) run(0.05)
        sim.toggleTurn(TurnSignal.RIGHT)
        while (sim.guideMeters > -20) run(0.05)

        assertEquals(0.0, sim.lateralMeters, 0.0)
        assertEquals(DriveHint.SOLID_LINE, sim.hint)
    }

    @Test
    fun `1차로에서 왼쪽 방향지시등을 켜고 있으면 교차로에서 스스로 좌회전한다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(40.0)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.turnRad == null && sim.guide != null) run(0.05)
        // 안내 지점에서 시속 15km가 되도록 미리 감속하고, 좌회전을 시작하면 안내는 끝난다. 감속은 프레임마다 계산해서
        // 안내 지점에 닿는 프레임에는 시속 15km보다 조금 빠르다.
        assertNotNull(sim.turnRad)
        assertEquals(15.0, sim.speedKmh, 1.0)
        assertEquals(TurnSignal.LEFT, sim.turn)
        assertNull(sim.signals().nextManeuver)

        var maxWheel = 0.0
        while (sim.guide != null) {
            run(0.05)
            maxWheel = max(maxWheel, sim.steeringDeg)
        }
        // 좌회전을 마치면 가로지르던 도로의 1차로에 있고, 방향지시등이 꺼지고, 풍경이 90도 돌아가 있다.
        assertEquals(300.0, maxWheel, 0.001)
        assertNull(sim.turnRad)
        assertEquals(-3.5, sim.lateralMeters, 0.0)
        assertEquals(TurnSignal.OFF, sim.turn)
        assertEquals(PI / 2, sim.sceneryRad, 0.01)
    }

    @Test
    fun `1차로에 늦게 들어와도 안내 지점에서는 시속 15km로 좌회전을 시작한다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(49.0)
        // 안내 지점 92m 앞에서 방향지시등을 켜면 62m 앞에서 차로 변경을 시작한다. 시속 49km로 계속 달리면 약 8m
        // 앞에서야 1차로에 닿아서(계산값), 1차로에 닿은 뒤에 감속하면 안내 지점까지 시속 15km로 줄이지 못한다.
        while (sim.guideMeters > 92) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.turnRad == null && sim.guide != null) run(0.05)

        assertNotNull(sim.turnRad)
        assertEquals(15.0, sim.speedKmh, 1.0)
    }

    @Test
    fun `좌회전하는 동안에는 스티어링 휠을 손으로 돌려도 반영하지 않는다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(40.0)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.turnRad == null && sim.guide != null) run(0.05)
        run(0.5)
        val turning = sim.steeringDeg

        sim.grabWheel()
        sim.turnWheel(Math.toRadians(90.0))
        assertEquals(turning, sim.steeringDeg, 0.0)
    }

    @Test
    fun `1차로가 아니면 좌회전하지 않고 직진하고 교차로는 차가 지나갈 때까지 남는다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        accelerateTo(40.0)
        while (sim.guideMeters > 0) run(0.05)
        assertNull(sim.turnRad)
        assertEquals(DriveHint.LEFT_TURN_NEEDS_LANE, sim.hintAt(now))
        assertEquals(DriveGuide.LEFT_TURN, sim.guide)
        assertNull(sim.signals().nextManeuver)

        // 건너편 횡단보도는 안내 지점 23m 뒤에서 끝난다.
        while (sim.guideMeters > -23) run(0.05)
        assertEquals(DriveGuide.LEFT_TURN, sim.guide)
        run(0.5)
        assertNull(sim.guide)
    }

    @Test
    fun `왼쪽 방향지시등을 교차로 30m 전부터 켜지 않으면 직진하고 방향지시등을 끈다`() {
        accelerateTo(40.0)
        sim.toggleTurn(TurnSignal.LEFT)
        run(10.0)
        assertEquals(-3.5, sim.lateralMeters, 0.001)
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        while (sim.guideMeters > 20) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 0) run(0.05)

        assertNull(sim.turnRad)
        assertEquals(DriveHint.LEFT_TURN_NEEDS_SIGNAL, sim.hintAt(now))
        assertEquals(TurnSignal.OFF, sim.turn)
    }

    @Test
    fun `안내 지점까지 남은 거리는 달린 만큼 줄고 지점을 지나면 안내가 사라진다`() {
        sim.chooseGuide(DriveGuide.MERGE, now)
        assertEquals(500.0, sim.signals().nextManeuver?.distanceMeters ?: 0.0, 0.0)
        assertEquals("TURN_DEVIDE_MERGE", sim.signals().nextManeuver?.turnTypeName)

        accelerateTo(100.0)
        assertEquals(500 - sim.travelledMeters, sim.guideMeters, 0.001)
        assertEquals(sim.guideMeters, sim.signals().nextManeuver?.distanceMeters ?: 0.0, 0.0)
        while (sim.guide != null) run(0.05)
        assertNull(sim.signals().nextManeuver)
    }

    @Test
    fun `좌회전 안내는 시속 50km를 넘으면 켜지 않는다`() {
        accelerateTo(60.0)
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        assertNull(sim.guide)
        assertEquals(DriveHint.LEFT_TURN_TOO_FAST, sim.hintAt(now))
    }

    @Test
    fun `좌회전 안내 중에는 시속 50km까지만 빨라진다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        sim.pressAccel(true, now)
        // 시속 50km까지 약 5초가 걸린다(계산값). 제한속도에 닿았을 때 한 번만 알린다.
        run(5.2)
        assertEquals(50.0, sim.speedKmh, 0.001)
        assertEquals(DriveHint.CITY_SPEED_LIMIT, sim.hintAt(now))

        run(3.0)
        assertEquals(50.0, sim.speedKmh, 0.001)
        assertNull(sim.hintAt(now))
    }

    @Test
    fun `좌회전 안내를 켰을 때 1차로가 아니면 한 번 알린다`() {
        accelerateTo(40.0)
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        run(0.05)
        assertEquals(DriveHint.LEFT_TURN_LANE, sim.hintAt(now))
        run(3.0)
        assertNull(sim.hintAt(now))
    }

    @Test
    fun `1차로에 있으면 차로 대신 왼쪽 방향지시등을 켜라고 알린다`() {
        accelerateTo(40.0)
        sim.toggleTurn(TurnSignal.LEFT)
        run(10.0)
        assertEquals(-3.5, sim.lateralMeters, 0.001)
        sim.chooseGuide(DriveGuide.LEFT_TURN, now)
        // 방향지시등을 켜야 하는 자리(교차로 30m 앞)보다 50m 먼저 알린다.
        while (sim.guideMeters > 85) run(0.05)
        assertNull(sim.hintAt(now))
        while (sim.guideMeters > 75) run(0.05)
        assertEquals(DriveHint.LEFT_TURN_SIGNAL, sim.hintAt(now))
    }

    @Test
    fun `합류 안내를 켜면 본선 오른쪽의 진입로에서 시작하고 끄면 3차로에 둔다`() {
        accelerateTo(80.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        assertEquals(7.0, sim.lateralMeters, 0.0)
        assertEquals(DriveGuide.MERGE, sim.guide)

        sim.chooseGuide(null, now)
        assertEquals(3.5, sim.lateralMeters, 0.0)
    }

    @Test
    fun `합류 구간 전에는 실선이라 본선으로 변경하지 않고 구간 안에서는 3차로로 변경한다`() {
        accelerateTo(80.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 200) run(0.05)
        assertEquals(7.0, sim.lateralMeters, 0.0)
        assertEquals(DriveHint.MERGE_TOO_EARLY, sim.hint)

        while (sim.guideMeters > 60) run(0.05)
        assertEquals(3.5, sim.lateralMeters, 0.001)
        assertEquals(TurnSignal.OFF, sim.turn)
        // 왼쪽 방향지시등을 켜 두었으므로 합류하라는 알림은 표시하지 않는다.
        assertEquals(DriveHint.MERGE_TOO_EARLY, sim.hint)
    }

    @Test
    fun `합류 구간까지 신호 거리만큼 남았는데 왼쪽 방향지시등을 켜지 않았으면 한 번 알린다`() {
        accelerateTo(90.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        // 합류 구간 150m와 신호 100m를 더한 250m 앞부터 알린다.
        while (sim.guideMeters > 255) run(0.05)
        assertNull(sim.hintAt(now))
        while (sim.guideMeters > 245) run(0.05)
        assertEquals(DriveHint.MERGE_SIGNAL, sim.hintAt(now))
        assertEquals(7.0, sim.lateralMeters, 0.0)
    }

    @Test
    fun `알림을 받고 왼쪽 방향지시등을 켜면 진입로가 좁아지기 전에 본선으로 변경한다`() {
        accelerateTo(90.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.hintAt(now) != DriveHint.MERGE_SIGNAL) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        // 진입로는 합류 지점 60m 앞부터 좁아진다.
        while (sim.guideMeters > 60) run(0.05)
        assertEquals(3.5, sim.lateralMeters, 0.001)
        assertEquals(DriveHint.MERGE_SIGNAL, sim.hint)
    }

    @Test
    fun `합류 장면은 고속도로라서 시속 80km 이하에서도 신호 100m를 달린 뒤에 본선으로 변경한다`() {
        accelerateTo(60.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.guideMeters > 180) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        // 신호 30m를 넘겼고 합류 구간 안이지만, 100m를 채우기 전이라 아직 변경하지 않는다.
        while (sim.guideMeters > 100) run(0.05)
        assertEquals(7.0, sim.lateralMeters, 0.0)

        while (sim.guideMeters > 10) run(0.05)
        assertEquals(3.5, sim.lateralMeters, 0.001)
    }

    @Test
    fun `진입로가 끝날 때까지 본선으로 변경하지 않으면 합류 지점 전에 본선으로 옮기고 이유를 알린다`() {
        accelerateTo(90.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.guideMeters > 0) run(0.05)

        assertEquals(3.5, sim.lateralMeters, 0.0)
        assertEquals(DriveHint.MERGE_FORCED, sim.hint)
    }

    @Test
    fun `진입로가 끝나 본선으로 옮기는 동안 가속해도 합류 지점 전에 본선에 들어온다`() {
        accelerateTo(10.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.hint != DriveHint.MERGE_FORCED) run(0.05)
        sim.pressAccel(true, now)
        while (sim.guideMeters > 0) run(0.05)

        assertEquals(3.5, sim.lateralMeters, 0.0)
    }

    @Test
    fun `진입로가 끝나 본선으로 옮기는 동안 왼쪽 방향지시등을 켜면 옮긴 뒤에 끈다`() {
        accelerateTo(90.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.hint != DriveHint.MERGE_FORCED) run(0.05)
        sim.toggleTurn(TurnSignal.LEFT)
        while (sim.guideMeters > 0) run(0.05)

        assertEquals(3.5, sim.lateralMeters, 0.0)
        assertEquals(TurnSignal.OFF, sim.turn)
    }

    @Test
    fun `진입로에서 본선으로 옮기는 동안 안내를 끄면 차를 3차로에 둔다`() {
        accelerateTo(90.0)
        sim.chooseGuide(DriveGuide.MERGE, now)
        while (sim.hint != DriveHint.MERGE_FORCED) run(0.05)
        run(0.5)
        assertTrue(sim.lateralMeters > 3.5)

        sim.chooseGuide(null, now)
        assertEquals(3.5, sim.lateralMeters, 0.0)
    }
}

// 시속 60km까지 가속한 뒤 brake를 실행하고, 서비스처럼 0.25초마다 엔진에 신호를 전달해 2.5초 동안 반환된 VERY_BUSY
// 근거를 반환한다. lateOnceMillis를 주면 감속을 시작한 뒤 세 번째 확인을 그만큼 늦춘다(메인 스레드가 잠깐 정지한
// 경우). 늦어진 확인 간격이 엔진이 판단하는 구간의 맨 앞에 오는 때(약 1.8초 뒤)까지 확인한다.
private fun veryBusyCauseAfter(lateOnceMillis: Long, brake: (DriveSim, Long) -> Unit): BusyCause? {
    val car = DriveSim()
    val engine = BusyEngine()
    var clock = 0L
    var nextCheck = 0L
    var checksWhileBraking = -1
    var cause: BusyCause? = null
    fun tick() {
        clock += 10
        car.step(0.01, clock)
        if (clock < nextCheck) return
        val status = engine.update(car.signals(), clock)
        if (status.state == BusyState.VERY_BUSY) cause = status.cause
        if (checksWhileBraking >= 0) checksWhileBraking++
        nextCheck = clock + 250 + if (checksWhileBraking == 2) lateOnceMillis else 0
    }
    car.pressAccel(true, clock)
    while (car.speedKmh < 60) tick()
    car.pressAccel(false, clock)
    repeat(100) { tick() }
    cause = null
    checksWhileBraking = 0
    brake(car, clock)
    repeat(250) { tick() }
    return cause
}
