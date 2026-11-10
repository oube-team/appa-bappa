package studio.oube.appabappa.ui.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveSim
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.ui.component.ButtonSize
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.component.PleosSegmented
import studio.oube.appabappa.ui.component.PleosToggleButton
import studio.oube.appabappa.ui.steeringText
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType

private const val BLINK_MILLIS = 500L

// 급제동 글자 색(informative_negative 야간)이다.
private val Negative = Color(0xFFFF4C28)

// 계기판과 조작부에 보여 줄 가상의 차 상태다. 값이 같으면 조작부를 다시 구성하지 않는다.
@Immutable
internal data class CockpitInfo(
    val gear: Gear,
    val speedKmh: Int,
    val turn: TurnSignal,
    val blinkOn: Boolean,
    val steeringDeg: Double,
    val brakeDown: Boolean,
    val hardBraking: Boolean,
    val accelDown: Boolean,
    val guide: DriveGuide?
) {
    companion object {
        // 켜진 방향지시등은 0.5초마다 깜빡인다. 꺼져 있을 때도 값을 변경하면 0.5초마다 조작부를 다시 구성하게 되므로
        // false로 둔다. 가속 페달은 기어가 D일 때만 누른 모양으로 그린다.
        fun of(sim: DriveSim, nowMillis: Long): CockpitInfo {
            val hardBraking = sim.isHardBraking(nowMillis)
            return CockpitInfo(
                gear = sim.gear,
                speedKmh = sim.speedKmh.roundToInt(),
                turn = sim.turn,
                blinkOn = sim.turn != TurnSignal.OFF && (nowMillis / BLINK_MILLIS) % 2 == 0L,
                steeringDeg = sim.steeringDeg,
                brakeDown = sim.brakeHeld || hardBraking,
                hardBraking = hardBraking,
                accelDown = sim.accelHeld && sim.gear == Gear.DRIVE,
                guide = sim.guide
            )
        }
    }
}

// 계기판, 계기판 화면, 조작부다. 조작은 가상의 차(sim)에 바로 전달한다. 가상의 차는 프레임 시각을 기준으로 동작하므로,
// 시각이 필요한 조작에는 마지막 프레임 시각(view.millis)을 사용한다. 값(view.cockpit)이 바뀔 때만 다시 구성한다.
@Composable
internal fun Cockpit(sim: DriveSim, view: DriveView, layout: DriveLayout, light: () -> DashLight) {
    val info = view.cockpit ?: return
    val clock = { view.millis }
    DashBoard(layout, light, info.turn)
    layout.cluster?.let { Cluster(it, info.gear, info.speedKmh, info.turn, info.blinkOn) }
    TurnControls(layout, info.turn, info.blinkOn, sim::toggleTurn)
    SteeringWheel(layout.wheel, info.steeringDeg, sim::grabWheel, sim::turnWheel, sim::releaseWheel)
    Label(wheelHint(info.steeringDeg), layout.wheel.x, layout.wheelHintTop, layout.wheel.width)
    GearControl(layout, info.gear) { sim.chooseGear(it, clock()) }
    PleosButton(
        "급제동",
        { sim.hardBrake(clock()) },
        Modifier.offset(
            layout.hardBrake.left,
            layout.hardBrake.top
        ).width(layout.hardBrake.widthDp),
        size = ButtonSize.SMALL,
        icon = WarningIcon,
        contentColor = Negative
    )
    Pedal(
        layout.brake,
        BrakePedal,
        info.brakeDown,
        info.hardBraking,
        "브레이크 페달, 누르고 있는 동안 느려져요",
        sim::pressBrake
    )
    Pedal(layout.accel, AccelPedal, info.accelDown, false, "가속 페달, 누르고 있는 동안 빨라져요") {
        sim.pressAccel(it, clock())
    }
    PedalLabels(layout)
    NavControls(layout, info.guide) { sim.chooseGuide(it, clock()) }
}

