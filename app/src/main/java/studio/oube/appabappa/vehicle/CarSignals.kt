package studio.oube.appabappa.vehicle

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import kotlin.math.abs
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.sdkCall

class CarSignals(private val context: Context) {
    @Volatile
    var speedMps = 0.0
        private set

    @Volatile
    var gearSelection: Int? = null
        private set

    private var car: Car? = null
    private var properties: CarPropertyManager? = null

    // 등록에 성공한 차량 신호 목록이다. 권한이 없어 등록하지 못한 신호는 나중에 listenMissing에서 등록한다.
    private val listening = mutableSetOf<Int>()

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
                AppLog.w("car property error: ${VehiclePropertyIds.toString(propertyId)}")
            }
        }

    fun start() {
        val connected =
            runCatching { Car.createCar(context) }
                .onFailure { AppLog.w("car connection failed", it) }
                .getOrNull()
        val manager = connected?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        car = connected
        properties = manager
        if (manager == null) {
            AppLog.w("car property service is not available")
            return
        }
        listenMissing()
    }

    // 서비스가 실행 중일 때 사용자가 차속 권한을 허용하면 앱 화면이 서비스에 시작 요청을 다시 보내므로(onStartCommand),
    // 그때 권한이 없어 등록하지 못했던 신호를 등록한다.
    fun listenMissing() {
        val manager = properties ?: return
        listen(manager, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
        listen(manager, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
    }

    fun stop() {
        unlisten()
        sdkCall("car disconnect") { car?.disconnect() }
        car = null
        properties = null
    }

    // subscribePropertyEvents는 API 34부터 제공되어서, minSdk 28에서도 동작하는 registerCallback을 사용한다.
    @Suppress("DEPRECATION")
    private fun listen(manager: CarPropertyManager, propertyId: Int, rate: Float) {
        if (propertyId in listening) return
        val registered = runCatching { manager.registerCallback(callback, propertyId, rate) }
        if (registered.getOrDefault(false)) {
            listening += propertyId
        } else {
            AppLog.w("cannot listen to ${VehiclePropertyIds.toString(propertyId)}: $registered")
        }
    }

    @Suppress("DEPRECATION")
    private fun unlisten() {
        sdkCall("car property unregister") { properties?.unregisterCallback(callback) }
        listening.clear()
    }
}

// getPropertyStatus는 API 34부터 제공되어서, minSdk 28에서도 동작하는 getStatus를 사용한다.
@Suppress("DEPRECATION")
private fun CarPropertyValue<*>.isAvailable() = status == CarPropertyValue.STATUS_AVAILABLE
