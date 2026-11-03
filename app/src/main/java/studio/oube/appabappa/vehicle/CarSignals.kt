package studio.oube.appabappa.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlin.math.abs

private const val TAG = "AppaBappa"

class CarSignals(private val context: Context) {
    @Volatile
    var speedMps = 0.0
        private set

    @Volatile
    var gearSelection: Int? = null
        private set

    private var car: Car? = null
    private var properties: CarPropertyManager? = null

    private val callback =
        object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                // 오류 상태로 수신한 값까지 사용하면 차속이 급변한 것처럼 보여 급제동으로 잘못 판단할 수 있으므로, 정상 상태의 값만 사용한다.
                if (!value.isAvailable()) return
                when (value.propertyId) {
                    // 후진하면 음수로 수신되므로 절댓값을 사용한다.
                    VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                        (value.value as? Float)?.let { speedMps = abs(it.toDouble()) }

                    VehiclePropertyIds.GEAR_SELECTION -> gearSelection = value.value as? Int
                }
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.w(TAG, "car property error: ${VehiclePropertyIds.toString(propertyId)}")
            }
        }

    fun start() {
        val connected = Car.createCar(context)
        val manager = connected?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        car = connected
        properties = manager
        if (manager == null) {
            Log.w(TAG, "car property service is not available")
            return
        }
        listen(manager, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
        listen(manager, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
    }

    fun stop() {
        unlisten()
        car?.disconnect()
        car = null
        properties = null
    }

    // subscribePropertyEvents는 API 34부터 제공되어서, minSdk 28에서도 동작하는 registerCallback을 사용한다.
    @Suppress("DEPRECATION")
    private fun listen(manager: CarPropertyManager, propertyId: Int, rate: Float) {
        val registered = runCatching { manager.registerCallback(callback, propertyId, rate) }
        if (!registered.getOrDefault(false)) {
            Log.w(TAG, "cannot listen to ${VehiclePropertyIds.toString(propertyId)}: $registered")
        }
    }

    @Suppress("DEPRECATION")
    private fun unlisten() {
        properties?.unregisterCallback(callback)
    }
}

// getPropertyStatus는 API 34부터 제공되어서, minSdk 28에서도 동작하는 getStatus를 사용한다.
@Suppress("DEPRECATION")
private fun CarPropertyValue<*>.isAvailable() = status == CarPropertyValue.STATUS_AVAILABLE
