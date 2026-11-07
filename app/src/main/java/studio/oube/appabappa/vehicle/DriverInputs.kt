package studio.oube.appabappa.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import kotlin.math.abs
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.sdkCall

// 반올림 오차로 조향각이 범위를 조금 넘을 수 있어서(판단), 차가 알려 준 값의 1.1배까지는 범위 안으로 간주한다.
private const val RANGE_TOLERANCE = 1.1

// 범위를 벗어난 값을 이 횟수만큼 연속으로 수신하면 조회한 범위가 틀렸다고 간주하고, 그 뒤로는 조향각을 변환하지
// 않고 그대로 사용한다(판단). 그보다 적게 튀는 값은 잡음으로 간주해 버린다.
private const val OUT_OF_RANGE_LIMIT = 3

class DriverInputs(private val vehicle: Vehicle) {
    @Volatile
    var turnSignal = TurnSignal.OFF
        private set

    // SDK가 주는 값은 앞바퀴 조향각이어서, 조향각 범위로 스티어링 휠 각도를 구해 판단 엔진에 넘긴다(wheelAngleOf).
    @Volatile
    var steeringAngleDeg = 0.0
        private set

    // 조향각 범위의 끝(도)이다. 범위를 모르면 null이며, 그때는 수신한 값을 그대로 사용한다.
    @Volatile
    private var fullLockDeg: Double? = null

    // 범위를 벗어난 값이 연속으로 수신된 횟수다. 조향각 콜백에서만 변경한다.
    private var outOfRangeCount = 0

    private val turnSignalListener =
        object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
                turnSignal = turnSignalOf(state)
            }

            override fun onFailed(e: Exception) {
                AppLog.w("turn signal listener failed", e)
            }
        }

    private val steeringListener =
        object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) {
                updateSteering(angle?.toDouble() ?: 0.0)
            }

            override fun onFailed(e: Exception) {
                AppLog.w("steering wheel listener failed", e)
            }
        }

    fun start() {
        val steeringWheel = vehicle.getSteeringWheel()
        sdkCall("turn signal register") {
            vehicle.getTurnSignal().registerTurnSignalState(turnSignalListener)
        }
        steeringWheel.checkSteeringWheelAngleCapabilities(
            onComplete = { _, bounds ->
                fullLockDeg = fullLockOf(bounds.minValue, bounds.maxValue)
                AppLog.i("steering range ${bounds.minValue}..${bounds.maxValue}")
            },
            onFailed = { AppLog.w("steering range check failed", it) }
        )
        sdkCall("steering register") { steeringWheel.registerSteeringWheelAngle(steeringListener) }
    }

    fun stop() {
        sdkCall("turn signal unregister") {
            vehicle.getTurnSignal().unregisterTurnSignalState(turnSignalListener)
        }
        sdkCall("steering unregister") {
            vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener)
        }
    }

    private fun updateSteering(frontWheelDeg: Double) {
        val range = fullLockDeg
        if (range != null && abs(frontWheelDeg) > range * RANGE_TOLERANCE) {
            outOfRangeCount += 1
            if (outOfRangeCount < OUT_OF_RANGE_LIMIT) return
            AppLog.w("steering $frontWheelDeg is outside range $range, using raw angle")
            fullLockDeg = null
        } else {
            outOfRangeCount = 0
        }
        steeringAngleDeg = wheelAngleOf(frontWheelDeg, fullLockDeg)
    }
}
