package studio.oube.appabappa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.trip.TripSummary
import studio.oube.appabappa.ui.component.AppHeader
import studio.oube.appabappa.ui.component.BusyOrange
import studio.oube.appabappa.ui.component.ButtonKind
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.balanced
import studio.oube.appabappa.ui.theme.tabular

// 기어가 P가 되어 운전을 마치면 보여 주는 화면이다. 동승자가 있었으면 기다려 준 것에 고마움을 전하고, 혼자 운전했으면
// 바빴던 순간만 알린다. 바빴던 순간은 조명과 같은 주황색으로 이번 운전의 시간 막대에 표시한다.
@Composable
internal fun AfterScreen(trip: TripSummary, who: String, layout: AppLayout, onDone: () -> Unit) {
    val colors = Pleos.colors
    Box(Modifier.fillMaxSize()) {
        AppHeader("$who 바빠")
        Column(Modifier.offset(layout.padX, layout.afterTop).width(layout.afterWidth)) {
            PleosText("운전을 마쳤어요", PleosType.bodyLarge.normal, colors.text.secondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                PleosText(
                    "${trip.busySpans.size}",
                    PleosType.speed.tabular(),
                    colors.text.primary,
                    Modifier.alignByBaseline()
                )
                PleosText(
                    "번",
                    PleosType.titleLarge.extraStrong,
                    colors.text.primary,
                    Modifier.alignByBaseline()
                )
            }
            PleosText(
                if (trip.withRiders) "기다려 줬어요" else "바쁜 순간이 있었어요",
                PleosType.headlineSmall.extraStrong,
                colors.text.primary
            )
            if (trip.withRiders) {
                PleosText(
                    "덕분에 ${withJosa(who, "이", "가")} 운전에 집중할 수 있었어요. 고마워요",
                    PleosType.bodyLarge.normal.balanced(),
                    colors.text.secondary,
                    Modifier.padding(top = 24.dp)
                )
            }
        }
        TripCard(
            trip,
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = layout.tripTop, end = layout.padX)
                .width(layout.tripWidth)
        )
        PleosButton(
            "확인",
            onDone,
            Modifier.offset(layout.padX, layout.afterButtonTop).width(layout.afterButtonWidth),
            kind = ButtonKind.FILLED
        )
    }
}

@Composable
private fun TripCard(trip: TripSummary, modifier: Modifier) {
    val colors = Pleos.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .background(colors.popup, RoundedCornerShape(24.dp))
            .padding(32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PleosText(
                "이번 운전",
                PleosType.titleSmall.strong,
                colors.text.primary,
                Modifier.weight(1f)
            )
            Box(Modifier.size(24.dp, 12.dp).background(BusyOrange, RoundedCornerShape(999.dp)))
            Spacer(Modifier.width(10.dp))
            PleosText("바빴던 순간", PleosType.labelMedium.normal, colors.text.tertiary)
        }
        BusyTimeline(trip)
        Row {
            val small = PleosType.labelMedium.normal.tabular()
            PleosText(
                "${clockText(trip.startedAt)} 출발",
                small,
                colors.text.tertiary,
                Modifier.weight(1f)
            )
            PleosText("${clockText(trip.endedAt)} 도착", small, colors.text.tertiary)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row {
            PleosText(
                if (trip.withRiders) "기다려 준 시간" else "바빴던 시간",
                PleosType.bodyLarge.normal,
                colors.text.primary,
                Modifier.weight(1f).alignByBaseline()
            )
            PleosText(
                durationText(trip.busyMillis),
                PleosType.titleSmall.strong.tabular(),
                colors.text.primary,
                Modifier.alignByBaseline()
            )
        }
    }
}

// 출발부터 도착까지를 막대 하나로 그리고, 바빴던 순간을 그 자리에 주황색으로 칠한다. 긴 운전에서는 몇 초짜리 순간이
// 1dp보다 좁아져 보이지 않으므로, 너비를 4dp 이상으로 설정한다.
@Composable
private fun BusyTimeline(trip: TripSummary) {
    val minWidth = 4.dp
    val total = (trip.endedAt - trip.startedAt).coerceAtLeast(1)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Pleos.colors.divider)
    ) {
        for (span in trip.busySpans) {
            val start = (span.fromMillis.toFloat() / total).coerceIn(0f, 1f)
            val end = (span.toMillis.toFloat() / total).coerceIn(start, 1f)
            Box(
                Modifier
                    .offset(x = (maxWidth * start).coerceAtMost(maxWidth - minWidth))
                    .width((maxWidth * (end - start)).coerceAtLeast(minWidth))
                    .fillMaxHeight()
                    .background(BusyOrange)
            )
        }
    }
}
