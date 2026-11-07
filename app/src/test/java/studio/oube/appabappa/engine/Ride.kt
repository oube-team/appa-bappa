package studio.oube.appabappa.engine

import kotlin.math.roundToInt

private const val TICK_MILLIS = 250L
private const val TICK_SECONDS = TICK_MILLIS / 1_000.0
private const val KMH_PER_MPS = 3.6

class Ride {
    private val engine = BusyEngine()
    private val visitedStates = mutableListOf<BusyState>()
    private val visitedStatuses = mutableListOf<BusyStatus>()
    private var nowMillis = 0L

    // 상태나 판단 결과가 바뀔 때만 기록한다.
    val states: List<BusyState> get() = visitedStates
    val statuses: List<BusyStatus> get() = visitedStatuses

    fun drive(seconds: Double, signals: (elapsedSeconds: Double) -> VehicleSignals) {
        repeat((seconds / TICK_SECONDS).roundToInt()) { tick ->
            val status = engine.update(signals(tick * TICK_SECONDS), nowMillis)
            if (visitedStates.lastOrNull() != status.state) visitedStates += status.state
            if (visitedStatuses.lastOrNull() != status) visitedStatuses += status
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
