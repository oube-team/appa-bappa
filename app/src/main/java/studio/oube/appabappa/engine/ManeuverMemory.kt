package studio.oube.appabappa.engine

// 바쁜 상황 동안 알리는 안내를 기억하고, 그 지점을 지났는지 판단한다. 방향지시등을 켠 채 계속 달리는 경우처럼 지점을
// 지난 뒤 passingMeters 넘게 달리면, 그 뒤의 조작은 지나온 안내와 관계없는 것으로 판단해 기억을 초기화한다.
internal class ManeuverMemory(private val config: BusyConfig) {
    var turnTypeName: String? = null
        private set

    var passed = false
        private set

    private var lastMeters = 0.0
    private var pastMeters = 0.0

    // nearby는 근처 안내(BusyCriteria.nearbyManeuver), next는 내비게이션의 다음 안내, movedMeters는 지난번 판단 뒤 달린
    // 거리다. 근처 안내가 사라졌는데 같은 종류의 안내가 passingMeters 안에 남아 있으면, 차가 정지해서 근처 안내가 아니게
    // 된 것이므로 아직 지나지 않았다. 남아 있지 않고 마지막으로 확인한 거리가 passedMeters 이하였으면 지점을 지났다.
    // 그보다 멀리 있던 안내가 사라졌으면 경로가 바뀐 것이므로 기억을 초기화한다.
    fun update(nearby: Maneuver?, next: Maneuver?, movedMeters: Double) {
        val ahead =
            next?.takeIf {
                it.turnTypeName == turnTypeName && it.distanceMeters <= config.passingMeters
            }
        when {
            nearby != null -> remember(nearby)
            turnTypeName == null -> Unit
            passed -> leaveBehind(movedMeters)
            ahead != null -> lastMeters = ahead.distanceMeters
            lastMeters <= config.passedMeters -> passed = true
            else -> clear()
        }
    }

    fun clear() {
        turnTypeName = null
        passed = false
        lastMeters = 0.0
        pastMeters = 0.0
    }

    private fun remember(maneuver: Maneuver) {
        turnTypeName = maneuver.turnTypeName
        passed = false
        lastMeters = maneuver.distanceMeters
        pastMeters = 0.0
    }

    private fun leaveBehind(movedMeters: Double) {
        pastMeters += movedMeters
        if (pastMeters > config.passingMeters) clear()
    }
}
