package studio.oube.appabappa.demo

import studio.oube.appabappa.AppLog
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.status.DemoProgress

// 앱 화면이나 adb 명령으로 요청한 시연 장면을 기어가 P일 때 재생하고, 재생 중인 장면의 신호로 판단한 상태를 반환한다.
// 단위 테스트에서 Android 로그 없이 실행할 수 있도록 기록 함수를 생성자 매개변수로 전달받는다(기본값은 AppLog). warn은
// 재생하지 못한 요청을 기록한다.
internal class DemoPlayer(
    private val log: (String) -> Unit = AppLog::i,
    private val warn: (String) -> Unit = AppLog::w
) {
    private var requestedId: String? = null
    private var run: DemoRun? = null

    val sceneId: String? get() = run?.scene?.id
    val progress: DemoProgress? get() = run?.progress

    fun request(id: String) {
        requestedId = id
    }

    fun stop() {
        requestedId = null
        run?.let { log("demo ${it.scene.id} stopped from the app screen") }
        run = null
    }

    // 실제 기어 값을 수신하기 전(null)에는 P인지 확인할 수 없어서, 값을 수신할 때까지 요청한 장면의 재생을 보류한다.
    // 재생 중인 장면이 없거나 장면이 끝나면 null을 반환한다.
    fun update(realGear: Gear?, nowMillis: Long): BusyStatus? {
        if (realGear != null) startRequested(realGear, nowMillis)
        val current = run
        if (current == null || realGear == null) return null
        val status = current.update(realGear, nowMillis)
        if (status == null) {
            val reason = if (realGear == Gear.PARK) "finished" else "stopped, gear $realGear"
            log("demo ${current.scene.id} $reason")
            run = null
        }
        return status
    }

    private fun startRequested(gear: Gear, nowMillis: Long) {
        val id = requestedId ?: return
        requestedId = null
        val scene = DemoScenes.find(id)
        when {
            scene == null -> {
                val known = DemoScenes.all.joinToString { it.id }
                warn("unknown demo scene $id, scenes: $known")
            }

            gear != Gear.PARK -> warn("demo $id needs gear P, now $gear")

            else -> {
                run = DemoRun(scene, nowMillis)
                log("demo $id started")
            }
        }
    }
}
