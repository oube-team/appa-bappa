// 계기판 화면과 동승자 화면의 테두리 값은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.ui.component.EdgeLight
import studio.oube.appabappa.ui.letter
import studio.oube.appabappa.ui.statusCopy
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.balanced
import studio.oube.appabappa.ui.theme.tabular

// 방향지시등이 켜지면 계기판처럼 초록색(informative_active 야간)으로 깜빡인다.
internal val SignalGreen = Color(0xFF00E074)

internal val GearOrder = listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE)
internal val GearLetters = GearOrder.map { it.letter() }

// 가로 창에만 있는 운전석 계기판 화면이다. 방향지시등, 기어, 차속을 보여 준다. 꺼진 방향지시등은 그리지 않는다
// (어둡게 남겨 두면 대비 기준 4.5:1에 못 미치는 그림이 된다).
@Composable
internal fun Cluster(spot: Spot, gear: Gear, speedKmh: Int, turn: TurnSignal, blinkOn: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .offset(spot.left, spot.top)
            .size(spot.widthDp, spot.heightDp)
            .dropShadow(shape, Shadow(26.dp, Color(0x80000000), offset = DpOffset(0.dp, 12.dp)))
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1014), Color(0xFF050607))), shape)
            .innerShadow(shape, Shadow(0.dp, Color(0x0DFFFFFF), offset = DpOffset(0.dp, 1.dp)))
            .border(1.dp, Color(0x12FFFFFF), shape)
            .padding(horizontal = 22.dp)
    ) {
        SignalArrow(ClusterLeftArrow, turn == TurnSignal.LEFT && blinkOn)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (each in GearOrder) {
                PleosText(
                    each.letter(),
                    PleosType.labelMedium.normal,
                    if (each == gear) Pleos.colors.text.primary else Pleos.colors.text.tertiary
                )
            }
        }
        // 차속 칸은 두 자리 차속에 맞춘 너비(150)이고, 세 자리가 되면 단위가 줄바꿈되지 않도록 칸을 넓힌다.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            modifier = Modifier.widthIn(min = 150.dp)
        ) {
            PleosText(
                "$speedKmh",
                PleosType.headlineMedium.strong.tabular(),
                Pleos.colors.text.primary,
                Modifier.alignByBaseline(),
                maxLines = 1,
                softWrap = false
            )
            PleosText(
                "km/h",
                PleosType.labelMedium.normal,
                Pleos.colors.text.tertiary,
                Modifier.alignByBaseline(),
                maxLines = 1,
                softWrap = false
            )
        }
        SignalArrow(ClusterRightArrow, turn == TurnSignal.RIGHT && blinkOn)
    }
}

@Composable
private fun SignalArrow(icon: ImageVector, lit: Boolean) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(if (lit) SignalGreen else Color.Transparent),
        modifier = Modifier.size(36.dp)
    )
}

// 계기판 위에 놓인 동승자 화면이다. 앱의 지금 상태 화면과 같은 문구와 가장자리 빛을 작게 보여 준다. 바쁜 이유가
// 있으면 굵게, 없으면 설명 문장을 흐리게 보여 준다.
@Composable
internal fun PassengerDisplay(
    layout: DriveLayout,
    mood: Mood,
    reason: Reason?,
    approaching: Boolean,
    who: String,
    lightEnabled: Boolean
) {
    val spot = layout.display
    val frame = RoundedCornerShape(26.dp)
    val colors = Pleos.colors
    Box(
        Modifier
            .offset(spot.left, spot.top)
            .size(spot.widthDp, spot.heightDp)
            .dropShadow(frame, Shadow(34.dp, Color(0x8C000000), offset = DpOffset(0.dp, 16.dp)))
            .background(Color(0xFF030405), frame)
            .border(1.dp, Color(0x14FFFFFF), frame)
            .padding(8.dp)
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(colors.background)) {
            EdgeLight(mood, scale = layout.displayGlowScale)
            val copy = statusCopy(mood, reason, approaching, who, lightEnabled)
            val (padX, padY) = layout.displayPadding
            Column(
                verticalArrangement = Arrangement.spacedBy(layout.displayGap),
                modifier = Modifier.padding(horizontal = padX, vertical = padY)
            ) {
                PleosText("동승자 화면", PleosType.labelMedium.normal, colors.text.tertiary)
                Column {
                    val lines = if (layout.splitDisplayTitle) {
                        copy.title
                    } else {
                        listOf(
                            copy.title.joinToString(" ")
                        )
                    }
                    for (line in lines) {
                        PleosText(
                            line,
                            layout.displayTitle,
                            colors.text.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                val reasonLine = copy.reason
                if (reasonLine != null) {
                    PleosText(
                        reasonLine,
                        PleosType.bodySmall.strong.balanced(),
                        colors.text.primary
                    )
                } else {
                    PleosText(
                        copy.sub,
                        PleosType.bodySmall.normal.balanced(),
                        colors.text.secondary
                    )
                }
            }
        }
    }
}
