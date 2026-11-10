package studio.oube.appabappa.ui.drive

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 운전석 카메라의 높이와, 도로를 그리는 가장 먼 거리와 가장 가까운 거리다(미터).
private const val CAMERA_HEIGHT = 1.25
internal const val FAR = 420.0
internal const val NEAR = 0.6

// 잘라 낸 뒤 꼭짓점이 세 개보다 적으면 다각형이 아니므로 그리지 않는다.
private const val MIN_CORNERS = 3

// 휘는 띠를 나누는 간격이다. 1m에서 시작해 멀어질수록 거리의 5%씩 넓힌다.
private const val DEPTH_STEP = 1.0
private const val DEPTH_STEP_GROWTH = 0.05

// 도로 위 한 점이다. lat은 도로의 가운데 차로 중앙에서 오른쪽으로 측정한 거리, z는 차에서 도로를 따라 앞쪽으로 측정한
// 거리, h는 높이다(미터).
internal class Ground(val lat: Double, val z: Double, val h: Double = 0.0)

// 운전석 카메라 기준 좌표다. x는 오른쪽, z는 앞쪽, h는 높이다.
private class Seen(val x: Double, val z: Double, val h: Double)

// 깊이 z에서 옆으로 a부터 b까지 걸친 띠의 한 줄이다. a가 b보다 왼쪽이어야 한다.
internal class Band(val z: Double, val a: Double, val b: Double)

// 운전석 카메라다. 도로를 기준으로 한 차의 옆 위치(lateral)와 앞유리 풍경의 각도(yaw)로 도로 위 점을 화면 좌표(dp)로
// 변환한다.
internal class RoadCamera private constructor(
    private val centerX: Double,
    private val horizon: Double,
    val focal: Double,
    private val lateral: Double,
    private val yaw: Double
) {
    private val cos = cos(yaw)
    private val sin = sin(yaw)

    constructor(width: Float, layout: DriveLayout, lateral: Double, yaw: Double) :
        this(width / 2.0, layout.horizon.toDouble(), layout.focal.toDouble(), lateral, yaw)

    // 왼쪽으로 가로지르는 도로를 기준으로 한 좌표를 전달받는 같은 카메라다. lateral은 그 도로를 기준으로 한 차의 옆 위치다.
    fun onCrossRoad(lateral: Double) = RoadCamera(centerX, horizon, focal, lateral, yaw - PI / 2)

    fun distanceAhead(lat: Double, z: Double): Double = see(Ground(lat, z)).z

    fun screen(lat: Double, z: Double, h: Double): Offset = screen(see(Ground(lat, z, h)))

    // 카메라 바로 앞(NEAR)보다 가까운 부분을 잘라 낸 다각형을 path에 더한다. 한 path에 겹쳐 그리는 다각형은 위에서
    // 봤을 때 도는 방향이 같아야 겹친 자리가 비지 않는다.
    fun polygon(path: Path, points: List<Ground>) {
        val seen = points.map(::see)
        val kept = ArrayList<Seen>()
        seen.forEachIndexed { i, a ->
            val b = seen[(i + 1) % seen.size]
            if (a.z >= NEAR) kept += a
            if ((a.z >= NEAR) != (b.z >= NEAR)) {
                val t = (NEAR - a.z) / (b.z - a.z)
                kept += Seen(a.x + (b.x - a.x) * t, NEAR, a.h + (b.h - a.h) * t)
            }
        }
        if (kept.size < MIN_CORNERS) return
        kept.forEachIndexed { i, v ->
            val o = screen(v)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        path.close()
    }

    private fun see(p: Ground): Seen {
        val x = p.lat - lateral
        return Seen(x * cos + p.z * sin, -x * sin + p.z * cos, p.h)
    }

    private fun screen(v: Seen) = Offset(
        (centerX + v.x * focal / v.z).toFloat(),
        (horizon + (CAMERA_HEIGHT - v.h) * focal / v.z).toFloat()
    )
}

// 땅 위의 사각형이다. z1부터 z2까지, 옆으로 l1부터 l2까지 걸친다. z1이 z2보다 앞이 아니면 그리지 않는다.
internal fun RoadCamera.quad(path: Path, z1: Double, z2: Double, l1: Double, l2: Double) {
    if (z1 >= z2) return
    polygon(path, listOf(Ground(l1, z1), Ground(l1, z2), Ground(l2, z2), Ground(l2, z1)))
}

// 깊이 z에 도로를 가로질러 선 면에 그린 다각형이다. 점은 옆 위치와 높이다.
internal fun RoadCamera.facing(path: Path, z: Double, points: List<Pair<Double, Double>>) =
    polygon(path, points.map { (lat, h) -> Ground(lat, z, h) })

// 깊이 z에 도로를 가로질러 선 사각형이다. 옆으로 l1부터 l2까지, 높이 heights에 걸친다.
internal fun RoadCamera.upright(
    path: Path,
    l1: Double,
    l2: Double,
    heights: ClosedFloatingPointRange<Double>,
    z: Double
) {
    val low = heights.start
    val high = heights.endInclusive
    facing(path, z, listOf(l1 to low, l2 to low, l2 to high, l1 to high))
}

// 앞차의 옆면처럼 차로와 나란히 선 면이다. 앞으로 z1부터 z2까지, 높이 heights에 걸친다.
internal fun RoadCamera.side(
    path: Path,
    lat: Double,
    z1: Double,
    z2: Double,
    heights: ClosedFloatingPointRange<Double>
) {
    val low = heights.start
    val high = heights.endInclusive
    polygon(
        path,
        listOf(
            Ground(lat, z1, low),
            Ground(lat, z2, low),
            Ground(lat, z2, high),
            Ground(lat, z1, high)
        )
    )
}

// 깊이마다 옆으로 걸친 띠(Band)를 이어 휘는 띠를 그린다. a 쪽 가장자리를 따라 나갔다가 b 쪽으로 돌아온다.
internal fun RoadCamera.strip(path: Path, rows: List<Band>) {
    if (rows.size < 2) return
    val out = rows.map { Ground(it.a, it.z) }
    val back = rows.asReversed().map { Ground(it.b, it.z) }
    polygon(path, out + back)
}

// 땅 위의 점(옆 위치와 깊이)을 차례로 이은 다각형이다.
internal fun RoadCamera.outline(path: Path, points: List<Pair<Double, Double>>) =
    polygon(path, points.map { (lat, z) -> Ground(lat, z) })

// 가까울수록 촘촘하게 나눈 깊이 목록이다. 휘는 띠를 매끄럽게 그리는 데 사용한다.
internal fun depths(from: Double, to: Double): List<Double> {
    val list = ArrayList<Double>()
    var z = from
    while (z < to) {
        list += z
        z += DEPTH_STEP + z * DEPTH_STEP_GROWTH
    }
    list += to
    return list
}
