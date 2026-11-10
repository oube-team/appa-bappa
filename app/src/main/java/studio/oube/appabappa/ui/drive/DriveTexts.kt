package studio.oube.appabappa.ui.drive

import kotlin.math.abs
import kotlin.math.roundToInt
import studio.oube.appabappa.demo.CITY_LIMIT_KMH
import studio.oube.appabappa.demo.DriveGuide
import studio.oube.appabappa.demo.DriveHint
import studio.oube.appabappa.demo.HARD_BRAKE_MIN_KMH
import studio.oube.appabappa.demo.ROAD_SIGNAL_METERS
import studio.oube.appabappa.engine.BusyCause
import studio.oube.appabappa.engine.BusyConfig
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.status.DriveProgress

private const val MILLIS_PER_SECOND = 1_000
private const val NOT_BUSY = "바쁜 상황이 아니에요"

// ruleText는 프레임마다 호출하므로, 기본 판단 기준을 호출할 때마다 새로 생성하지 않는다.
private val DefaultConfig = BusyConfig()

// 숫자는 엔진의 판단 기준(BusyConfig)에서 가져와서, 기준을 변경하면 문장도 함께 바뀐다. 3.5처럼 소수가 있으면
// 그대로, 60.0처럼 소수가 없으면 정수로 표시한다.
internal fun Double.text(): String = if (this % 1.0 == 0.0) roundToInt().toString() else toString()

// 직접 조작 시연의 판단 표시에서 엔진이 지금 상태를 정한 근거를 한 문장으로 보여 준다. 서비스가 아직 판단하지
// 않았으면(progress가 null) 바쁘지 않은 것으로 보여 준다.
internal fun ruleText(
    progress: DriveProgress?,
    signals: VehicleSignals,
    config: BusyConfig = DefaultConfig
): String {
    val status = progress?.status
    return when (status?.state) {
        BusyState.VERY_BUSY, BusyState.BUSY -> busyRule(status.cause, signals, config)

        BusyState.RECOVERING -> {
            val seconds = progress.recoverHoldMillis / MILLIS_PER_SECOND
            "다시 바빠질 수 있어서 ${seconds}초 동안 주황을 유지해요"
        }

        BusyState.SOON -> soonRule(config)

        BusyState.FREE, null -> freeRule(signals, config)
    }
}

private fun soonRule(config: BusyConfig) = "안내 지점까지 ${config.soonSeconds.text()}초 안에 도착해요"

private fun busyRule(cause: BusyCause?, signals: VehicleSignals, config: BusyConfig): String =
    when (cause) {
        BusyCause.HARD_BRAKING ->
            "${config.hardBrakeWindowMillis / MILLIS_PER_SECOND}초 사이에 차속이 " +
                "초속 ${config.hardBrakeDropMps.text()}m 이상 줄었어요"

        BusyCause.FAST_SIGNALING -> "시속 ${config.highSpeedKmh.text()}km 이상에서 방향지시등을 켰어요"

        BusyCause.REVERSING -> "기어가 R이에요"

        BusyCause.MANEUVER -> {
            val meters = signals.nextManeuver?.distanceMeters
            if (meters == null || meters <= config.passingMeters) {
                "안내 지점 ${config.passingMeters.text()}m 안으로 들어왔어요"
            } else {
                "안내 지점까지 ${config.passingSeconds.text()}초 안에 도착해요"
            }
        }

        BusyCause.SIGNALING -> "주행 중에 방향지시등을 켰어요"

        BusyCause.TURNING -> "주행 중에 스티어링 휠을 ${config.turningAngleDeg.text()}도 이상 돌렸어요"

        BusyCause.FORWARD_COLLISION_WARNING -> "차가 전방 충돌 경고를 보냈어요"

        BusyCause.BLIND_SPOT_WARNING -> "차가 후측방 경고를 보냈어요"

        BusyCause.UPCOMING_MANEUVER, null -> soonRule(config)
    }

