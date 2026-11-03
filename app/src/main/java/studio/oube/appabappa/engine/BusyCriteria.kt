package studio.oube.appabappa.engine

import kotlin.math.abs

internal class BusyCriteria(private val config: BusyConfig) {
    fun isVeryBusy(signals: VehicleSignals) =
        signals.turnSignal != TurnSignal.OFF && signals.speedKmh >= config.highSpeedKmh

    fun isBusy(signals: VehicleSignals) =
        signals.gear == Gear.REVERSE || (isMoving(signals) && isManeuvering(signals))

    fun isSoon(signals: VehicleSignals): Boolean {
        val maneuver = demandingManeuver(signals) ?: return false
        return isMoving(signals) && secondsTo(maneuver, signals) <= config.soonSeconds
    }

    private fun isMoving(signals: VehicleSignals) = signals.speedKmh > config.movingSpeedKmh

    private fun isManeuvering(signals: VehicleSignals): Boolean {
        val signaling = signals.turnSignal != TurnSignal.OFF
        val turning = abs(signals.steeringAngleDeg) >= config.turningAngleDeg
        return signaling || turning || isPassingManeuver(signals)
    }

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
