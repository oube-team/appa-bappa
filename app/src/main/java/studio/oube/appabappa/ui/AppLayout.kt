package studio.oube.appabappa.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.theme.PleosType

@Immutable
internal class CopyStyle(val title: TextStyle, val reason: TextStyle, val sub: TextStyle)

// 상태 문구를 놓는 자리와 글자 스타일이다. gap은 제목과 다음 줄 사이 간격이다.
@Immutable
internal class CopyBox(val left: Dp, val top: Dp, val width: Dp, val style: CopyStyle, val gap: Dp)

// 앱 영역은 Pleos 디스플레이 가이드의 Connect-S 규격(세로 창 840 x 1184, 가로 창 1700 x 1184)이다.
// 요소의 자리는 이 크기에서 정했고, 좌우 여백은 Pleos Page Navigation처럼 창 너비와 관계없이 40이다. 창의 오른쪽에
// 놓는 카드는 창 너비가 규격과 달라도 오른쪽 여백이 유지되도록 오른쪽 끝에서 측정한 거리(End)로 놓는다.
@Immutable
internal class AppLayout(val wide: Boolean) {
    val padX: Dp = 40.dp

    // 지금 상태 화면의 문구는 Headline Large(60/80)로 크게 보여 준다.
    val statusCopy: CopyBox =
        CopyBox(
            left = padX,
            top = if (wide) 360.dp else 300.dp,
            width = if (wide) 1100.dp else 760.dp,
            style = CopyStyle(
                title = PleosType.headlineLarge.extraStrong,
                reason = PleosType.titleSmall.strong,
                sub = PleosType.bodyLarge.normal
            ),
            gap = 24.dp
        )

    // 시연 재생 화면은 신호 표와 함께 보여 주므로 제목은 Headline Small로, 이유와 설명은 한 단계씩 작게 한다.
    val demoCopy: CopyBox =
        CopyBox(
            left = padX,
            top = if (wide) 300.dp else 760.dp,
            width = if (wide) 820.dp else 760.dp,
            style = CopyStyle(
                title = PleosType.headlineSmall.extraStrong,
                reason = PleosType.bodyLarge.strong,
                sub = PleosType.bodyMedium.normal
            ),
            gap = 20.dp
        )

    // 시연 재생 화면의 설명과 신호 표 카드다. demoCaptionHeight는 설명의 줄 수가 바뀌어도 표가 움직이지 않도록 고정한
    // 높이다.
    val demoPanelEnd: Dp = if (wide) padX else 24.dp
    val demoPanelTop: Dp = 160.dp
    val demoPanelWidth: Dp = if (wide) 640.dp else 792.dp
    val demoCaptionHeight: Dp = if (wide) 120.dp else 80.dp

    // 시연 재생 화면의 가장자리 빛은 카드와 겹치지 않도록 지금 상태 화면보다 낮게 번진다.
    val demoGlowScale: Float = if (wide) 0.8f else 0.6f

    // 도착 뒤 화면이다. 세로 창은 문구 아래에, 가로 창은 문구 오른쪽에 이번 운전 카드를 둔다.
    val afterTop: Dp = if (wide) 200.dp else 160.dp
    val afterWidth: Dp = if (wide) 820.dp else 760.dp
    val tripTop: Dp = if (wide) 360.dp else 700.dp
    val tripWidth: Dp = if (wide) 640.dp else 760.dp
    val afterButtonTop: Dp = if (wide) 1040.dp else 1064.dp
    val afterButtonWidth: Dp = if (wide) 400.dp else 760.dp

    val noticeTitle: TextStyle =
        if (wide) PleosType.headlineLarge.extraStrong else PleosType.headlineSmall.extraStrong

    // 권한 안내 제목과 앱 머리 사이 간격이다.
    val permissionTitleGap: Dp = if (wide) 34.dp else 58.dp
    val permissionListWidth: Dp = if (wide) 1100.dp else 760.dp
    val permissionButtonWidth: Dp = if (wide) 400.dp else 760.dp
}
