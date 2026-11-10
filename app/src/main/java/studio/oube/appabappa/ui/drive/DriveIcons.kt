package studio.oube.appabappa.ui.drive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val ICON_GRID = 24f
private const val ICON_STROKE = 1.5f
private const val ARROW_GRID = 48f

// 선으로 그린 24 단위 아이콘이다. 색은 아이콘을 사용하는 곳에서 tint로 설정한다. 모양은 공개 아이콘 세트 Lucide(ISC 라이선스)의
// triangle-alert, arrow-up-left, corner-up-left이고, 선 굵기는 Pleos 아이콘처럼 1.5다.
private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, ICON_GRID.dp, ICON_GRID.dp, ICON_GRID, ICON_GRID)
        .apply {
            for (data in paths) {
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = ICON_STROKE,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

internal val WarningIcon: ImageVector =
    lineIcon(
        "Warning",
        "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
        "M12 9v4",
        "M12 17h.01"
    )

// 오른쪽 진입로에서 왼쪽 본선으로 들어가므로 합류 아이콘은 왼쪽 위를 가리킨다.
internal val MergeIcon: ImageVector = lineIcon("Merge", "M7 17V7h10", "M17 17 7 7")

internal val LeftTurnIcon: ImageVector =
    lineIcon("LeftTurn", "M9 14 4 9 9 4", "M20 20v-7a4 4 0 0 0-4-4H4")

// 계기판의 방향지시등 표시다. 48 단위 화살표를 채워서 그린다.
private fun arrowIcon(name: String, data: String): ImageVector =
    ImageVector.Builder(name, ARROW_GRID.dp, ARROW_GRID.dp, ARROW_GRID, ARROW_GRID)
        .addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black))
        .build()

internal val ClusterLeftArrow: ImageVector =
    arrowIcon("ClusterLeft", "M4 24 22 8 22 17 44 17 44 31 22 31 22 40Z")

internal val ClusterRightArrow: ImageVector =
    arrowIcon("ClusterRight", "M44 24 26 8 26 17 4 17 4 31 26 31 26 40Z")
