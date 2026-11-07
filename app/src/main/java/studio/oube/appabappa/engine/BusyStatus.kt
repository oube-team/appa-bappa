package studio.oube.appabappa.engine

// 엔진이 상태를 정한 근거로, 판단 규칙의 근거와 하나씩 대응한다. 화면에서 바쁜 이유를 알릴 때 사용한다.
enum class BusyCause {
    // VERY_BUSY 근거
    HARD_BRAKING,
    FAST_SIGNALING,

    // BUSY 근거
    REVERSING,
    MANEUVER,
    SIGNALING,
    TURNING,

    // SOON 근거
    UPCOMING_MANEUVER
}

// 엔진의 판단 결과다. FREE에서는 cause가 null이다. RECOVERING에서는 방금 끝난 바쁜 상황의 근거를 유지한다.
// turnTypeName은 근처에 있는 회전이나 합류 같은 안내의 종류로, 바쁜 이유를 "합류하고 있어요"처럼 구체적으로 알릴 때 사용한다.
data class BusyStatus(
    val state: BusyState,
    val cause: BusyCause? = null,
    val turnTypeName: String? = null
)
