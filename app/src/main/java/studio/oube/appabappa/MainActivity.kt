package studio.oube.appabappa

import android.Manifest
import android.app.Activity
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import studio.oube.appabappa.service.BusyService

private const val PERMISSION_REQUEST = 1

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val missing = runtimePermissions().filterNot(::isGranted)
        if (missing.isEmpty()) {
            startBusyService()
        } else {
            requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 차속 권한을 거절하면 차속 없이 판단하고 알림 권한을 거절하면 알림만 표시되지 않으므로, 권한 요청 결과와 관계없이 서비스를 시작한다.
        if (requestCode == PERMISSION_REQUEST) startBusyService()
    }

    private fun runtimePermissions() = buildList {
        add(Car.PERMISSION_SPEED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isGranted(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun startBusyService() {
        startForegroundService(Intent(this, BusyService::class.java))
    }
}
