package studio.oube.appabappa.demo

import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.KMH_PER_MPS
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals

class DemoStep(val seconds: Double, val signals: (elapsedSeconds: Double) -> VehicleSignals) {
    constructor(seconds: Double, signals: VehicleSignals) : this(seconds, { signals })
}

class DemoScene(val id: String, private val steps: List<DemoStep>) {
    val durationSeconds: Double = steps.sumOf { it.seconds }

    // 장면이 끝났으면 null을 반환한다.
    fun signalsAt(elapsedSeconds: Double): VehicleSignals? {
        var stepStart = 0.0
        for (step in steps) {
            val stepEnd = stepStart + step.seconds
            if (elapsedSeconds < stepEnd) return step.signals(elapsedSeconds - stepStart)
            stepStart = stepEnd
        }
        return null
    }
}

internal fun driving(
    speedKmh: Double,
    turnSignal: TurnSignal = TurnSignal.OFF,
    steeringAngleDeg: Double = 0.0,
    nextManeuver: Maneuver? = null,
    gear: Gear = Gear.DRIVE
) = VehicleSignals(
    speedMps = speedKmh / KMH_PER_MPS,
    gear = gear,
    turnSignal = turnSignal,
    steeringAngleDeg = steeringAngleDeg,
    nextManeuver = nextManeuver
)
