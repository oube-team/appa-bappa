// 도로 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import studio.oube.appabappa.demo.DriveSim

// 지평선 높이와 도로 그림 높이로 정해지는 그라데이션과 번짐 붓이다. 배치가 바뀔 때만 다시 만든다.
private class RoadPaint(layout: DriveLayout) {
    private val top = layout.horizon
    private val bottom = layout.roadHeight
    val hills = Hills(layout.horizon, layout.sceneryQuarter)
    val sky =
        Brush.verticalGradient(
            0f to Color(0xFF070D1A),
            0.5f to Color(0xFF16243C),
            0.82f to Color(0xFF344A6A),
            1f to Color(0xFF5F7696),
            startY = 0f,
            endY = top + 4
        )
    val haze =
        Brush.verticalGradient(
            0f to Color(0x007C91AE),
            0.55f to Color(0x737C91AE),
            1f to Color(0x007C91AE),
            startY = top - 46,
            endY = top + 46
        )
    val ground =
        Brush.verticalGradient(
            0f to Color(0xFF36465E),
            0.22f to Color(0xFF1D2636),
            1f to Color(0xFF0E131B),
            startY = top,
            endY = bottom
        )
    val shoulder =
        Brush.verticalGradient(
            0f to Color(0xFF55627A),
            0.14f to Color(0xFF353F4E),
            1f to Color(0xFF20252E),
            startY = top,
            endY = bottom
        )
    val road =
        Brush.verticalGradient(
            0f to Color(0xFF4A566B),
            0.14f to Color(0xFF2B3442),
            1f to Color(0xFF171C24),
            startY = top,
            endY = bottom
        )
    val mark =
        Brush.verticalGradient(
            0f to Color(0xFF7D8AA0),
            0.16f to Color(0xFFC3CAD5),
            1f to Color(0xFFF4F6F9),
            startY = top,
            endY = bottom
        )
    val yellow =
        Brush.verticalGradient(
            0f to Color(0xFF7C6D48),
            0.16f to Color(0xFFC9A23F),
            1f to Color(0xFFF2C230),
            startY = top,
            endY = bottom
        )
    val ribbon =
        Brush.verticalGradient(
            0f to Color(0x0035A8FF),
            0.22f to Color(0x3835A8FF),
            1f to Color(0x8035A8FF),
            startY = top,
            endY = bottom
        )
    val chevron =
        Brush.verticalGradient(
            0f to Color(0x00FFFFFF),
            0.2f to Color(0x73FFFFFF),
            1f to Color(0xF2FFFFFF),
            startY = top,
            endY = bottom
        )
    val post =
        Brush.verticalGradient(
            0f to Color(0xFF8C97A8),
            1f to Color(0xFF5C6574),
            startY = top,
            endY = top + 134
        )
    val glow = BlurBrush(5f)
    val wideGlow = BlurBrush(9f)
    val lead = LeadCarPaint()

    // 전조등 빛은 화면 아래 바깥에 중심을 둔 타원(가로 반지름은 그림 너비의 0.48배, 세로 반지름은 300)이라 위쪽
    // 가장자리만 은은하게 보인다. 그라데이션은 그림 너비마다 한 번만 만든다.
    val beamTop: Float = bottom + 40 - 300
    private val beams = HashMap<Float, Brush>()

    fun beam(width: Float): Brush = beams.getOrPut(width) {
        ellipseGradient(
            Offset(width / 2, bottom + 40 + 300),
            width * 0.48f * 1.5f,
            300f * 1.5f,
            listOf(0f to Color(0x33FFF1D6), 0.55f to Color(0x0FFFF1D6), 1f to Color(0x00FFF1D6))
        )
    }
}

