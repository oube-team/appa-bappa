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
import studio.oube.appabappa.demo.DemoPlayer
import studio.oube.appabappa.demo.DirectDriveRun
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.RouteProgress
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.light.AmbientLight
import studio.oube.appabappa.light.Brightness
import studio.oube.appabappa.sdkCall
import studio.oube.appabappa.settings.SettingsStore
import studio.oube.appabappa.status.BusyMonitor
import studio.oube.appabappa.status.MonitorState
import studio.oube.appabappa.status.MoodTracker
import studio.oube.appabappa.status.Reason
import studio.oube.appabappa.trip.TripRecorder
import studio.oube.appabappa.trip.TripStore
import studio.oube.appabappa.vehicle.CarSignals
import studio.oube.appabappa.vehicle.DriverInputs
import studio.oube.appabappa.vehicle.LowBeam
import studio.oube.appabappa.vehicle.NaviGuidance
import studio.oube.appabappa.vehicle.PassengerSeats
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
    private val demos = DemoPlayer()
    private val drives = DirectDriveRun()
    private val moodTracker = MoodTracker()
    private val trips = TripRecorder()
    private var lightEnabled = true
    private var brightness = Brightness.MID

    private lateinit var vehicle: Vehicle
    private lateinit var car: CarSignals
    private lateinit var driver: DriverInputs
    private lateinit var navi: NaviGuidance
    private lateinit var light: AmbientLight
    private lateinit var lowBeam: LowBeam
    private lateinit var settings: SettingsStore
    private lateinit var seats: PassengerSeats
    private lateinit var tripStore: TripStore

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
        settings = SettingsStore.of(this)
        tripStore = TripStore.of(this)
        seats = PassengerSeats(vehicle.getSeat(), handler).apply { start() }
        lightEnabled = settings.state.value.lightEnabled
        brightness = settings.state.value.brightness
        val savedColors = getSharedPreferences(LIGHT_PREFERENCES, MODE_PRIVATE)
        lowBeam = LowBeam(vehicle.getLight(), handler) { light.refresh() }
        light =
            AmbientLight(vehicle.getLight(), handler, savedColors) {
                settings.state.value.brightness.level(night = lowBeam.isOn)
            }
        light.start()
        lowBeam.start()
        handler.post(tick)
        AppLog.i("busy service started")
    }

    // 화면은 권한 요청이 끝나면 시작 요청을 다시 보낸다. 알림 권한을 허용하기 전에 게시한 알림이 허용한 뒤에도 알림
    // 센터에 표시되도록 알림을 다시 게시하고, 허용한 차속 권한으로 차속을 등록한다.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        intent?.getStringExtra(EXTRA_DEMO_SCENE)?.let(demos::request)
        if (intent?.getBooleanExtra(EXTRA_STOP_DEMO, false) == true) demos.stop()
        car.listenMissing()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        BusyMonitor.publish(MonitorState())
        navi.stop()
        driver.stop()
        car.stop()
        lowBeam.stop()
        seats.stop()
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

    // 실제 기어 값은 판단마다 한 번만 조회해서, 판단하는 도중에 값을 수신해도 같은 값으로 판단한다. 값을 수신하기
    // 전(null)에는 엔진에 D를 전달하고, 운전 기록과 시연은 실제 기어를 알 때만 진행한다. 장면 재생과 직접 조작 시연은
    // 둘 다 갱신한 뒤에 장면 재생의 결과를 먼저 고른다.
    private fun update() {
        applySettings()
        val now = SystemClock.elapsedRealtime()
        val gearSelection = car.gearSelection
        val realGear = gearSelection?.let(::gearOf)
        val speedMps = car.speedMps
        val signals =
            VehicleSignals(
                speedMps = speedMps,
                gear = gearOf(gearSelection),
                turnSignal = driver.turnSignal,
                steeringAngleDeg = driver.steeringAngleDeg,
                nextManeuver = nextManeuver(speedMps, now)
            )
        val realStatus = engine.update(signals, now)
        if (realGear != null) recordTrip(signals, realStatus, now)
        val demoStatus = demos.update(realGear, now)
        val driveStatus = drives.update(realGear, now)
        val next = demoStatus ?: driveStatus ?: realStatus
        if (next != status) {
            val source =
                demos.sceneId?.let { "demo $it" }
                    ?: drives.signals?.let { "direct drive $it" }
                    ?: signals.toString()
            AppLog.i("state ${status.describe()} -> ${next.describe()}, $source")
            // 조명 색은 상태로만 정하므로, 같은 상태 안에서 이유만 바뀌면 조명을 변경하지 않는다. 시연이 끝나 실제
            // 판단으로 돌아갈 때도 같은 방식으로 변경해서, 시연이 바쁜 상태에서 끝나면 바쁜 상황이 끝난 때처럼 초록을
            // 거친다.
            if (next.state != status.state && lightEnabled) {
                light.show(previous = status.state, next = next.state)
            }
            status = next
        }
        publish(realGear, now)
    }

    // 운전 기록에는 실제 신호로 판단한 상태(realStatus)만 사용하므로 시연은 기록에 포함되지 않는다.
    private fun recordTrip(signals: VehicleSignals, realStatus: BusyStatus, now: Long) {
        val riders = seats.anyOccupied
        val trip =
            trips.update(signals, realStatus.state, riders, now, System.currentTimeMillis())
                ?: return
        AppLog.i("trip finished, busy ${trip.busySpans.size} times, riders ${trip.withRiders}")
        tripStore.finish(trip)
    }

    // 설정 화면에서 변경한 조명 설정은 다음 판단 때 반영한다. 실내 조명으로 알리기를 끄면 초록을 거치지 않고 원래 색으로
    // 되돌리고(FREE에서 FREE), 다시 켜면 지금 상태의 색을 바로 켠다.
    private fun applySettings() {
        val current = settings.state.value
        if (current.lightEnabled != lightEnabled) {
            lightEnabled = current.lightEnabled
            val shown = if (lightEnabled) status.state else BusyState.FREE
            light.show(previous = BusyState.FREE, next = shown)
        }
        if (current.brightness != brightness) {
            brightness = current.brightness
            light.refresh()
        }
    }

    // 기어 값을 수신하기 전에는 P인지 알 수 없지만, 화면의 시연과 설정 버튼을 숨기지 않도록 P로 간주한다.
    private fun publish(realGear: Gear?, now: Long) {
        BusyMonitor.publish(
            MonitorState(
                mood = moodTracker.update(status.state, now),
                reason = Reason.of(status),
                parked = realGear == null || realGear == Gear.PARK,
                demo = demos.progress,
                drive = drives.progress
            )
        )
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

    companion object {
        // 앱 화면의 시연 목록에서 고른 장면 이름이다. 디버그 빌드에서는 adb 명령으로도 전달할 수 있다
        // (MainActivity.kt의 demoSceneOf).
        // adb shell am start --activity-single-top -n studio.oube.appabappa/.MainActivity \
        //     --es demo highway_merge
        const val EXTRA_DEMO_SCENE = "demo"

        // 앱 화면의 멈추기 버튼이 보내는 요청이다.
        const val EXTRA_STOP_DEMO = "stop_demo"
    }
}

private fun BusyStatus.describe() =
    listOfNotNull(state, cause, turnTypeName, "turned".takeIf { turned }).joinToString(" ")
