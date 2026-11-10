package studio.oube.appabappa.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private val LocalPleosColors = staticCompositionLocalOf { LightColors }

@Composable
fun PleosTheme(night: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPleosColors provides if (night) DarkColors else LightColors,
        content = content
    )
}

object Pleos {
    val colors: PleosColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPleosColors.current
}

// Pleos 그림자 스타일 Shadow/Light/Level1(Y 6, Blur 24, 검정 8%)이다. 탭의 고른 칸과 스위치 손잡이에 사용한다.
fun Modifier.level1Shadow(shape: Shape = RectangleShape): Modifier = dropShadow(
    shape,
    Shadow(radius = 24.dp, color = Color.Black.copy(alpha = 0.08f), offset = DpOffset(0.dp, 6.dp))
)

// Material의 Text를 사용하지 않으므로, Pleos 글자 스타일과 색을 함께 전달받는 글자 컴포저블을 둔다.
@Composable
fun PleosText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        overflow = TextOverflow.Ellipsis,
        softWrap = softWrap,
        maxLines = maxLines
    )
}
