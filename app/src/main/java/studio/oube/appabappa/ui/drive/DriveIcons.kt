package studio.oube.appabappa.ui.drive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

private const val ICON_GRID = 24f
private const val ICON_STROKE = 1.5f
private const val FILLED_GRID = 48f

// Pleos 원본 아이콘 ic_annotation의 alert_tri(Figma design assets 215)다. 48 단위 칸 안에 채운 삼각형이고 느낌표
// 자리는 비어 있다. 색은 아이콘을 사용하는 곳에서 tint로 설정한다.
internal val WarningIcon: ImageVector =
    ImageVector.Builder("Warning", FILLED_GRID.dp, FILLED_GRID.dp, FILLED_GRID, FILLED_GRID)
        .apply {
            group(translationX = 2.732f, translationY = 3.5f) {
                addPath(
                    pathData = addPathNodes(ALERT_TRIANGLE_PATH),
                    fill = SolidColor(Color.Black),
                    pathFillType = PathFillType.EvenOdd
                )
            }
        }.build()

private const val ALERT_TRIANGLE_PATH =
    "M17.753 2.085 C19.269 -0.695 23.26 -0.695 24.776 2.084 L42.041 33.731 " +
        "C43.495 36.397 41.566 39.647 38.529 39.647 L4.006 39.647 " +
        "C0.97 39.647 -0.96 36.397 0.494 33.732 L17.753 2.085 Z " +
        "M21.269 25.327 C20.337 25.327 19.564 24.579 19.539 23.647 L19.228 13.747 " +
        "C19.194 12.596 20.118 11.647 21.269 11.647 C22.42 11.647 23.344 12.596 23.31 13.747 " +
        "L23 23.647 C22.974 24.579 22.21 25.327 21.269 25.327 Z " +
        "M23.768 31.647 C23.768 30.266 22.649 29.147 21.268 29.147 " +
        "C19.887 29.147 18.768 30.266 18.768 31.647 C18.768 33.027 19.887 34.147 21.268 34.147 " +
        "C22.649 34.147 23.768 33.027 23.768 31.647 Z"

// 선으로 그린 24 단위 아이콘이다. 색은 아이콘을 사용하는 곳에서 tint로 설정한다. 모양은 공개 아이콘 세트 Lucide의
// arrow-up-left, corner-up-left이고, 선 굵기는 Pleos 아이콘처럼 1.5다. Lucide의 ISC 라이선스와, 두 아이콘의 원작인
// Feather의 MIT 라이선스 고지는 README.md에 포함했다.
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

// 오른쪽 진입로에서 왼쪽 본선으로 들어가므로 합류 아이콘은 왼쪽 위를 가리킨다.
internal val MergeIcon: ImageVector = lineIcon("Merge", "M7 17V7h10", "M17 17 7 7")

internal val LeftTurnIcon: ImageVector =
    lineIcon("LeftTurn", "M9 14 4 9 9 4", "M20 20v-7a4 4 0 0 0-4-4H4")

// 계기판의 방향지시등 표시다. 직접 그린 모양이고, 48 단위 칸 안에 화살표를 채워서 그린다.
private fun arrowIcon(name: String, data: String): ImageVector =
    ImageVector.Builder(name, FILLED_GRID.dp, FILLED_GRID.dp, FILLED_GRID, FILLED_GRID)
        .addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black))
        .build()

internal val ClusterLeftArrow: ImageVector =
    arrowIcon("ClusterLeft", "M4 24 22 8 22 17 44 17 44 31 22 31 22 40Z")

internal val ClusterRightArrow: ImageVector =
    arrowIcon("ClusterRight", "M44 24 26 8 26 17 4 17 4 31 26 31 26 40Z")