// 앞유리 너머 도로 그림이다. 세 겹으로 나눠 각 겹을 별도의 그래픽 레이어로 그려서, 바뀐 겹만 다시 그린다. 하늘과 산은
// 앞유리 풍경의 각도(sceneryRad)가 바뀔 때, 도로와 앞차와 안내선은 도로 그림을 변경한 프레임(frameMillis)마다, 앞유리 그늘과
// 테두리는 계기판 조명 띠가 앞유리에 비친 빛(light)이 바뀔 때 다시 그린다. 이 값들은 그리기 단계에서 조회한다.
// avoid는 앞유리 위에 놓인 판단 카드의 자리(px)로, AR 표지판이 카드 글자 뒤로 비치지 않게 하는 데 사용한다.
// 머리 위로 지나가는 신호등처럼 앞유리 밖으로 나가는 그림이 시연 중 띠를 덮지 않도록, 도로 겹은 그림 영역 안만 그린다.
@Composable
internal fun RoadView(
    sim: DriveSim,
    frameMillis: () -> Long,
    sceneryRad: () -> Double,
    avoid: () -> Rect,
    layout: DriveLayout,
    light: () -> DashLight,
    modifier: Modifier = Modifier
) {
    val paint = remember(layout) { RoadPaint(layout) }
    Box(modifier.fillMaxWidth().height(layout.roadHeight.dp)) {
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            val turned = sceneryRad()
            inDesignUnits { width, _ -> drawScenery(width, layout, paint, turned) }
        }
        Canvas(Modifier.matchParentSize().graphicsLayer(clip = true)) {
            val now = frameMillis()
            inDesignUnits { width, _ ->
                val camera = RoadCamera(width, layout, sim.lateralMeters, sim.yawRad)
                val shapes = roadShapes(sim, camera, now)
                drawRoad(shapes, paint, width)
                drawRoadside(shapes, paint)
                shapes.leads.forEach { drawLeadCar(it, paint.lead) }
                val card = avoid().let {
                    Rect(
                        it.left / density,
                        it.top / density,
                        it.right / density,
                        it.bottom / density
                    )
                }
                shapes.sign?.let { sign ->
                    sim.navGuide?.let { drawGuideSign(sign, it, paint.glow, card) }
                }
            }
        }
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            inDesignUnits { width, _ -> drawWindshield(width, layout, light()) }
        }
    }
}

// 밤하늘, 산, 안개, 땅이다. 산은 앞유리 풍경의 각도(sceneryRad)에 맞춰 옆으로 옮긴다.
private fun DrawScope.drawScenery(
    width: Float,
    layout: DriveLayout,
    paint: RoadPaint,
    sceneryRad: Double
) {
    val top = layout.horizon
    val turns = sceneryRad / (2 * PI)
    drawRect(paint.sky, Offset.Zero, Size(width, top + 4))
    drawHills(paint.hills.far, turns, FarHillColors)
    drawHills(paint.hills.near, turns, NearHillColors)
    drawRect(paint.haze, Offset(0f, top - 46), Size(width, 92f))
    drawRect(paint.ground, Offset(0f, top), Size(width, layout.roadHeight - top))
}

// 산 한 겹을 그린다. turns는 차가 왼쪽으로 돈 바퀴 수이고, 왼쪽으로 돌면 산이 오른쪽으로 움직인다. 산은 한 바퀴를 두른
// 그림이라, 한 바퀴 너비만큼 왼쪽에 한 번 더 그려서 끊긴 자리가 보이지 않게 한다.
private fun DrawScope.drawHills(layer: HillLayer, turns: Double, colors: HillColors) {
    val shift = (turns * layer.round).mod(layer.round.toDouble()).toFloat()
    for (left in listOf(shift - layer.round, shift)) {
        translate(left = left) {
            drawPath(layer.base, colors.base)
            drawPath(layer.light, colors.light)
            drawPath(layer.shade, colors.shade)
        }
    }
}

// 갓길, 도로, 차선, 전조등 빛, 내비게이션 안내선이다.
private fun DrawScope.drawRoad(shapes: RoadShapes, paint: RoadPaint, width: Float) {
    drawPath(shapes.shoulder, paint.shoulder)
    drawPath(shapes.road, paint.road)
    drawPath(shapes.yellow, paint.yellow)
    drawPath(shapes.marks, paint.mark)
    val beamLeft = width / 2 - width * 0.48f
    drawOval(paint.beam(width), Offset(beamLeft, paint.beamTop), Size(width * 0.96f, 600f))
    paint.wideGlow.fill(this, shapes.ribbon, Color(0x5935A8FF))
    drawPath(shapes.ribbon, paint.ribbon)
    drawPath(shapes.chevrons, paint.chevron)
}

// 반사 기둥과 교차로 신호등이다. 반사판과 켜진 신호는 번지는 빛을 먼저 칠하고 그 위에 본래 색을 칠한다.
private fun DrawScope.drawRoadside(shapes: RoadShapes, paint: RoadPaint) {
    drawPath(shapes.posts, paint.post)
    paint.glow.fill(this, shapes.reflectors, Color(0xCCFFB547))
    drawPath(shapes.reflectors, Color(0xFFFFC56B))
    drawPath(shapes.signalPole, Color(0xFF0E1218))
    drawPath(shapes.signalBox, Color(0xFF080A0E))
    paint.glow.fill(this, shapes.signalLamp, Color(0xE63CFF8F))
    drawPath(shapes.signalLamp, Color(0xFF8CFFC0))
}

