package studio.oube.appabappa.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import studio.oube.appabappa.ui.component.lineBelow
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
                .verticalScroll(rememberScrollState())
                .padding(start = layout.padX, end = layout.padX, top = 28.dp, bottom = 32.dp)
        ) {
            PleosText(
                "기어가 P일 때만 재생해요. 재생 중에 기어를 바꾸면 멈춰요",
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
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
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

// 직접 조작 시연으로 들어가는 줄이다. 장면 줄처럼 누르는 동안 button/basic/pressed 바탕을 보여 준다.
@Composable
private fun DriveRow(onClick: () -> Unit) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .background(if (pressed) colors.button.basic.pressed else Color.Transparent)
            .lineBelow(colors.divider)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp)
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

// Pleos에는 목록 컴포넌트가 없어서, 줄 아래 divider/secondary 선을 긋고 누르는 동안 button/basic/pressed 바탕을 보여 준다.
@Composable
private fun SceneRow(name: String, seconds: Int, onClick: () -> Unit, modifier: Modifier) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .height(88.dp)
            .background(if (pressed) colors.button.basic.pressed else Color.Transparent)
            .lineBelow(colors.divider)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
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

private val PlayIcon: ImageVector =
    ImageVector.Builder("Play", 28.dp, 28.dp, 24f, 24f)
        .addPath(pathData = addPathNodes("M7 4.5v15l12-7.5z"), fill = SolidColor(Color.Black))
        .build()