// 방향지시등 레버를 올리면 오른쪽, 내리면 왼쪽이 켜지므로 "오른쪽" 버튼을 위에 둔다.
@Composable
private fun TurnControls(
    layout: DriveLayout,
    turn: TurnSignal,
    blinkOn: Boolean,
    onToggle: (TurnSignal) -> Unit
) {
    val spot = layout.turn
    TurnButton(
        "오른쪽",
        on = turn == TurnSignal.RIGHT,
        blinkOn = blinkOn,
        dotAt = Alignment.TopEnd,
        modifier = Modifier.offset(spot.left, layout.turnRightTop.dp).size(spot.widthDp, 64.dp)
    ) { onToggle(TurnSignal.RIGHT) }
    TurnButton(
        "왼쪽",
        on = turn == TurnSignal.LEFT,
        blinkOn = blinkOn,
        dotAt = Alignment.TopStart,
        modifier = Modifier.offset(spot.left, layout.turnLeftTop.dp).size(spot.widthDp, 64.dp)
    ) { onToggle(TurnSignal.LEFT) }
    Label("방향지시등", spot.x, layout.turnLabelTop, spot.width)
}

// 켜진 쪽 버튼의 모서리(dotAt)에 깜빡이는 초록 점을 둔다.
@Composable
private fun TurnButton(
    label: String,
    on: Boolean,
    blinkOn: Boolean,
    dotAt: Alignment,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val lit = on && blinkOn
    Box(modifier) {
        PleosToggleButton(label, on, { onClick() }, Modifier.fillMaxSize(), size = ButtonSize.SMALL)
        Box(
            Modifier
                .align(dotAt)
                .padding(8.dp)
                .size(10.dp)
                .background(if (lit) SignalGreen else Color.Transparent, CircleShape)
        )
    }
}

// 기어는 Pleos Tabs와 색, 높이, 모서리가 같은 네 칸 선택이다.
@Composable
private fun GearControl(layout: DriveLayout, gear: Gear, onSelect: (Gear) -> Unit) {
    val spot = layout.gear
    Label("기어", spot.x, layout.gearLabelTop)
    PleosSegmented(
        options = GearLetters,
        selected = GearOrder.indexOf(gear),
        onSelect = { onSelect(GearOrder[it]) },
        modifier = Modifier.offset(spot.left, spot.top).width(spot.widthDp)
    )
}

// 페달 이름표다. 페달은 누르고 있는 동안에만 동작하므로, 그 아래에 누르고 있으라고 알린다.
@Composable
private fun PedalLabels(layout: DriveLayout) {
    Label("브레이크", layout.brake.x - 12, layout.pedalLabelTop, layout.brake.width + 24)
    Label("가속", layout.accel.x - 16, layout.pedalLabelTop, layout.accel.width + 32)
    Label("누르고 있어 보세요", layout.gear.x, layout.pedalHintTop, layout.gear.width)
}

// 내비게이션 안내 버튼이다. 고른 안내를 한 번 더 누르면 처음 거리부터 안내한다.
@Composable
private fun NavControls(layout: DriveLayout, guide: DriveGuide?, onChoose: (DriveGuide?) -> Unit) {
    val top = layout.navTop.dp
    Label("내비게이션 안내", layout.navLabel.x, layout.navLabel.y)
    for ((choice, spot) in listOf(
        DriveGuide.MERGE to layout.navMerge,
        DriveGuide.LEFT_TURN to layout.navLeft
    )) {
        PleosToggleButton(
            "${choice.label()} ${choice.startMeters.text()}m",
            guide == choice,
            { onChoose(choice) },
            Modifier.offset(spot.left, top).width(spot.widthDp),
            size = ButtonSize.SMALL
        )
    }
    PleosButton(
        "끄기",
        { onChoose(null) },
        Modifier.offset(layout.navOff.left, top).width(layout.navOff.widthDp),
        size = ButtonSize.SMALL,
        enabled = guide != null
    )
}

// 스티어링 휠이 가운데에 있으면 돌려 보라고 권하고, 돌리는 동안에는 방향과 각도를 보여 준다.
private fun wheelHint(steeringDeg: Double): String =
    if (abs(steeringDeg).roundToInt() == 0) "잡고 돌려 보세요" else steeringText(steeringDeg)

// 조작부 이름표다. width를 전달하면 그 너비 안 가운데에 놓는다.
@Composable
private fun Label(text: String, x: Float, y: Float, width: Float? = null) {
    val style = PleosType.labelMedium.normal
    PleosText(
        text,
        if (width != null) style.copy(textAlign = TextAlign.Center) else style,
        Pleos.colors.text.tertiary,
        Modifier.offset(x.dp, y.dp).then(if (width != null) Modifier.width(width.dp) else Modifier),
        maxLines = 1
    )
}
