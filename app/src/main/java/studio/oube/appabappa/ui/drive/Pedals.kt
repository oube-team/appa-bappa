// 페달 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val PRESS_MILLIS = 120

// 알루미늄 페달 그림이다. width와 height는 그림 단위의 크기이고, 화면 자리에 비율을 지켜 가운데 맞춘다. 금속 빛은
// 페달 윤곽의 위 끝에서 아래 끝까지 칠한다.
internal class PedalShape(
    val width: Float,
    val height: Float,
    outline: String,
    highlight: String,
    grooves: List<RoundRect>
) {
    val outline: Path = svgPath(outline)
    val highlight: Path = svgPath(highlight)
    val grooves: Path = Path().apply { grooves.forEach { addRoundRect(it) } }
    val metal: Brush =
        this.outline.getBounds().let { bounds ->
            Brush.verticalGradient(
                0f to Color(0xFFB4BCC5),
                0.45f to Color(0xFF7C858F),
                1f to Color(0xFF555D67),
                startY = bounds.top,
                endY = bounds.bottom
            )
        }
}

internal val BrakePedal =
    PedalShape(
        150f,
        118f,
        "M16 6 L134 6 Q146 6 145 18 L140 102 Q139 112 128 112 L22 112 Q11 112 10 102 " +
            "L5 18 Q4 6 16 6 Z",
        "M16 7 L134 7",
        listOf(
            RoundRect(20f, 28f, 130f, 38f, CornerRadius(5f)),
            RoundRect(22f, 50f, 128f, 60f, CornerRadius(5f)),
            RoundRect(24f, 72f, 126f, 82f, CornerRadius(5f))
        )
    )

internal val AccelPedal =
    PedalShape(
        96f,
        158f,
        "M18 6 L78 6 Q90 6 90 18 L88 140 Q88 152 76 152 L20 152 Q8 152 8 140 L6 18 Q6 6 18 6 Z",
        "M18 7 L78 7",
        listOf(
            RoundRect(24f, 24f, 33f, 136f, CornerRadius(4.5f)),
            RoundRect(43f, 24f, 52f, 136f, CornerRadius(4.5f)),
            RoundRect(62f, 24f, 71f, 136f, CornerRadius(4.5f))
        )
    )

// 누르고 있는 동안 동작하는 페달이다. 누르면 6만큼 내려가고 어두워진다. 급제동 중인 브레이크는 붉은 빛이 번진다.
// 손가락이 페달 밖으로 나가도 뗄 때까지 누른 것으로 판단한다.
@Composable
internal fun Pedal(
    spot: Spot,
    shape: PedalShape,
    pressed: Boolean,
    hardBraking: Boolean,
    description: String,
    onPress: (Boolean) -> Unit
) {
    val press by rememberUpdatedState(onPress)
    val shift by animateDpAsState(if (pressed) 6.dp else 0.dp, tween(PRESS_MILLIS), label = "pedal")
    val glow = remember { BlurBrush(8f) }
    Canvas(
        Modifier
            .offset { IntOffset(spot.left.roundToPx(), (spot.top + shift).roundToPx()) }
            .size(spot.widthDp, spot.heightDp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    press(true)
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        press(false)
                    }
                }
            }
    ) {
        val unit = min(size.width / shape.width, size.height / shape.height)
        translate((size.width - shape.width * unit) / 2, (size.height - shape.height * unit) / 2) {
            scale(unit, unit, Offset.Zero) {
                if (hardBraking) glow.fill(this, shape.outline, Color(0xCCFF3B30))
                drawPath(shape.outline, shape.metal)
                drawPath(
                    shape.highlight,
                    Color(0x73FFFFFF),
                    style = Stroke(2f, cap = StrokeCap.Round)
                )
                drawPath(shape.grooves, Color(0xFF16191D))
                // CSS의 brightness 필터처럼, 불투명한 그림 위에 반투명한 검은색을 칠해 어둡게 한다.
                val dim =
                    when {
                        hardBraking -> 0.08f
                        pressed -> 0.2f
                        else -> 0f
                    }
                if (dim > 0f) drawPath(shape.outline, Color.Black.copy(alpha = dim))
            }
        }
    }
}
