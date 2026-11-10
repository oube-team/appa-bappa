// 계기판 조명 띠의 색과 알파 값은 그림 데이터라서 값마다 이름을 붙이지 않는다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.ui.drive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import studio.oube.appabappa.light.FADE_MILLIS
import studio.oube.appabappa.light.LightPlan
import studio.oube.appabappa.status.Mood

// 계기판 조명 띠의 색이다. color는 번지는 빛, core는 띠 가운데 선의 색이고, spill, glow, reflect는 넓게 번지는 빛,
// 띠 둘레 빛, 앞유리에 비친 빛의 알파 값이다.
@Immutable
internal data class DashLight(
    val color: Color,
    val core: Color,
    val spill: Float,
    val glow: Float,
    val reflect: Float
)

// 띠는 실내 조명을 그린 것이라 앱 조명과 같은 색(LightPlan)이다. RECOVERING은 조명처럼 BUSY와 같은 색이다.
private fun dashLightOf(mood: Mood): DashLight = when (mood) {
    Mood.FREE -> DashLight(Color(0xFFDDE3EA), Color.White, 0.07f, 0.45f, 0.05f)

    Mood.SOON -> DashLight(Color(LightPlan.SOFT_ORANGE), Color(0xFFFFB45C), 0.2f, 0.85f, 0.12f)

    Mood.BUSY, Mood.RECOVERING ->
        DashLight(Color(LightPlan.ORANGE), Color(0xFFFFD08A), 0.24f, 0.95f, 0.16f)

    Mood.THANKS -> DashLight(Color(LightPlan.GREEN), Color(0xFF9BF0B4), 0.22f, 0.9f, 0.14f)
}

// 실내 조명처럼 FADE_MILLIS에 걸쳐 색을 변경한다. 변하는 값을 구성 단계에서 조회하면 색이 변하는 2초 동안 화면 전체를
// 프레임마다 다시 구성하게 되므로, 그리기 단계에서 호출해 지금 색을 조회하는 함수를 반환한다.
@Composable
internal fun animatedDashLight(mood: Mood): () -> DashLight {
    val target = dashLightOf(mood)
    val millis = FADE_MILLIS.toInt()
    val color = animateColorAsState(target.color, tween(millis), label = "dash light")
    val core = animateColorAsState(target.core, tween(millis), label = "dash light core")
    val spill = animateFloatAsState(target.spill, tween(millis), label = "dash light spill")
    val glow = animateFloatAsState(target.glow, tween(millis), label = "dash light glow")
    val reflect = animateFloatAsState(target.reflect, tween(millis), label = "dash light reflect")
    return remember {
        { DashLight(color.value, core.value, spill.value, glow.value, reflect.value) }
    }
}
