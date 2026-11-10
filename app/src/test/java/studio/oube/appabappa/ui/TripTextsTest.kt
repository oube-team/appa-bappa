package studio.oube.appabappa.ui

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTextsTest {
    @Test
    fun `바빴던 시간은 1분이 안 되면 초만, 1시간 이상이면 시간과 분만 보여 준다`() {
        assertEquals("45초", durationText(45_000))
        assertEquals("3분 30초", durationText(210_000))
        assertEquals("3분", durationText(180_000))
        assertEquals("1시간 5분", durationText(3_900_000))
        assertEquals("1시간", durationText(3_600_000))
    }

    @Test
    fun `출발과 도착 시각은 오전과 오후를 붙여 12시간제로 보여 준다`() {
        val seoul = TimeZone.getTimeZone("Asia/Seoul")
        // 2026-11-09 14:05와 09:05(한국 시각)
        assertEquals("오후 2:05", clockText(1_794_200_700_000, seoul))
        assertEquals("오전 9:05", clockText(1_794_182_700_000, seoul))
    }
}
