package studio.oube.appabappa.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManeuverMemoryTest {
    private val memory = ManeuverMemory(BusyConfig())

    private fun left(meters: Double) = Maneuver("TURN_LEFT", meters)

    @Test
    fun `근처 안내가 있으면 그 종류를 기억한다`() {
        memory.update(left(40.0), left(40.0), 0.0)

        assertEquals("TURN_LEFT", memory.turnTypeName)
        assertFalse(memory.passed)
    }

    @Test
    fun `20m 안에서 확인한 안내가 사라지면 지점을 지난 것으로 판단하고 종류를 유지한다`() {
        memory.update(left(5.0), left(5.0), 2.0)
        memory.update(null, null, 2.0)

        assertEquals("TURN_LEFT", memory.turnTypeName)
        assertTrue(memory.passed)
    }

    @Test
    fun `차가 정지해 근처 안내가 아니게 됐어도 같은 종류의 안내가 50m 안에 남아 있으면 지나지 않은 것이다`() {
        memory.update(left(12.0), left(12.0), 2.0)
        memory.update(null, left(10.0), 0.0)

        assertEquals("TURN_LEFT", memory.turnTypeName)
        assertFalse(memory.passed)
    }

    @Test
    fun `20m보다 멀리 있던 안내가 사라지면 경로가 바뀐 것으로 판단해 기억을 초기화한다`() {
        memory.update(left(45.0), left(45.0), 2.0)
        memory.update(null, null, 2.0)

        assertNull(memory.turnTypeName)
        assertFalse(memory.passed)
    }

    @Test
    fun `지점을 지난 뒤 50m 넘게 달리면 기억을 초기화한다`() {
        memory.update(left(5.0), left(5.0), 2.0)
        memory.update(null, null, 2.0)
        memory.update(null, null, 50.0)
        assertEquals("TURN_LEFT", memory.turnTypeName)

        memory.update(null, null, 1.0)
        assertNull(memory.turnTypeName)
    }

    @Test
    fun `지점을 지난 뒤 다른 안내가 근처에 오면 새 안내를 기억한다`() {
        memory.update(left(5.0), left(5.0), 2.0)
        memory.update(null, null, 2.0)
        memory.update(Maneuver("TURN_RIGHT", 30.0), Maneuver("TURN_RIGHT", 30.0), 2.0)

        assertEquals("TURN_RIGHT", memory.turnTypeName)
        assertFalse(memory.passed)
    }
}
