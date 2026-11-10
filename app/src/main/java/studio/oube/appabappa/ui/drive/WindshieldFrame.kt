// 앞유리 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// 앞유리의 그늘, 반사, 기둥, 룸미러, 계기판 윗면이다. 앞유리 아래쪽에는 계기판 조명 띠의 빛이 비친다.
internal fun DrawScope.drawWindshield(width: Float, layout: DriveLayout, light: DashLight) {
    val height = layout.roadHeight
    drawRect(
        ellipseGradient(
            Offset(width / 2, height * 0.55f),
            width * 0.75f,
            height * 0.75f,
            listOf(0.6f to Color.Black.copy(alpha = 0f), 1f to Color.Black.copy(alpha = 0.45f))
        ),
        Offset.Zero,
        Size(width, height)
    )
    drawPath(
        polygon(
            width * 0.088f to 10f,
            width * 0.445f to 10f,
            width * 0.193f to height,
            width * 0.026f to height
        ),
        Color.White.copy(alpha = 0.03f)
    )
    drawRect(
        Brush.verticalGradient(
            0f to light.color.copy(alpha = 0f),
            1f to light.color.copy(alpha = light.reflect),
            startY = height - 80,
            endY = height
        ),
        Offset(0f, height - 80),
        Size(width, 80f)
    )
    drawRect(Color(0xFF030405), Offset.Zero, Size(width, 12f))
    drawPillars(width, layout)
    if (layout.showMirror) drawMirror(width)
    drawDashTop(width, height)
}

private fun DrawScope.drawPillars(width: Float, layout: DriveLayout) {
    val height = layout.roadHeight
    val top = layout.pillarTop
    val bottom = layout.pillarBottom
    drawPath(
        polygon(0f to 0f, top to 0f, bottom to height, 0f to height),
        Brush.horizontalGradient(
            0f to Color(0xFF030405),
            1f to Color(0xFF11151B),
            startX = 0f,
            endX = top
        )
    )
    drawPath(
        polygon(width to 0f, width - top to 0f, width - bottom to height, width to height),
        Brush.horizontalGradient(
            0f to Color(0xFF11151B),
            1f to Color(0xFF030405),
            startX = width - top,
            endX = width
        )
    )
    val edges =
        Path().apply {
            moveTo(top, 12f)
            lineTo(bottom, height)
            moveTo(width - top, 12f)
            lineTo(width - bottom, height)
        }
    drawPath(edges, Color.White.copy(alpha = 0.07f), style = Stroke(2f))
}

// 가로 창에만 보이는 룸미러다.
private fun DrawScope.drawMirror(width: Float) {
    drawRect(Color(0xFF0B0D10), Offset(width / 2 - 7, 0f), Size(14f, 24f))
    val topLeft = Offset(width / 2 - 85, 18f)
    val mirrorSize = Size(170f, 46f)
    drawRoundRect(
        Brush.verticalGradient(
            0f to Color(0xFF1C222A),
            1f to Color(0xFF090B0E),
            startY = 18f,
            endY = 64f
        ),
        topLeft,
        mirrorSize,
        CornerRadius(14f)
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.08f),
        topLeft,
        mirrorSize,
        CornerRadius(14f),
        style = Stroke(1f)
    )
}

private fun DrawScope.drawDashTop(width: Float, height: Float) {
    val edge =
        listOf(
            0f to height - 20,
            width * 0.176f to height - 27,
            width / 2 to height - 31,
            width * 0.824f to height - 27,
            width to height - 20
        )
    drawPath(
        polygonOf(listOf(0f to height) + edge + listOf(width to height)),
        Brush.verticalGradient(
            0f to Color(0xFF101318),
            1f to Color(0xFF1A1E23),
            startY = height - 31,
            endY = height
        )
    )
    val line = Path()
    edge.forEachIndexed { i, (x, y) -> if (i == 0) line.moveTo(x, y) else line.lineTo(x, y) }
    drawPath(line, Color.White.copy(alpha = 0.06f), style = Stroke(1.5f))
}
