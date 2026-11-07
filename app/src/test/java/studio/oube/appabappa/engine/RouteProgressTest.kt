package studio.oube.appabappa.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteProgressTest {
    private val turnLeft = Maneuver("TURN_LEFT", 80.0)
    private val turnRight = Maneuver("TURN_RIGHT", 380.0)

    private fun progress(vararg maneuvers: Maneuver) =
        RouteProgress().apply { reset(maneuvers.toList(), receivedAtMillis = 0) }

    @Test
    fun `목록을 수신한 직후에는 첫 안내를 그대로 반환한다`() {
        assertEquals(turnLeft, progress(turnLeft, turnRight).next())
    }

    @Test
    fun `달린 거리만큼 첫 안내까지 남은 거리가 줄어든다`() {
        val progress = progress(turnLeft, turnRight)
        progress.advance(speedMps = 10.0, nowMillis = 3_000)
        assertEquals(Maneuver("TURN_LEFT", 50.0), progress.next())
    }

    @Test
    fun `첫 안내를 지나면 다음 안내와 그 안내까지 남은 거리를 반환한다`() {
        val progress = progress(turnLeft, turnRight)
        progress.advance(speedMps = 10.0, nowMillis = 10_000)
        assertEquals(Maneuver("TURN_RIGHT", 360.0), progress.next())
    }

    @Test
    fun `목록의 안내를 모두 지나면 다음 안내가 없다`() {
        val progress = progress(turnLeft, turnRight)
        progress.advance(speedMps = 10.0, nowMillis = 60_000)
        assertNull(progress.next())
    }

    @Test
    fun `새 목록을 수신하면 달린 거리를 다시 측정한다`() {
        val progress = progress(turnLeft, turnRight)
        progress.advance(speedMps = 10.0, nowMillis = 5_000)
        progress.reset(listOf(turnRight), receivedAtMillis = 5_000)
        assertEquals(turnRight, progress.next())
    }

    @Test
    fun `정차해 있으면 남은 거리가 줄지 않는다`() {
        val progress = progress(turnLeft)
        progress.advance(speedMps = 0.0, nowMillis = 30_000)
        assertEquals(turnLeft, progress.next())
    }

    @Test
    fun `시각이 앞으로 가지 않으면 거리를 더하지 않는다`() {
        val progress = progress(turnLeft)
        progress.advance(speedMps = 10.0, nowMillis = 2_000)
        progress.advance(speedMps = 10.0, nowMillis = 1_000)
        assertEquals(Maneuver("TURN_LEFT", 60.0), progress.next())
    }

    @Test
    fun `목록을 수신하기 전에는 다음 안내가 없다`() {
        val progress = RouteProgress()
        progress.advance(speedMps = 10.0, nowMillis = 1_000)
        assertNull(progress.next())
    }
}
