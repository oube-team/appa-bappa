package studio.oube.appabappa.trip

import studio.oube.appabappa.engine.BusyConfig
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.VehicleSignals

// 바빴던 순간의 시작과 끝이다. 운전을 시작한 때부터 측정한 밀리초다.
data class BusySpan(val fromMillis: Long, val toMillis: Long) {
    val millis: Long get() = toMillis - fromMillis
}

// 운전 한 번의 기록이다. startedAt과 endedAt은 기기 시각(밀리초)이고, 그 차이는 운전한 시간과 같다.
data class TripSummary(
    val startedAt: Long,
    val endedAt: Long,
    val busySpans: List<BusySpan>,
    val withRiders: Boolean
) {
    val busyMillis: Long get() = busySpans.sumOf { it.millis }

    // 앱을 다시 열 때 보여 주도록 SharedPreferences에 문자열 하나로 저장한다.
    fun encode(): String {
        val spans = busySpans.joinToString(",") { "${it.fromMillis}-${it.toMillis}" }
        return "$startedAt;$endedAt;$withRiders;$spans"
    }

    companion object {
        private const val FIELD_COUNT = 4

        // 형식이 맞지 않으면(앱 업데이트로 형식이 바뀐 경우) null을 반환한다.
        fun decode(text: String): TripSummary? = runCatching {
            val parts = text.split(";")
            require(parts.size == FIELD_COUNT)
            val (startedAt, endedAt, withRiders) = parts
            TripSummary(
                startedAt = startedAt.toLong(),
                endedAt = endedAt.toLong(),
                busySpans = parts.last().split(",").filter { it.isNotEmpty() }.map(::spanOf),
                withRiders = withRiders.toBooleanStrict()
            )
        }.getOrNull()

        private fun spanOf(text: String): BusySpan {
            val (from, to) = text.split("-")
            return BusySpan(from.toLong(), to.toLong())
        }
    }
}

// 기어가 P를 벗어나면 운전을 시작하고 다시 P가 되면 끝낸 것으로 판단한다. 다시 주차하려고 P에서 R이나 D로 잠깐
// 변경한 것까지 기록하면 방금 끝난 운전의 기록을 덮어쓰므로, 전진하며 엔진이 주행으로 판단하는 차속(BusyConfig의
// movingSpeedKmh)을 넘은 적이 있는 운전만 기록한다. 차속 권한이 없으면 차속이 0이라 운전을 기록하지 않는다.
// 운전하는 동안 BUSY나 VERY_BUSY가 된 때부터 FREE나 SOON이 될 때까지를 바빴던 순간 하나로 센다. RECOVERING에서
// 다시 바빠지면 동승자는 계속 기다린 것이라 한 번으로 센다. 동승자는 운전하는 동안 조수석이나 뒷자리에 한 번이라도
// 앉아 있었으면 있는 것으로 판단한다. 운전 중에 프로세스가 종료되면 진행 중이던 기록은 사라지고, 서비스가 다시
// 시작한 때부터 새 운전으로 기록한다.
class TripRecorder(private val config: BusyConfig = BusyConfig()) {
    private var trip: OpenTrip? = null

    // 기록할 운전이 끝나면 그 기록을 반환한다. signals는 실제 차의 신호, nowMillis는 경과 시간(elapsedRealtime),
    // clockMillis는 기기 시각이다.
    fun update(
        signals: VehicleSignals,
        state: BusyState,
        riders: Boolean,
        nowMillis: Long,
        clockMillis: Long
    ): TripSummary? {
        val parked = signals.gear == Gear.PARK
        if (trip == null && !parked) trip = OpenTrip(nowMillis)
        val current = trip
        val moving = signals.gear == Gear.DRIVE && signals.speedKmh > config.movingSpeedKmh
        current?.track(state, riders, moving, nowMillis)
        if (current == null || !parked) return null
        trip = null
        return current.finish(nowMillis, clockMillis)
    }

    private class OpenTrip(private val startMillis: Long) {
        private val spans = mutableListOf<BusySpan>()
        private var busySince: Long? = null
        private var withRiders = false
        private var moved = false

        fun track(state: BusyState, riders: Boolean, moving: Boolean, nowMillis: Long) {
            if (riders) withRiders = true
            if (moving) moved = true
            val since = busySince
            if (since == null && (state == BusyState.BUSY || state == BusyState.VERY_BUSY)) {
                busySince = nowMillis
            } else if (since != null && (state == BusyState.FREE || state == BusyState.SOON)) {
                spans += BusySpan(since - startMillis, nowMillis - startMillis)
                busySince = null
            }
        }

        // 운전 중에 기기 시각이 조정돼도 운전한 시간이 틀리지 않도록, 출발 시각은 도착 시각에서 경과 시간을 빼서 정한다.
        // 전진한 적이 없는 운전이면 null을 반환한다.
        fun finish(nowMillis: Long, clockMillis: Long): TripSummary? {
            if (!moved) return null
            busySince?.let { spans += BusySpan(it - startMillis, nowMillis - startMillis) }
            val startedAt = clockMillis - (nowMillis - startMillis)
            return TripSummary(startedAt, clockMillis, spans.toList(), withRiders)
        }
    }
}
