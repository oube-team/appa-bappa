// 계기판 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import studio.oube.appabappa.engine.TurnSignal

// 계기판 그림 가운데 그림 크기에 따라 정해지는 모양과 붓이다. 조명 색이 변하는 동안과 레버가 움직이는 동안에는 프레임마다
// 다시 그리므로, 크기가 같으면 다시 생성하지 않는다.
private class DashFrame(val width: Float, val height: Float) {
    val panel =
        Brush.verticalGradient(
            0f to Color(0xFF121418),
            1f to Color(0xFF07080A),
            startY = 0f,
            endY = height
        )
    val pad = polygon(0f to 0f, width to 0f, width to 42f, width / 2 to 34f, 0f to 42f)
    val padBrush =
        Brush.verticalGradient(
            0f to Color(0xFF1A1E23),
            1f to Color(0xFF111317),
            startY = 0f,
            endY = 42f
        )
    val trim = Path().apply {
        moveTo(120f, 58f)
        quadraticTo(width / 2, 46f, width - 120, 58f)
    }
    val trimBrush =
        Brush.horizontalGradient(
            0f to Color(0x002A2F36),
            0.12f to Color(0xFF2A2F36),
            0.88f to Color(0xFF2A2F36),
            1f to Color(0x002A2F36),
            startX = 120f,
            endX = width - 120
        )
    val doorLeft = Path().apply {
        moveTo(0f, 0f)
        lineTo(16f, 0f)
        quadraticTo(52f, 4f, 54f, 56f)
        cubicTo(56f, height * 0.34f, 58f, height * 0.67f, 60f, height)
        lineTo(0f, height)
        close()
    }
    val doorLeftBrush =
        Brush.horizontalGradient(
            0f to Color(0xFF07080A),
            1f to Color(0xFF14171B),
            startX = 0f,
            endX = 60f
        )
    val doorRight = Path().apply {
        moveTo(width, 0f)
        lineTo(width - 16, 0f)
        quadraticTo(width - 52, 4f, width - 54, 56f)
        cubicTo(width - 56, height * 0.34f, width - 58, height * 0.67f, width - 60, height)
        lineTo(width, height)
        close()
    }
    val doorRightBrush =
        Brush.horizontalGradient(
            0f to Color(0xFF14171B),
            1f to Color(0xFF07080A),
            startX = width - 60,
            endX = width
        )
    val strip = Path().apply {
        moveTo(26f, height - 10)
        cubicTo(24f, height * 0.68f, 22f, height * 0.36f, 21f, 92f)
        quadraticTo(20f, 32f, 70f, 24f)
        cubicTo(width * 0.3f, 12f, width * 0.7f, 12f, width - 70, 24f)
        quadraticTo(width - 20, 32f, width - 21, 92f)
        cubicTo(width - 22, height * 0.36f, width - 24, height * 0.68f, width - 26, height - 10)
    }
}

// 배치에 따라 정해지는 번짐 붓, 그림자, 레버의 붓이다.
private class DashPaint(layout: DriveLayout) {
    val spill = BlurBrush(24f)
    val glow = BlurBrush(7f)
    val soft = BlurBrush(10f)

    private val wheel = layout.wheel
    private val shadowY = layout.brake.y + layout.brake.height + 6
    val wheelShadow =
        oval(wheel.x + wheel.width / 2, wheel.y + wheel.height + 10, wheel.width * 0.44f, 20f)
    val brakeShadow =
        oval(layout.brake.x + layout.brake.width / 2, shadowY, layout.brake.width * 0.49f, 10f)
    val accelShadow =
        oval(layout.accel.x + layout.accel.width / 2, shadowY, layout.accel.width / 2, 10f)

    // 방향지시등 레버는 스티어링 휠 중심보다 조금 왼쪽 위를 축(pivot)으로 돌고, 손잡이(knob)는 방향지시등 버튼 옆에 있다.
    val pivot = Offset(wheel.x + wheel.width * 0.41f, wheel.y + wheel.height * 0.465f)
    val knobX = layout.turn.x + layout.turn.width + 18
    val leverBrush =
        Brush.verticalGradient(
            0f to Color(0xFF727A84),
            0.5f to Color(0xFF2E333A),
            1f to Color(0xFF4C535C),
            startY = pivot.y - 7,
            endY = pivot.y + 7
        )
    val knobBrush =
        Brush.radialGradient(
            0f to Color(0xFF8A929C),
            1f to Color(0xFF3A4048),
            center = Offset(knobX - 3, pivot.y - 4.5f),
            radius = 21f
        )

