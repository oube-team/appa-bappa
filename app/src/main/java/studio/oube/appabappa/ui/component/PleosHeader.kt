package studio.oube.appabappa.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosText
import studio.oube.appabappa.ui.theme.PleosType

// Pleos Page Navigation 머리의 높이(위 40, 제목 줄 80, 아래 24)다. 시연 중 띠도 같은 높이를 사용한다.
internal val HeaderHeight = 144.dp

// Pleos Page Navigation 머리다. 왼쪽 여백은 40, 오른쪽 여백은 24다.
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .padding(start = 40.dp, top = 40.dp, end = 24.dp, bottom = 24.dp)
    ) {
        PleosText(title, PleosType.titleLarge.strong, Pleos.colors.text.primary, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), content = actions)
    }
}

// Page Navigation의 하위 화면(Depth) 머리다. 뒤로 화살표(56)의 왼쪽 끝을 창 여백 40에 맞추려고 80 버튼을 12만큼
// 당기고, 화살표와 제목 사이는 depth_gap_horizontal(24)이 되도록 버튼 밖 간격을 12로 둔다.
@Composable
fun DepthHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val divider = Pleos.colors.divider
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .lineBelow(divider)
            .padding(start = 28.dp, top = 40.dp, end = 24.dp, bottom = 24.dp)
    ) {
        BackButton(onBack)
        PleosText(title, PleosType.titleLarge.strong, Pleos.colors.text.primary, maxLines = 1)
    }
}

// Pleos 아이콘 버튼(80 x 80, 모서리 20)이다. 누르는 동안에는 95%로 축소하고 button/basic/pressed 바탕을 보여 준다.
@Composable
private fun BackButton(onBack: () -> Unit) {
    val colors = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onBack)
            .pressShrink(pressed)
            .background(
                if (pressed) colors.button.basic.pressed else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
    ) {
        Image(
            painter = rememberVectorPainter(ArrowBack),
            contentDescription = "뒤로",
            colorFilter = ColorFilter.tint(colors.text.primary),
            modifier = Modifier.size(56.dp)
        )
    }
}

// Pleos 원본 아이콘 ic_arrow(Figma design assets 215의 인스턴스에 저장된 모양)를 좌우로 뒤집어 뒤로 화살표로 사용한다.
private val ArrowBack: ImageVector =
    ImageVector.Builder("ArrowBack", 56.dp, 56.dp, 52f, 52f)
        .apply {
            group(translationX = 35.75f, translationY = 8.67f, scaleX = -1f) {
                addPath(pathData = addPathNodes(ARROW_PATH), fill = SolidColor(Color.Black))
            }
        }
        .build()

private const val ARROW_PATH =
    "M0.635 0.635 C1.481 -0.212 2.853 -0.212 3.699 0.635 L18.865 15.801 " +
        "C19.712 16.647 19.712 18.019 18.865 18.865 L3.699 34.032 C2.853 34.878 1.481 34.878 " +
        "0.635 34.032 C-0.212 33.186 -0.212 31.814 0.635 30.968 L14.269 17.333 L0.635 3.699 " +
        "C-0.212 2.853 -0.212 1.481 0.635 0.635 Z"
