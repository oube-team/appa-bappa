package studio.oube.appabappa.status

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.VehicleSignals

// 시연 재생 화면에 표시할 장면의 진행 상황이다. signals와 engineState는 시연 장면이 만든 신호와 그 신호로 판단한 결과다.
data class DemoProgress(
    val sceneId: String,
    val elapsedSeconds: Double,
    val durationSeconds: Double,
    val signals: VehicleSignals,
    val engineState: BusyState
)

// 직접 조작 시연 화면에 표시할 판단 결과다. recoverHoldMillis는 RECOVERING을 유지하는 시간이다.
data class DriveProgress(val status: BusyStatus, val recoverHoldMillis: Long)

// 서비스가 실행 중이 아니면 기어를 알 수 없으므로, 서비스가 종료된 뒤에도 설정 화면을 열 수 있도록 parked의 기본값을
// true로 설정한다. approaching은 바쁜 이유로 알리는 안내 지점에 아직 닿기 전이면 true다(Reason.isApproaching).
data class MonitorState(
    val mood: Mood = Mood.FREE,
    val reason: Reason? = null,
    val approaching: Boolean = false,
    val parked: Boolean = true,
    val demo: DemoProgress? = null,
    val drive: DriveProgress? = null
)

// 서비스와 앱 화면은 같은 프로세스에서 실행되므로, 서비스가 판단한 상태를 프로세스 안의 StateFlow로 화면에 전달한다.
object BusyMonitor {
    private val mutableState = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = mutableState.asStateFlow()

    fun publish(state: MonitorState) {
        mutableState.value = state
    }
}
