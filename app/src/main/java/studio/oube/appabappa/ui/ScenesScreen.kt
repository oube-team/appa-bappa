package studio.oube.appabappa.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import studio.oube.appabappa.demo.DemoScenes
import studio.oube.appabappa.ui.component.DepthHeader
import studio.oube.appabappa.ui.component.PleosListRow
import studio.oube.appabappa.ui.component.scrollIfLong
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType
import studio.oube.appabappa.ui.theme.tabular

// 시연 장면 목록이다. 가로 창에서는 두 열로 나눠 한 화면에 모두 보여 준다. 목록 아래에는 직접 조작 시연을 둔다.
@Composable
internal fun ScenesScreen(
    layout: AppLayout,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onDrive: () -> Unit
) {
    val colors = Pleos.colors
    Column(Modifier.fillMaxSize()) {
        DepthHeader("시연", onBack)
        Column(
            Modifier
                .weight(1f)
                .scrollIfLong()
                .padding(start = layout.padX, end = layout.padX, top = 28.dp, bottom = 32.dp)
        ) {
            PleosText(
                "기어가 P일 때만 재생해요. 재생 중에 기어를 변경하면 멈춰요",
                PleosType.bodySmall.normal,
                colors.text.tertiary
            )
            Spacer(Modifier.height(16.dp))
            PleosText(
                "장면 재생",
                PleosType.labelMedium.normal,
                colors.text.tertiary,
                Modifier.padding(vertical = 12.dp)
            )
            val columns = if (layout.wide) 2 else 1
            for (row in DemoScenes.all.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(layout.columnGap)) {
                    for (scene in row) {
                        SceneRow(
                            name = SceneTexts[scene.id]?.name ?: scene.id,
                            seconds = scene.durationSeconds.roundToInt(),
                            onClick = { onPlay(scene.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            PleosText(
                "직접 조작",
                PleosType.labelMedium.normal,
                colors.text.tertiary,
                Modifier.padding(top = 28.dp).padding(vertical = 12.dp)
            )
            DriveRow(onDrive)
        }
    }
}

@Composable
private fun DriveRow(onClick: () -> Unit) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    PleosListRow(
        interaction = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        minHeight = 112.dp,
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            PleosText("직접 조작해 보기", PleosType.titleSmall.normal, colors.text.primary)
            PleosText(
                "화면의 페달, 스티어링 휠, 방향지시등으로 가상의 차를 운전해요",
                PleosType.bodySmall.normal,
                colors.text.tertiary
            )
        }
        Image(
            painter = rememberVectorPainter(PlayIcon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.text.primary),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SceneRow(name: String, seconds: Int, onClick: () -> Unit, modifier: Modifier) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    PleosListRow(
        interaction = interaction,
        modifier = modifier
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        minHeight = 88.dp
    ) {
        PleosText(
            name,
            PleosType.titleSmall.normal,
            colors.text.primary,
            Modifier.weight(1f),
            maxLines = 1
        )
        PleosText(
            "${seconds}초",
            PleosType.bodySmall.normal.tabular(),
            colors.text.tertiary,
            maxLines = 1
        )
        Image(
            painter = rememberVectorPainter(PlayIcon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.text.primary),
            modifier = Modifier.size(28.dp)
        )
    }
}

// 재생을 뜻하는 삼각형이다. 모양은 공개 아이콘 세트 Lucide의 play이고, 선으로 그리는 원본과 달리 안을 채워서 그린다.
// Lucide의 ISC 라이선스 고지는 README.md에 포함했다.
private val PlayIcon: ImageVector =
    ImageVector.Builder("Play", 28.dp, 28.dp, 24f, 24f)
        .addPath(pathData = addPathNodes(PLAY_PATH), fill = SolidColor(Color.Black))
        .build()

private const val PLAY_PATH =
    "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"
