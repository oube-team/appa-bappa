package studio.oube.appabappa.light

import kotlin.math.roundToInt
import studio.oube.appabappa.engine.BusyState

internal const val OPAQUE = 0xFF000000.toInt()
private const val CHANNEL_MAX = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

// 설정 화면의 밝기다. 앱의 색을 켤 때 RGB 값에 곱하는 밝기이고(판단, 실차 확인 못 함), 기본값은 MID다. 앱의 색은
// 운전자가 바깥을 잘 봐야 하는 바쁜 순간에 켜지므로 LOW와 MID는 밝기를 낮추고, 밤에는 실내가 밝으면 바깥이 덜
// 보여서 세 단계 모두 더 낮춘다.
// 차의 밝기 설정을 변경하면 나중에 원래 값으로 되돌려야 해서, 그 대신 RGB 값을 낮춘다.
enum class Brightness(private val dayLevel: Double, private val nightLevel: Double) {
    LOW(0.5, 0.25),
    MID(0.8, 0.4),
    HIGH(1.0, 0.6);

    fun level(night: Boolean): Double = if (night) nightLevel else dayLevel
}

internal object LightPlan {
    const val ORANGE = 0xFFFF8C00.toInt()
    const val SOFT_ORANGE = 0xFFB36200.toInt()
    const val GREEN = 0xFF2E9E4F.toInt()
    const val RGB_MASK = 0x00FFFFFF

    private val busyStates = setOf(BusyState.BUSY, BusyState.VERY_BUSY, BusyState.RECOVERING)

    private val ownColors =
        listOf(ORANGE, SOFT_ORANGE, GREEN).flatMap { color ->
            Brightness.entries.flatMap { listOf(it.level(night = false), it.level(night = true)) }
                .map { level -> dimmed(color, level) }
        }

    // null은 조명을 원래 색으로 되돌리라는 뜻이다.
    fun colorFor(state: BusyState): Int? = when (state) {
        BusyState.SOON -> SOFT_ORANGE
        BusyState.BUSY, BusyState.VERY_BUSY, BusyState.RECOVERING -> ORANGE
        BusyState.FREE -> null
    }

    fun showsGreen(previous: BusyState, next: BusyState) =
        next == BusyState.FREE && previous in busyStates

    // 알파 값은 조명에 반영되지 않고 조회하면 0이 반환될 수도 있어서, 색은 RGB만 비교한다.
    fun sameColor(a: Int, b: Int) = (a and RGB_MASK) == (b and RGB_MASK)

    fun isOwnColor(color: Int) = ownColors.any { sameColor(it, color) }

    // RGB 값마다 level을 곱해 색조는 그대로 두고 밝기를 낮춘다.
    fun dimmed(color: Int, level: Double): Int =
        rgb(channels(color).map { (it * level).roundToInt() })

    // from에 to를 fraction의 비율로 섞은 색이다. RGB 값마다 선형 보간한다.
    fun blend(from: Int, to: Int, fraction: Double): Int =
        rgb(channels(from).zip(channels(to)) { a, b -> (a + (b - a) * fraction).roundToInt() })

    private fun channels(color: Int) =
        listOf(color shr RED_SHIFT, color shr GREEN_SHIFT, color).map { it and CHANNEL_MAX }

    private fun rgb(channels: List<Int>): Int {
        val (red, green, blue) = channels.map { it.coerceIn(0, CHANNEL_MAX) }
        return OPAQUE or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
    }
}

// 알파 값은 비교에 사용하지 않아서, 로그에는 색을 RGB만 기록한다.
internal fun hex(color: Int?) = color?.let { "#%06X".format(it and LightPlan.RGB_MASK) } ?: "null"
