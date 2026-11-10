package studio.oube.appabappa

import android.car.Car
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import studio.oube.appabappa.service.BusyService
import studio.oube.appabappa.settings.AppSettings
import studio.oube.appabappa.settings.SettingsStore
import studio.oube.appabappa.status.BusyMonitor
import studio.oube.appabappa.trip.TripStore
import studio.oube.appabappa.ui.AppActions
import studio.oube.appabappa.ui.AppScreen
import studio.oube.appabappa.ui.Notice

private const val PERMISSION_GUIDE_KEY = "permission_guide"

class MainActivity : ComponentActivity() {
    private var demoScene: String? = null
    private var speedNotice by mutableStateOf<Notice?>(null)
    private val settings by lazy { SettingsStore.of(this) }
    private val trips by lazy { TripStore.of(this) }
    private var permissionGuide by mutableStateOf(false)
    private var requestingPermissions = false
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionResult()
        }
    private val actions =
        AppActions(
            continuePermission = ::requestMissingPermissions,
            showPermissionGuide = { permissionGuide = it },
            playScene = { id -> sendToService { putExtra(BusyService.EXTRA_DEMO_SCENE, id) } },
            stopDemo = { sendToService { putExtra(BusyService.EXTRA_STOP_DEMO, true) } },
            changeSettings = ::changeSettings,
            dismissTrip = { trips.dismiss() }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 설정이 변경되어 화면이 다시 생성될 때 같은 장면이 반복해서 재생되지 않도록, 처음 실행할 때만 인텐트에서 장면 이름을 가져온다.
        if (savedInstanceState == null) {
            demoScene = demoSceneOf(intent)
        }
        // 권한 요청 창을 화면이 생성될 때마다 표시하면 거절한 요청이 반복되므로, 권한 안내에서 "계속"을 누를 때만 표시한다.
        // 언어나 글꼴 크기 변경, 프로세스 종료로 화면이 다시 생성되어도 안내를 계속 표시하도록 permissionGuide를 저장한다.
        // 처음에는 차속 권한을 물을 수 있을 때만 안내를 표시한다. 차속 권한이 차단된 뒤에는 "계속"을 눌러도 권한 요청 창이
        // 표시되지 않고, 알림 권한만 없을 때는 바쁜 순간을 알아채는 데 지장이 없기 때문이다.
        permissionGuide =
            savedInstanceState?.getBoolean(PERMISSION_GUIDE_KEY)
                ?: (!isGranted(Car.PERMISSION_SPEED) && !isSpeedBlocked())
        // 차속 권한이 없어도 후진은 판단할 수 있고 권한을 나중에 허용하면 서비스가 차속을 등록하므로, 권한과 관계없이
        // 서비스를 시작한다.
        startBusyService()
        // 최상위 화면에서 finish()로 종료하면 Pleos 심사에서 반려될 수 있어서(CCP 개발 가이드), Back(CCP의 Back 키 포함)을
        // 누르면 앱을 종료하지 않고 뒤로 보낸다. 앱 화면 안의 시연 목록과 설정은 Compose의 BackHandler가 먼저 닫는다.
        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
        setContent {
            val monitor by BusyMonitor.state.collectAsState()
            val current by settings.state.collectAsState()
            val pendingTrip by trips.pending.collectAsState()
            AppScreen(monitor, current, speedNotice, permissionGuide, pendingTrip, actions)
        }
    }

    // 바쁜 순간 알리기를 끄면 서비스를 종료하고, 다시 켜면 서비스를 시작한다. 조명 설정은 서비스가 다음 판단 때 조회해
    // 반영한다(BusyService.applySettings). 설정 두 개를 거의 동시에 변경해도 앞의 변경이 사라지지 않도록, 새 값 대신
    // 지금 값을 변경하는 함수를 전달받는다.
    private fun changeSettings(change: (AppSettings) -> AppSettings) {
        val wasEnabled = settings.state.value.enabled
        settings.update(change)
        val enabled = settings.state.value.enabled
        when {
            enabled && !wasEnabled -> startBusyService()
            !enabled && wasEnabled -> stopService(Intent(this, BusyService::class.java))
        }
    }

    // 요청을 보내면 서비스가 시작되므로, 설정에서 바쁜 순간 알리기를 껐으면 요청을 보내지 않는다.
    private fun sendToService(extras: Intent.() -> Unit) {
        if (!settings.state.value.enabled) return
        startForegroundService(Intent(this, BusyService::class.java).apply(extras))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PERMISSION_GUIDE_KEY, permissionGuide)
    }

    // 권한 요청 창이 닫히거나 설정을 변경하고 돌아와도 안내 문구가 실제 상태와 맞도록, 화면이 다시 표시될 때마다
    // 차속 권한을 확인한다.
    override fun onResume() {
        super.onResume()
        checkSpeedPermission()
    }

    // 차속 권한 상태에 맞는 안내를 정한다. 그사이 차속 권한이 허용됐으면 서비스가 차속을 등록하도록
    // (CarSignals.listenMissing) 서비스 시작을 다시 요청한다.
    private fun checkSpeedPermission() {
        val before = speedNotice
        // 두 번 거절하면 Android가 더는 묻지 않고 Pleos 설정에도 앱별 위치 권한 메뉴가 없어서(2026-11-07 에뮬레이터),
        // 이때는 앱을 삭제한 뒤 새로 설치하는 방법을 안내한다.
        speedNotice =
            when {
                isGranted(Car.PERMISSION_SPEED) -> null
                isSpeedBlocked() -> Notice.SPEED_BLOCKED
                else -> Notice.SPEED_NEEDED
            }
        if (before != null && speedNotice == null) startBusyService()
    }

    // 권한 요청 창이 표시되어 있는 동안에는 창 뒤로 권한 안내가 보이도록, 요청이 끝난 뒤에 안내를 닫는다
    // (onPermissionResult). 안내를 먼저 닫으면 아직 답하지 않았는데 창 뒤로 "차속 권한이 없어서"가 보인다. 요청이
    // 끝나기 전에 "계속"을 한 번 더 눌러도 권한을 다시 요청하지 않는다.
    private fun requestMissingPermissions() {
        if (requestingPermissions) return
        val missing = missingPermissions()
        // 권한이 이미 모두 허용돼 있으면 권한 요청 창이 표시되지 않아 화면이 다시 표시되지 않으므로, 여기서 안내를
        // 닫고 갱신한다.
        if (missing.isEmpty()) {
            permissionGuide = false
            checkSpeedPermission()
            return
        }
        requestingPermissions = true
        rememberSpeedRationale(shouldShowRequestPermissionRationale(Car.PERMISSION_SPEED))
        permissionRequest.launch(missing.toTypedArray())
    }

    // 앱 화면이 표시된 상태에서 --activity-single-top으로 장면 이름을 전달하면 onCreate 대신 이 함수가 호출된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        demoScene = demoSceneOf(intent)
        startBusyService()
    }

    private fun onPermissionResult() {
        requestingPermissions = false
        permissionGuide = false
        // 차속 권한을 두 번 거절하면 Android가 더는 묻지 않고, shouldShowRequestPermissionRationale이 true에서 false로 바뀐다.
        // 처음 묻는 창을 Back으로 닫으면 거절로 처리되지 않아 다시 물을 수 있는데, 이때도 이 값은 false다(2026-11-07 에뮬레이터).
        // 지금 값만으로는 두 경우를 구별할 수 없으므로 요청 전 값과 비교한다.
        val rationaleNow = shouldShowRequestPermissionRationale(Car.PERMISSION_SPEED)
        if (!isGranted(Car.PERMISSION_SPEED) && speedRationaleBeforeRequest() && !rationaleNow) {
            markSpeedBlocked()
        }
        // 사용자가 알림 권한을 허용했으면 서비스가 알림을 다시 게시하도록 서비스 시작을 다시 요청한다.
        startBusyService()
    }

    // 권한 요청이 끝나거나 화면이 다시 표시되어 서비스 시작을 다시 요청할 때 같은 시연이 처음부터 재생되지 않도록, 시연
    // 장면은 한 번만 전달하고 demoScene을 초기화한다.
    private fun startBusyService() {
        val scene = demoScene
        demoScene = null
        sendToService { putExtra(BusyService.EXTRA_DEMO_SCENE, scene) }
    }
}

// 다른 앱이 인텐트로 시연을 시작해 실내 조명 색을 변경하지 못하도록, 시연 장면은 adb로 시험하는 디버그 빌드에서만
// 인텐트에서 가져온다. 출시 빌드에서는 앱 화면의 시연 목록에서 시작한다.
private fun demoSceneOf(intent: Intent): String? =
    if (BuildConfig.DEBUG) intent.getStringExtra(BusyService.EXTRA_DEMO_SCENE) else null
