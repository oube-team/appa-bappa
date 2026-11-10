package studio.oube.appabappa.demo

import kotlin.math.cos
import kotlin.math.sin

// 직접 조작 시연의 도로와 장면 위치(미터)다. 가상의 차(DriveSim)와 도로 그림(ui/drive)이 같은 값을 사용해야 그려진
// 차선과 차로 변경 규칙이 어긋나지 않으므로 한곳에 둔다. 옆 방향 위치는 가운데 차로 중앙에서 오른쪽으로 측정한다.
// 구간의 길이는 시연용으로 정한 값이다.
internal object DriveRoad {
    const val LANE_WIDTH_M = 3.5

    // 본선은 세 차로다. 1차로(왼쪽 끝)와 3차로(오른쪽 끝)의 중앙이다.
    const val LEFT_LANE_M = -LANE_WIDTH_M
    const val RIGHT_LANE_M = LANE_WIDTH_M

    // 합류 장면의 진입로(가속 차로)는 본선 3차로의 오른쪽에 붙은 별도의 차로이고, 합류 지점(안내 지점)에서 끝난다.
    // 진입로와 본선 사이 선은 합류 지점 150m 앞까지 실선이라 그 전에는 본선으로 옮기지 않는다. 150m 앞부터 합류
    // 지점까지가 합류 구간이고, 진입로는 마지막 60m 동안 좁아진다.
    const val RAMP_LANE_M = RIGHT_LANE_M + LANE_WIDTH_M
    const val MERGE_ZONE_M = 150.0
    const val RAMP_TAPER_M = 60.0

    // 좌회전 장면의 교차로다. 가로지르는 도로(너비 14m)의 가까운 끝은 안내 지점 4.5m 뒤에 있고, 그 앞 6~36m는
    // 차로를 변경할 수 없는 실선 구간이다. 건너편 횡단보도는 가까운 끝에서 18.5m 뒤에 끝난다.
    const val CROSS_ENTRY_M = 4.5
    const val CROSS_WIDTH_M = 14.0
    const val CROSS_EXIT_M = 18.5
    const val SOLID_NEAR_M = 6.0
    const val SOLID_FAR_M = 36.0

    // 좌회전은 안내 지점에서 시작해 반지름 8m의 호를 따라 가로지르는 도로의 1차로로 들어간다. 가로지르는 도로도 본선과
    // 같은 세 차로이고 그 가운데 차로의 중앙은 가까운 끝에서 7m 뒤에 있으므로, 1차로 중앙은 안내 지점에서
    // 8m(4.5 + 7 - 3.5) 뒤다.
    const val TURN_RADIUS_M = CROSS_ENTRY_M + CROSS_WIDTH_M / 2 - LANE_WIDTH_M

    // 좌회전 호를 headingRad만큼 돈 차가 본선의 1차로 중앙에서 왼쪽으로 옮겨 간 거리다.
    fun turnAcross(headingRad: Double): Double = TURN_RADIUS_M * (1 - cos(headingRad))

    // 좌회전 호를 headingRad만큼 돈 차가 안내 지점에서 본선 방향으로 나아간 거리다.
    fun turnAlong(headingRad: Double): Double = TURN_RADIUS_M * sin(headingRad)
}
