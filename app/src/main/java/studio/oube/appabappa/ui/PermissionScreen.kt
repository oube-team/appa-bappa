package studio.oube.appabappa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.component.AppHeader
import studio.oube.appabappa.ui.component.ButtonKind
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.component.lineAbove
import studio.oube.appabappa.ui.component.lineBelow
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.balanced

// 차속 권한은 Android 권한 요청 창에 위치 정보로 표시되어 거절하기 쉬워서, 권한 요청 창을 표시하기 전에 왜 필요한지
// 먼저 알린다.
@Composable
internal fun PermissionScreen(who: String, layout: AppLayout, onContinue: () -> Unit) {
    val colors = Pleos.colors
    Column(Modifier.fillMaxSize()) {
        AppHeader("$who 바빠")
        Column(
            Modifier
                .weight(1f)
                .padding(start = layout.padX, end = layout.padX, bottom = 40.dp)
        ) {
            Spacer(Modifier.height(layout.permissionTitleGap))
            PleosText(
                "바쁜 순간을 알아채려면 권한이 필요해요",
                PleosType.headlineSmall.extraStrong.balanced(),
                colors.text.primary
            )
            Spacer(Modifier.height(40.dp))
            Column(Modifier.width(layout.permissionListWidth)) {
                PermissionItem(
                    "차속",
                    "방향지시등을 켠 채 달리는지, 급하게 감속하는지 알려면 차속이 필요해요. 다음 창에는 위치 정보로 표시돼요",
                    last = false
                )
                PermissionItem(
                    "알림",
                    "앱 화면을 닫아도 운전 상황을 확인하는 동안에는 알림 센터에 알림을 표시해요",
                    last = true
                )
            }
            Spacer(Modifier.weight(1f))
            PleosText(
                "차속을 허용하지 않으면 후진할 때만 알려 드릴 수 있어요",
                PleosType.bodySmall.normal,
                colors.text.tertiary,
                Modifier.width(layout.permissionListWidth)
            )
            Spacer(Modifier.height(24.dp))
            PleosButton(
                "계속",
                onContinue,
                Modifier.width(layout.permissionButtonWidth),
                kind = ButtonKind.FILLED
            )
        }
    }
}

// 항목 위에는 늘 구분선을 긋고, 마지막 항목만 아래에도 긋는다.
@Composable
private fun PermissionItem(title: String, body: String, last: Boolean) {
    val divider = Pleos.colors.divider
    Column(
        Modifier
            .fillMaxWidth()
            .lineAbove(divider)
            .then(if (last) Modifier.lineBelow(divider) else Modifier)
            .padding(vertical = 32.dp)
    ) {
        PleosText(title, PleosType.titleSmall.strong, Pleos.colors.text.primary)
        Spacer(Modifier.height(6.dp))
        PleosText(body, PleosType.bodySmall.normal, Pleos.colors.text.secondary)
    }
}
