package studio.oube.appabappa.engine

internal object ManeuverTypes {
    // 회전하거나 차로를 옮겨야 하는 안내를 넣는다.
    private val demanding =
        setOf(
            "TURN_LEFT",
            "TURN_RIGHT",
            "TURN_SHARP_L",
            "TURN_SHARP_R",
            "TURN_UTURN",
            "TURN_ENTER_ROUNDABOUT_02",
            "TURN_ENTER_ROUNDABOUT_03",
            "TURN_ENTER_ROUNDABOUT_04",
            "TURN_ENTER_ROUNDABOUT_06",
            "TURN_ENTER_ROUNDABOUT_08",
            "TURN_ENTER_ROUNDABOUT_09",
            "TURN_ENTER_ROUNDABOUT_10",
            "TURN_ENTER_ROUNDABOUT_12",
            "TURN_EXIT_ROUNDABOUT",
            "TURN_KEEP_L",
            "TURN_KEEP_R",
            "TURN_ENTER_IC_L",
            "TURN_ENTER_IC_R",
            "TURN_ENTER_IC_C",
            "TURN_EXIT_IC_L",
            "TURN_EXIT_IC_R",
            "TURN_EXIT_IC_C",
            "TURN_ENTER_UNDER_L",
            "TURN_ENTER_UNDER_R",
            "TURN_AVOID_UNDER_L",
            "TURN_AVOID_UNDER_R",
            "TURN_ENTER_OVER_L",
            "TURN_ENTER_OVER_R",
            "TURN_AVOID_OVER_L",
            "TURN_AVOID_OVER_R",
            "TURN_DEVIDE_MERGE",
            "TURN_TOLL",
            "TURN_ENTER_SERVICEAREA",
            "TURN_RESTAREA"
        )

    fun isDemanding(turnTypeName: String): Boolean = turnTypeName in demanding
}
