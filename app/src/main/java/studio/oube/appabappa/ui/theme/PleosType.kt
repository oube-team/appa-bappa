package studio.oube.appabappa.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Pleos 글꼴은 Asta Sans다. Pleos 시스템 글꼴 설정(fonts_customization.xml)이 기본 글꼴(sans-serif)을 Asta Sans로
// 변경해 두어서(2026-11-09 에뮬레이터에서 확인), 시스템 글꼴을 그대로 사용한다.
private val PleosFont: FontFamily = FontFamily.Default

// 같은 글꼴 설정은 굵기 600에 Asta Sans Bold 파일을, 700과 800에 ExtraBold 파일을 연결한다. Figma의 Bold(Strong, 차속)가
// ExtraBold로 그려지지 않도록 Bold는 600으로 요청한다.
private val AstaBold = FontWeight.SemiBold
private val AstaExtraBold = FontWeight.ExtraBold

// 한국어 단어 중간에서 줄이 바뀌지 않도록 구(phrase) 단위로 줄을 바꾸고(Android 13부터 적용), CSS처럼 줄 높이의 가운데에
// 글자를 놓는다.
private val baseStyle =
    TextStyle(
        fontFamily = PleosFont,
        lineBreak = LineBreak(
            strategy = LineBreak.Strategy.HighQuality,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        ),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

// Pleos Typography 표의 한 단계(크기와 줄 높이)다. 굵기는 표의 Normal(Regular), Strong(Bold), Extra strong(ExtraBold)이다.
// 앱은 Headline의 Strong과 Extra strong, Title의 세 굵기, Body의 Normal과 Strong, Label의 Normal을 사용한다.
@Immutable
class PleosScale(size: Int, lineHeight: Int) {
    private val style = baseStyle.copy(fontSize = size.sp, lineHeight = lineHeight.sp)
    val normal: TextStyle = style.copy(fontWeight = FontWeight.Normal)
    val strong: TextStyle = style.copy(fontWeight = AstaBold)
    val extraStrong: TextStyle = style.copy(fontWeight = AstaExtraBold)
}

// 숫자가 바뀌어도 글자 폭이 흔들리지 않도록 숫자 너비를 고정한다(초, 차속처럼 계속 바뀌는 숫자에 사용한다).
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

// 두 줄로 넘어가는 설명 문장이 "알려 / 드려요"처럼 끝 단어만 넘어가지 않도록, CSS의 text-wrap: balance처럼 줄 길이를
// 고르게 나눈다.
fun TextStyle.balanced(): TextStyle =
    copy(lineBreak = lineBreak.copy(strategy = LineBreak.Strategy.Balanced))

object PleosType {
    val headlineLarge = PleosScale(60, 80)
    val headlineMedium = PleosScale(56, 72)
    val headlineSmall = PleosScale(48, 64)
    val titleLarge = PleosScale(40, 52)
    val titleSmall = PleosScale(32, 40)
    val bodyLarge = PleosScale(30, 38)
    val bodyMedium = PleosScale(28, 36)
    val bodySmall = PleosScale(26, 34)
    val labelMedium = PleosScale(24, 28)

    // Driving View의 차속 글자 스타일(Driving/Speed_kmh)이다. 도착 뒤 화면의 횟수 숫자에 사용한다.
    val speed: TextStyle =
        baseStyle.copy(
            fontSize = 112.sp,
            lineHeight = 134.sp,
            fontWeight = AstaBold,
            letterSpacing = (-0.03).em
        )
}
