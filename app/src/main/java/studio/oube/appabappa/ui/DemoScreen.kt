package studio.oube.appabappa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt
import studio.oube.appabappa.demo.DemoScenes
import studio.oube.appabappa.status.DemoProgress
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.ui.component.EdgeLight
import studio.oube.appabappa.ui.component.HeaderHeight
import studio.oube.appabappa.ui.component.PleosButton
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosTheme
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.balanced
import studio.oube.appabappa.ui.theme.bannerBackground
import studio.oube.appabappa.ui.theme.tabular

// 시연 장면을 재생하는 동안 보여 주는 화면이다. 실제 판단과 같은 화면 문구와 가장자리 빛에, 장면이 만든 신호와 판단 결과를
// 함께 보여 준다. 서비스가 장면을 시작하기 전에는 progress가 null이다.
@Composable
internal fun DemoScreen(
    sceneId: String,
    progress: DemoProgress?,
    mood: Mood,
    reason: Reason?,
    approaching: Boolean,
    who: String,
    lightEnabled: Boolean,
    layout: AppLayout,
    onStop: () -> Unit
) {
    val text = SceneTexts[sceneId]
    val duration = progress?.durationSeconds ?: DemoScenes.find(sceneId)?.durationSeconds ?: 0.0
    val elapsed = progress?.elapsedSeconds ?: 0.0
    Box(Modifier.fillMaxSize()) {
        EdgeLight(mood, scale = layout.demoGlowScale)
        DemoBanner(text?.name ?: sceneId) { PleosButton("멈추기", onStop) }
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = layout.demoPanelTop, end = layout.demoPanelEnd)
                .width(layout.demoPanelWidth)
                .background(Pleos.colors.popup, RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            PleosText(
                text?.captionAt(elapsed) ?: "",
                PleosType.titleSmall.strong.balanced(),
                Pleos.colors.text.primary,
                Modifier.height(layout.demoCaptionHeight)
            )
            progress?.let { ReadoutGrid(readouts(it.signals, it.engineState.name)) }
            DemoProgressBar(elapsed, duration)
        }
        StatusCopyBlock(statusCopy(mood, reason, approaching, who, lightEnabled), layout.demoCopy)
    }
}

// 시연 중 띠다. 장면 재생과 직접 조작 시연이 함께 사용하고, 오른쪽에 buttons를 16 간격으로 놓는다.
// 띠는 두 테마 모두 어두운 바탕이라, 띠 안의 글자와 버튼은 야간 토큰으로 그린다. 높이와 좌우 여백(왼쪽 40, 오른쪽 24)은
// Page Navigation 머리와 같다. 머리의 세로 여백(위 40, 아래 24)을 바탕을 칠한 띠에 사용하면 내용이 아래로 치우쳐
// 보이므로, 내용을 띠의 세로 가운데에 둔다.
@Composable
internal fun DemoBanner(name: String, buttons: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .background(bannerBackground(Pleos.colors.night))
            .padding(start = 40.dp, end = 24.dp)
    ) {
        PleosTheme(night = true) {
            // 크기가 다른 한글을 밑선에 맞추면 작은 "시연 중"이 아래로 치우쳐 보이므로, 세로 가운데로 맞춘다.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                PleosText(
                    "시연 중",
                    PleosType.labelMedium.normal,
                    Pleos.colors.text.tertiary,
                    maxLines = 1
                )
                PleosText(name, PleosType.bodyLarge.strong, Pleos.colors.text.primary, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), content = buttons)
        }
    }
}

@Composable
private fun ReadoutGrid(items: List<Readout>) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        for (row in readoutRows(items)) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                for (item in row) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PleosText(
                            item.label,
                            PleosType.labelMedium.normal,
                            Pleos.colors.text.tertiary
                        )
                        PleosText(
                            item.value,
                            PleosType.titleSmall.strong.tabular(),
                            Pleos.colors.text.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// 좁은 칸은 두 개씩 한 줄로 묶고, 넓은 칸은 그 아래에 한 줄을 혼자 차지한다.
private fun readoutRows(items: List<Readout>): List<List<Readout>> =
    items.filterNot { it.wide }.chunked(2) + items.filter { it.wide }.map { listOf(it) }

@Composable
private fun DemoProgressBar(elapsed: Double, duration: Double) {
    val colors = Pleos.colors
    val fraction = if (duration > 0) (elapsed / duration).coerceIn(0.0, 1.0).toFloat() else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(colors.divider, RoundedCornerShape(999.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(colors.text.primary, RoundedCornerShape(999.dp))
            )
        }
        PleosText(
            "${floor(elapsed).roundToInt()}초 / ${duration.roundToInt()}초",
            PleosType.labelMedium.normal.tabular(),
            colors.text.tertiary,
            maxLines = 1
        )
    }
}
