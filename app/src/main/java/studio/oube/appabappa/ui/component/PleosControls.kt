package studio.oube.appabappa.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.level1Shadow

private const val KNOB_MOVE_MILLIS = 150

// Pleos Switch(Medium 80 x 48, 모서리 16, 안쪽 여백 8)다. 손잡이(32, 모서리 8)에는 Level 1 그림자가 있다.
// 켜고 끄는 동작은 설정 줄 전체가 처리하므로, 이 컴포저블은 모양만 그린다.
@Composable
fun PleosSwitch(checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = Pleos.colors.switch
    val track =
        when {
            !enabled -> colors.disabled
            checked -> colors.on
            else -> colors.off
        }
    val knob =
        when {
            !enabled -> colors.knobDisabled
            checked -> colors.knobOn
            else -> colors.knobOff
        }
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 32.dp else 0.dp,
        animationSpec = tween(KNOB_MOVE_MILLIS),
        label = "switch knob"
    )
    val knobShape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .size(80.dp, 48.dp)
            .background(track, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobOffset.roundToPx(), 0) }
                .size(32.dp)
                .level1Shadow(knobShape)
                .background(knob, knobShape)
        )
    }
}

// Pleos Tabs(Solid, Medium)다. 트랙은 높이 80(모서리 24, 안쪽 여백 8)이고 세 칸일 때 너비는 780이다(창이 더 좁으면
// 창 너비). 고른 칸은 비활성이어도 button/switch 바탕과 Level 1 그림자를 유지하고, 고르지 않은 칸끼리만 2 x 36
// 구분선으로 나눈다.
@Composable
fun PleosSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = Pleos.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .widthIn(max = 780.dp)
            .fillMaxWidth()
            .height(80.dp)
            .background(colors.tabBackground, RoundedCornerShape(24.dp))
            .padding(8.dp)
            .selectableGroup()
    ) {
        options.forEachIndexed { index, label ->
            if (index > 0) {
                val shown = index - 1 != selected && index != selected
                Box(
                    Modifier
                        .size(2.dp, 36.dp)
                        .background(if (shown) colors.divider else Color.Transparent)
                )
            }
            Segment(label, index == selected, enabled, { onSelect(index) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    val background =
        when {
            selected && pressed -> colors.button.switch.pressed
            selected -> colors.button.switch.enabled
            pressed -> colors.button.basic.pressed
            else -> Color.Transparent
        }
    val labelColor =
        when {
            !enabled -> colors.text.quaternary
            selected -> colors.text.primary
            else -> colors.text.secondary
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(64.dp)
            .then(if (selected) Modifier.level1Shadow(shape) else Modifier)
            .background(background, shape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 18.dp)
    ) {
        PleosText(label, PleosType.bodyLarge.strong, labelColor, maxLines = 1)
    }
}
