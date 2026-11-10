package studio.oube.appabappa.engine

// 엔진이 상태를 정한 근거로, 판단 규칙의 근거와 하나씩 대응한다. 화면에서 바쁜 이유를 알릴 때 사용한다.
enum class BusyCause {
    // VERY_BUSY 근거
    FORWARD_COLLISION_WARNING,
    BLIND_SPOT_WARNING,
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

// 엔진의 판단 결과다. FREE에서는 cause가 null이다. RECOVERING에서는 방금 끝난 바쁜 상황의 근거를 유지하고, 조작이나 안내가
// 끝나지 않은 채 차가 정지해서 RECOVERING이 됐으면 cause와 turnTypeName이 null이다.
// turnTypeName은 근처에 있는 회전이나 합류 같은 안내의 종류로, 바쁜 이유를 "합류하고 있어요"처럼 구체적으로 알릴 때 사용한다.
// turned는 이번 바쁜 상황 동안 스티어링 휠을 회전 기준(BusyConfig의 turningAngleDeg) 이상 돌린 적이 있으면 true다.
// 회전 안내 지점을 지난 뒤, 실제로 회전했는지 회전하지 않고 지나갔는지 구분해서 알리는 데 사용한다.
// approaching은 바쁜 상황(BUSY, VERY_BUSY)에서 turnTypeName의 안내 지점이 앞에 있고, 그 지점까지 BusyConfig의
// passingSeconds와 passingMeters보다 많이 남았으면 true다. 안내 지점에 닿기 전에 방향지시등을 켜서 바빠진 경우에, 아직
// 시작하지 않은 회전이나 합류를 하고 있다고 알리지 않는 데 사용한다.
data class BusyStatus(
    val state: BusyState,
    val cause: BusyCause? = null,
    val turnTypeName: String? = null,
    val turned: Boolean = false,
    val approaching: Boolean = false
)
