package studio.oube.appabappa.engine

private const val KMH_PER_MPS = 3.6

enum class Gear {
    PARK,
    REVERSE,
    NEUTRAL,
    DRIVE
}

enum class TurnSignal {
    OFF,
    LEFT,
    RIGHT
}

// 운전자가 바로 대응해야 하는 ADAS 경고다.
enum class SafetyWarning {
    FORWARD_COLLISION,
    BLIND_SPOT
}

// 엔진이 Pleos SDK에 의존하지 않도록 NaviHelper의 TurnType 대신 문자열을 사용한다.
data class Maneuver(val turnTypeName: String, val distanceMeters: Double)

data class VehicleSignals(
    val speedMps: Double,
    val gear: Gear,
    val turnSignal: TurnSignal,
    val steeringAngleDeg: Double,
    val nextManeuver: Maneuver?,
    // 가상 콘솔로는 이 경고를 발생시킬 수 없고 실차에서 수신하려면 PRIVATE_CAR_SAFETY 권한이 필요해서,
    // 시연 장면에서만 값을 설정한다. SDK 소스 주석에 따르면 이 권한은 일반 앱에 허용되지 않을 수 있다.
    val safetyWarning: SafetyWarning? = null
) {
    val speedKmh: Double get() = speedMps * KMH_PER_MPS
}
