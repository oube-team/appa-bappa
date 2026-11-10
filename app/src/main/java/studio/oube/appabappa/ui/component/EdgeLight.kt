package studio.oube.appabappa.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import studio.oube.appabappa.light.FADE_MILLIS
import studio.oube.appabappa.light.LightPlan
import studio.oube.appabappa.status.Mood
import studio.oube.appabappa.ui.theme.Pleos

// 바쁜 상황을 알리는 조명 색이다. 도착 뒤 화면의 시간 막대도 이 색을 사용한다.
internal val BusyOrange = Color(LightPlan.ORANGE)
private val Green = Color(LightPlan.GREEN)

// strip은 화면 아래 끝 선의 알파 값, wash는 그 위로 번지는 빛의 알파 값이다. 야간에는 눈부시지 않도록 더 옅게 켠다.
private class Glow(
    val color: Color,
    val strip: Float,
    val stripHeight: Dp,
    val wash: Float,
    val height: Dp
)

private val DayGlows =
    mapOf(
        Mood.FREE to Glow(Color(0xFF131417), 0.1f, 2.dp, 0f, 2.dp),
        Mood.SOON to Glow(BusyOrange, 0.75f, 4.dp, 0.3f, 300.dp),
        Mood.BUSY to Glow(BusyOrange, 1f, 6.dp, 0.55f, 500.dp),
        Mood.THANKS to Glow(Green, 1f, 5.dp, 0.38f, 400.dp)
    )

private val NightGlows =
    mapOf(
        Mood.FREE to Glow(Color.White, 0.1f, 2.dp, 0f, 2.dp),
        Mood.SOON to Glow(BusyOrange, 0.5f, 3.dp, 0.18f, 250.dp),
        Mood.BUSY to Glow(BusyOrange, 0.85f, 5.dp, 0.36f, 430.dp),
        Mood.THANKS to Glow(Green, 0.8f, 4.dp, 0.26f, 340.dp)
    )

// 아래 끝에서 위로 갈수록 빠르게 옅어지는 빛이다. 앞의 값은 위에서부터 측정한 위치, 뒤의 값은 wash에 곱할 비율이다.
private val WashStops =
    listOf(
        0f to 0f,
        0.25f to 0.025f,
        0.5f to 0.08f,
        0.74f to 0.2f,
        0.9f to 0.42f,
        0.97f to 0.7f,
        1f to 1f
    )

// 상태 색을 화면 아래 가장자리 빛으로 보여 준다. 색은 실내 조명과 같은 앱 고유 색이고, 상태가 바뀌면 조명처럼
// FADE_MILLIS에 걸쳐 변경한다. RECOVERING에서는 조명처럼 BUSY와 같은 빛을 켠다. scale은 번지는 빛의 높이 비율이다.
// 빛이 변하는 동안 프레임마다 다시 구성하지 않도록, 상태마다 빛을 한 겹씩 두고 알파 값은 그리기 단계에서 조회한다.
@Composable
fun EdgeLight(mood: Mood, modifier: Modifier = Modifier, scale: Float = 1f) {
    val shown = if (mood == Mood.RECOVERING) Mood.BUSY else mood
    val glows = if (Pleos.colors.night) NightGlows else DayGlows
    Box(modifier.fillMaxSize()) {
        for ((layer, glow) in glows) {
            val alpha = animateFloatAsState(
                targetValue = if (layer == shown) 1f else 0f,
                animationSpec = tween(FADE_MILLIS.toInt()),
                label = "edge light $layer"
            )
            GlowLayer(glow, alpha, scale, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun GlowLayer(glow: Glow, alpha: State<Float>, scale: Float, modifier: Modifier) {
    val glowing = glow.wash > 0f
    val wash = remember(glow) { washBrush(glow) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (glowing) glow.height * scale else glow.height)
            .graphicsLayer { this.alpha = alpha.value }
            .background(wash)
    ) {
        val stripGlow =
            if (glowing) {
                Modifier
                    .dropShadow(
                        RectangleShape,
                        Shadow(60.dp, glow.color.copy(alpha = glow.strip * 0.35f), spread = 10.dp)
                    )
                    .dropShadow(
                        RectangleShape,
                        Shadow(18.dp, glow.color.copy(alpha = glow.strip * 0.7f), spread = 3.dp)
                    )
            } else {
                Modifier
            }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(glow.stripHeight)
                .then(stripGlow)
                .background(glow.color.copy(alpha = glow.strip))
        )
    }
}

// 색 멈춤점 7개를 전달하는 짧은 배열이라, spread 연산자가 배열을 복사하는 비용은 무시할 만하다.
@Suppress("SpreadOperator")
private fun washBrush(glow: Glow): Brush {
    if (glow.wash == 0f) return SolidColor(Color.Transparent)
    val stops = WashStops.map { (at, share) -> at to glow.color.copy(alpha = glow.wash * share) }
    return Brush.verticalGradient(*stops.toTypedArray())
}