// 산 한 겹의 바탕, 밝은 비탈, 어두운 비탈 색이다.
private class HillColors(base: Long, light: Long, shade: Long) {
    val base = Color(base)
    val light = Color(light)
    val shade = Color(shade)
}

private val FarHillColors = HillColors(0xFF3C4F6E, 0xFF465A7A, 0xFF334566)
private val NearHillColors = HillColors(0xFF222E42, 0xFF29364C, 0xFF1B2536)

// 지평선 위로 솟은 산 한 겹이다. start부터 step 간격으로 heights 높이의 꼭짓점을 잇고, 봉우리마다 왼쪽 비탈은
// 밝게(lightRun), 오른쪽 비탈은 어둡게(shadeRun) 칠한다. run은 비탈 그늘이 지평선에 닿는 곳까지의 거리다. 산은 한
// 바퀴(round)를 두른 그림이라, 마지막 꼭짓점 뒤에 첫 꼭짓점을 다시 이어서 끝과 처음이 맞닿게 한다.
private class HillLayer(
    horizon: Float,
    start: Float,
    step: Float,
    heights: List<Int>,
    lightRun: Float,
    shadeRun: Float
) {
    val round = step * heights.size
    val base = Path()
    val light = Path()
    val shade = Path()

    init {
        val around = heights + heights.first()
        val points = around.mapIndexed { i, h -> Offset(start + i * step, horizon - h) }
        base.moveTo(points.first().x, horizon)
        points.forEach { base.lineTo(it.x, it.y) }
        base.lineTo(points.last().x, horizon)
        base.close()
        for (i in 1 until points.lastIndex) {
            val before = points[i - 1]
            val peak = points[i]
            val after = points[i + 1]
            if (peak.y < before.y && peak.y < after.y) {
                val lightFoot = peak.x - lightRun to horizon
                val shadeFoot = peak.x + shadeRun to horizon
                light.addPath(polygon(before.x to before.y, peak.x to peak.y, lightFoot))
                shade.addPath(polygon(peak.x to peak.y, after.x to after.y, shadeFoot))
            }
        }
    }
}

// 먼 산과 가까운 산 두 겹이다. quarter는 차가 90도를 돌 때 산이 옆으로 움직이는 거리이고, 두 겹 모두 그 네 배가 한
// 바퀴가 되도록 꼭짓점 수를 정한다. 그래서 좌회전할 때마다 다른 산이 보이고, 네 번 돌면 처음 산으로 돌아온다.
private class Hills(horizon: Float, quarter: Float) {
    val far = HillLayer(horizon, -760f, 80f, FarHeights.take((4 * quarter / 80).toInt()), 18f, 14f)
    val near =
        HillLayer(horizon, -700f, 120f, NearHeights.take((4 * quarter / 120).toInt()), 26f, 20f)
}

// 산 꼭짓점의 높이다. 골짜기와 봉우리가 번갈아 나오고, 가로 창은 전부(72개와 48개), 세로 창은 앞의 48개와 32개를
// 사용한다.
private val FarHeights =
    listOf(
        24, 52, 30, 66, 34, 48, 22, 58, 36, 70, 28, 54, 32, 62, 26, 50, 38, 72, 30, 56,
        24, 64, 34, 52, 28, 68, 36, 58, 22, 50, 32, 60, 26, 54, 30, 66, 24, 48, 34, 62,
        28, 64, 32, 50, 26, 68, 36, 56, 22, 60, 30, 72, 34, 52, 28, 66, 24, 58, 38, 48,
        30, 70, 26, 54, 34, 62, 22, 50, 36, 68, 28, 58
    )

private val NearHeights =
    listOf(
        18, 40, 22, 54, 16, 46, 24, 60, 20, 42, 14, 50, 26, 58, 18, 44, 22, 56, 16, 48,
        24, 52, 20, 46, 18, 60, 22, 48, 16, 56, 24, 42, 20, 52, 14, 46, 22, 58, 18, 44,
        26, 54, 16, 50, 20, 60, 24, 42
    )
