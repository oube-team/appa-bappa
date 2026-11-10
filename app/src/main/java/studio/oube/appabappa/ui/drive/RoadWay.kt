// 도로 그림의 위치와 크기(미터)는 그림 데이터라서 값마다 이름을 붙이지 않는다. 장면의 위치는 가상의 차와 같은
// 값(DriveRoad)을 사용한다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.ui.graphics.Path
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import studio.oube.appabappa.demo.DriveRoad
import studio.oube.appabappa.demo.DriveSim

private typealias Span = ClosedFloatingPointRange<Double>

// 편도 3차로 도로 하나다. camera는 이 도로를 기준으로 한 카메라이고, travelled는 이 도로를 따라 달린 거리로 점선과 반사
// 기둥의 자리를 정한다. from부터 FAR까지 그리고, 교차로가 있으면 crossing이 가로지르는 도로의 가까운 끝이다.
internal class Way(
    val camera: RoadCamera,
    val travelled: Double,
    val from: Double = NEAR,
    val crossing: Double? = null
)

// 신호등의 왼쪽 화살표 등이다. 점은 옆 위치와 높이다.
private val ArrowLamp =
    listOf(
        -0.55 to 5.15,
        -0.36 to 5.3,
        -0.36 to 5.2,
        -0.05 to 5.2,
        -0.05 to 5.1,
        -0.36 to 5.1,
        -0.36 to 5.0
    )

// 도로의 바닥, 선, 반사 기둥이다. 교차로가 있으면 정지선부터 건너편 횡단보도 끝까지는 선을 긋지 않고, 교차로 앞 8m부터
// 건너편 횡단보도 뒤까지는 반사 기둥을 세우지 않는다.
internal fun addWay(shapes: RoadShapes, way: Way) {
    addSurface(shapes, way)
    val entry = way.crossing
    if (entry == null) {
        addLines(shapes, way, way.from..FAR)
        addPosts(shapes, way, way.from..FAR)
        return
    }
    val stop = entry - DriveRoad.SOLID_NEAR_M
    addLines(shapes, way, way.from..stop, solidFrom = entry - DriveRoad.SOLID_FAR_M)
    addLines(shapes, way, entry + DriveRoad.CROSS_EXIT_M..FAR)
    addCrosswalks(shapes, way.camera, entry)
    addPosts(shapes, way, way.from..entry - 8)
    addPosts(shapes, way, entry + 20..FAR)
}

private fun addSurface(shapes: RoadShapes, way: Way) {
    way.camera.quad(shapes.shoulder, way.from, FAR, -6.9, 6.9)
    way.camera.quad(shapes.road, way.from, FAR, -5.25, 5.25)
}

// 도로의 한 구간에 긋는 선이다. 왼쪽 끝의 노란 두 줄, 차로를 나누는 두 선, 오른쪽 가장자리 선이다. 차로를 나누는 선은
// 점선이고, solidFrom부터는 차로를 변경할 수 없는 실선이다. 합류 장면은 오른쪽 가장자리 선을 별도로 긋는다(edge).
private fun addLines(
    shapes: RoadShapes,
    way: Way,
    span: Span,
    solidFrom: Double = span.endInclusive,
    edge: Boolean = true
) {
    val camera = way.camera
    val from = span.start
    val to = span.endInclusive
    camera.quad(shapes.yellow, from, to, -5.2, -5.08)
    camera.quad(shapes.yellow, from, to, -5.0, -4.88)
    for (lat in listOf(-1.75, 1.75)) {
        addDashes(shapes.marks, way, lat - 0.08..lat + 0.08, from..min(solidFrom, to))
        camera.quad(shapes.marks, max(solidFrom, from), to, lat - 0.08, lat + 0.08)
    }
    if (edge) camera.quad(shapes.marks, from, to, 5.05, 5.2)
}

// 12m마다 5m씩 긋는 점선이다. 달린 거리만큼 뒤로 옮겨서 점선이 땅에 고정된 것처럼 보이게 한다.
private fun addDashes(path: Path, way: Way, lat: Span, span: Span) {
    val offset = way.travelled.mod(12.0)
    var start = floor((span.start + offset) / 12) * 12 - offset
    while (start < span.endInclusive) {
        val from = max(start, span.start)
        val to = min(start + 5, span.endInclusive)
        way.camera.quad(path, from, to, lat.start, lat.endInclusive)
        start += 12
    }
}

// 길가의 반사 기둥이다. 20m마다 세우고 차에서 270m보다 먼 곳에는 세우지 않는다. 달린 거리만큼 뒤로 옮겨서 땅에 고정된
// 것처럼 보이게 한다. 갓길 위에, 도로 바닥 끝에서 0.55m 바깥에 세운다. 갓길은 먼 곳에서 땅과 색이 비슷해서, 갓길의
// 바깥 끝이나 갓길 밖에 세우면 가로지르는 도로처럼 옆에서 보는 도로의 기둥이 가장자리 선과 떨어져 공중에 뜬 것처럼
// 보인다. right는 그 깊이에 세울 오른쪽 기둥의 옆 위치다.
private fun addPosts(
    shapes: RoadShapes,
    way: Way,
    span: Span,
    right: (Double) -> Double = { 5.8 }
) {
    val offset = way.travelled.mod(20.0) - 6
    var z = (floor((max(span.start, -270.0) + offset) / 20) + 1) * 20 - offset
    while (z < min(span.endInclusive, 270.0)) {
        for (lat in listOf(-5.8, right(z))) {
            way.camera.upright(shapes.posts, lat - 0.07, lat + 0.07, 0.0..1.0, z)
            way.camera.upright(shapes.reflectors, lat - 0.07, lat + 0.07, 0.72..0.88, z)
        }
        z += 20
    }
}

