package studio.oube.appabappa.light

import org.junit.Assert.assertEquals
import org.junit.Test

class LightRecordTest {
    private val ice = 0xFF00E5FF.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val orange = LightPlan.dimmed(LightPlan.ORANGE, Brightness.MID.level(night = false))
    private val green = LightPlan.dimmed(LightPlan.GREEN, Brightness.MID.level(night = false))

    // 원래 색이 ice일 때 바쁜 상황이 한 번 지나간 뒤의 기록이다. 앱이 조명을 주황, 초록, 원래 색 순서로 변경했다.
    private val afterOneBusy =
        LightRecord()
            .atStart(ice)
            .withChange(ice, orange)
            .withChange(orange, green)
            .withChange(green, ice)

    @Test
    fun `처음 시작하면 조회한 색을 원래 색으로 저장한다`() {
        assertEquals(LightRecord(original = ice), LightRecord().atStart(ice))
    }

    @Test
    fun `앱이 주황을 켠 채 종료됐다가 시작하면 저장해 둔 원래 색을 사용한다`() {
        val record = LightRecord().atStart(ice).withChange(ice, orange)

        assertEquals(ice, record.atStart(orange).original)
    }

    @Test
    fun `앱이 주황으로 변경하는 도중에 종료됐다가 시작하면 저장해 둔 원래 색을 사용한다`() {
        val record = LightRecord().atStart(ice).withChange(ice, orange)
        val halfway = LightPlan.blend(ice, orange, 0.5)

        assertEquals(ice, record.atStart(halfway).original)
    }

    @Test
    fun `앱이 원래 색으로 되돌린 뒤 종료됐다가 시작하면 저장해 둔 원래 색을 그대로 사용한다`() {
        assertEquals(ice, afterOneBusy.atStart(ice).original)
    }

    @Test
    fun `앱이 종료된 동안 사용자가 색을 변경했으면 시작할 때 그 색을 원래 색으로 저장하고 마지막 변경을 삭제한다`() {
        assertEquals(LightRecord(original = white), afterOneBusy.atStart(white))
    }

    @Test
    fun `앱이 실행 중일 때 사용자가 색을 변경하면 그 색을 원래 색으로 저장하고 마지막 변경을 삭제한다`() {
        assertEquals(LightRecord(original = white), afterOneBusy.withUserColor(white))
    }

    // 2026-11-11 에뮬레이터에서 찾은 순서다. 마지막 변경을 삭제하지 않았을 때는 ice를 앱이 켠 색으로 판단해
    // 조명을 white로 변경했다.
    @Test
    fun `앱이 실행 중일 때 사용자가 색을 변경하고 앱이 종료된 동안 예전 색으로 되돌리면 시작할 때 되돌린 색을 원래 색으로 사용한다`() {
        val record = afterOneBusy.withUserColor(white)

        assertEquals(ice, record.atStart(ice).original)
    }

    @Test
    fun `사용자가 색을 변경한 뒤에 앱이 주황을 켠 채 종료됐다가 시작하면 사용자가 변경한 색을 원래 색으로 사용한다`() {
        val record = afterOneBusy.withUserColor(white).withChange(white, orange)

        assertEquals(white, record.atStart(orange).original)
    }

    @Test
    fun `앱이 종료된 동안 사용자가 색을 변경하고 그다음 종료된 동안 예전 색으로 되돌려도 시작할 때마다 조회한 색을 원래 색으로 사용한다`() {
        val record = afterOneBusy.atStart(white)

        assertEquals(white, record.original)
        assertEquals(ice, record.atStart(ice).original)
    }

    @Test
    fun `색을 조회하지 못하면 저장해 둔 기록을 그대로 사용한다`() {
        assertEquals(afterOneBusy, afterOneBusy.atStart(null))
    }
}
