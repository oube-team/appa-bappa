package studio.oube.appabappa.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.oube.appabappa.light.Brightness

private const val PREFERENCES = "settings"
private const val ENABLED_KEY = "enabled"
private const val LIGHT_ENABLED_KEY = "light_enabled"
private const val BRIGHTNESS_KEY = "brightness"
private const val WHO_KEY = "who"

// 설정 화면에서 고르는 호칭이다. 직접 입력하지 않고 목록에서 고르게 해서, 화면 문구의 조사("가", "이")가 맞는지 미리
// 확인할 수 있다.
val WhoOptions = listOf("아빠", "엄마", "운전자")

// 설정 화면에서 고른 값이다. enabled가 false면 서비스를 종료하고, 차가 부팅하거나 앱이 업데이트되어도 서비스를 시작하지
// 않는다.
data class AppSettings(
    val enabled: Boolean = true,
    val lightEnabled: Boolean = true,
    val brightness: Brightness = Brightness.MID,
    val who: String = WhoOptions.first()
)

// 설정 값을 SharedPreferences에 저장하고, 서비스와 앱 화면이 같은 값을 사용하도록 프로세스 안의 StateFlow로 알린다.
class SettingsStore private constructor(private val preferences: SharedPreferences) {
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = mutableState.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(mutableState.value)
        preferences.edit {
            putBoolean(ENABLED_KEY, next.enabled)
            putBoolean(LIGHT_ENABLED_KEY, next.lightEnabled)
            putString(BRIGHTNESS_KEY, next.brightness.name)
            putString(WHO_KEY, next.who)
        }
        mutableState.value = next
    }

    // 저장된 값이 목록에 없으면(앱 업데이트로 값이 바뀐 경우) 기본값을 사용한다.
    private fun load(): AppSettings {
        val defaults = AppSettings()
        val brightness = preferences.getString(BRIGHTNESS_KEY, null)
        val who = preferences.getString(WHO_KEY, null)
        return AppSettings(
            enabled = preferences.getBoolean(ENABLED_KEY, defaults.enabled),
            lightEnabled = preferences.getBoolean(LIGHT_ENABLED_KEY, defaults.lightEnabled),
            brightness =
                Brightness.entries.firstOrNull { it.name == brightness } ?: defaults.brightness,
            who = who?.takeIf { it in WhoOptions } ?: defaults.who
        )
    }

    companion object {
        private var instance: SettingsStore? = null

        // 서비스와 화면이 같은 저장소를 사용하도록 프로세스에 하나만 생성한다. 메인 스레드에서만 호출한다.
        fun of(context: Context): SettingsStore = instance ?: SettingsStore(
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        ).also { instance = it }
    }
}
