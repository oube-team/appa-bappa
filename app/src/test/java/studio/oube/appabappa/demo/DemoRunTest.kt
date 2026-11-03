package studio.oube.appabappa.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.Gear

class DemoRunTest {
    private val reversing =
        DemoScene("reversing", listOf(DemoStep(1.0, driving(0.0, gear = Gear.REVERSE))))

    @Test
    fun `실제 기어가 P가 아니면 시연 장면을 재생하지 않는다`() {
        val run = DemoRun(reversing, startMillis = 0)

        assertNull(run.update(Gear.DRIVE, 0))
    }

    @Test
    fun `장면이 끝나면 null을 반환한다`() {
        val run = DemoRun(reversing, startMillis = 0)

        assertEquals(BusyState.BUSY, run.update(Gear.PARK, 750))
        assertNull(run.update(Gear.PARK, 1_000))
    }
}
