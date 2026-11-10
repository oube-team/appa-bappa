package studio.oube.appabappa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.light.Brightness
import studio.oube.appabappa.settings.AppSettings
import studio.oube.appabappa.settings.WhoOptions
import studio.oube.appabappa.ui.component.DepthHeader
import studio.oube.appabappa.ui.component.PleosSegmented
import studio.oube.appabappa.ui.component.PleosSwitch
import studio.oube.appabappa.ui.component.lineBelow
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType

private fun Brightness.label(): String = when (this) {
    Brightness.LOW -> "낮게"
    Brightness.MID -> "보통"
    Brightness.HIGH -> "높게"
}

private val BrightnessLabels = Brightness.entries.map { it.label() }

// 바쁜 순간 알리기를 끄면 조명 설정은 의미가 없어서 비활성으로 보여 주고, 실내 조명으로 알리기를 끄면 밝기를 비활성으로
// 보여 준다. 가로 창에서도 줄이 너무 길어지지 않도록 내용 너비를 1148(여백 포함 1228)까지로 둔다.
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    layout: AppLayout,
    onBack: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        DepthHeader("설정", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 1228.dp)
                .fillMaxWidth()
                .padding(horizontal = layout.padX)
        ) {
            SwitchRow(
                title = "바쁜 순간 알리기",
                hint = "끄면 바쁜 순간을 알리지 않고, 차가 켜져도 다시 시작하지 않아요",
                checked = settings.enabled,
                enabled = true
            ) { value -> onChange { it.copy(enabled = value) } }
            SwitchRow(
                title = "실내 조명으로 알리기",
                hint = "끄면 화면으로만 알려요",
                checked = settings.lightEnabled,
                enabled = settings.enabled
            ) { value -> onChange { it.copy(lightEnabled = value) } }
            SettingRow(
                title = "밝기",
                hint = "하향등이 켜져 있으면 더 어둡게 켜요",
                enabled = settings.enabled && settings.lightEnabled
            ) {
                PleosSegmented(
                    options = BrightnessLabels,
                    selected = settings.brightness.ordinal,
                    onSelect = { index ->
                        onChange { it.copy(brightness = Brightness.entries[index]) }
                    },
                    enabled = settings.enabled && settings.lightEnabled
                )
            }
            SettingRow(
                title = "운전하는 사람",
                hint = "고른 호칭이 화면 문구에 들어가요",
                enabled = true,
                last = true
            ) {
                PleosSegmented(
                    options = WhoOptions,
                    selected = WhoOptions.indexOf(settings.who),
                    onSelect = { index -> onChange { it.copy(who = WhoOptions[index]) } }
                )
            }
        }
    }
}

// 줄 전체를 눌러도 켜고 끌 수 있게 해서 운전석에서 누르기 쉽게 한다. 시연 목록의 줄처럼 누르는 동안
// button/basic/pressed 바탕을 보여 준다.
@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) colors.button.basic.pressed else Color.Transparent)
            .lineBelow(colors.divider)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange
            )
            .padding(vertical = 32.dp)
    ) {
        Column(Modifier.weight(1f)) { RowLabel(title, hint, enabled) }
        PleosSwitch(checked, enabled)
    }
}

@Composable
private fun SettingRow(
    title: String,
    hint: String,
    enabled: Boolean,
    last: Boolean = false,
    control: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (last) Modifier else Modifier.lineBelow(Pleos.colors.divider))
            .padding(vertical = 32.dp)
    ) {
        Column { RowLabel(title, hint, enabled) }
        control()
    }
}

@Composable
private fun RowLabel(title: String, hint: String, enabled: Boolean) {
    val colors = Pleos.colors
    PleosText(
        title,
        PleosType.titleSmall.strong,
        if (enabled) colors.text.primary else colors.text.quaternary
    )
    Spacer(Modifier.height(6.dp))
    PleosText(
        hint,
        PleosType.bodySmall.normal,
        if (enabled) colors.text.tertiary else colors.text.quaternary
    )
}
