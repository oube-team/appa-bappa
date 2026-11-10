package studio.oube.appabappa.status

import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus

// 동승자에게 알릴 바쁜 이유다. 엔진의 근거(BusyCause)와 근처 안내의 종류(turnTypeName)로 정한다.
enum class Reason {
    MERGE,
    HIGHWAY_ENTRY,
    HIGHWAY_EXIT,
    LANE_CHANGE,
    LEFT_TURN,
    RIGHT_TURN,
    U_TURN,
    PASSED_LEFT_TURN,
    PASSED_RIGHT_TURN,
    PASSED_U_TURN,
    ROUNDABOUT,
    JUNCTION,
    TOLLGATE,
    REST_AREA,
    DROWSY_REST_AREA,
    TURNING,
    REVERSING,
    HARD_BRAKING,
    FORWARD_COLLISION_WARNING,
    BLIND_SPOT_WARNING;

    companion object {
        // 근처에 안내가 있으면 방향지시등이나 스티어링 휠도 그 안내 때문에 조작한 것으로 판단해 안내의 종류를 알린다.
        // 안내가 없을 때 방향지시등 조작은 차로 변경으로 알리고, 스티어링 휠 조작은 회전으로 알린다.
        // 좌회전, 우회전, 유턴 안내 지점을 지난 뒤(RECOVERING)에는 그동안 스티어링 휠을 돌린 적이 없으면 회전하지 않고
        // 지나간 것으로 판단해, 회전했다고 알리지 않고 지점을 지났다고 알린다.
        fun of(status: BusyStatus): Reason? = when (status.cause) {
            null -> null
            BusyCause.FORWARD_COLLISION_WARNING -> FORWARD_COLLISION_WARNING
            BusyCause.BLIND_SPOT_WARNING -> BLIND_SPOT_WARNING
            BusyCause.HARD_BRAKING -> HARD_BRAKING
            BusyCause.REVERSING -> REVERSING
            BusyCause.FAST_SIGNALING, BusyCause.SIGNALING -> guidedReason(status) ?: LANE_CHANGE
            BusyCause.TURNING -> guidedReason(status) ?: TURNING
            BusyCause.MANEUVER, BusyCause.UPCOMING_MANEUVER -> guidedReason(status)
        }

        private fun guidedReason(status: BusyStatus): Reason? {
            val reason = maneuverReason(status.turnTypeName)
            val passed = status.state == BusyState.RECOVERING && !status.turned
            return if (passed) reason?.withoutTurning() else reason
        }

        private fun Reason.withoutTurning(): Reason = when (this) {
            LEFT_TURN -> PASSED_LEFT_TURN
            RIGHT_TURN -> PASSED_RIGHT_TURN
            U_TURN -> PASSED_U_TURN
            else -> this
        }

        // NaviHelper TurnType 이름의 접두사로 안내의 종류를 나눈다. ManeuverTypes가 바쁜 상황으로 판단하는 안내만 다룬다.
        // 동승자에게 사실과 다른 이유를 알리지 않도록 javadoc의 안내 설명보다 넓게 말하지 않는다. TURN_KEEP은 "Right
        // side", "Left side"뿐이라 갈림길로, TURN_RESTAREA는 "Drowsy driving rest area"라서 졸음쉼터로 알린다.
        fun maneuverReason(turnTypeName: String?): Reason? {
            val name = turnTypeName ?: return null
            return maneuverPrefixes.firstOrNull { (prefix, _) -> name.startsWith(prefix) }?.second
        }

        private val maneuverPrefixes =
            listOf(
                "TURN_DEVIDE_MERGE" to MERGE,
                "TURN_ENTER_IC" to HIGHWAY_ENTRY,
                "TURN_EXIT_IC" to HIGHWAY_EXIT,
                "TURN_KEEP" to JUNCTION,
                "TURN_LEFT" to LEFT_TURN,
                "TURN_SHARP_L" to LEFT_TURN,
                "TURN_RIGHT" to RIGHT_TURN,
                "TURN_SHARP_R" to RIGHT_TURN,
                "TURN_UTURN" to U_TURN,
                "TURN_ENTER_ROUNDABOUT" to ROUNDABOUT,
                "TURN_EXIT_ROUNDABOUT" to ROUNDABOUT,
                "TURN_ENTER_UNDER" to JUNCTION,
                "TURN_AVOID_UNDER" to JUNCTION,
                "TURN_ENTER_OVER" to JUNCTION,
                "TURN_AVOID_OVER" to JUNCTION,
                "TURN_TOLL" to TOLLGATE,
                "TURN_ENTER_SERVICEAREA" to REST_AREA,
                "TURN_RESTAREA" to DROWSY_REST_AREA
            )
    }
}
