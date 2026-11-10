package studio.oube.appabappa.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import studio.oube.appabappa.engine.Gear

class DemoPlayerTest {
    private val logs = mutableListOf<String>()
    private val warnings = mutableListOf<String>()
    private val player = DemoPlayer(log = { logs += it }, warn = { warnings += it })

    @Test
    fun `요청한 장면이 없으면 재생하지 않는다`() {
        assertNull(player.update(Gear.PARK, 0))
        assertNull(player.sceneId)
    }

    @Test
    fun `기어가 P이면 요청한 장면을 재생한다`() {
        player.request("parking")
        assertNotNull(player.update(Gear.PARK, 0))
        assertEquals("parking", player.sceneId)
        assertEquals("parking", player.progress?.sceneId)
    }

    @Test
    fun `기어 값을 수신하기 전에는 재생을 보류했다가 P를 수신하면 재생한다`() {
        player.request("parking")
        assertNull(player.update(null, 0))
        assertNull(player.sceneId)

        assertNotNull(player.update(Gear.PARK, 250))
        assertEquals("parking", player.sceneId)
    }

    @Test
    fun `기어가 P가 아니면 요청을 버리고 이유를 기록한다`() {
        player.request("parking")
        assertNull(player.update(Gear.DRIVE, 0))
        assertNull(player.update(Gear.PARK, 250))
        assertEquals(listOf("demo parking needs gear P, now DRIVE"), warnings)
    }

    @Test
    fun `목록에 없는 장면을 요청하면 재생하지 않고 장면 목록을 기록한다`() {
        player.request("nothing")
        assertNull(player.update(Gear.PARK, 0))
        assertEquals(true, warnings.single().startsWith("unknown demo scene nothing, scenes: "))
    }

    @Test
    fun `재생 중에 기어가 P를 벗어나면 재생을 중단한다`() {
        player.request("parking")
        player.update(Gear.PARK, 0)
        assertNull(player.update(Gear.DRIVE, 250))
        assertNull(player.sceneId)
    }

    @Test
    fun `장면이 끝나면 재생을 마친다`() {
        player.request("parking")
        player.update(Gear.PARK, 0)
        val scene = requireNotNull(DemoScenes.find("parking"))
        val end = (scene.durationSeconds * 1_000).toLong()

        assertNull(player.update(Gear.PARK, end))
        assertNull(player.sceneId)
        assertEquals("demo parking finished", logs.last())
    }

    @Test
    fun `멈추기를 누르면 재생 중인 장면과 보류한 요청을 모두 초기화한다`() {
        player.request("parking")
        player.update(Gear.PARK, 0)
        player.stop()
        assertNull(player.update(Gear.PARK, 250))

        player.request("parking")
        player.stop()
        assertNull(player.update(Gear.PARK, 500))
    }
}
