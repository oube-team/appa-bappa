package studio.oube.appabappa.engine

class BusyEngine(private val config: BusyConfig = BusyConfig()) {
    private val criteria = BusyCriteria(config)
    private val hardBrake = HardBrakeDetector(config.hardBrakeDropMps, config.hardBrakeWindowMillis)
    private var status = BusyStatus(BusyState.FREE)
    private var hadVeryBusy = false
    private var recoverUntilMillis = 0L

    fun update(signals: VehicleSignals, nowMillis: Long): BusyStatus {
        val hardBraking = hardBrake.update(signals.speedMps, nowMillis)
        val veryBusyCause = criteria.veryBusyCause(signals, hardBraking)
        val busyCause = criteria.busyCause(signals)
        val next =
            when {
                signals.gear == Gear.PARK -> BusyState.FREE
                veryBusyCause != null -> BusyState.VERY_BUSY
                busyCause != null -> BusyState.BUSY
                wasBusy() || isStillRecovering(nowMillis) -> BusyState.RECOVERING
                criteria.isSoon(signals) -> BusyState.SOON
                else -> BusyState.FREE
            }
        if (next == BusyState.VERY_BUSY) hadVeryBusy = true
        if (next == BusyState.RECOVERING && wasBusy()) {
            recoverUntilMillis = nowMillis + recoverMillis()
        }
        if (next == BusyState.FREE || next == BusyState.SOON) hadVeryBusy = false
        status = statusOf(next, veryBusyCause ?: busyCause, criteria.nearbyManeuver(signals))
        return status
    }

    // 회전 지점을 지난 뒤에도 무엇 때문에 바빴는지 알릴 수 있도록, 바쁜 상황이 이어지는 동안(BUSY, VERY_BUSY,
    // RECOVERING)에는 근처 안내가 사라져도 앞서 있던 안내를 유지한다.
    private fun statusOf(next: BusyState, cause: BusyCause?, nearby: Maneuver?): BusyStatus {
        val inBusyPeriod = wasBusy() || status.state == BusyState.RECOVERING
        val keptTurnType = status.turnTypeName.takeIf { inBusyPeriod }
        return when (next) {
            BusyState.VERY_BUSY, BusyState.BUSY ->
                BusyStatus(next, cause, nearby?.turnTypeName ?: keptTurnType)

            BusyState.RECOVERING -> status.copy(state = BusyState.RECOVERING)

            BusyState.SOON -> BusyStatus(next, BusyCause.UPCOMING_MANEUVER, nearby?.turnTypeName)

            BusyState.FREE -> BusyStatus(next)
        }
    }

    private fun wasBusy() = status.state == BusyState.BUSY || status.state == BusyState.VERY_BUSY

    private fun isStillRecovering(nowMillis: Long) =
        status.state == BusyState.RECOVERING && nowMillis < recoverUntilMillis

    private fun recoverMillis() =
        if (hadVeryBusy) config.veryBusyRecoverMillis else config.recoverMillis
}
