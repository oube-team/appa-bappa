package studio.oube.appabappa.engine

import kotlin.math.abs

internal class BusyCriteria(private val config: BusyConfig) {
    // VERY_BUSY 근거를 반환한다. 두 근거가 함께 참이면 더 갑작스러운 상황인 급제동을 고른다.
    fun veryBusyCause(signals: VehicleSignals, hardBraking: Boolean): BusyCause? = when {
        hardBraking -> BusyCause.HARD_BRAKING

        signals.turnSignal != TurnSignal.OFF && signals.speedKmh >= config.highSpeedKmh ->
            BusyCause.FAST_SIGNALING

        else -> null
    }

    // BUSY 근거를 반환한다. 여러 근거가 함께 참이면 무엇을 하는 중인지 더 분명히 알려 주는 근거를 고른다.
    // 같은 회전에서는 방향지시등 근거와 스티어링 휠 근거가 함께 참이 되므로, 안내 지점을 지나는 중이면 안내를 먼저 고른다.
    fun busyCause(signals: VehicleSignals): BusyCause? = when {
        signals.gear == Gear.REVERSE -> BusyCause.REVERSING
        !isMoving(signals) -> null
        isPassingManeuver(signals) -> BusyCause.MANEUVER
        signals.turnSignal != TurnSignal.OFF -> BusyCause.SIGNALING
        abs(signals.steeringAngleDeg) >= config.turningAngleDeg -> BusyCause.TURNING
        else -> null
    }

    fun isSoon(signals: VehicleSignals): Boolean {
        val maneuver = demandingManeuver(signals) ?: return false
        return isMoving(signals) && secondsTo(maneuver, signals) <= config.soonSeconds
    }

    // 바쁜 이유와 함께 알릴 안내로, 주행 중이고 SOON 시간 안에 닿거나 지점을 지나는 중인 회전, 합류 같은 안내만 반환한다.
    fun nearbyManeuver(signals: VehicleSignals): Maneuver? {
        val maneuver = demandingManeuver(signals)?.takeIf { isMoving(signals) } ?: return null
        return maneuver.takeIf {
            it.distanceMeters <= config.passingMeters ||
                secondsTo(it, signals) <= config.soonSeconds
        }
    }

    private fun isMoving(signals: VehicleSignals) = signals.speedKmh > config.movingSpeedKmh

    private fun isPassingManeuver(signals: VehicleSignals): Boolean {
        val maneuver = demandingManeuver(signals) ?: return false
        return maneuver.distanceMeters <= config.passingMeters ||
            secondsTo(maneuver, signals) <= config.passingSeconds
    }

    private fun demandingManeuver(signals: VehicleSignals) =
        signals.nextManeuver?.takeIf { ManeuverTypes.isDemanding(it.turnTypeName) }

    private fun secondsTo(maneuver: Maneuver, signals: VehicleSignals) =
        maneuver.distanceMeters / signals.speedMps
}
