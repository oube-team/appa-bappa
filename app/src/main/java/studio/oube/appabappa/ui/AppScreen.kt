package studio.oube.appabappa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import studio.oube.appabappa.settings.AppSettings
import studio.oube.appabappa.status.MonitorState
import studio.oube.appabappa.trip.TripSummary
import studio.oube.appabappa.ui.drive.DriveScreen
import studio.oube.appabappa.ui.theme.Pleos
import studio.oube.appabappa.ui.theme.PleosTheme

// 앱 화면이 권한, 서비스, 설정에 요청하는 동작이다. showPermissionGuide는 권한 안내를 열거나(true) 닫는다(false).
@Stable
class AppActions(
    val continuePermission: () -> Unit,
    val showPermissionGuide: (Boolean) -> Unit,
    val playScene: (String) -> Unit,
    val stopDemo: () -> Unit,
    val changeSettings: ((AppSettings) -> AppSettings) -> Unit,
    val dismissTrip: () -> Unit
)

// 지금 상태 화면 위에 여는 화면이다.
internal enum class Sheet {
    NONE,
    SCENES,
    DRIVE,
    SETTINGS
}

// 앱 화면의 맨 위 컴포저블이다. 시스템의 야간 설정으로 주간과 야간 테마를 고르고, 창이 가로로 길면 가로 창 배치를
// 사용한다.
// speedNotice는 차속 권한 상태에 따른 안내(SPEED_NEEDED, SPEED_BLOCKED)이고, 허용됐으면 null이다.
// pendingTrip은 아직 확인하지 않은 마지막 운전 기록이다.
@Composable
fun AppScreen(
    monitor: MonitorState,
    settings: AppSettings,
    speedNotice: Notice?,
    permissionGuide: Boolean,
    pendingTrip: TripSummary?,
    actions: AppActions
) {
    var sheet by rememberSaveable { mutableStateOf(Sheet.NONE) }
    val demo = rememberDemoPlayback(monitor.demo?.sceneId, actions)
    val parked = monitor.parked
    // 주행을 시작하면 열어 둔 시연 목록이나 설정을 닫아서, 차를 세웠을 때 지금 상태 화면이나 도착 뒤 화면부터 보여 준다.
    // 서비스가 재생 요청을 처리하지 못해 시작하지 못한 시연도 초기화해서, 차를 세웠을 때 진행하지 않는 시연 화면이
    // 표시되지 않게 한다.
    LaunchedEffect(parked) {
        if (!parked) {
            sheet = Sheet.NONE
            if (demo.playing != null) demo.stop()
        }
    }
    BackHandler(enabled = demo.playing != null, onBack = demo::stop)
    BackHandler(enabled = demo.playing == null && sheet != Sheet.NONE) { sheet = Sheet.NONE }
    // 권한 안내는 Back으로 닫고 지금 상태 화면으로 돌아간다. 닫은 안내는 지금 상태 화면의 버튼으로 다시 열 수 있다.
    BackHandler(enabled = parked && permissionGuide) { actions.showPermissionGuide(false) }
    PleosTheme(night = isSystemInDarkTheme()) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Pleos.colors.background)) {
            val wide = maxWidth > maxHeight
            val layout = remember(wide) { AppLayout(wide) }
            val screen = AppContent(monitor, settings, speedNotice, layout)
            // 주행 중에는 지금 상태 화면만 보여 준다(기어가 P가 아니면 서비스가 시연을 종료한다). 주행을 시작하거나
            // 마칠 때 지금 상태 화면을 새로 구성하면 가장자리 빛과 문구가 서서히 바뀌지 않고 한 번에 바뀌므로, 지금 상태
            // 화면은 한 자리(else)에서만 호출한다.
            val scene = demo.playing.takeIf { parked }
            val trip = pendingTrip.takeIf { parked }
            val open = if (parked) sheet else Sheet.NONE
            when {
                parked && permissionGuide -> screen.Permission(actions.continuePermission)

                scene != null -> screen.Demo(scene, demo::stop)

                trip != null -> screen.After(trip, actions.dismissTrip)

                open == Sheet.DRIVE -> screen.Drive(onStop = { sheet = Sheet.NONE })

                open == Sheet.SCENES ->
                    screen.Scenes(
                        onBack = { sheet = Sheet.NONE },
                        onPlay = { id ->
                            demo.play(id)
                            sheet = Sheet.NONE
                        },
                        onDrive = { sheet = Sheet.DRIVE }
                    )

                open == Sheet.SETTINGS ->
                    screen.Settings(onBack = { sheet = Sheet.NONE }, actions.changeSettings)

                else -> screen.Status(actions) { sheet = it }
            }
        }
    }
}

