package studio.oube.appabappa.light

import studio.oube.appabappa.engine.BusyState

internal object LightPlan {
    const val ORANGE = 0xFFFF8C00.toInt()
    const val SOFT_ORANGE = 0xFFB36200.toInt()
    const val GREEN = 0xFF2E9E4F.toInt()
    const val RGB_MASK = 0x00FFFFFF

    private val busyStates = setOf(BusyState.BUSY, BusyState.VERY_BUSY, BusyState.RECOVERING)

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

    fun isOwnColor(color: Int) = listOf(ORANGE, SOFT_ORANGE, GREEN).any { sameColor(it, color) }
}

// 알파 값은 비교에 사용하지 않아서, 로그에는 색을 RGB만 기록한다.
internal fun hex(color: Int?) = color?.let { "#%06X".format(it and LightPlan.RGB_MASK) } ?: "null"
