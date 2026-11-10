package studio.oube.appabappa.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF131417)
private val White = Color.White
private val Basic100Dark = Color(0xFF313236)

// text/dark 토큰이다. 이름과 달리 야간에는 흰색 계열이 된다.
@Immutable
class TextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val quaternary: Color,
    val onFilled: Color
)

// button/basic, button/filled, button/switch 토큰의 상태별 값이다.
@Immutable
class StateColors(val enabled: Color, val pressed: Color, val disabled: Color)

@Immutable
class ButtonColors(val basic: StateColors, val filled: StateColors, val switch: StateColors)

@Immutable
data class SwitchColors(
    val on: Color,
    val off: Color,
    val disabled: Color,
    val knobOn: Color,
    val knobOff: Color,
    val knobDisabled: Color
)

// Pleos Figma(design assets 215)의 색 토큰 중 앱이 사용하는 토큰이다. 주간은 Light, 야간은 Dark 모드 값이고,
// 묶음과 이름은 토큰 이름을 따른다(예: text.secondary는 text/dark/secondary, divider는 divider/secondary).
@Immutable
data class PleosColors(
    val night: Boolean,
    val background: Color,
    val popup: Color,
    val text: TextColors,
    val divider: Color,
    val button: ButtonColors,
    val tabBackground: Color,
    val switch: SwitchColors
)

val LightColors =
    PleosColors(
        night = false,
        background = Color(0xFFF7F8FA),
        popup = White,
        text = TextColors(
            primary = Ink,
            secondary = Ink.copy(alpha = 0.84f),
            tertiary = Ink.copy(alpha = 0.64f),
            quaternary = Ink.copy(alpha = 0.32f),
            onFilled = White
        ),
        divider = Ink.copy(alpha = 0.1f),
        button = ButtonColors(
            basic = StateColors(
                enabled = Ink.copy(alpha = 0.05f),
                pressed = Ink.copy(alpha = 0.1f),
                disabled = White.copy(alpha = 0.2f)
            ),
            filled = StateColors(
                enabled = Color(0xFF44464E),
                pressed = Color(0xFF313236),
                disabled = Ink.copy(alpha = 0.05f)
            ),
            switch = StateColors(
                enabled = White,
                pressed = White.copy(alpha = 0.5f),
                disabled = White.copy(alpha = 0.3f)
            )
        ),
        tabBackground = Ink.copy(alpha = 0.05f),
        switch = SwitchColors(
            on = Color(0xFF02C265),
            off = Ink.copy(alpha = 0.3f),
            disabled = Ink.copy(alpha = 0.1f),
            knobOn = White,
            knobOff = White,
            knobDisabled = White
        )
    )

val DarkColors =
    PleosColors(
        night = true,
        background = Color(0xFF1D1E21),
        popup = Color(0xFF313236),
        text = TextColors(
            primary = White,
            secondary = White.copy(alpha = 0.84f),
            tertiary = White.copy(alpha = 0.64f),
            quaternary = White.copy(alpha = 0.32f),
            onFilled = Ink
        ),
        divider = White.copy(alpha = 0.1f),
        button = ButtonColors(
            basic = StateColors(
                enabled = White.copy(alpha = 0.05f),
                pressed = White.copy(alpha = 0.1f),
                disabled = Ink.copy(alpha = 0.2f)
            ),
            filled = StateColors(
                enabled = Color(0xFFE0E1E6),
                pressed = Color(0xFFC7C9CE),
                disabled = White.copy(alpha = 0.05f)
            ),
            switch = StateColors(
                enabled = White.copy(alpha = 0.2f),
                pressed = White.copy(alpha = 0.1f),
                disabled = Ink.copy(alpha = 0.3f)
            )
        ),
        tabBackground = White.copy(alpha = 0.05f),
        switch = SwitchColors(
            on = Color(0xFF00E074),
            off = White.copy(alpha = 0.1f),
            disabled = White.copy(alpha = 0.1f),
            knobOn = White,
            knobOff = Color(0xFFA4A8AE),
            knobDisabled = White.copy(alpha = 0.2f)
        )
    )

// 시연 중 띠는 두 테마 모두 어두운 면이라 띠 안의 글자와 버튼은 야간 토큰으로 그린다. 띠 바탕은 주간에는
// static/dark/100, 야간에는 basic/100이다.
fun bannerBackground(night: Boolean): Color = if (night) Basic100Dark else Ink
