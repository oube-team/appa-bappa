// 앞차 그림의 위치와 크기(미터)는 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveSim

// 앞차 한 대를 놓을 자리와 크기다. 그림은 뒤에서 본 세단(가로 200 단위 = 1.86m)이고 바닥 가운데가 (100, 152)다.
// side는 우리가 옆 차로에 있을 때 보이는 앞차의 옆면이고, 앞차가 왼쪽에 있으면 오른쪽 옆면이 보인다(sideOnRight).
internal class LeadShape(
    val x: Float,
    val y: Float,
    val scale: Float,
    val braking: Boolean,
    val side: Path,
    val sideOnRight: Boolean
)

// AR 안내 표지판을 놓을 자리와 크기다. 그림은 24 단위이고, 가까워지면 옅어진다.
internal class SignPlacement(val x: Float, val y: Float, val scale: Float, val alpha: Float)

// 한 프레임의 도로 그림이다. 모든 path는 도로 그림 왼쪽 위를 원점으로 한 dp 좌표다. marks는 흰 선(차선, 가장자리 선,
// 정지선, 횡단보도)이다.
internal class RoadShapes {
    val shoulder = Path()
    val road = Path()
    val yellow = Path()
    val marks = Path()
    val ribbon = Path()
    val chevrons = Path()
    val posts = Path()
    val reflectors = Path()
    val signalPole = Path()
    val signalBox = Path()
    val signalLamp = Path()
    val leads = ArrayList<LeadShape>()
    var sign: SignPlacement? = null
}

// 가상의 차 상태로 이번 프레임의 도로 그림을 만든다. camera는 지금 달리는 도로를 기준으로 한 카메라이고, nowMillis는 AR
// 화살표가 흘러가는 위치를 정한다. 좌회전하는 동안에는 가로지르는 도로의 앞차도 그려서, 좌회전을 마쳤을 때 앞차가 갑자기
// 나타나지 않게 한다.
internal fun roadShapes(sim: DriveSim, camera: RoadCamera, nowMillis: Long): RoadShapes {
    val shapes = RoadShapes()
    val braking = sim.leadBraking(nowMillis)
    when (sim.guide) {
        null -> addWay(shapes, Way(camera, sim.travelledMeters))

        DriveGuide.MERGE -> addMergeRoad(shapes, Way(camera, sim.travelledMeters), sim.guideMeters)

        DriveGuide.LEFT_TURN -> {
            val cross = camera.onCrossRoad(sim.crossLateralMeters)
            addCrossroads(shapes, sim, camera, cross)
            if (sim.turnRad != null) addLead(shapes, cross, sim, sim.crossLateralMeters, braking)
        }
    }
    addGuide(shapes, camera, sim, nowMillis)
    addLead(shapes, camera, sim, sim.lateralMeters, braking)
    return shapes
}

// 앞차는 도로의 가운데 차로를 지킨다. lateral은 그 도로를 기준으로 한 우리 차의 옆 위치다. 좌회전하는 동안 앞차가
// 카메라의 옆이나 뒤로 가면(카메라 앞쪽으로 2m 안) 화면 밖이므로 그리지 않는다.
private fun addLead(
    shapes: RoadShapes,
    camera: RoadCamera,
    sim: DriveSim,
    lateral: Double,
    braking: Boolean
) {
    val gap = sim.leadGapMeters
    val lane = sim.leadLateralMeters
    val ahead = camera.distanceAhead(lane, gap)
    if (ahead < 2) return
    val bottom = camera.screen(lane, gap, 0.0)
    val scale = (camera.focal / ahead / 107.5).toFloat()
    val offset = lane - lateral
    val side = Path()
    if (abs(offset) > 0.9) {
        val edge = if (offset < 0) lane + 0.88 else lane - 0.88
        camera.side(side, edge, gap + 0.05, gap + 4.6, 0.32..0.84)
    }
    shapes.leads += LeadShape(bottom.x, bottom.y, scale, braking, side, offset < 0)
}
