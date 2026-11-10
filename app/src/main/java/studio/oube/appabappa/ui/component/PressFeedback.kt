package studio.oube.appabappa.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.theme.Pleos

private const val PRESSED_SCALE = 0.95f
private const val SHRINK_MILLIS = 100

private val RowPressOutset = 16.dp
private val RowPressRadius = 16.dp

// Pleos 디자인 가이드(Interaction)는 버튼과 터치할 수 있는 요소를 누르는 동안 95%로 축소하고 배경색을 변경한다고
// 명시한다. 축소하는 데 걸리는 시간은 가이드의 글에 없어서 0.1초로 정했다(판단). 누르는 영역까지 축소하면 가장자리를
// 누른 손가락이 영역 밖으로 나가 실행이 취소되므로, 누르는 동작(clickable 등) 뒤에 적용해 그림만 축소한다. 크기는
// 그리기 단계에서 조회해서 축소하는 동안 다시 구성하지 않는다.
@Composable
fun Modifier.pressShrink(pressed: Boolean): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(SHRINK_MILLIS),
        label = "press shrink"
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

// 내용이 창보다 길 때만 세로로 스크롤한다. Compose는 스크롤할 수 있는 영역 안의 터치가 스크롤인지 먼저 확인하느라 누른
// 표시를 약 0.1초 늦게 보여 주므로(에뮬레이터에서 측정), 내용이 창에 다 들어오면 스크롤을 비활성화해서 누른 표시를
// 바로 보여 준다.
@Composable
fun Modifier.scrollIfLong(): Modifier {
    val state = rememberScrollState()
    return verticalScroll(state, enabled = state.canScrollForward || state.canScrollBackward)
}

// 목록의 한 줄이다. Pleos에는 목록 컴포넌트가 없어서 줄 아래에 divider/secondary 선을 긋고, 누르는 동안 줄의 내용을
// 축소하며 button/basic/pressed 바탕을 보여 준다. 바탕이 글자에 붙지 않도록 줄보다 좌우로 16 넓게, 모서리 16으로
// 그린다. modifier에는 줄의 크기와 누르는 동작(clickable, toggleable)을 전달하고, 그 동작과 같은 interaction을
// 전달한다. 선과 누르는 영역은 축소하지 않는다.
@Composable
fun PleosListRow(
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gap: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit
) {
    val colors = Pleos.colors
    val pressed by interaction.collectIsPressedAsState()
    val highlight = colors.button.basic.pressed
    Box(modifier.lineBelow(colors.divider)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .pressShrink(pressed)
                .drawBehind {
                    if (pressed) {
                        val outset = RowPressOutset.toPx()
                        drawRoundRect(
                            highlight,
                            Offset(-outset, 0f),
                            Size(size.width + 2 * outset, size.height),
                            CornerRadius(RowPressRadius.toPx())
                        )
                    }
                }
                .padding(contentPadding),
            content = content
        )
    }
}
