package studio.oube.appabappa.ui.drive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.component.HeaderHeight
import studio.oube.appabappa.ui.theme.PleosType

// 화면 위 사각형 자리다. 단위는 dp이고, 계기판 쪽 자리는 계기판 위 끝에서 측정한다.
@Immutable
internal class Spot(val x: Float, val y: Float, val width: Float = 0f, val height: Float = 0f) {
    val left: Dp get() = x.dp
    val top: Dp get() = y.dp
    val widthDp: Dp get() = width.dp
    val heightDp: Dp get() = height.dp
}

// 직접 조작 시연 화면의 배치다. 값은 Connect-S 규격의 세로 창(840 x 1184)과 가로 창(1700 x 1184)에서 정했다.
// 위에서부터 시연 중 띠(144), 앞유리 너머 도로(roadHeight), 계기판(남은 높이) 순서로 놓는다. 세로 창은 폭이 좁아서
// 계기판 화면을 두지 않고, 차속은 앞유리 표시로, 기어와 방향지시등은 조작부의 선택 상태로 보여 준다.
@Immutable
internal class DriveLayout(val wide: Boolean) {
    val bannerHeight: Dp = HeaderHeight
    val designWidth: Dp = if (wide) 1700.dp else 840.dp
    val roadHeight: Float = if (wide) 448f else 340f

    // 도로 그림의 지평선 높이, 초점 거리, 앞유리 기둥의 위와 아래 너비다.
    val horizon: Float = if (wide) 168f else 124f
    val focal: Float = if (wide) 860f else 560f
    val pillarTop: Float = if (wide) 132f else 72f
    val pillarBottom: Float = if (wide) 40f else 22f
    val showMirror: Boolean = wide

    // 차가 90도를 돌 때 먼 산이 옆으로 움직이는 거리다. 화면 가운데의 도로가 움직이는 거리(초점 거리에 각도를 곱한 값,
    // 가로 창 약 1351, 세로 창 약 880)와 비슷하면서 산 꼭짓점 간격(80과 120)으로 나누어떨어지는 값이다.
    val sceneryQuarter: Float = if (wide) 1440f else 960f

    // 앞유리 위에 놓는 판단 카드, 차속, 내비게이션 안내 칩의 자리다. speedTop은 도로 그림 위 끝에서 측정한 거리다.
    val judge: Spot = if (wide) Spot(150f, 24f, 560f) else Spot(24f, 16f, 792f)
    val speedTop: Dp = if (wide) 320.dp else 244.dp
    val speedHeight: Dp = if (wide) 72.dp else 64.dp
    val speedStyle: TextStyle =
        if (wide) PleosType.headlineMedium.strong else PleosType.headlineSmall.strong

    // 계기판 위의 동승자 화면이다. 가장자리 빛은 지금 상태 화면의 야간 값을 화면 높이에 맞춰 줄인다.
    val display: Spot = if (wide) Spot(1150f, 24f, 494f, 330f) else Spot(56f, 48f, 728f, 172f)
    val displayPadding: Pair<Dp, Dp> = if (wide) 32.dp to 28.dp else 24.dp to 20.dp
    val displayGap: Dp = if (wide) 10.dp else 6.dp
    val displayTitle: TextStyle =
        if (wide) PleosType.titleLarge.extraStrong else PleosType.titleSmall.extraStrong
    val displayGlowScale: Float = if (wide) 0.44f else 0.26f
    val splitDisplayTitle: Boolean = wide
    val cluster: Spot? = if (wide) Spot(300f, 24f, 460f, 92f) else null

    // 조작부다.
    val turn: Spot = if (wide) Spot(160f, 0f, 150f) else Spot(56f, 0f, 126f)
    val turnRightTop: Float = if (wide) 196f else 300f
    val turnLeftTop: Float = if (wide) 296f else 380f
    val turnLabelTop: Float = if (wide) 372f else 458f
    val wheel: Spot = if (wide) Spot(360f, 132f, 340f, 340f) else Spot(208f, 240f, 280f, 280f)
    val wheelHintTop: Float = if (wide) 480f else 526f
    val gear: Spot = if (wide) Spot(790f, 150f, 320f) else Spot(512f, 274f, 272f)
    val gearLabelTop: Float = if (wide) 116f else 240f
    val hardBrake: Spot = if (wide) Spot(790f, 246f, 320f) else Spot(560f, 618f, 224f)
    val brake: Spot = if (wide) Spot(800f, 380f, 150f, 118f) else Spot(524f, 422f, 126f, 98f)
    val accel: Spot = if (wide) Spot(984f, 340f, 96f, 158f) else Spot(684f, 386f, 88f, 134f)
    val pedalLabelTop: Float = if (wide) 506f else 528f

    // 페달 이름표 아래에 "누르고 있어 보세요"를 둔다. 토스트로 알리면 출발하기 전까지 아래 줄의 버튼을 가린다.
    val pedalHintTop: Float = if (wide) 544f else 566f
    val navLabel: Spot = if (wide) Spot(1150f, 386f) else Spot(56f, 582f)
    val navTop: Float = if (wide) 422f else 618f
    val navMerge: Spot = if (wide) Spot(1150f, 0f, 170f) else Spot(56f, 0f, 170f)
    val navLeft: Spot = if (wide) Spot(1332f, 0f, 200f) else Spot(238f, 0f, 198f)
    val navOff: Spot = if (wide) Spot(1544f, 0f, 100f) else Spot(448f, 0f, 100f)
}
