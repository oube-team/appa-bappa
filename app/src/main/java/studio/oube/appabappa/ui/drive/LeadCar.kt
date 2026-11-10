// 앞차와 AR 표지판 그림의 좌표와 색은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import studio.oube.appabappa.demo.DriveGuide

private val SideLight = Color(0xFF5A616B)
private val SideDark = Color(0xFF22262D)
private val TailGlow = Color(0xFFFF3B30)

// 뒤에서 본 세단 그림이다. 가로 200 단위이고 바닥 가운데가 (100, 152)다. 그림 모양은 바뀌지 않아서 한 번만 만든다.
internal class LeadCarPaint {
    private val shadowBlur = BlurBrush(4f)
    private val glowBlur = BlurBrush(5f)

    // 차 밑 그림자는 바닥 가운데 (100, 151)를 중심으로 한 가로 반지름 118, 세로 반지름 9의 타원이다.
    private val shadow = Path().apply { addOval(Rect(-18f, 142f, 218f, 160f)) }
    private val lowerBody =
        svgPath("M10 108 L190 108 L186 134 Q185 140 178 140 L22 140 Q15 140 14 134 Z")
    private val mirrors =
        svgPath(
            "M30 48 L14 46 Q10 46 10 50 L11 55 Q12 58 16 58 L30 56 Z " +
                "M170 48 L186 46 Q190 46 190 50 L189 55 Q188 58 184 58 L170 56 Z"
        )
    private val cabin = svgPath("M30 54 L170 54 L152 12 Q150 6 142 6 L58 6 Q50 6 48 12 Z")
    private val glass = svgPath("M42 50 L158 50 L143 15 Q141 11 136 11 L64 11 Q59 11 57 15 Z")
    private val glassShine = svgPath("M72 50 L94 50 L110 11 L98 11 Z")
    private val roofLine = svgPath("M58 6.6 L142 6.6")
    private val trunkLip = svgPath("M26 59 L174 59 Q178 55 172 53 L28 53 Q22 55 26 59 Z")
    private val body =
        svgPath(
            "M6 108 C4 92 4 76 12 66 Q16 60 26 59 L174 59 Q184 60 188 66 C196 76 196 92 194 108 Z"
        )
    private val bodyEdge = svgPath("M12 66 Q16 60 26 59 L174 59 Q184 60 188 66")
    private val bumperLine = svgPath("M10 108 L190 108")
    private val tails =
        svgPath(
            "M8 70 C9 66 12 63 18 62 L52 62 L50 76 L12 78 Q8 78 8 74 Z " +
                "M192 70 C191 66 188 63 182 62 L148 62 L150 76 L188 78 Q192 78 192 74 Z"
        )
    private val tailBar = Path().apply {
        addRoundRect(RoundRect(24f, 66f, 176f, 71f, CornerRadius(2.5f)))
    }
    private val highLamp = Path().apply {
        addRoundRect(RoundRect(84f, 13f, 116f, 16f, CornerRadius(1.5f)))
    }

    private val cabinBrush = Brush.verticalGradient(
        0f to Color(0xFFA9B0BA),
        1f to Color(0xFF7A818C),
        startY = 6f,
        endY = 54f
    )
    private val glassBrush = Brush.verticalGradient(
        0f to Color(0xFF2B3646),
        1f to Color(0xFF0B0F14),
        startY = 11f,
        endY = 50f
    )
    private val bodyBrush = Brush.verticalGradient(
        0f to Color(0xFFBCC2CB),
        1f to Color(0xFF666D78),
        startY = 59f,
        endY = 108f
    )

    // 브레이크를 밟으면 미등과 보조 제동등이 밝아지고 붉은 빛이 번진다.
    fun draw(scope: DrawScope, braking: Boolean) = with(scope) {
        shadowBlur.fill(this, shadow, Color.Black.copy(alpha = 0.55f))
        drawRoundRect(Color(0xFF0B0C0E), Offset(12f, 116f), Size(34f, 36f), CornerRadius(7f))
        drawRoundRect(Color(0xFF0B0C0E), Offset(154f, 116f), Size(34f, 36f), CornerRadius(7f))
        drawPath(lowerBody, Color(0xFF23272D))
        drawRoundRect(Color(0xFF121418), Offset(48f, 128f), Size(104f, 8f), CornerRadius(4f))
        drawRoundRect(Color(0xFF7A1A14), Offset(20f, 116f), Size(16f, 4f), CornerRadius(2f))
        drawRoundRect(Color(0xFF7A1A14), Offset(164f, 116f), Size(16f, 4f), CornerRadius(2f))
        drawPath(mirrors, Color(0xFF9AA1AB))
        drawPath(cabin, cabinBrush)
        drawPath(glass, glassBrush)
        drawPath(glassShine, Color.White.copy(alpha = 0.1f))
        drawPath(roofLine, Color(0xD9E6EAEE), style = Stroke(1.6f, cap = StrokeCap.Round))
        drawPath(highLamp, if (braking) Color(0xFFFF4A3D) else Color(0xFF3A1210))
        drawPath(trunkLip, Color(0xFFD3D8DE))
        drawPath(body, bodyBrush)
        drawPath(bodyEdge, Color(0x99EEF1F4), style = Stroke(1.2f))
        drawPath(bumperLine, Color(0xFF4A5059), style = Stroke(1f))
        withAlpha(Rect(-20f, -10f, 220f, 100f), if (braking) 0.95f else 0.35f) {
            glowBlur.fill(this, tails, TailGlow)
            glowBlur.fill(this, tailBar, TailGlow)
            if (braking) glowBlur.fill(this, highLamp, TailGlow)
        }
        drawPath(tails, if (braking) Color(0xFFFF4A3D) else Color(0xFF9E2219))
        drawPath(tailBar, if (braking) Color(0xFFFF6A5E) else Color(0xFFB8281E))
        drawRoundRect(Color(0xFFE8EBEF), Offset(76f, 84f), Size(48f, 16f), CornerRadius(2f))
        drawRoundRect(
            Color(0xFF2A2F36),
            Offset(76f, 84f),
            Size(48f, 16f),
            CornerRadius(2f),
            style = Stroke(1f)
        )
        drawRoundRect(Color(0xFFB9BFC7), Offset(82f, 89f), Size(36f, 6f), CornerRadius(1f))
    }
}

