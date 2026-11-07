package studio.oube.appabappa

import android.app.Activity
import android.car.Car
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import studio.oube.appabappa.service.BusyService

private const val PERMISSION_REQUEST = 1

class MainActivity : Activity() {
    private var demoScene: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 설정이 변경되어 화면이 다시 생성될 때 같은 장면이 반복해서 재생되지 않도록, 처음 실행할 때만 인텐트에서 장면 이름을 가져온다.
        if (savedInstanceState == null) {
            demoScene = demoSceneOf(intent)
        }
        // 화면이 다시 만들어질 때마다(주간과 야간 전환, 창 크기 변경) 사용자가 거절한 권한 요청 창이 또 표시되어서,
        // 화면을 처음 만들 때만 권한을 요청한다(2026-11-07 에뮬레이터). 그 뒤에는 사용자가 화면의 안내를 누르면
        // 다시 요청한다.
        if (savedInstanceState == null && missingPermissions().isNotEmpty()) {
            requestMissingPermissions()
        } else {
            startBusyService()
        }
    }

    // 권한 요청 창이 닫히거나 설정을 변경하고 돌아와도 안내 문구가 실제 상태와 맞도록, 화면이 다시 표시될 때마다
    // 차속 권한을 확인한다. 권한 요청 창 밖에서 차속 권한이 허용되면, 서비스가 다음 시작 요청을 수신할 때
    // 차속을 등록한다(CarSignals.listenMissing).
    override fun onResume() {
        super.onResume()
        showSpeedPermissionNotice()
    }

    // 차속 권한이 없으면 서비스는 실행되지만 차속이 0으로 유지되어 후진할 때 외에는 바쁜 순간을 알아채지 못하므로,
    // 화면에 안내를 표시한다. 다시 요청할 수 있는 상태면 안내를 눌러 요청하게 한다.
    // 두 번 거절하면 Android가 더는 묻지 않고 Pleos 설정에도 앱별 위치 권한 메뉴가 없어서(2026-11-07 에뮬레이터),
    // 앱을 삭제한 뒤 새로 설치하는 방법을 안내한다. 앱 화면을 새로 만들 때 이 안내를 그 화면으로 옮긴다.
    private fun showSpeedPermissionNotice() {
        val status = findViewById<TextView>(R.id.status)
        status.setOnClickListener(null)
        status.isClickable = false
        when {
            isGranted(Car.PERMISSION_SPEED) -> status.setText(R.string.app_name)

            isSpeedBlocked() -> status.setText(R.string.speed_permission_blocked)

            else -> {
                status.setText(R.string.speed_permission_needed)
                status.setOnClickListener { requestMissingPermissions() }
            }
        }
    }

    private fun requestMissingPermissions() {
        val missing = missingPermissions()
        if (missing.isEmpty()) return
        rememberSpeedRationale(shouldShowRequestPermissionRationale(Car.PERMISSION_SPEED))
        requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST)
    }

    // 앱 화면이 표시된 상태에서 --activity-single-top으로 장면 이름을 전달하면 onCreate 대신 이 함수가 호출된다.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        demoScene = demoSceneOf(intent)
        startBusyService()
    }

    // 최상위 화면에서 finish()로 종료하면 Pleos 심사에서 반려될 수 있어서(CCP 개발 가이드), Back(CCP의 Back 키 포함)을
    // 누르면 앱을 종료하지 않고 뒤로 보낸다. onBackPressed는 API 33부터 사용 중단됐지만, 매니페스트에서
    // enableOnBackInvokedCallback을 true로 설정하지 않은 앱에는 계속 호출된다.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST) return
        // 차속 권한을 두 번 거절하면 Android가 더는 묻지 않고, shouldShowRequestPermissionRationale이 true에서 false로 바뀐다.
        // 처음 묻는 창을 Back으로 닫으면 거절로 처리되지 않아 다시 물을 수 있는데, 이때도 이 값은 false다(2026-11-07 에뮬레이터).
        // 지금 값만으로는 두 경우를 구별할 수 없으므로 요청 전 값과 비교한다.
        val rationaleNow = shouldShowRequestPermissionRationale(Car.PERMISSION_SPEED)
        if (!isGranted(Car.PERMISSION_SPEED) && speedRationaleBeforeRequest() && !rationaleNow) {
            markSpeedBlocked()
        }
        // 차속 권한을 거절하면 차속 없이 판단하고 알림 권한을 거절하면 알림만 표시되지 않으므로, 권한 요청 결과와 관계없이 서비스를 시작한다.
        startBusyService()
    }

    private fun startBusyService() {
        val service =
            Intent(this, BusyService::class.java).putExtra(BusyService.EXTRA_DEMO_SCENE, demoScene)
        startForegroundService(service)
        // 권한 요청이 끝나거나 사용자가 안내를 눌러 서비스 시작을 다시 요청할 때 같은 시연이 처음부터 재생되지 않도록,
        // 시연 장면은 한 번만 전달하고 demoScene을 초기화한다.
        demoScene = null
    }

    // 다른 앱이 인텐트로 시연을 시작해 실내 조명 색을 변경하지 못하도록, 시연 장면은 adb로 시험하는 디버그 빌드에서만
    // 인텐트에서 가져온다. 출시 빌드에서는 앱 화면의 시연 버튼으로 시작한다. 시연 버튼은 앱 화면 작업에서 추가한다.
    private fun demoSceneOf(intent: Intent): String? =
        if (BuildConfig.DEBUG) intent.getStringExtra(BusyService.EXTRA_DEMO_SCENE) else null
}
