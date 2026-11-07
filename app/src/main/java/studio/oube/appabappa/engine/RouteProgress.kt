package studio.oube.appabappa.engine

private const val MILLIS_PER_SECOND = 1_000.0

// 내비게이션이 마지막으로 알려 준 다음 안내 목록에서 그 뒤에 달린 거리를 빼서, 지금 남은 거리를 추정한다.
// 에뮬레이터의 내비게이션은 차 위치를 따라오지 않아 남은 거리가 줄지 않았다(2026-11-07 시험).
// 실차에서 남은 거리가 얼마나 자주 갱신되는지는 확인하지 못했다.
// 새 목록을 수신하면 그 거리로 다시 시작하므로, 목록이 자주 갱신되면 추정한 거리는 거의 사용되지 않는다.
// 지금 위치에서 측정한 거리라면 뒤로 갈수록 커져야 하는데 에뮬레이터 실측값은 80, 380, 730, 1200, 530m로
// 중간에 작아져서, 목록의 거리를 앞 안내에서 다음 안내까지의 거리로 판단한다(추정).
class RouteProgress {
    private var maneuvers: List<Maneuver> = emptyList()
    private var traveledMeters = 0.0
    private var lastMillis: Long? = null

    // 새 안내 목록을 수신한 시각부터 달린 거리를 다시 측정한다.
    fun reset(maneuvers: List<Maneuver>, receivedAtMillis: Long) {
        this.maneuvers = maneuvers
        traveledMeters = 0.0
        lastMillis = receivedAtMillis
    }

    // 서비스가 0.25초마다 호출하므로 그 사이 차속이 일정하다고 간주해, 지난번 호출 뒤 지금 차속으로 달린 거리를 더한다.
    fun advance(speedMps: Double, nowMillis: Long) {
        val last = lastMillis ?: return
        if (nowMillis <= last) return
        traveledMeters += speedMps * (nowMillis - last) / MILLIS_PER_SECOND
        lastMillis = nowMillis
    }

    // 지금 다가오는 안내와 남은 거리를 반환한다. 목록의 안내를 모두 지났으면 null을 반환한다.
    fun next(): Maneuver? {
        var passed = traveledMeters
        for (maneuver in maneuvers) {
            if (maneuver.distanceMeters > passed) {
                return maneuver.copy(distanceMeters = maneuver.distanceMeters - passed)
            }
            passed -= maneuver.distanceMeters
        }
        return null
    }
}
