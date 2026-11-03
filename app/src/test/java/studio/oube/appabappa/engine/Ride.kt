package studio.oube.appabappa.engine

import kotlin.math.roundToInt

private const val TICK_MILLIS = 250L
private const val TICK_SECONDS = TICK_MILLIS / 1_000.0
private const val KMH_PER_MPS = 3.6

class Ride {
    private val engine = BusyEngine()
    private val visitedStates = mutableListOf<BusyState>()
    private var nowMillis = 0L

    val states: List<BusyState> get() = visitedStates

    fun drive(seconds: Double, signals: (elapsedSeconds: Double) -> VehicleSignals) {
        repeat((seconds / TICK_SECONDS).roundToInt()) { tick ->
            val state = engine.update(signals(tick * TICK_SECONDS), nowMillis)
            if (visitedStates.lastOrNull() != state) visitedStates += state
            nowMillis += TICK_MILLIS
        }
    }

    fun drive(seconds: Double, signals: VehicleSignals) = drive(seconds) { signals }
}

fun driving(
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
