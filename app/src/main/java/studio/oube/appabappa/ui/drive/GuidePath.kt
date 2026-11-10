// 안내선의 위치와 크기(미터)는 그림 데이터라서 값마다 이름을 붙이지 않는다. 장면의 위치는 가상의 차와 같은
// 값(DriveRoad)을 사용한다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveRoad
import studio.oube.appabappa.demo.DriveSim

// 안내선 위의 한 점이다. lat과 z는 자리이고, across와 along은 안내선이 나아가는 방향의 옆 성분과 앞 성분이다.
private class PathPoint(val lat: Double, val z: Double, val across: Double, val along: Double) {
    // 이 점에서 안내선의 오른쪽으로 side, 나아가는 쪽으로 ahead만큼 떨어진 자리(옆 위치와 깊이)다.
    fun offset(side: Double, ahead: Double): Pair<Double, Double> =
        lat + side * along + ahead * across to z - side * across + ahead * along
}

// 내비게이션이 차로 위에 그리는 안내선이다. length는 차에서 안내선 끝까지의 길이이고, at은 차에서 앞으로 간 거리(호를
// 도는 구간에서는 호를 따라 간 거리)로 그 자리의 점을 구한다. bends는 안내선이 휘는 구간이라 촘촘하게 나눠 그릴 거리다.
private class GuidePath(
    val length: Double,
    val bends: List<Double> = emptyList(),
    val at: (Double) -> PathPoint
)

// 차가 있는 자리(from)에서 안내하는 차로(to)로 옮겨 가는 곧은 안내선이다. 깊이 start부터 end 사이에서 옮겨 간다.
private class LaneShift(val from: Double, val to: Double, val start: Double, val end: Double) {
    fun at(z: Double): PathPoint {
        val across = lat(z + 0.5) - lat(z - 0.5)
        val length = hypot(across, 1.0)
        return PathPoint(lat(z), z, across / length, 1 / length)
    }

    private fun lat(z: Double): Double {
        val t = ((z - start) / (end - start)).coerceIn(0.0, 1.0)
        return from + (to - from) * t * t * (3 - 2 * t)
    }
}

private val ArrowShape =
    listOf(-0.95 to 0.0, 0.0 to 0.8, 0.95 to 0.0, 0.95 to -0.4, 0.0 to 0.4, -0.95 to -0.4)

// 안내선과 그 위를 흘러가는 화살표, 안내 지점 위의 AR 표지판을 그린다. 표지판은 안내 지점을 지나기 전까지만 표시한다.
internal fun addGuide(shapes: RoadShapes, camera: RoadCamera, sim: DriveSim, nowMillis: Long) {
    val path = guidePath(sim) ?: return
    addRibbon(shapes, camera, path)
    addChevrons(shapes, camera, path, nowMillis)
    if (sim.navGuide != null) shapes.sign = signAt(camera, path.at(sim.guideMeters))
}

private fun guidePath(sim: DriveSim): GuidePath? = when (sim.guide.takeIf { sim.showsGuideLine }) {
    DriveGuide.MERGE -> mergePath(sim)
    DriveGuide.LEFT_TURN -> leftTurnPath(sim)
    null -> null
}

// 합류 안내선은 합류 구간 안에서 진입로에서 본선 3차로로 옮겨 가고, 진입로가 끝나는 합류 지점에서 끝난다. 차가 이미
// 본선에 있으면 지금 차로를 따라 곧게 그린다.
private fun mergePath(sim: DriveSim): GuidePath {
    val point = sim.guideMeters
    val lane = min(sim.lateralMeters, DriveRoad.RIGHT_LANE_M)
    val shift = LaneShift(sim.lateralMeters, lane, point - 140, point - 60)
    return GuidePath(point, at = shift::at)
}

// 좌회전 안내선은 실선 구간 전에 1차로로 옮겨 가고, 안내 지점부터 차가 도는 호를 따라 가로지르는 도로의 1차로에서 끝난다.
// 좌회전하는 중에는 차가 호 위에 있으므로, 호의 시작(toArc)이 차 뒤에 있다.
private fun leftTurnPath(sim: DriveSim): GuidePath {
    val radius = DriveRoad.TURN_RADIUS_M
    val entry = sim.guideMeters + DriveRoad.CROSS_ENTRY_M
    val shift = LaneShift(sim.lateralMeters, DriveRoad.LEFT_LANE_M, entry - 110, entry - 45)
    val toArc = sim.turnRad?.let { -radius * it } ?: sim.guideMeters
    val bends = (0..12).map { toArc + radius * it * PI / 24 }
    return GuidePath(toArc + radius * PI / 2, bends) { s ->
        if (s < toArc) {
            shift.at(s)
        } else {
            val turned = (s - toArc) / radius
            PathPoint(
                DriveRoad.LEFT_LANE_M - DriveRoad.turnAcross(turned),
                sim.guideMeters + DriveRoad.turnAlong(turned),
                -sin(turned),
                cos(turned)
            )
        }
    }
}

// 안내선은 차 앞 3m부터 너비 2.2m로 그린다.
private fun addRibbon(shapes: RoadShapes, camera: RoadCamera, path: GuidePath) {
    val end = min(path.length, FAR)
    val steps = (depths(3.0, end) + path.bends.filter { it > 3 && it < end }).sorted()
    if (steps.size < 2) return
    val points = steps.map(path.at)
    val left = points.map { it.offset(-1.1, 0.0) }
    val right = points.map { it.offset(1.1, 0.0) }
    camera.outline(shapes.ribbon, left + right.asReversed())
}

// 화살표는 8m 간격으로 초속 6m씩 앞으로 흘러간다.
private fun addChevrons(shapes: RoadShapes, camera: RoadCamera, path: GuidePath, nowMillis: Long) {
    var s = 7 + (nowMillis / 1000.0 * 6) % 8
    while (s <= min(path.length - 1, FAR - 10)) {
        val point = path.at(s)
        val arrow = ArrowShape.map { (side, ahead) -> point.offset(side, ahead) }
        camera.outline(shapes.chevrons, arrow)
        s += 8
    }
}

// 안내 지점 위 2.8m 높이에 AR 표지판을 표시한다. 260m 안으로 들어오면 보이고, 12~26m 앞에서 옅어진다.
private fun signAt(camera: RoadCamera, point: PathPoint): SignPlacement? {
    val ahead = camera.distanceAhead(point.lat, point.z)
    if (point.z >= 260 || ahead <= 4) return null
    val at = camera.screen(point.lat, point.z, 2.8)
    val scale = min(4.0, 3.2 * camera.focal / ahead / 24).toFloat()
    val alpha = ((ahead - 12) / 14).coerceIn(0.0, 1.0).toFloat()
    return SignPlacement(at.x - 12 * scale, at.y - 12 * scale, scale, alpha)
}
