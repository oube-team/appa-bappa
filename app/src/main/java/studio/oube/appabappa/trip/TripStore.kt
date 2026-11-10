package studio.oube.appabappa.trip

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFERENCES = "trip"
private const val PENDING_KEY = "pending"

// 도착 뒤 화면에 보여 줄 마지막 운전 기록이다. 기어가 P가 될 때 앱 화면이 닫혀 있었으면 다음에 열 때 보여 주도록
// SharedPreferences에 저장하고, 사용자가 확인하면 삭제한다.
class TripStore private constructor(private val preferences: SharedPreferences) {
    private val mutablePending =
        MutableStateFlow(preferences.getString(PENDING_KEY, null)?.let(TripSummary::decode))
    val pending: StateFlow<TripSummary?> = mutablePending.asStateFlow()

    // 도착 뒤 화면은 방금 끝난 운전만 알리므로, 바빴던 순간이 없는 운전이 끝나면 앞서 확인하지 않은 기록도 삭제한다.
    fun finish(trip: TripSummary) {
        val shown = trip.takeIf { it.busySpans.isNotEmpty() }
        preferences.edit {
            if (shown == null) remove(PENDING_KEY) else putString(PENDING_KEY, shown.encode())
        }
        mutablePending.value = shown
    }

    fun dismiss() {
        preferences.edit { remove(PENDING_KEY) }
        mutablePending.value = null
    }

    companion object {
        private var instance: TripStore? = null

        // 서비스와 화면이 같은 기록을 사용하도록 프로세스에 하나만 생성한다. 메인 스레드에서만 호출한다.
        fun of(context: Context): TripStore = instance ?: TripStore(
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        ).also { instance = it }
    }
}
