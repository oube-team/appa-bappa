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

private const val TICK_MILLIS = 250L

class DemoScenesTest {
    @Test
    fun `고속도로 합류 장면은 FREE, SOON, VERY_BUSY, RECOVERING, FREE 순서로 바뀐다`() {
        assertEquals(listOf(FREE, SOON, VERY_BUSY, RECOVERING, FREE), statesOf("highway_merge"))
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
}
