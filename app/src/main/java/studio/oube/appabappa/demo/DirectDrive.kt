package studio.oube.appabappa.demo

import studio.oube.appabappa.AppLog
import studio.oube.appabappa.engine.BusyEngine
import studio.oube.appabappa.engine.BusyStatus
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.status.DriveProgress

// 화면이 정지하거나 닫혀서 이 시간 넘게 신호를 수신하지 못하면 직접 조작 시연이 끝난 것으로 판단한다.
private const val STALE_MILLIS = 1_000L

// 직접 조작 시연 화면이 보내는 가상의 차 신호를 서비스에 전달한다. 화면과 서비스는 같은 프로세스의 메인 스레드에서
// 실행되므로, 마지막으로 보낸 신호와 시각만 보관한다. 시각은 둘 다 SystemClock.elapsedRealtime()을 사용한다.
object DirectDrive {
    private var session = 0
    private var latest: VehicleSignals? = null
    private var sentAtMillis = 0L

    // 시연을 시작하거나 처음부터 다시 하면 서비스도 새 엔진으로 판단하도록 시연 번호를 변경하고, 그 번호를 반환한다.
    fun restart(): Int {
        latest = null
        return ++session
    }

    // 지금 시연 번호로 보낸 신호만 보관한다. "처음으로"를 누른 직후에는 이전 가상의 차가 한 번 더 신호를 보낼 수 있는데,
    // 그 신호를 새 시연의 신호로 판단하면 차속이 갑자기 0이 된 것으로 보여 급제동으로 판단하게 된다.
    fun send(session: Int, signals: VehicleSignals, nowMillis: Long) {
        if (session != this.session) return
        latest = signals
        sentAtMillis = nowMillis
    }

    fun end() {
        latest = null
    }

    // 시연 중이면 시연 번호와 마지막으로 보낸 신호를 반환한다.
    internal fun current(nowMillis: Long): Pair<Int, VehicleSignals>? =
        latest?.takeIf { nowMillis - sentAtMillis <= STALE_MILLIS }?.let { session to it }
}

// 직접 조작 시연 화면이 신호를 보내는 동안, 실제 기어가 P이면 그 신호로 판단한다. 실제 신호로 판단하는 엔진과
// 상태가 섞이지 않도록 별도의 엔진을 사용하고, 시연을 시작하거나 처음부터 다시 할 때마다 새 엔진으로 판단한다.
// 시작과 끝을 log로 기록한다. 단위 테스트에서 Android 로그 없이 실행할 수 있도록 기록 함수를 생성자 매개변수로
// 전달받는다(기본값은 AppLog).
internal class DirectDriveRun(private val log: (String) -> Unit = AppLog::i) {
    private var engine: BusyEngine? = null
    private var engineSession = -1

    var signals: VehicleSignals? = null
        private set
    var progress: DriveProgress? = null
        private set

    // 실제 기어 값을 수신하기 전(null)에는 P인지 확인할 수 없어서 판단하지 않는다. 시연 중이 아니면 null을 반환한다.
    fun update(realGear: Gear?, nowMillis: Long): BusyStatus? {
        val sending = DirectDrive.current(nowMillis)?.takeIf { realGear == Gear.PARK }
        if (sending == null) {
            stop()
            return null
        }
        val (session, sent) = sending
        val current = engine?.takeIf { engineSession == session } ?: start(session)
        val status = current.update(sent, nowMillis)
        signals = sent
        progress = DriveProgress(status, current.recoverHoldMillis)
        return status
    }

    private fun start(session: Int): BusyEngine {
        stop()
        log("direct drive $session started")
        engineSession = session
        return BusyEngine().also { engine = it }
    }

    private fun stop() {
        if (engine != null) log("direct drive $engineSession ended")
        engine = null
        signals = null
        progress = null
    }
}
