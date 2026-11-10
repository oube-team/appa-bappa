package studio.oube.appabappa.demo

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.MoodTracker
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.ui.statusCopy

private const val STEP_MILLIS = 10L
private const val STEP_SECONDS = STEP_MILLIS / 1_000.0
private const val MAX_STEPS = 30_000

// 직접 조작 시연을 조작 순서대로 진행하면서, 동승자 화면에 표시되는 바쁜 이유가 어떻게 바뀌는지 확인한다. 가상의 차는
// 0.01초마다 움직이고, 엔진은 서비스처럼 checkMillis마다 가상의 차의 신호로 판단한다. 서비스는 0.25초마다 판단하지만
// 실제 기기에서는 간격이 조금씩 길어지므로, 같은 조작 순서를 0.25초 간격과 그보다 긴 간격으로 확인한다. reasons에는
// 이유가 바뀔 때마다 그 문장을 기록하고, 이유가 없으면 null을 기록한다.
@RunWith(Parameterized::class)
class DriveStoryTest(private val checkMillis: Long) {
    private val sim = DriveSim()
    private val engine = BusyEngine()
    private val moods = MoodTracker()
    private val reasons = mutableListOf<String?>()
    private var mood = Mood.FREE
    private var clock = 0L
    private var nextCheck = 0L

    @Test
    fun `합류한 뒤에 켠 방향지시등은 합류로 알리지 않는다`() {
        sim.chooseGuide(DriveGuide.MERGE, clock)
        speedUpTo(90.0)
        until { sim.guideMeters <= 300 }
        sim.toggleTurn(TurnSignal.LEFT)
        until { sim.guide == null }
        run(3.0)
        sim.toggleTurn(TurnSignal.RIGHT)
        run(5.0)

        assertEquals(
            listOf(
                null,
                "합류나 분기 지점이 가까워요",
                "합류나 분기 지점을 지나고 있어요",
                "방금 합류나 분기 지점을 지났어요",
                "방향지시등을 켜고 있어요"
            ),
            reasons
        )
    }

