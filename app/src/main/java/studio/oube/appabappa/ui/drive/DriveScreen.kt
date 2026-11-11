package studio.oube.appabappa.ui.drive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.shadow.Shadow as BoxShadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.demo.DirectDrive
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveSim
import studio.oube.appabappa.settings.AppSettings
import studio.oube.appabappa.status.MonitorState
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.ui.DemoBanner
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosTheme
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.balanced
import studio.oube.appabappa.ui.theme.tabular

// 차 안 그림의 바탕(밤하늘 아래 실내)과, 앞유리에 비치는 표시와 판단 카드의 바탕(static/dark/100의 40%와 50%)이다.
private val CabinBackground = Color(0xFF08090B)
private val GlassPanel = Color(0x66131417)
private val JudgePanel = Color(0x80131417)

// 직접 조작 시연 화면이다. 화면의 페달, 스티어링 휠, 기어, 방향지시등, 내비게이션 안내로 가상의 차를 움직이고, 그
// 신호를 서비스에 보내 실제 판단 엔진이 바쁜 상황을 판단하게 한다. 실내 조명과 동승자 화면도 지금 상태 화면과 똑같이
// 바뀐다. 차 안 그림이라 주간에도 야간 테마로 그린다. 화면에 보일 값은 프레임 루프가 DriveView에 담고, 각 부분은 자기
// 값이 바뀔 때만 다시 구성한다. "처음으로"를 누르면 새 가상의 차로 프레임 루프를 다시 시작한다.
@Composable
internal fun DriveScreen(
    monitor: MonitorState,
    settings: AppSettings,
    wide: Boolean,
    onStop: () -> Unit
) {
    val layout = remember(wide) { DriveLayout(wide) }
    var sim by remember { mutableStateOf(DriveSim()) }
    val view = remember { DriveView() }
    val progress by rememberUpdatedState(monitor.drive)
    DisposableEffect(Unit) { onDispose { DirectDrive.end() } }
    LaunchedEffect(sim) { drive(sim, view) { progress } }
    val light = animatedDashLight(if (settings.lightEnabled) monitor.mood else Mood.FREE)
    PleosTheme(night = true) {
        Box(Modifier.fillMaxSize().background(CabinBackground)) {
            DemoBanner("직접 조작") {
                PleosButton("처음으로", { sim = DriveSim() })
                PleosButton("종료", onStop)
            }
            Windshield(sim, view, layout, light)
            // 계기판과 조작부의 자리는 규격 창 너비(designWidth)에서 정한 값이다. 실제 창 너비에 맞춰 그리면 창 끝을
            // 기준으로 그리는 문과 조명 띠만 움직여 조작부에 붙으므로, 규격 너비 그대로 창 가운데에 놓는다.
            Box(Modifier.padding(top = layout.bannerHeight + layout.roadHeight.dp).fillMaxSize()) {
                Box(Modifier.requiredWidth(layout.designWidth).fillMaxHeight()) {
                    Cockpit(sim, view, layout, light)
                    PassengerDisplay(
                        layout,
                        monitor.mood,
                        monitor.reason,
                        monitor.approaching,
                        settings.who,
                        settings.lightEnabled
                    )
                }
            }
            DriveToast(view)
        }
    }
}

// 앞유리 너머 도로와 그 위에 놓은 판단 카드, 차속, 내비게이션 안내다. 판단 카드와 차속 줄은 값을 각자 조회해서, 한쪽
// 값이 바뀔 때 다른 쪽을 다시 구성하지 않게 한다.
@Composable
private fun Windshield(
    sim: DriveSim,
    view: DriveView,
    layout: DriveLayout,
    light: () -> DashLight
) {
    val card = remember { mutableStateOf(Rect.Zero) }
    Box(Modifier.padding(top = layout.bannerHeight).fillMaxWidth().height(layout.roadHeight.dp)) {
        RoadView(sim, { view.road }, { view.scenery }, { card.value }, layout, light)
        JudgeCard(layout.judge, view) { card.value = it }
        GlassRow(view, layout)
    }
}

