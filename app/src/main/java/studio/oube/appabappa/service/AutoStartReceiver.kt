package studio.oube.appabappa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import studio.oube.appabappa.AppLog

// 앱을 업데이트하면 실행 중이던 서비스가 종료되어서, 부팅 완료 알림뿐 아니라 업데이트 완료 알림(MY_PACKAGE_REPLACED)에서도
// 서비스를 시작한다.
private val startActions = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)

// 운전자가 앱을 열지 않아도 바쁜 상황을 판단하도록, 차가 부팅을 마치면 서비스를 시작한다.
// 시동을 끄고 켤 때 차가 다시 부팅하는지, Suspend 상태에서 Resume되는지는 확인하지 못했다.
// Android는 한 번도 열지 않은 앱(stopped 상태)에 부팅 완료와 업데이트 완료 알림을 보내지 않아서,
// 사용자가 권한 요청과 안내를 보기 전에는 서비스가 시작되지 않는다(2026-11-07 에뮬레이터).
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in startActions) return
        AppLog.i("${intent.action}, starting busy service")
        runCatching { context.startForegroundService(Intent(context, BusyService::class.java)) }
            .onFailure { AppLog.w("busy service start failed after ${intent.action}", it) }
    }
}