// 바쁘지 않은데 방향지시등을 켰거나 스티어링 휠을 돌렸으면, 왜 바쁘지 않은지 알려 준다.
private fun freeRule(signals: VehicleSignals, config: BusyConfig): String = when {
    signals.gear == Gear.PARK -> "기어가 P라서 바쁘지 않아요"

    signals.speedKmh > config.movingSpeedKmh -> NOT_BUSY

    signals.turnSignal != TurnSignal.OFF -> "${slowSpeed(config)} 방향지시등을 켜도 바쁘지 않아요"

    abs(signals.steeringAngleDeg) >= config.turningAngleDeg ->
        "${slowSpeed(config)} 스티어링 휠을 돌려도 바쁘지 않아요"

    else -> NOT_BUSY
}

private fun slowSpeed(config: BusyConfig) = "시속 ${config.movingSpeedKmh.text()}km 이하에서는"

// 내비게이션 안내의 이름이다. 안내 버튼과 앞유리의 안내 표시가 함께 사용한다.
internal fun DriveGuide.label(): String = when (this) {
    DriveGuide.MERGE -> "합류"
    DriveGuide.LEFT_TURN -> "좌회전"
}

// 조작을 반영하지 않았거나 다음에 할 조작을 알릴 때 화면에 잠깐 표시하는 문장이다. 세로 창에서 두 줄이 되는 긴 문장은
// 마지막 낱말만 아랫줄로 넘어가지 않도록 끊을 자리에 줄바꿈(\n)을 추가한다.
internal fun DriveHint.text(): String = when (this) {
    DriveHint.STOP_BEFORE_PARK -> "차가 멈춘 뒤에 P로 바꿀 수 있어요"

    DriveHint.STOP_BEFORE_REVERSE -> "차가 멈춘 뒤에 R로 바꿀 수 있어요"

    DriveHint.SHIFT_TO_DRIVE -> "출발하려면 기어를 D로 바꿔 보세요"

    DriveHint.NEUTRAL_GEAR -> "기어가 N이에요. D로 바꿔 보세요"

    DriveHint.REVERSE_STAYS -> "도로에서는 후진할 수 없어서 R에서는 움직이지 않아요"

    DriveHint.HARD_BRAKE_TOO_SLOW ->
        "시속 ${HARD_BRAKE_MIN_KMH.text()}km 이상에서 급제동을 시연할 수 있어요"

    DriveHint.HARD_BRAKE_NO_LEAD -> "같은 차로에 앞차가 있을 때 급제동을 시연할 수 있어요"

    DriveHint.SOLID_LINE -> "교차로와 그 앞 실선 구간에서는 차로를 바꿀 수 없어요"

    DriveHint.CITY_SPEED_LIMIT -> "도심 도로라 시속 ${CITY_LIMIT_KMH.text()}km까지 달릴 수 있어요"

    DriveHint.LEFT_TURN_TOO_FAST ->
        "좌회전 안내는 도심 도로 장면이라\n시속 ${CITY_LIMIT_KMH.text()}km 이하에서 켤 수 있어요"

    DriveHint.LEFT_TURN_LANE -> "좌회전하려면 왼쪽 방향지시등을 켜고 1차로로 옮겨 보세요"

    DriveHint.LEFT_TURN_SIGNAL -> "좌회전하려면 왼쪽 방향지시등을 켜 보세요"

    DriveHint.LEFT_TURN_NEEDS_LANE -> "1차로가 아니라서 좌회전하지 않고 직진해요"

    DriveHint.LEFT_TURN_NEEDS_SIGNAL ->
        "왼쪽 방향지시등을 ${ROAD_SIGNAL_METERS.text()}m 전부터 켜지 않아서\n좌회전하지 않고 직진해요"

    DriveHint.MERGE_TOO_EARLY -> "합류 구간까지는 실선이라 아직 차로를 바꿀 수 없어요"

    DriveHint.MERGE_SIGNAL -> "합류하려면 왼쪽 방향지시등을 켜 보세요"

    DriveHint.MERGE_FORCED -> "진입로가 끝나서 본선으로 들어가요\n합류할 때는 왼쪽 방향지시등을 켜 보세요"
}