// 엔진이 정한 상태와 그 근거다. 심사하는 사람이 판단 규칙을 확인하도록 상태 이름을 그대로 보여 준다. AR 표지판이 카드를
// 피하도록 카드가 놓인 자리를 onPlaced로 알린다. offset 앞에서 측정하면 옮기기 전 자리가 반환되므로 offset 뒤에서 측정한다.
@Composable
private fun JudgeCard(spot: Spot, view: DriveView, onPlaced: (Rect) -> Unit) {
    val colors = Pleos.colors
    val (state, rule) = view.judge
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .offset(spot.left, spot.top)
            .onPlaced { onPlaced(it.boundsInParent()) }
            .widthIn(max = spot.widthDp)
            .background(JudgePanel, RoundedCornerShape(20.dp))
            .padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PleosText(
                "판단",
                PleosType.labelMedium.normal,
                colors.text.tertiary,
                Modifier.alignByBaseline()
            )
            PleosText(
                state.name,
                PleosType.bodySmall.strong,
                colors.text.primary,
                Modifier.alignByBaseline()
            )
        }
        PleosText(rule, PleosType.bodySmall.normal.balanced(), colors.text.secondary)
    }
}

// 앞유리에 비치는 차속과, 그 왼쪽에 붙이는 내비게이션 안내다.
@Composable
private fun GlassRow(view: DriveView, layout: DriveLayout) {
    val glass = view.glass
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.offset(y = layout.speedTop).fillMaxWidth()
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            glass.guide?.let { GuideChip(it, glass.guideMeters, layout) }
        }
        SpeedBox(glass.speedKmh, layout)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun GuideChip(guide: DriveGuide, meters: Int, layout: DriveLayout) {
    val merge = guide == DriveGuide.MERGE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .height(layout.speedHeight)
            .background(GlassPanel, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp)
    ) {
        Image(
            painter = rememberVectorPainter(if (merge) MergeIcon else LeftTurnIcon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(34.dp)
        )
        PleosText(
            "${guide.label()} ${meters}m",
            shadowed(PleosType.bodyMedium.strong.tabular()),
            Color.White
        )
    }
}

// 차속 글자의 줄 높이(72, 64)가 칸 높이와 같아서, 밑선에 맞추기만 해도 숫자와 단위가 칸의 세로 가운데에 놓인다.
@Composable
private fun SpeedBox(speedKmh: Int, layout: DriveLayout) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .size(200.dp, layout.speedHeight)
            .background(GlassPanel, RoundedCornerShape(24.dp))
    ) {
        PleosText(
            "$speedKmh",
            shadowed(layout.speedStyle.tabular()),
            Color.White,
            Modifier.alignByBaseline()
        )
        PleosText(
            "km/h",
            shadowed(PleosType.labelMedium.normal),
            Pleos.colors.text.tertiary,
            Modifier.alignByBaseline()
        )
    }
}

// 밝은 도로 위에서도 글자가 보이도록 글자 둘레에 어두운 그림자를 14만큼 번지게 한다.
@Composable
private fun shadowed(style: TextStyle): TextStyle {
    val blur = with(LocalDensity.current) { 14.dp.toPx() }
    return style.copy(shadow = Shadow(Color(0x99000000), blurRadius = blur))
}

// Pleos Toast다(바탕 #44464E, 모서리 16, 글자 26/34 Bold). 화면 아래에서 24 띄운다.
@Composable
private fun BoxScope.DriveToast(view: DriveView) {
    val text = view.hint ?: return
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp)
            .widthIn(max = 792.dp)
            .dropShadow(shape, BoxShadow(40.dp, Color(0x3D000000), offset = DpOffset(0.dp, 20.dp)))
            .dropShadow(shape, BoxShadow(4.dp, Color(0x33000000), offset = DpOffset(0.dp, 2.dp)))
            .background(Color(0xFF44464E), shape)
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        val style = PleosType.bodySmall.strong.copy(textAlign = TextAlign.Center).balanced()
        PleosText(text, style, Color.White)
    }
}
