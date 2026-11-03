package studio.oube.appabappa.light

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.oube.appabappa.engine.BusyState.BUSY
import studio.oube.appabappa.engine.BusyState.FREE
import studio.oube.appabappa.engine.BusyState.RECOVERING
import studio.oube.appabappa.engine.BusyState.SOON
import studio.oube.appabappa.engine.BusyState.VERY_BUSY

class LightPlanTest {
    @Test
    fun `SOON에서는 은은한 주황을, BUSY와 VERY_BUSY와 RECOVERING에서는 같은 주황을 사용한다`() {
        assertEquals(LightPlan.SOFT_ORANGE, LightPlan.colorFor(SOON))
        assertEquals(LightPlan.ORANGE, LightPlan.colorFor(BUSY))
        assertEquals(LightPlan.ORANGE, LightPlan.colorFor(VERY_BUSY))
        assertEquals(LightPlan.ORANGE, LightPlan.colorFor(RECOVERING))
    }

    @Test
    fun `FREE가 되면 원래 색으로 되돌린다`() {
        assertNull(LightPlan.colorFor(FREE))
    }

    @Test
    fun `바쁜 상황이 끝나 FREE가 되면 초록을 잠깐 켠다`() {
        assertTrue(LightPlan.showsGreen(RECOVERING, FREE))
        assertTrue(LightPlan.showsGreen(BUSY, FREE))
    }

    @Test
    fun `SOON에서 FREE가 될 때와 RECOVERING에서 SOON이 될 때는 초록을 켜지 않는다`() {
        assertFalse(LightPlan.showsGreen(SOON, FREE))
        assertFalse(LightPlan.showsGreen(RECOVERING, SOON))
    }

    @Test
    fun `알파 값이 달라도 RGB가 같으면 같은 색으로 판단한다`() {
        assertTrue(LightPlan.sameColor(0xFFFF8C00.toInt(), 0x00FF8C00))
        assertFalse(LightPlan.sameColor(LightPlan.ORANGE, LightPlan.GREEN))
    }

    @Test
    fun `앱이 켜는 색과 원래 색을 구분한다`() {
        assertTrue(LightPlan.isOwnColor(LightPlan.ORANGE))
        assertTrue(LightPlan.isOwnColor(LightPlan.SOFT_ORANGE))
        assertTrue(LightPlan.isOwnColor(0x002E9E4F))
        assertFalse(LightPlan.isOwnColor(0xFFFFFFFF.toInt()))
    }
}
