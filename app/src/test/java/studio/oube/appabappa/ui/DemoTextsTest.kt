package studio.oube.appabappa.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.oube.appabappa.demo.DemoScenes

class DemoTextsTest {
    @Test
    fun `장면마다 이름과 처음부터 보여 줄 설명이 있다`() {
        for (scene in DemoScenes.all) {
            val text = SceneTexts[scene.id]
            assertNotNull(scene.id, text)
            assertTrue(scene.id, text?.name.orEmpty().isNotBlank())
            assertTrue(scene.id, text?.captionAt(0.0).orEmpty().isNotBlank())
        }
        assertEquals(DemoScenes.all.map { it.id }.toSet(), SceneTexts.keys)
    }

    @Test
    fun `스티어링 휠 각도는 부호 대신 왼쪽과 오른쪽으로 보여 준다`() {
        assertEquals("0도", steeringText(0.0))
        assertEquals("왼쪽 4도", steeringText(4.0))
        assertEquals("오른쪽 4도", steeringText(-4.0))
        assertEquals("왼쪽 180도", steeringText(180.0))
    }

    @Test
    fun `설명은 그 시각부터 다음 설명 전까지 보여 준다`() {
        val text = requireNotNull(SceneTexts["parking"])

        assertEquals("후진 기어(R)로 주차하고 있어요", text.captionAt(2.9))
        assertEquals("주차 기어(P)로 변경해서\n바쁜 상황이 바로 끝났어요", text.captionAt(3.0))
    }
}
