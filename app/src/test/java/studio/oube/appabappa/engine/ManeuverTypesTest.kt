package studio.oube.appabappa.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ManeuverTypesTest {
    private val allTurnTypes =
        listOf(
            "TURN_UNKNOWN",
            "UNKNOWN",
            "TURN_STRAIGHT",
            "TURN_RIGHT",
            "TURN_LEFT",
            "TURN_KEEP_R",
            "TURN_KEEP_L",
            "TURN_SHARP_R",
            "TURN_SHARP_L",
            "TURN_UTURN",
            "TURN_ENTER_IC_R",
            "TURN_ENTER_IC_L",
            "TURN_ENTER_IC_C",
            "TURN_EXIT_IC_R",
            "TURN_EXIT_IC_L",
            "TURN_EXIT_IC_C",
            "TURN_ENTER_UNDER_R",
            "TURN_ENTER_UNDER_L",
            "TURN_ENTER_UNDER_C",
            "TURN_AVOID_UNDER_R",
            "TURN_AVOID_UNDER_L",
            "TURN_AVOID_UNDER_C",
            "TURN_ENTER_OVER_R",
            "TURN_ENTER_OVER_L",
            "TURN_ENTER_OVER_C",
            "TURN_AVOID_OVER_R",
            "TURN_AVOID_OVER_L",
            "TURN_AVOID_OVER_C",
            "TURN_ENTER_ROUNDABOUT_02",
            "TURN_ENTER_ROUNDABOUT_03",
            "TURN_ENTER_ROUNDABOUT_04",
            "TURN_ENTER_ROUNDABOUT_06",
            "TURN_ENTER_ROUNDABOUT_08",
            "TURN_ENTER_ROUNDABOUT_09",
            "TURN_ENTER_ROUNDABOUT_10",
            "TURN_ENTER_ROUNDABOUT_12",
            "TURN_EXIT_ROUNDABOUT",
            "TURN_WAY",
            "TURN_ARRIVE",
            "TURN_TUNNEL",
            "TURN_DEVIDE_MERGE",
            "TURN_TOLL",
            "TURN_ENTER_SERVICEAREA",
            "TURN_RESTAREA"
        )

    @Test
    fun `안내 종류 44가지 중 34가지를 운전자가 바쁜 상황인지 판단하는 데 사용한다`() {
        assertEquals(44, allTurnTypes.toSet().size)
        assertEquals(34, allTurnTypes.count(ManeuverTypes::isDemanding))
    }

    @Test
    fun `직진, 지하차도와 고가도로 정면 진입과 진출, 터널, 경유지, 도착 안내는 운전자가 바쁜 상황인지 판단하는 데 사용하지 않는다`() {
        listOf(
            "TURN_STRAIGHT",
            "TURN_ENTER_UNDER_C",
            "TURN_AVOID_UNDER_C",
            "TURN_ENTER_OVER_C",
            "TURN_AVOID_OVER_C",
            "TURN_TUNNEL",
            "TURN_WAY",
            "TURN_ARRIVE"
        ).forEach { assertFalse(it, ManeuverTypes.isDemanding(it)) }
    }

    @Test
    fun `SDK에 새로 생긴 안내 종류는 검토해서 목록에 추가하기 전까지 사용하지 않는다`() {
        assertFalse(ManeuverTypes.isDemanding("TURN_SOMETHING_NEW"))
    }
}