// 교차로의 정지선과 앞뒤 횡단보도다. entry는 가로지르는 도로의 가까운 끝이다.
private fun addCrosswalks(shapes: RoadShapes, camera: RoadCamera, entry: Double) {
    val stop = entry - DriveRoad.SOLID_NEAR_M
    val far = entry + DriveRoad.CROSS_WIDTH_M + 1
    camera.quad(shapes.marks, stop, stop + 0.4, -5.25, 5.25)
    for (step in -5..4) {
        val left = step.toDouble()
        camera.quad(shapes.marks, entry - 4.5, entry - 1, left, left + 0.5)
        camera.quad(shapes.marks, far, entry + DriveRoad.CROSS_EXIT_M, left, left + 0.5)
    }
}

// 건너편 신호등이다. 직진과 좌회전을 함께 허용하는 신호라서 왼쪽 화살표 등과 초록 등을 켠다. 등은 왼쪽부터 빨강, 노랑,
// 화살표, 초록 순서로 놓인다.
private fun addSignal(shapes: RoadShapes, camera: RoadCamera, entry: Double) {
    val z = entry + DriveRoad.CROSS_WIDTH_M + 2
    camera.upright(shapes.signalPole, 6.8, 7.1, 0.0..5.6, z)
    camera.upright(shapes.signalPole, -1.8, 7.1, 5.4..5.6, z)
    camera.upright(shapes.signalBox, -1.8, 0.6, 4.9..5.4, z)
    camera.facing(shapes.signalLamp, z, ArrowLamp)
    camera.upright(shapes.signalLamp, 0.05, 0.5, 5.0..5.3, z)
}

// 합류 장면의 도로다. 진입로(가속 차로)는 본선 3차로의 오른쪽에 붙은 차로이고, 합류 지점(point) 60m 앞부터 좁아져 합류
// 지점에서 끝난다. 진입로와 본선 사이 선은 합류 구간 전까지 실선, 합류 구간에서는 점선이고, 합류 지점 뒤로는 본선의
// 오른쪽 가장자리 선으로 이어진다. 갓길과 오른쪽 반사 기둥은 진입로의 바깥 끝(outer)을 따라간다.
internal fun addMergeRoad(shapes: RoadShapes, way: Way, point: Double) {
    val camera = way.camera
    val zone = point - DriveRoad.MERGE_ZONE_M
    val taper = point - DriveRoad.RAMP_TAPER_M
    val width = DriveRoad.LANE_WIDTH_M
    val outer = { z: Double ->
        5.25 + width * ((point - z) / DriveRoad.RAMP_TAPER_M).coerceIn(0.0, 1.0)
    }
    addSurface(shapes, way)
    addLines(shapes, way, way.from..FAR, edge = false)
    camera.quad(shapes.marks, way.from, zone, 5.05, 5.2)
    addDashes(shapes.marks, way, 5.05..5.2, max(zone, way.from)..point)
    camera.quad(shapes.marks, max(point, way.from), FAR, 5.05, 5.2)
    val rows = (depths(way.from, min(point, FAR)) + taper).filter { it in way.from..point }.sorted()
    camera.strip(shapes.shoulder, rows.map { Band(it, 5.25, outer(it) + 1.65) })
    camera.strip(shapes.road, rows.map { Band(it, 5.25, outer(it)) })
    camera.strip(shapes.marks, rows.map { Band(it, outer(it) - 0.2, outer(it) - 0.05) })
    addPosts(shapes, way, way.from..FAR) { outer(it) + 0.55 }
}

// 좌회전 장면의 교차로다. 달리는 도로와 왼쪽으로 가로지르는 도로를 각각 그 도로를 기준으로 한 카메라(camera, cross)로
// 그린다. 좌회전하는 동안 달리던 도로가 차 옆과 뒤로 보이므로 차 뒤 30m부터 그리고, 가로지르는 도로는 양쪽으로 FAR까지
// 그린다. 신호등은 달리는 도로의 건너편에만 세운다.
internal fun addCrossroads(
    shapes: RoadShapes,
    sim: DriveSim,
    camera: RoadCamera,
    cross: RoadCamera
) {
    val entry = sim.guideMeters + DriveRoad.CROSS_ENTRY_M
    val crossEntry = sim.lateralMeters - DriveRoad.CROSS_WIDTH_M / 2
    addWay(shapes, Way(camera, sim.travelledMeters, -30.0, entry))
    addWay(shapes, Way(cross, sim.crossTravelledMeters, -FAR, crossEntry))
    addSignal(shapes, camera, entry)
}
