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

// 엔진이 Pleos SDK에 의존하지 않도록 NaviHelper의 TurnType 대신 문자열을 사용한다.
data class Maneuver(val turnTypeName: String, val distanceMeters: Double)

data class VehicleSignals(
    val speedMps: Double,
    val gear: Gear,
    val turnSignal: TurnSignal,
    val steeringAngleDeg: Double,
    val nextManeuver: Maneuver?
) {
    val speedKmh: Double get() = speedMps * KMH_PER_MPS
}
