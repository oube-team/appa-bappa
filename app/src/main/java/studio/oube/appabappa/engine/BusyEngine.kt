package studio.oube.appabappa.engine

import kotlin.math.abs

private const val MILLIS_PER_SECOND = 1_000.0
private const val MAX_STEP_MILLIS = 1_000L

// 운전자의 조작이나 내비게이션 안내가 근거인 바쁜 상황이다. 조작이 계속되거나 안내 지점을 지나기 전이면 끝난 것이 아니다.
private val operatedCauses =
    setOf(BusyCause.MANEUVER, BusyCause.SIGNALING, BusyCause.FAST_SIGNALING, BusyCause.TURNING)

class BusyEngine(private val config: BusyConfig = BusyConfig()) {
    private val criteria = BusyCriteria(config)
    private val hardBrake = HardBrakeDetector(config.hardBrakeDropMps, config.hardBrakeWindowMillis)
    private val memory = ManeuverMemory(config)
    private var status = BusyStatus(BusyState.FREE)
    private var hadVeryBusy = false
    private var turned = false
    private var recoverUntilMillis = 0L
    private var lastMillis: Long? = null

    // RECOVERING을 유지하는 시간이다. 이번 바쁜 상황에 VERY_BUSY가 있었으면 더 길다. 직접 조작 시연 화면이 판단 근거를
    // 보여 줄 때 사용한다.
    val recoverHoldMillis: Long
        get() = if (hadVeryBusy) config.veryBusyRecoverMillis else config.recoverMillis

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
            recoverUntilMillis = nowMillis + recoverHoldMillis
        }
        if (next == BusyState.FREE || next == BusyState.SOON) hadVeryBusy = false
        val nearby = criteria.nearbyManeuver(signals)
        // 지점을 지난 뒤 passingMeters 넘게 달렸거나 다음 안내가 가까워지면 track()이 기억을 초기화하거나 다음 안내로
        // 변경한다. 바쁜 상황이 같은 판단에서 끝나도 지나온 안내로 끝났다고 알리도록, 기억한 안내의 지점을 지났는지는
        // track()을 호출하기 전의 값도 확인한다.
        val passedBefore = memory.passed
        track(next, nearby, signals, nowMillis)
        val passed = passedBefore || memory.passed
        status = statusOf(next, veryBusyCause ?: busyCause, nearby, signals, passed)
        return status
    }

    // 바쁜 상황 동안 근처 안내와 스티어링 휠을 회전 기준 이상 돌린 적이 있는지(turned)를 기억한다. 안내 지점을 지나면 근처
    // 안내가 사라지는데, 그 뒤에도 회전을 마칠 때까지 같은 안내로 알리고 실제로 회전했는지 구분하려는 것이다. 바쁜 상황이
    // 끝나면(FREE, SOON) 기억한 값을 초기화한다. 회전하다가 횡단보도 앞에서 RECOVERING이 끝날 때까지 정지하면 지점을 지난
    // 안내를 기억한 채 FREE가 된다. 이때는 초기화하지 않아서, 다시 출발해 회전을 마칠 때 같은 안내로 알린다. 지점에서
    // passingMeters 넘게 달리면 ManeuverMemory가 이 기억을 초기화한다. 기어가 P가 되면 운전이 끝난 것이므로, 다음
    // 운전에서 지나온 안내로 알리지 않도록 지점을 지난 안내도 초기화한다.
    private fun track(
        next: BusyState,
        nearby: Maneuver?,
        signals: VehicleSignals,
        nowMillis: Long
    ) {
        val moved = movedMeters(signals, nowMillis)
        val over = next == BusyState.FREE || next == BusyState.SOON
        if (signals.gear == Gear.PARK || (over && !memory.passed)) {
            forget()
            return
        }
        memory.update(nearby, signals.nextManeuver, moved)
        if (criteria.isTurning(signals)) turned = true
    }

    private fun statusOf(
        next: BusyState,
        cause: BusyCause?,
        nearby: Maneuver?,
        signals: VehicleSignals,
        passed: Boolean
    ): BusyStatus = when (next) {
        // 근처 안내가 있는데 그 지점을 아직 지나는 중이 아니면, 안내 지점에 닿기 전이다(approaching).
        BusyState.VERY_BUSY, BusyState.BUSY -> {
            val approaching = nearby != null && !criteria.isPassingManeuver(signals)
            BusyStatus(next, cause, memory.turnTypeName, turned, approaching)
        }

        BusyState.RECOVERING -> recovering(signals, passed)

        BusyState.SOON -> BusyStatus(next, BusyCause.UPCOMING_MANEUVER, nearby?.turnTypeName)

        BusyState.FREE -> BusyStatus(next)
    }

    // 바쁜 상황의 근거가 된 조작이나 안내가 끝나서 RECOVERING이 됐으면, 화면이 "방금 좌회전했어요"처럼 알릴 수 있도록 그
    // 판단 결과를 유지하고 기억한 값을 초기화한다. 초기화하지 않으면 RECOVERING에서 다시 바빠졌을 때 지나온 안내를
    // 이어받아서, 합류 지점을 지난 뒤에 켠 방향지시등까지 합류로 알리게 된다. 끝나지 않았는데 차가 정지해서 RECOVERING이
    // 됐으면 끝났다고 알리지 않도록 cause와 turnTypeName이 없는 판단 결과를 반환하고, 다시 출발했을 때 같은 안내로
    // 알리도록 기억한 값은 그대로 둔다. approaching은 바쁜 상황에서만 사용하는 값이라 RECOVERING에서는 false로 설정한다.
    private fun recovering(signals: VehicleSignals, passed: Boolean): BusyStatus = when {
        status.state == BusyState.RECOVERING -> status

        isFinished(signals, passed) ->
            status.copy(state = BusyState.RECOVERING, approaching = false).also { forget() }

        else -> BusyStatus(BusyState.RECOVERING)
    }

    // 방향지시등을 켠 채, 또는 스티어링 휠을 돌린 채 정지했거나 안내 지점을 지나기 전이면 조작이나 안내가 끝나지 않은
    // 것이다. 경고, 급제동, 후진은 조작과 관계없이 끝난 것으로 판단한다. passed는 방금 끝난 바쁜 상황에서 알리던 안내의
    // 지점을 지났으면 true다.
    private fun isFinished(signals: VehicleSignals, passed: Boolean): Boolean {
        if (status.cause !in operatedCauses) return true
        val operating =
            signals.turnSignal != TurnSignal.OFF ||
                abs(signals.steeringAngleDeg) >= config.turningAngleDeg
        return !operating && (status.turnTypeName == null || passed)
    }

    private fun forget() {
        memory.clear()
        turned = false
    }

    // 지난번 판단 뒤 달린 거리다. 판단이 오래 정지했다가 다시 시작해도 거리가 한 번에 늘지 않도록 1초까지만 계산한다.
    private fun movedMeters(signals: VehicleSignals, nowMillis: Long): Double {
        val elapsed = (nowMillis - (lastMillis ?: nowMillis)).coerceIn(0, MAX_STEP_MILLIS)
        lastMillis = nowMillis
        return signals.speedMps * elapsed / MILLIS_PER_SECOND
    }

    private fun wasBusy() = status.state == BusyState.BUSY || status.state == BusyState.VERY_BUSY

    private fun isStillRecovering(nowMillis: Long) =
        status.state == BusyState.RECOVERING && nowMillis < recoverUntilMillis
}
