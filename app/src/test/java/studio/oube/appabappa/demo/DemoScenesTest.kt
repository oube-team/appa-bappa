package studio.oube.appabappa.demo

import org.junit.Assert.assertEquals
import org.junit.Test
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyState.BUSY
import studio.oube.appabappa.engine.BusyState.FREE
import studio.oube.appabappa.engine.BusyState.RECOVERING
import studio.oube.appabappa.engine.BusyState.SOON
import studio.oube.appabappa.engine.BusyState.VERY_BUSY
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.status.MoodTracker
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.ui.statusCopy

private const val TICK_MILLIS = 250L

class DemoScenesTest {
    @Test
    fun `고속도로 합류 장면은 FREE, SOON, VERY_BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, SOON, VERY_BUSY, RECOVERING, FREE), statesOf("highway_merge"))
    }

    @Test
    fun `고속도로 합류 장면은 합류 구간에서 스티어링 휠을 왼쪽으로 돌렸다가 오른쪽으로 되돌려 본선으로 옮긴다`() {
        val scene = requireNotNull(DemoScenes.find("highway_merge"))
        // 방향지시등은 9초에 켜고, 합류 지점 150m 앞에는 15초에 닿는다.
        assertEquals(0.0, scene.signalsAt(14.9)?.steeringAngleDeg)
        assertEquals(4.0, scene.signalsAt(16.0)?.steeringAngleDeg)
        assertEquals(-4.0, scene.signalsAt(18.0)?.steeringAngleDeg)
        assertEquals(0.0, scene.signalsAt(19.0)?.steeringAngleDeg)
        assertEquals(150.0, scene.signalsAt(15.0)?.nextManeuver?.distanceMeters)
    }

    @Test
    fun `교차로 좌회전 장면은 신호를 기다리는 동안 FREE를 유지하고 출발하면 BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, BUSY, RECOVERING, FREE), statesOf("left_turn"))
    }

    @Test
    fun `신호 대기 장면은 FREE를 유지한다`() {
        assertEquals(listOf(FREE), statesOf("red_light"))
    }

    @Test
    fun `급제동 장면은 FREE, VERY_BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, VERY_BUSY, RECOVERING, FREE), statesOf("hard_brake"))
    }

    @Test
    fun `전방 충돌 경고 장면은 FREE, VERY_BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, VERY_BUSY, RECOVERING, FREE), statesOf("forward_collision"))
    }

    @Test
    fun `후측방 경고 장면은 방향지시등으로 BUSY가 된 뒤 VERY_BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(BUSY, VERY_BUSY, RECOVERING, FREE), statesOf("blind_spot"))
    }

    @Test
    fun `정체 장면은 FREE를 유지한다`() {
        assertEquals(listOf(FREE), statesOf("stop_and_go"))
    }

    @Test
    fun `짧은 방향지시등 장면은 FREE, BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, BUSY, RECOVERING, FREE), statesOf("short_signal"))
    }

    @Test
    fun `주차 장면은 R에서 BUSY가 되고 P에서 바로 FREE가 된다`() {
        assertEquals(listOf(BUSY, FREE), statesOf("parking"))
    }

    @Test
    fun `이어지는 바쁜 상황 장면은 RECOVERING 중에 FREE를 거치지 않고 다시 BUSY가 된다`() {
        assertEquals(
            listOf(BUSY, RECOVERING, BUSY, RECOVERING, FREE),
            statesOf("back_to_back")
        )
    }

    @Test
    fun `고속도로 합류 장면은 지점이 가깝다고, 지나고 있다고, 방금 지났다고 차례로 알린다`() {
        assertEquals(
            listOf(
                null,
                "합류나 분기 지점이 가까워요",
                "합류나 분기 지점을 지나고 있어요",
                "방금 합류나 분기 지점을 지났어요",
                null
            ),
            reasonsOf("highway_merge", TICK_MILLIS)
        )
    }

    @Test
    fun `교차로 좌회전 장면은 좌회전하고 있다고, 방금 좌회전했다고 차례로 알린다`() {
        assertEquals(
            listOf(null, "좌회전하고 있어요", "방금 좌회전했어요", null),
            reasonsOf("left_turn", TICK_MILLIS)
        )
    }

    // 서비스는 0.25초마다 판단하지만 실제 기기에서는 간격이 조금씩 길어진다. 고속도로 합류 장면은 합류 지점을 지나 정확히
    // 50m(엔진이 지나온 안내로 알리는 거리) 달린 때에 방향지시등을 끄므로, 간격이 길어지면 그 판단에서 50m를 넘는다.
    @Test
    fun `판단 간격이 조금 길어져도 장면마다 알리는 이유는 같다`() {
        for (scene in DemoScenes.all) {
            val expected = reasonsOf(scene.id, TICK_MILLIS)
            for (tickMillis in listOf(251L, 257L, 263L, 290L)) {
                val reasons = reasonsOf(scene.id, tickMillis)
                assertEquals("${scene.id} ${tickMillis}ms", expected, reasons)
            }
        }
    }

    @Test
    fun `장면 이름은 서로 중복되지 않는다`() {
        val ids = DemoScenes.all.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    // BusyService와 같은 0.25초 간격으로 장면을 끝까지 재생하며, 상태가 바뀔 때만 기록한다.
    private fun statesOf(id: String): List<BusyState> {
        val run = DemoRun(requireNotNull(DemoScenes.find(id)), startMillis = 0)
        val states =
            generateSequence(0L) { it + TICK_MILLIS }
                .map { run.update(Gear.PARK, it)?.state }
                .takeWhile { it != null }
                .filterNotNull()
                .toList()
        return states.filterIndexed { i, state -> i == 0 || states[i - 1] != state }
    }

    // 장면을 tickMillis 간격으로 끝까지 재생하며, 동승자 화면에 표시되는 바쁜 이유가 바뀔 때만 그 문장을 기록한다.
    // 이유가 없으면 null을 기록한다.
    private fun reasonsOf(id: String, tickMillis: Long): List<String?> {
        val run = DemoRun(requireNotNull(DemoScenes.find(id)), startMillis = 0)
        val moods = MoodTracker()
        val reasons =
            generateSequence(0L) { it + tickMillis }
                .map { now -> run.update(Gear.PARK, now)?.let { status -> status to now } }
                .takeWhile { it != null }
                .filterNotNull()
                .map { (status, now) ->
                    val mood = moods.update(status.state, now)
                    val reason = Reason.of(status)
                    val approaching = Reason.isApproaching(status)
                    statusCopy(mood, reason, approaching, "아빠", lightEnabled = true).reason
                }
                .toList()
        return reasons.filterIndexed { i, reason -> i == 0 || reasons[i - 1] != reason }
    }
}