    private var frame: DashFrame? = null

    fun frame(width: Float, height: Float): DashFrame {
        val kept = frame?.takeIf { it.width == width && it.height == height }
        return kept ?: DashFrame(width, height).also { frame = it }
    }
}

// 계기판 바탕, 문 안쪽, 조명 띠, 스티어링 휠과 페달 그림자, 방향지시등 레버다. 레버는 오른쪽을 켜면 위로, 왼쪽을 켜면
// 아래로 기운다. 조명 색(light)과 레버 각도는 그리기 단계에서 조회해서, 값이 변하는 동안 다시 구성하지 않고 다시 그리기만
// 한다.
@Composable
internal fun DashBoard(
    layout: DriveLayout,
    light: () -> DashLight,
    turn: TurnSignal,
    modifier: Modifier = Modifier
) {
    val leverTarget =
        when (turn) {
            TurnSignal.RIGHT -> 8f
            TurnSignal.LEFT -> -8f
            TurnSignal.OFF -> 0f
        }
    val lever = animateFloatAsState(leverTarget, tween(200), label = "turn lever")
    val paint = remember(layout) { DashPaint(layout) }
    Canvas(modifier.fillMaxSize()) {
        inDesignUnits { width, height ->
            val frame = paint.frame(width, height)
            drawPanel(frame)
            drawDoors(frame)
            drawLightStrip(frame, light(), paint)
            drawShadows(paint)
            drawLever(paint, lever.value)
        }
    }
}

private fun DrawScope.drawPanel(frame: DashFrame) {
    drawRect(frame.panel, Offset.Zero, Size(frame.width, frame.height))
    drawPath(frame.pad, frame.padBrush)
    drawPath(frame.trim, frame.trimBrush, style = Stroke(2f))
}

// 운전석과 조수석 문의 안쪽 면이다. 계기판 양 끝을 덮는다.
private fun DrawScope.drawDoors(frame: DashFrame) {
    drawPath(frame.doorLeft, frame.doorLeftBrush)
    drawPath(frame.doorRight, frame.doorRightBrush)
}

// 문과 계기판을 따라 놓인 실내 조명 띠다. 넓게 번지는 빛, 띠 둘레 빛, 가운데 선 순서로 칠한다.
private fun DrawScope.drawLightStrip(frame: DashFrame, light: DashLight, paint: DashPaint) {
    paint.spill.stroke(this, frame.strip, light.color.copy(alpha = light.spill), 80f)
    paint.glow.stroke(this, frame.strip, light.color.copy(alpha = light.glow), 14f)
    drawPath(frame.strip, light.core, style = Stroke(3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawShadows(paint: DashPaint) {
    paint.soft.fill(this, paint.wheelShadow, Color.Black.copy(alpha = 0.6f))
    paint.soft.fill(this, paint.brakeShadow, Color.Black.copy(alpha = 0.7f))
    paint.soft.fill(this, paint.accelShadow, Color.Black.copy(alpha = 0.7f))
}

// 스티어링 휠 뒤에서 왼쪽으로 뻗은 방향지시등 레버다.
private fun DrawScope.drawLever(paint: DashPaint, degrees: Float) {
    val pivot = paint.pivot
    val knobX = paint.knobX
    rotate(degrees, pivot) {
        drawRoundRect(
            paint.leverBrush,
            Offset(knobX, pivot.y - 7),
            Size(pivot.x - knobX, 14f),
            CornerRadius(7f)
        )
        drawCircle(paint.knobBrush, 15f, Offset(knobX, pivot.y))
        drawCircle(Color.Black.copy(alpha = 0.4f), 15f, Offset(knobX, pivot.y), style = Stroke(1f))
    }
}

private fun oval(centerX: Float, centerY: Float, radiusX: Float, radiusY: Float): Path =
    Path().apply {
        addOval(Rect(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY))
    }
