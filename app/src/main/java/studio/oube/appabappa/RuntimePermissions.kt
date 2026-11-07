package studio.oube.appabappa

import android.Manifest
import android.car.Car
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit

private const val PERMISSION_PREFERENCES = "permissions"
private const val SPEED_RATIONALE_KEY = "speed_rationale_before_request"
private const val SPEED_BLOCKED_KEY = "speed_blocked"

// 실행 중에 사용자에게 허락받는 권한이다. 차속 권한은 운전자가 바쁜지 판단하는 데 꼭 필요하다.
// 알림 권한은 포그라운드 서비스 알림을 표시하는 데 사용한다.
private fun runtimePermissions() = buildList {
    add(Car.PERMISSION_SPEED)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

internal fun Context.isGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

internal fun Context.missingPermissions(): List<String> = runtimePermissions().filterNot {
    isGranted(it)
}

private fun Context.permissionPreferences() =
    getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)

// 권한 요청 창이 표시되어 있는 동안 화면이 다시 만들어질 수 있어서, 요청 전의 shouldShowRequestPermissionRationale
// 값을 SharedPreferences에 저장한다. 이 값은 차속 권한을 한 번 거절한 뒤에 true다.
internal fun Context.rememberSpeedRationale(shouldShow: Boolean) =
    permissionPreferences().edit { putBoolean(SPEED_RATIONALE_KEY, shouldShow) }

internal fun Context.speedRationaleBeforeRequest() =
    permissionPreferences().getBoolean(SPEED_RATIONALE_KEY, false)

// 차속 권한을 두 번 거절해서 Android가 더는 묻지 않는 상태를 저장한다. 앱을 삭제하면 이 값도 삭제된다.
internal fun Context.markSpeedBlocked() =
    permissionPreferences().edit { putBoolean(SPEED_BLOCKED_KEY, true) }

internal fun Context.isSpeedBlocked() = permissionPreferences().getBoolean(SPEED_BLOCKED_KEY, false)
