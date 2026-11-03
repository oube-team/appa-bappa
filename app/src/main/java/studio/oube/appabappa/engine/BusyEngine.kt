package studio.oube.appabappa.engine

class BusyEngine(private val config: BusyConfig = BusyConfig()) {
    private val criteria = BusyCriteria(config)
    private val hardBrake = HardBrakeDetector(config.hardBrakeDropMps, config.hardBrakeWindowMillis)
    private var state = BusyState.FREE
    private var hadVeryBusy = false
    private var recoverUntilMillis = 0L

    fun update(signals: VehicleSignals, nowMillis: Long): BusyState {
        val hardBraking = hardBrake.update(signals.speedMps, nowMillis)
        val next =
            when {
                signals.gear == Gear.PARK -> BusyState.FREE
                hardBraking || criteria.isVeryBusy(signals) -> BusyState.VERY_BUSY
                criteria.isBusy(signals) -> BusyState.BUSY
                wasBusy() || isStillRecovering(nowMillis) -> BusyState.RECOVERING
                criteria.isSoon(signals) -> BusyState.SOON
                else -> BusyState.FREE
            }
        if (next == BusyState.VERY_BUSY) hadVeryBusy = true
        if (next == BusyState.RECOVERING && wasBusy()) {
            recoverUntilMillis = nowMillis + recoverMillis()
        }
        if (next == BusyState.FREE || next == BusyState.SOON) hadVeryBusy = false
        state = next
        return next
    }

    private fun wasBusy() = state == BusyState.BUSY || state == BusyState.VERY_BUSY

    private fun isStillRecovering(nowMillis: Long) =
        state == BusyState.RECOVERING && nowMillis < recoverUntilMillis

    private fun recoverMillis() =
        if (hadVeryBusy) config.veryBusyRecoverMillis else config.recoverMillis
}