    @Test
    fun `좌회전 지점에 닿기 전에 방향지시등을 켜면 지점이 가깝다고 알리다가 50m 안에서 좌회전하고 있다고 알린다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, clock)
        speedUpTo(50.0)
        until { sim.guideMeters <= 150 }
        sim.toggleTurn(TurnSignal.LEFT)
        run(1.0)

        assertEquals(Mood.BUSY, mood)
        assertEquals("좌회전할 지점이 가까워요", reasons.last())

        // 엔진은 안내 지점까지 5초 또는 50m 남으면 지점을 지나는 중으로 판단한다.
        until { sim.guideMeters <= 50 }
        run(0.5)

        assertEquals(Mood.BUSY, mood)
        assertEquals("좌회전하고 있어요", reasons.last())
    }

    @Test
    fun `합류 지점에 닿기 전에 방향지시등을 켜면 지점이 가깝다고 알리다가 50m 안에서 지점을 지나고 있다고 알린다`() {
        sim.chooseGuide(DriveGuide.MERGE, clock)
        speedUpTo(90.0)
        until { sim.guideMeters <= 300 }
        sim.toggleTurn(TurnSignal.LEFT)
        run(1.0)

        assertEquals(Mood.BUSY, mood)
        assertEquals("합류나 분기 지점이 가까워요", reasons.last())

        until { sim.guideMeters <= 50 }
        run(0.5)

        assertEquals(Mood.BUSY, mood)
        assertEquals("합류나 분기 지점을 지나고 있어요", reasons.last())
    }

    @Test
    fun `합류 지점을 지난 뒤에도 방향지시등을 켜 두면 방향지시등으로 알린다`() {
        sim.chooseGuide(DriveGuide.MERGE, clock)
        speedUpTo(90.0)
        until { sim.guideMeters <= 300 }
        sim.toggleTurn(TurnSignal.RIGHT)
        until { sim.guide == null }
        run(3.0)

        assertEquals(
            listOf(
                null,
                "합류나 분기 지점이 가까워요",
                "합류나 분기 지점을 지나고 있어요",
                "방향지시등을 켜고 있어요"
            ),
            reasons
        )
    }

    @Test
    fun `좌회전하지 않고 지나간 뒤에 켠 방향지시등은 좌회전으로 알리지 않는다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, clock)
        speedUpTo(50.0)
        until { sim.guide == null }
        sim.toggleTurn(TurnSignal.LEFT)
        run(3.0)

        assertEquals(
            listOf(
                null,
                "좌회전할 지점이 가까워요",
                "좌회전하고 있어요",
                "방금 좌회전할 지점을 지났어요",
                "방향지시등을 켜고 있어요"
            ),
            reasons
        )
    }

    @Test
    fun `좌회전 안내 지점 앞에 정차했다가 출발하면 지났다고 알리지 않고 좌회전으로 알린다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, clock)
        speedUpTo(40.0)
        until { sim.guideMeters <= 150 }
        sim.toggleTurn(TurnSignal.LEFT)
        until { sim.guideMeters <= 45 }
        stop()
        run(8.0)
        driveUntilSceneEnds()

        assertEquals(stoppedThenTurnedLeft, reasons)
    }

    @Test
    fun `좌회전하는 도중에 정차했다가 출발해도 좌회전으로 알린다`() {
        sim.chooseGuide(DriveGuide.LEFT_TURN, clock)
        speedUpTo(50.0)
        until { sim.guideMeters <= 150 }
        sim.toggleTurn(TurnSignal.LEFT)
        until { sim.turnRad != null }
        run(1.0)
        stop()
        run(7.0)
        driveUntilSceneEnds()

        assertEquals(stoppedThenTurnedLeft, reasons)
    }

    @Test
    fun `안내 지점에 닿기 전에 안내를 끄면 지났다고 알리지 않는다`() {
        sim.chooseGuide(DriveGuide.MERGE, clock)
        speedUpTo(80.0)
        until { sim.guideMeters <= 40 }
        sim.chooseGuide(null, clock)
        run(8.0)

        assertEquals(
            listOf(null, "합류나 분기 지점이 가까워요", "합류나 분기 지점을 지나고 있어요", null),
            reasons
        )
    }

    // 정차한 동안에는 이유를 표시하지 않고(null), 다시 출발하면 같은 안내로 알린다.
    private val stoppedThenTurnedLeft =
        listOf(
            null,
            "좌회전할 지점이 가까워요",
            "좌회전하고 있어요",
            null,
            "좌회전하고 있어요",
            "방금 좌회전했어요"
        )

    private fun speedUpTo(kmh: Double) {
        sim.pressAccel(true, clock)
        until { sim.speedKmh >= kmh }
        sim.pressAccel(false, clock)
    }

    private fun stop() {
        sim.pressBrake(true)
        until { sim.speedMps == 0.0 }
        sim.pressBrake(false)
    }

    private fun driveUntilSceneEnds() {
        sim.pressAccel(true, clock)
        until { sim.guide == null }
        sim.pressAccel(false, clock)
        run(1.0)
    }

    private fun run(seconds: Double) = repeat((seconds / STEP_SECONDS).roundToInt()) { step() }

    // 조작 순서를 잘못 작성해서 조건이 참이 되지 않으면 테스트가 끝나지 않으므로, 5분까지만 진행한다.
    private fun until(condition: () -> Boolean) {
        var steps = 0
        while (!condition()) {
            check(steps++ < MAX_STEPS) { "5분 동안 조건이 참이 되지 않았다" }
            step()
        }
    }

    private fun step() {
        clock += STEP_MILLIS
        sim.step(STEP_SECONDS, clock)
        if (clock < nextCheck) return
        nextCheck = clock + checkMillis
        val status = engine.update(sim.signals(), clock)
        mood = moods.update(status.state, clock)
        val approaching = Reason.isApproaching(status)
        val copy = statusCopy(mood, Reason.of(status), approaching, "아빠", lightEnabled = true)
        val reason = copy.reason
        if (reasons.isEmpty() || reasons.last() != reason) reasons += reason
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "판단 간격 {0}ms")
        fun checkIntervals(): List<Long> = listOf(250L, 263L)
    }
}
