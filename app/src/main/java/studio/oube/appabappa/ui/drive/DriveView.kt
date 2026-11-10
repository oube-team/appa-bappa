package studio.oube.appabappa.ui.drive

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlin.math.roundToInt
import studio.oube.appabappa.demo.DirectDrive
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveSim
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.status.DriveProgress

private const val MILLIS_PER_SECOND = 1_000.0

// 화면이 잠시 정지했다가 다시 그려져도 차가 한 번에 멀리 가지 않도록, 한 프레임에 0.2초까지만 움직인다.
private const val MAX_STEP_SECONDS = 0.2

// 도로 그림은 초당 30번까지만 다시 그린다.
private const val ROAD_FRAME_MILLIS = 33L

// 앞유리 위 판단 카드의 값이다.
@Immutable
internal data class JudgeInfo(val state: BusyState, val rule: String)

// 앞유리에 비치는 차속과 내비게이션 안내의 값이다.
@Immutable
internal data class GlassInfo(val speedKmh: Int, val guide: DriveGuide?, val guideMeters: Int)

// 프레임 루프가 가상의 차에서 화면에 보일 값을 꺼내 담는 곳이다. mutableStateOf에 같은 값을 설정하면 값이 바뀌지 않은
// 것으로 판단하므로, 값이 실제로 바뀐 부분만 다시 구성한다. 도로 그림은 road가 바뀔 때 다시 그리고, 하늘과 산의 각도
// (scenery)도 그때 함께 변경해서 풍경과 도로가 어긋나지 않게 한다. millis는 마지막 프레임 시각으로, 조작한 시각을 가상의
// 차에 전달할 때 사용한다. 화면 전체를 프레임마다 다시 구성하면 에뮬레이터에서 CPU 코어 하나의 절반 넘게
// 사용했다(2026-11-09 실측).
@Stable
internal class DriveView {
    var road by mutableLongStateOf(0L)
        private set
    var scenery by mutableDoubleStateOf(0.0)
        private set
    var cockpit by mutableStateOf<CockpitInfo?>(null)
        private set
    var glass by mutableStateOf(GlassInfo(0, null, 0))
        private set
    var judge by mutableStateOf(JudgeInfo(BusyState.FREE, ""))
        private set
    var hint by mutableStateOf<String?>(null)
        private set
    var millis = 0L
        private set

    private var shownRoad: RoadKey? = null

    fun update(sim: DriveSim, nowMillis: Long, progress: DriveProgress?) {
        millis = nowMillis
        cockpit = CockpitInfo.of(sim, nowMillis)
        glass = GlassInfo(sim.speedKmh.roundToInt(), sim.navGuide, sim.guideMeters.roundToInt())
        judge =
            JudgeInfo(progress?.status?.state ?: BusyState.FREE, ruleText(progress, sim.signals()))
        hint = sim.hintAt(nowMillis)?.text()
        val key = RoadKey.of(sim, nowMillis)
        if (key != shownRoad && nowMillis - road >= ROAD_FRAME_MILLIS) {
            shownRoad = key
            scenery = sim.sceneryRad
            road = nowMillis
        }
    }
}

// 도로 그림에 보이는 값이다. 안내선을 그리는 동안에는 AR 화살표가 시간에 따라 흘러가므로 시각(arrowMillis)도 포함한다.
private data class RoadKey(
    val travelledMeters: Double,
    val lateralMeters: Double,
    val yawRad: Double,
    val leadGapMeters: Double,
    val leadBraking: Boolean,
    val guide: DriveGuide?,
    val guideMeters: Double,
    val arrowMillis: Long?
) {
    companion object {
        fun of(sim: DriveSim, nowMillis: Long) = RoadKey(
            sim.travelledMeters,
            sim.lateralMeters,
            sim.yawRad,
            sim.leadGapMeters,
            sim.leadBraking(nowMillis),
            sim.guide,
            sim.guideMeters,
            nowMillis.takeIf { sim.showsGuideLine }
        )
    }
}

// 프레임마다 가상의 차를 움직이고, 화면에 보일 값을 갱신하고, 그 신호를 서비스에 보낸다. progress는 서비스가 마지막으로
// 판단한 결과다. 가상의 차마다 새 시연 번호로 신호를 보내서, 서비스가 차마다 새 엔진으로 판단하게 한다.
internal suspend fun drive(sim: DriveSim, view: DriveView, progress: () -> DriveProgress?) {
    val session = DirectDrive.restart()
    var last = withFrameMillis { it }
    while (true) {
        withFrameMillis { now ->
            sim.step(((now - last) / MILLIS_PER_SECOND).coerceAtMost(MAX_STEP_SECONDS), now)
            last = now
            view.update(sim, now, progress())
        }
        DirectDrive.send(session, sim.signals(), SystemClock.elapsedRealtime())
    }
}
