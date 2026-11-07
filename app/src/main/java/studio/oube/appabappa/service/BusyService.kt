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
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.demo.DemoRun
import studio.oube.appabappa.demo.DemoScenes
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.RouteProgress
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.light.AmbientLight
import studio.oube.appabappa.sdkCall
import studio.oube.appabappa.vehicle.CarSignals
import studio.oube.appabappa.vehicle.DriverInputs
import studio.oube.appabappa.vehicle.NaviGuidance
import studio.oube.appabappa.vehicle.RouteGuidance
import studio.oube.appabappa.vehicle.gearOf

private const val TICK_MILLIS = 250L
private const val LIGHT_PREFERENCES = "ambient_light"

// 운전 중에는 보통 내비게이션 화면이 표시되어 있어서, 앱 화면이 아니라 이 서비스에서 판단하고 조명 색을 변경한다.
class BusyService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine = BusyEngine()
    private val routeProgress = RouteProgress()
    private var trackedGuidance: RouteGuidance? = null
    private var status = BusyStatus(BusyState.FREE)
    private var demo: DemoRun? = null
    private var requestedDemoScene: String? = null

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
        vehicle = Vehicle(applicationContext)
        sdkCall("vehicle initialize") { vehicle.initialize() }
        car = CarSignals(applicationContext).apply { start() }
        driver = DriverInputs(vehicle).apply { start() }
        navi = NaviGuidance(applicationContext).apply { start() }
        val savedColors = getSharedPreferences(LIGHT_PREFERENCES, MODE_PRIVATE)
        light = AmbientLight(vehicle.getLight(), handler, savedColors).apply { start() }
        handler.post(tick)
        AppLog.i("busy service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_DEMO_SCENE)?.let { requestedDemoScene = it }
        car.listenMissing()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        navi.stop()
        driver.stop()
        car.stop()
        // 원래 색으로 되돌리는 요청이 끝나기 전에 Vehicle 연결을 해제하면 주황이 남을 수 있어서, 그 응답을 수신한 뒤에 해제한다.
        light.stop { sdkCall("vehicle release") { vehicle.release() } }
        AppLog.i("busy service stopped")
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
        val now = SystemClock.elapsedRealtime()
        val speedMps = car.speedMps
        val signals =
            VehicleSignals(
                speedMps = speedMps,
                gear = gearOf(car.gearSelection),
                turnSignal = driver.turnSignal,
                steeringAngleDeg = driver.steeringAngleDeg,
                nextManeuver = nextManeuver(speedMps, now)
            )
        val realStatus = engine.update(signals, now)
        startRequestedDemo(signals.gear, now)
        val next = demoStatus(signals.gear, now) ?: realStatus
        if (next != status) {
            val source = demo?.let { "demo ${it.scene.id}" } ?: signals.toString()
            AppLog.i("state ${status.describe()} -> ${next.describe()}, $source")
            // 조명 색은 상태로만 정하므로, 같은 상태 안에서 이유만 바뀌면 조명을 변경하지 않는다.
            if (next.state != status.state) light.show(previous = status.state, next = next.state)
            status = next
        }
    }

    // 내비게이션이 남은 거리를 자주 알려 주지 않아도 제때 판단하도록, 수신한 목록에서 그 뒤 달린 거리를 빼서
    // 다음 안내까지 남은 거리를 정한다.
    private fun nextManeuver(speedMps: Double, now: Long): Maneuver? {
        val guidance = navi.guidance
        if (guidance !== trackedGuidance) {
            trackedGuidance = guidance
            routeProgress.reset(guidance?.maneuvers.orEmpty(), guidance?.receivedAtMillis ?: now)
        }
        routeProgress.advance(speedMps, now)
        return routeProgress.next()
    }

    private fun startRequestedDemo(gear: Gear, now: Long) {
        val id = requestedDemoScene ?: return
        // 기어 값을 수신하기 전에는 P인지 확인할 수 없어서, 수신할 때까지 시연 시작을 보류한다.
        if (car.gearSelection == null) return
        requestedDemoScene = null
        val scene = DemoScenes.find(id)
        when {
            scene == null -> {
                val scenes = DemoScenes.all.joinToString { it.id }
                AppLog.w("unknown demo scene $id, scenes: $scenes")
            }

            gear != Gear.PARK -> AppLog.w("demo $id needs gear P, now $gear")

            else -> {
                demo = DemoRun(scene, now)
                AppLog.i("demo $id started")
            }
        }
    }

    private fun demoStatus(gear: Gear, now: Long): BusyStatus? {
        val run = demo ?: return null
        val next = run.update(gear, now)
        if (next == null) {
            val reason = if (gear == Gear.PARK) "finished" else "stopped, gear $gear"
            AppLog.i("demo ${run.scene.id} $reason")
            demo = null
        }
        return next
    }

    companion object {
        // 디버그 빌드에서는 adb 명령으로 장면 이름을 전달해 시연을 시작할 수 있다(MainActivity.demoSceneOf).
        // adb shell am start --activity-single-top -n studio.oube.appabappa/.MainActivity \
        //     --es demo highway_merge
        const val EXTRA_DEMO_SCENE = "demo"
    }
}

private fun BusyStatus.describe() = listOfNotNull(state, cause, turnTypeName).joinToString(" ")
