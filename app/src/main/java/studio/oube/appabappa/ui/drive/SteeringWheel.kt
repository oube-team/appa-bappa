// 스티어링 휠 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.atan2

private const val GRID = 340f
private val Center = Offset(170f, 170f)

// 스티어링 휠 중심 가까이에서는 손가락이 조금만 움직여도 각도가 크게 변하므로, 중심에서 반지름의 20% 안쪽에서는
// 돌리지 않는다. 값은 그림 너비에 대한 비율이다.
private const val HUB_SHARE = 0.1f

private val Spoke = svgPath(
    "M22 150 C70 147 100 144 124 143 L216 143 C240 144 270 147 318 150 L318 188 " +
        "C270 191 240 197 216 201 L124 201 C100 197 70 191 22 188 Z"
)
private val LowSpoke =
    svgPath("M140 196 C147 232 151 272 152 320 L188 320 C189 272 193 232 200 196 Z")
private val SpokeTrim =
    svgPath("M58 151 C88 148 108 145.5 124 145 M282 151 C252 148 232 145.5 216 145")
private val SpokeBrush =
    Brush.verticalGradient(
        0f to Color(0xFF30363E),
        0.5f to Color(0xFF1F2328),
        1f to Color(0xFF14171B),
        startY = 143f,
        endY = 201f
    )
private val LowSpokeBrush = Brush.verticalGradient(
    0f to Color(0xFF1D2126),
    1f to Color(0xFF101215),
    startY = 196f,
    endY = 320f
)
private val TrimBrush =
    Brush.horizontalGradient(
        0f to Color(0x007A838E),
        0.3f to Color(0xCC7A838E),
        0.7f to Color(0xCC7A838E),
        1f to Color(0x007A838E),
        startX = 58f,
        endX = 282f
    )
private val HubBrush =
    ellipseGradient(
        Offset(112f + 0.42f * 116f, 124f + 0.34f * 94f),
        0.8f * 116f,
        0.8f * 94f,
        listOf(0f to Color(0xFF2C3239), 1f to Color(0xFF111317))
    )
private val RimShade =
    Brush.verticalGradient(
        0f to Color(0x12FFFFFF),
        0.45f to Color(0x00FFFFFF),
        0.6f to Color(0x00000000),
        1f to Color(0x4D000000),
        startY = 24f,
        endY = 316f
    )

// 손가락 하나로 잡고 돌리는 스티어링 휠이다. 손가락이 스티어링 휠 중심을 도는 각도만큼 돌리고(시계 방향이 오른쪽),
// 손을 떼면 가상의 차가 스티어링 휠을 가운데로 되돌린다.
@Composable
internal fun SteeringWheel(
    spot: Spot,
    steeringDeg: Double,
    onGrab: () -> Unit,
    onTurn: (Double) -> Unit,
    onRelease: () -> Unit
) {
    val grab by rememberUpdatedState(onGrab)
    val turn by rememberUpdatedState(onTurn)
    val release by rememberUpdatedState(onRelease)
    Canvas(
        Modifier
            .offset(spot.left, spot.top)
            .size(spot.widthDp)
            .semantics { contentDescription = "스티어링 휠" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val hub = size.width * HUB_SHARE
                    var last = angleOf(down.position - center, hub)
                    grab()
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull {
                                it.id == down.id
                            }
                            if (change == null || !change.pressed) break
                            val angle = angleOf(change.position - center, hub)
                            if (angle != null && last != null) turn(wrap(angle - last))
                            last = angle
                            change.consume()
                        }
                    } finally {
                        release()
                    }
                }
            }
    ) {
        val unit = size.width / GRID
        scale(unit, unit, Offset.Zero) {
            rotate(-steeringDeg.toFloat(), Center) { drawWheel() }
        }
    }
}

// 스티어링 휠 중심에서 본 손가락의 각도다. 손가락이 중심에서 hub 안쪽에 있으면 null을 반환한다.
private fun angleOf(offset: Offset, hub: Float): Double? =
    if (offset.getDistance() < hub) null else atan2(offset.y.toDouble(), offset.x.toDouble())

// 손가락이 스티어링 휠 중심의 왼쪽을 지날 때 각도가 한 바퀴만큼 뛰지 않도록, 각도 차이를 -π에서 π 사이 값으로 변환한다.
private fun wrap(delta: Double): Double = when {
    delta > PI -> delta - 2 * PI
    delta < -PI -> delta + 2 * PI
    else -> delta
}

private fun DrawScope.drawWheel() {
    drawPath(Spoke, SpokeBrush)
    drawPath(LowSpoke, LowSpokeBrush)
    drawPath(SpokeTrim, TrimBrush, style = Stroke(1.5f, cap = StrokeCap.Round))
    for (x in listOf(66f, 94f, 224f, 252f)) {
        drawRoundRect(Color(0xFF111317), Offset(x, 161f), Size(22f, 16f), CornerRadius(6f))
        drawRoundRect(
            Color(0xFF262B31),
            Offset(x, 161f),
            Size(22f, 16f),
            CornerRadius(6f),
            style = Stroke(1f)
        )
    }
    drawRoundRect(HubBrush, Offset(112f, 124f), Size(116f, 94f), CornerRadius(34f))
    drawRoundRect(
        Color(0x0FFFFFFF),
        Offset(112f, 124f),
        Size(116f, 94f),
        CornerRadius(34f),
        style = Stroke(1f)
    )
    drawRoundRect(
        Color(0xFF262B31),
        Offset(120f, 132f),
        Size(100f, 78f),
        CornerRadius(28f),
        style = Stroke(1.5f)
    )
    drawCircle(Color(0xFF191C21), 146f, Center, style = Stroke(28f))
    drawCircle(Color(0xFF5A616B), 132.5f, Center, style = Stroke(1.5f))
    drawCircle(Color(0xFF8A9099), 159.3f, Center, style = Stroke(2f))
    drawRoundRect(Color(0xFF8E97A2), Offset(166f, 13f), Size(8f, 24f), CornerRadius(3f))
    drawCircle(RimShade, 146f, Center, style = Stroke(28f))
}
