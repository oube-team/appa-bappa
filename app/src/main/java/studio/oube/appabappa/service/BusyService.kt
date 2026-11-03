package studio.oube.appabappa.service

import ai.pleos.playground.vehicle.Vehicle
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.light.AmbientLight
import studio.oube.appabappa.vehicle.CarSignals
import studio.oube.appabappa.vehicle.DriverInputs
import studio.oube.appabappa.vehicle.NaviGuidance
import studio.oube.appabappa.vehicle.gearOf

private const val TAG = "AppaBappa"
private const val TICK_MILLIS = 250L
private const val LIGHT_PREFERENCES = "ambient_light"

// 운전 중에는 보통 내비게이션 화면이 표시되어 있어서, 앱 화면이 아니라 이 서비스에서 판단하고 조명 색을 변경한다.
class BusyService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine = BusyEngine()
    private var state = BusyState.FREE

    private lateinit var vehicle: Vehicle
    private lateinit var car: CarSignals
    private lateinit var driver: DriverInputs
    private lateinit var navi: NaviGuidance
    private lateinit var light: AmbientLight

    private val tick =
        object : Runnable {
            override fun run() {
                update()
                handler.postDelayed(this, TICK_MILLIS)
            }
        }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        vehicle = Vehicle(applicationContext).apply { initialize() }
        car = CarSignals(applicationContext).apply { start() }
        driver = DriverInputs(vehicle).apply { start() }
        navi = NaviGuidance(applicationContext).apply { start() }
        val savedColors = getSharedPreferences(LIGHT_PREFERENCES, MODE_PRIVATE)
        light = AmbientLight(vehicle.getLight(), handler, savedColors).apply { start() }
        handler.post(tick)
        Log.i(TAG, "busy service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        light.stop()
        navi.stop()
        driver.stop()
        car.stop()
        vehicle.release()
        Log.i(TAG, "busy service stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = BusyNotification.create(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                BusyNotification.ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(BusyNotification.ID, notification)
        }
    }

    private fun update() {
        val signals =
            VehicleSignals(
                speedMps = car.speedMps,
                gear = gearOf(car.gearSelection),
                turnSignal = driver.turnSignal,
                steeringAngleDeg = driver.steeringAngleDeg,
                nextManeuver = navi.nextManeuver
            )
        val next = engine.update(signals, SystemClock.elapsedRealtime())
        if (next != state) {
            Log.i(TAG, "state $state -> $next, $signals")
            light.show(previous = state, next = next)
            state = next
        }
    }
}