// 화면마다 같은 상태, 설정, 배치를 반복해서 전달하지 않도록 묶어 둔다.
private class AppContent(
    val monitor: MonitorState,
    val settings: AppSettings,
    val speedNotice: Notice?,
    val layout: AppLayout
) {
    // 바쁜 순간 알리기를 끄면 서비스가 실행되지 않으므로, 차속 권한 안내 대신 TURNED_OFF 안내를 보여 준다.
    @Composable
    fun Status(actions: AppActions, open: (Sheet) -> Unit) {
        StatusScreen(
            mood = monitor.mood,
            reason = monitor.reason,
            approaching = monitor.approaching,
            who = settings.who,
            lightEnabled = settings.lightEnabled,
            parked = monitor.parked,
            canPlayScenes = settings.enabled,
            notice = if (settings.enabled) speedNotice else Notice.TURNED_OFF,
            layout = layout,
            onOpenScenes = { open(Sheet.SCENES) },
            onOpenSettings = { open(Sheet.SETTINGS) },
            onAskSpeed = { actions.showPermissionGuide(true) }
        )
    }

    @Composable
    fun Permission(onContinue: () -> Unit) {
        PermissionScreen(settings.who, layout, onContinue)
    }

    @Composable
    fun Demo(sceneId: String, onStop: () -> Unit) {
        DemoScreen(
            sceneId = sceneId,
            progress = monitor.demo?.takeIf { it.sceneId == sceneId },
            mood = monitor.mood,
            reason = monitor.reason,
            approaching = monitor.approaching,
            who = settings.who,
            lightEnabled = settings.lightEnabled,
            layout = layout,
            onStop = onStop
        )
    }

    @Composable
    fun After(trip: TripSummary, onDone: () -> Unit) {
        AfterScreen(trip, settings.who, layout, onDone)
    }

    @Composable
    fun Scenes(onBack: () -> Unit, onPlay: (String) -> Unit, onDrive: () -> Unit) {
        ScenesScreen(layout, onBack, onPlay, onDrive)
    }

    @Composable
    fun Drive(onStop: () -> Unit) {
        DriveScreen(monitor, settings, layout.wide, onStop)
    }

    @Composable
    fun Settings(onBack: () -> Unit, onChange: ((AppSettings) -> AppSettings) -> Unit) {
        SettingsScreen(settings, layout, onBack, onChange)
    }
}

// 재생을 요청한 장면이다. 서비스가 그 장면을 재생하기 시작하면 started가 true가 되고, 장면이 끝나거나 기어가 바뀌어
// 서비스가 시연을 종료하면 playing을 초기화해 지금 상태 화면으로 돌아온다.
@Stable
private class DemoPlayback(
    private val playingState: MutableState<String?>,
    private val startedState: MutableState<Boolean>,
    private val actions: AppActions
) {
    val playing: String? get() = playingState.value

    fun play(id: String) {
        actions.playScene(id)
        playingState.value = id
        startedState.value = false
    }

    fun stop() {
        actions.stopDemo()
        playingState.value = null
        startedState.value = false
    }

    fun follow(runningScene: String?) {
        if (playing != null && runningScene == playing) startedState.value = true
        if (startedState.value && runningScene == null) {
            playingState.value = null
            startedState.value = false
        }
    }
}

@Composable
private fun rememberDemoPlayback(runningScene: String?, actions: AppActions): DemoPlayback {
    val playing = rememberSaveable { mutableStateOf<String?>(null) }
    val started = rememberSaveable { mutableStateOf(false) }
    val playback = remember(actions) { DemoPlayback(playing, started, actions) }
    LaunchedEffect(playing.value, runningScene) { playback.follow(runningScene) }
    return playback
}