// 앞차와, 우리가 옆 차로에 있을 때 보이는 앞차의 옆면이다. 옆면은 우리에게 가까운 뒤쪽 끝이 밝다.
internal fun DrawScope.drawLeadCar(lead: LeadShape, paint: LeadCarPaint) {
    if (!lead.side.isEmpty) {
        val bounds = lead.side.getBounds()
        val brush =
            if (lead.sideOnRight) {
                Brush.horizontalGradient(
                    0f to SideLight,
                    1f to SideDark,
                    startX = bounds.left,
                    endX = bounds.right
                )
            } else {
                Brush.horizontalGradient(
                    0f to SideDark,
                    1f to SideLight,
                    startX = bounds.left,
                    endX = bounds.right
                )
            }
        drawPath(lead.side, brush)
    }
    translate(lead.x - 100 * lead.scale, lead.y - 152 * lead.scale) {
        scale(lead.scale, lead.scale, Offset.Zero) { paint.draw(this, lead.braking) }
    }
}

// 카드 배경이 반투명이라 겹친 비율만큼만 흐리게 하면 카드 글자 뒤로 표지판 테두리가 비쳐서(2026-11-09 에뮬레이터
// 세로 창에서 확인), 표지판이 판단 카드에 3분의 1 넘게 들어가면 감춘다.
private const val SIGN_HIDDEN_COVERED = 1 / 3f

private val MergeArrow = svgPath("M16 8H8v8 M16 16 8 8")
private val LeftArrow = svgPath("M10 14 6 10 10 6 M18 18v-5a3 3 0 0 0-3-3H6")
private val SignHalo = Path().apply { addOval(Rect(Offset(12f, 12f), 15f)) }
private val SignBrush =
    Brush.radialGradient(
        0f to Color(0xFF6CC8FF),
        1f to Color(0xFF1F6FE8),
        center = Offset(9.4f, 8.1f),
        radius = 19.5f
    )

// 안내 지점 위 공중에 표시하는 AR 표지판이다. 24 단위 그림을 SignPlacement의 크기로 키운다.
// avoid는 판단 카드가 차지한 자리다(도로 그림 좌표). 표지판이 카드 글자 뒤로 비치지 않도록, 카드에 들어간 만큼 흐리게
// 하다가 SIGN_HIDDEN_COVERED만큼 들어가면 감춘다.
internal fun DrawScope.drawGuideSign(
    sign: SignPlacement,
    guide: DriveGuide,
    glow: BlurBrush,
    avoid: Rect
) {
    val bounds = Rect(
        sign.x - 3 * sign.scale,
        sign.y - 3 * sign.scale,
        sign.x + 27 * sign.scale,
        sign.y + 27 * sign.scale
    )
    val overlap = bounds.intersect(avoid)
    val covered =
        if (overlap.isEmpty) 0f else overlap.width * overlap.height / (bounds.width * bounds.height)
    val shown = (1 - covered / SIGN_HIDDEN_COVERED).coerceAtLeast(0f)
    translate(sign.x, sign.y) {
        scale(sign.scale, sign.scale, Offset.Zero) {
            withAlpha(Rect(-12f, -12f, 36f, 36f), sign.alpha * shown) {
                glow.fill(this, SignHalo, Color(0x7335A8FF))
                drawCircle(SignBrush, 13f, Offset(12f, 12f))
                drawCircle(Color(0xD9FFFFFF), 13f, Offset(12f, 12f), style = Stroke(1.2f))
                drawPath(
                    if (guide == DriveGuide.MERGE) MergeArrow else LeftArrow,
                    Color.White,
                    style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
