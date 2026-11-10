package studio.oube.appabappa.demo

import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.status.DemoProgress

private const val MILLIS_PER_SECOND = 1_000.0

// 실제 신호로 판단하는 엔진과 상태가 섞이지 않도록, 시연에는 별도의 엔진을 사용한다.
class DemoRun(val scene: DemoScene, private val startMillis: Long) {
    private val engine = BusyEngine()

    // 시연 재생 화면에 표시하도록 마지막으로 재생한 신호와 판단 결과를 보관한다.
    var progress: DemoProgress? = null
        private set

    // 주행 중에 시연 장면으로 조명 색이 바뀌지 않도록 실제 기어가 P일 때만 재생한다.
    // 실제 기어가 P가 아니거나 장면이 끝나면 null을 반환한다.
    fun update(realGear: Gear, nowMillis: Long): BusyStatus? {
        val elapsedMillis = nowMillis - startMillis
        val elapsedSeconds = elapsedMillis / MILLIS_PER_SECOND
        val signals = scene.signalsAt(elapsedSeconds)
        if (signals == null || realGear != Gear.PARK) return null
        val status = engine.update(signals, elapsedMillis)
        progress =
            DemoProgress(scene.id, elapsedSeconds, scene.durationSeconds, signals, status.state)
        return status
    }
}
