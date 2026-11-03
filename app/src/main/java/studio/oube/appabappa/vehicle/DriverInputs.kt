package studio.oube.appabappa.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.util.Log
import studio.oube.appabappa.engine.TurnSignal

private const val TAG = "AppaBappa"

class DriverInputs(private val vehicle: Vehicle) {
    @Volatile
    var turnSignal = TurnSignal.OFF
        private set

    @Volatile
    var steeringAngleDeg = 0.0
        private set

    private val turnSignalListener =
        object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
                turnSignal = turnSignalOf(state)
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "turn signal listener failed", e)
            }
        }

    private val steeringListener =
        object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) {
                steeringAngleDeg = angle?.toDouble() ?: 0.0
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "steering wheel listener failed", e)
            }
        }

    fun start() {
        vehicle.getTurnSignal().registerTurnSignalState(turnSignalListener)
        vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener)
    }

    fun stop() {
        vehicle.getTurnSignal().unregisterTurnSignalState(turnSignalListener)
        vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener)
    }
}
