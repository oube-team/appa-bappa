package studio.oube.appabappa.light

import kotlin.math.pow
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
    fun `밝기를 낮춘 앱의 색과 원래 색을 구분한다`() {
        assertTrue(LightPlan.isOwnColor(LightPlan.dimmed(LightPlan.ORANGE, LightPlan.DAY_LEVEL)))
        assertTrue(
            LightPlan.isOwnColor(LightPlan.dimmed(LightPlan.SOFT_ORANGE, LightPlan.NIGHT_LEVEL))
        )
        val nightGreen = LightPlan.dimmed(LightPlan.GREEN, LightPlan.NIGHT_LEVEL)
        assertTrue(LightPlan.isOwnColor(nightGreen and LightPlan.RGB_MASK))
        assertFalse(LightPlan.isOwnColor(0xFFFFFFFF.toInt()))
    }

    @Test
    fun `밝기를 낮추면 RGB 값이 모두 같은 비율로 줄어든다`() {
        assertEquals(0xFFCC7000.toInt(), LightPlan.dimmed(LightPlan.ORANGE, 0.8))
    }

    @Test
    fun `섞는 비율이 0이면 출발 색을, 1이면 도착 색을 반환한다`() {
        assertEquals(LightPlan.ORANGE, LightPlan.blend(LightPlan.ORANGE, LightPlan.GREEN, 0.0))
        assertEquals(LightPlan.GREEN, LightPlan.blend(LightPlan.ORANGE, LightPlan.GREEN, 1.0))
    }

    @Test
    fun `섞는 비율이 절반이면 RGB 값마다 가운데 값을 반환한다`() {
        assertEquals(
            0xFF808080.toInt(),
            LightPlan.blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.5)
        )
    }

    @Test
    fun `서서히 변경하는 동안 켜지는 색은 출발 색에서 시작해 도착 색으로 끝난다`() {
        val path = fadePath(LightPlan.ORANGE, LightPlan.GREEN)
        assertEquals(LightPlan.ORANGE, path.first())
        assertEquals(LightPlan.GREEN, path.last())
    }

    @Test
    fun `색각이상이 있어도 밝기로 구분되도록 낮과 밤 모두 주황을 초록보다 밝게 켠다`() {
        for (level in listOf(LightPlan.DAY_LEVEL, LightPlan.NIGHT_LEVEL)) {
            val orange = luminance(LightPlan.dimmed(LightPlan.ORANGE, level))
            val green = luminance(LightPlan.dimmed(LightPlan.GREEN, level))
            assertTrue("level $level", orange > green)
        }
    }

    // sRGB 색의 상대 휘도(WCAG 정의)다.
    private fun luminance(color: Int): Double {
        fun linear(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val red = linear(color shr 16 and 0xFF)
        val green = linear(color shr 8 and 0xFF)
        val blue = linear(color and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }
}
