package studio.oube.appabappa.ui.drive

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

// Android는 BlurMaskFilter의 반지름을 표준편차(반지름 x 0.57735 + 0.5, Skia의 변환식)로 변환해 번지게 한다.
// 그림 데이터의 번짐 값은 SVG feGaussianBlur의 표준편차이므로, 같은 번짐이 되도록 거꾸로 계산한 반지름을 사용한다.
private const val SIGMA_PER_RADIUS = 0.57735f
private const val SIGMA_OFFSET = 0.5f

// SVG의 feGaussianBlur(stdDeviation = sigma)처럼 번지게 칠하는 붓이다. 번짐은 캔버스 변환을 따라 커지고 작아진다.
internal class BlurBrush(sigma: Float) {
    private val paint = Paint()

    init {
        paint.asFrameworkPaint().maskFilter =
            BlurMaskFilter((sigma - SIGMA_OFFSET) / SIGMA_PER_RADIUS, BlurMaskFilter.Blur.NORMAL)
    }

    fun fill(scope: DrawScope, path: Path, color: Color) {
        paint.style = PaintingStyle.Fill
        paint.color = color
        scope.drawIntoCanvas { it.drawPath(path, paint) }
    }

    fun stroke(scope: DrawScope, path: Path, color: Color, width: Float) {
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = width
        paint.strokeCap = StrokeCap.Round
        paint.color = color
        scope.drawIntoCanvas { it.drawPath(path, paint) }
    }
}

// 그림 데이터의 좌표(dp)로 그리도록 캔버스를 화면 밀도만큼 키운다. block에는 dp 단위의 그림 너비와 높이를 전달한다.
internal fun DrawScope.inDesignUnits(block: DrawScope.(width: Float, height: Float) -> Unit) {
    val width = size.width / density
    val height = size.height / density
    scale(density, density, Offset.Zero) { block(width, height) }
}

// SVG의 그룹 opacity처럼, 겹치는 그림을 한 장으로 합친 뒤 투명도를 적용한다.
internal fun DrawScope.withAlpha(bounds: Rect, alpha: Float, draw: DrawScope.() -> Unit) {
    if (alpha <= 0f) return
    val layer = Paint().also { it.alpha = alpha }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, layer)
        draw()
        canvas.restore()
    }
}

// SVG의 objectBoundingBox 방사형 그라데이션처럼, 가로와 세로 반지름이 다른 타원형 그라데이션이다.
internal fun ellipseGradient(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    stops: List<Pair<Float, Color>>
): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val colors = stops.map { it.second }
        val offsets = stops.map { it.first }
        val shader = RadialGradientShader(center, radiusX, colors, offsets)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(1f, radiusY / radiusX, center.x, center.y)
            }
        )
        return shader
    }
}

// SVG path 문자열을 Compose Path로 변환한다.
internal fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

// 점들을 차례로 이은 닫힌 다각형이다.
internal fun polygon(vararg points: Pair<Float, Float>): Path = polygonOf(points.asList())

internal fun polygonOf(points: List<Pair<Float, Float>>): Path = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    close()
}
