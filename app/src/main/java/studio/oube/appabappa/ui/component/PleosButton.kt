package studio.oube.appabappa.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType

// Pleos Basic Button의 Type이다. Primary는 회색 바탕(button/basic), Secondary는 진한 바탕(button/filled)이다.
enum class ButtonKind {
    BASIC,
    FILLED
}

// Pleos Basic Button의 크기다(form_factor connect-s 값).
enum class ButtonSize(val height: Dp, val radius: Dp, val paddingHorizontal: Dp) {
    MEDIUM(80.dp, 24.dp, 32.dp),
    SMALL(64.dp, 16.dp, 24.dp);

    val label: TextStyle
        get() = if (this == MEDIUM) PleosType.bodyLarge.strong else PleosType.bodySmall.strong
}

// 비활성 버튼은 Pleos 규격대로 바탕에 2px divider/secondary 테두리를 두르고(buttons/basic/disabled_border), 글자는
// text/dark/quaternary로 흐리게 한다. 누르는 동안에는 바탕을 pressed 토큰으로 변경하고 물결 효과는 사용하지 않는다.
// icon은 글자 앞에 놓는 32 크기 아이콘이고, contentColor를 정하면 활성 상태의 글자와 아이콘을 그 색으로 칠한다.
@Composable
fun PleosButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.BASIC,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Color.Unspecified
) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(size.radius)
    val background =
        when (kind) {
            ButtonKind.BASIC -> when {
                !enabled -> colors.button.basic.disabled
                pressed -> colors.button.basic.pressed
                else -> colors.button.basic.enabled
            }

            ButtonKind.FILLED -> when {
                !enabled -> colors.button.filled.disabled
                pressed -> colors.button.filled.pressed
                else -> colors.button.filled.enabled
            }
        }
    val labelColor =
        when {
            !enabled -> colors.text.quaternary
            contentColor.isSpecified -> contentColor
            kind == ButtonKind.FILLED -> colors.text.onFilled
            else -> colors.text.primary
        }
    val outline =
        if (enabled) Modifier else Modifier.border(2.dp, colors.divider, shape)
    ButtonContent(
        label = label,
        labelColor = labelColor,
        size = size,
        icon = icon,
        modifier = modifier
            .height(size.height)
            .background(background, shape)
            .then(outline)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    )
}

// Pleos Toggle Button이다. 켜지면 button/switch 바탕과 text/dark/primary 글자, 꺼지면 button/basic 바탕과
// text/dark/secondary 글자를 사용한다.
@Composable
fun PleosToggleButton(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.MEDIUM
) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val states = if (checked) colors.button.switch else colors.button.basic
    ButtonContent(
        label = label,
        labelColor = if (checked) colors.text.primary else colors.text.secondary,
        size = size,
        icon = null,
        modifier = modifier
            .height(size.height)
            .background(
                if (pressed) states.pressed else states.enabled,
                RoundedCornerShape(size.radius)
            )
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    )
}

@Composable
private fun ButtonContent(
    label: String,
    labelColor: Color,
    size: ButtonSize,
    icon: ImageVector?,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier.padding(horizontal = size.paddingHorizontal)
    ) {
        if (icon != null) {
            Image(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(labelColor),
                modifier = Modifier.size(32.dp)
            )
        }
        PleosText(label, size.label, labelColor, maxLines = 1)
    }
}
