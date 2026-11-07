package studio.oube.appabappa

import android.util.Log

// Pleos 보안 가이드가 출시 전 디버그 로그 비활성화를 요구해서, 출시 빌드에서는 차량 신호와 상태 변화가 담긴
// 진행 로그를 기록하지 않는다. 경고는 원인을 찾는 데 필요해서 출시 빌드에도 기록한다.
// 경고에는 문제가 된 값만 담고, 상태 변화 같은 주행 기록은 담지 않는다.
internal object AppLog {
    private const val TAG = "AppaBappa"

    // 조명 반영 시간 측정처럼 조회를 반복하는 작업은 디버그 빌드에서 로그 단계를 DEBUG로 설정했을 때만 실행한다.
    // 설정 명령: adb shell setprop log.tag.AppaBappa DEBUG
    val isMeasuring: Boolean
        get() = BuildConfig.DEBUG && Log.isLoggable(TAG, Log.DEBUG)

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        Log.w(TAG, message, error)
    }
}
