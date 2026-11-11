package studio.oube.appabappa.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.light.Brightness
import studio.oube.appabappa.settings.AppSettings
import studio.oube.appabappa.settings.WhoOptions
import studio.oube.appabappa.ui.component.DepthHeader
import studio.oube.appabappa.ui.component.PleosListRow
import studio.oube.appabappa.ui.component.PleosSegmented
import studio.oube.appabappa.ui.component.PleosSwitch
import studio.oube.appabappa.ui.component.lineBelow
import studio.oube.appabappa.ui.component.scrollIfLong
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType

// 설정 두 개를 거의 동시에 변경해도 앞의 변경이 사라지지 않도록, 새 값 대신 지금 값을 변경하는 함수를 전달한다.
private typealias ChangeSettings = ((AppSettings) -> AppSettings) -> Unit

private fun Brightness.label(): String = when (this) {
    Brightness.LOW -> "어둡게"
    Brightness.MID -> "보통"
    Brightness.HIGH -> "밝게"
}

private val BrightnessLabels = Brightness.entries.map { it.label() }

// 바쁜 순간 알리기를 끄면 조명 설정은 의미가 없어서 비활성으로 보여 주고, 실내 조명으로 알리기를 끄면 밝기를 비활성으로
// 보여 준다. 가로 창과 세로 창 모두 같은 순서로 한 열에 놓는다. Pleos에 기본 탑재된 차량 설정 화면도 설정을 한 열에
// 놓는다.
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    layout: AppLayout,
    onBack: () -> Unit,
    onChange: ChangeSettings
) {
    Column(Modifier.fillMaxSize()) {
        DepthHeader("설정", onBack)
        Column(
            Modifier
                .weight(1f)
                .scrollIfLong()
                .fillMaxWidth()
                .padding(horizontal = layout.padX)
        ) {
            EnabledSetting(settings, onChange)
            LightSetting(settings, onChange)
            BrightnessSetting(settings, layout.wide, onChange)
            WhoSetting(settings, layout.wide, onChange)
        }
    }
}

@Composable
private fun EnabledSetting(settings: AppSettings, onChange: ChangeSettings) {
    // 세로 창에서는 설명의 마지막 낱말("않아요")만 아랫줄로 넘어가므로, 쉼표 뒤에 줄바꿈을 추가한다.
    SwitchRow(
        title = "바쁜 순간 알리기",
        hint = "끄면 바쁜 순간을 알리지 않고,\n차가 켜져도 앱이 다시 시작하지 않아요",
        checked = settings.enabled,
        enabled = true
    ) { value -> onChange { it.copy(enabled = value) } }
}

@Composable
private fun LightSetting(settings: AppSettings, onChange: ChangeSettings) {
    SwitchRow(
        title = "실내 조명으로 알리기",
        hint = "끄면 화면으로만 알려요",
        checked = settings.lightEnabled,
        enabled = settings.enabled
    ) { value -> onChange { it.copy(lightEnabled = value) } }
}

@Composable
private fun BrightnessSetting(settings: AppSettings, wide: Boolean, onChange: ChangeSettings) {
    val enabled = settings.enabled && settings.lightEnabled
    SettingRow(
        title = "밝기",
        hint = "하향등이 켜져 있으면 조명을 더 어둡게 켜요",
        enabled = enabled,
        wide = wide,
        last = false
    ) {
        PleosSegmented(
            options = BrightnessLabels,
            selected = settings.brightness.ordinal,
            onSelect = { index -> onChange { it.copy(brightness = Brightness.entries[index]) } },
            enabled = enabled
        )
    }
}

// 호칭이 마지막 줄이라 아래에 선을 긋지 않는다.
@Composable
private fun WhoSetting(settings: AppSettings, wide: Boolean, onChange: ChangeSettings) {
    SettingRow(
        title = "운전하는 사람",
        hint = "고른 호칭이 화면 문구에 들어가요",
        enabled = true,
        wide = wide,
        last = true
    ) {
        PleosSegmented(
            options = WhoOptions,
            selected = WhoOptions.indexOf(settings.who),
            onSelect = { index -> onChange { it.copy(who = WhoOptions[index]) } }
        )
    }
}

// 줄 전체를 눌러도 켜고 끌 수 있게 해서 운전석에서 누르기 쉽게 한다. 줄 전체가 누르는 영역이므로, 누르는 동안에는
// 시연 목록의 줄처럼 줄 전체를 축소하고 pressed 바탕을 보여 준다.
@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    PleosListRow(
        interaction = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange
            ),
        contentPadding = PaddingValues(vertical = 32.dp),
        gap = 24.dp
    ) {
        Column(Modifier.weight(1f)) { RowLabel(title, hint, enabled) }
        PleosSwitch(checked, enabled)
    }
}

// 가로 창에서는 스위치 줄처럼 이름을 왼쪽에, 컨트롤을 오른쪽 끝에 놓는다. 세로 창에서는 이름 옆에 컨트롤을 놓을 너비가
// 없어서 이름 아래에 놓는다.
@Composable
private fun SettingRow(
    title: String,
    hint: String,
    enabled: Boolean,
    wide: Boolean,
    last: Boolean,
    control: @Composable () -> Unit
) {
    val row = Modifier
        .fillMaxWidth()
        .then(if (last) Modifier else Modifier.lineBelow(Pleos.colors.divider))
        .padding(vertical = 32.dp)
    if (wide) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = row
        ) {
            Column(Modifier.weight(1f)) { RowLabel(title, hint, enabled) }
            control()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = row) {
            Column { RowLabel(title, hint, enabled) }
            control()
        }
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
