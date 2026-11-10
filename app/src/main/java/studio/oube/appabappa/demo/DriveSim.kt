package studio.oube.appabappa.demo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt
import studio.oube.appabappa.demo.DriveRoad.CROSS_ENTRY_M
import studio.oube.appabappa.demo.DriveRoad.CROSS_EXIT_M
import studio.oube.appabappa.demo.DriveRoad.LANE_WIDTH_M
import studio.oube.appabappa.demo.DriveRoad.LEFT_LANE_M
import studio.oube.appabappa.demo.DriveRoad.MERGE_ZONE_M
import studio.oube.appabappa.demo.DriveRoad.RAMP_LANE_M
import studio.oube.appabappa.demo.DriveRoad.RIGHT_LANE_M
import studio.oube.appabappa.demo.DriveRoad.SOLID_FAR_M
import studio.oube.appabappa.demo.DriveRoad.TURN_RADIUS_M
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.KMH_PER_MPS
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals

// 시연 도로는 편도 3차로이고, 경찰청장이 별도로 지정한 구간이 아닌 고속도로의 최고속도는 시속 100km이므로(도로교통법
// 시행규칙 제19조 제1항 제3호 나목), 가속 페달을 계속 눌러도 차속이 시속 100km를 넘지 않게 한다.
private const val MAX_SPEED_MPS = 100 / KMH_PER_MPS

// 좌회전 장면의 교차로는 도심에 있어서, 장면이 있는 동안에는 차속이 도시부 일반도로 제한속도(도로교통법 시행규칙
// 제19조, 2021-04-17 시행)인 시속 50km를 넘지 않게 한다. 화면의 알림 문장도 이 값을 사용한다.
internal const val CITY_LIMIT_KMH = 50.0
private const val CITY_LIMIT_MPS = CITY_LIMIT_KMH / KMH_PER_MPS
private const val ACCEL_MPS2 = 2.8

// 브레이크 페달은 엔진의 급제동 기준(1초 사이에 초속 3.5m)보다 천천히, 급제동 버튼은 그보다 빠르게 감속한다.
// 서비스는 0.25초마다 차속을 확인하므로, 확인이 늦어지면 1초보다 긴 구간의 감속을 1초 사이의 감속으로 판단한다.
// 1초에 초속 3m씩 줄이면 확인이 약 0.17초만 늦어져도 급제동으로 판단할 수 있어서, 브레이크 페달을 누르면 1초에
// 초속 2.5m씩 줄인다(확인이 0.4초 늦어지기 전까지는 급제동으로 판단하지 않는다).
private const val BRAKE_MPS2 = 2.5
private const val HARD_BRAKE_MPS2 = 7.0

// 급제동 버튼을 누르면 앞차가 먼저 브레이크등을 켜고, 0.3초 뒤부터 1.4초까지 우리 차가 감속한다.
private const val HARD_BRAKE_DELAY_MILLIS = 300L
private const val HARD_BRAKE_END_MILLIS = 1_400L

// 엔진은 1초 사이에 차속이 초속 3.5m(시속 12.6km) 이상 줄어야 급제동으로 판단하므로, 그보다 여유를 둔 이 차속
// 이상에서만 급제동을 시연한다. 화면의 알림 문장도 이 값을 사용한다.
internal const val HARD_BRAKE_MIN_KMH = 15.0

private const val HINT_MILLIS = 2_200L
private const val GEAR_CHANGE_MAX_KMH = 5.0
private const val SPEED_EPSILON = 0.0001

// 손을 떼면 스티어링 휠이 초당 420도로 돌아온다. 방향지시등은 스티어링 휠을 켠 쪽으로 60도 이상 돌린 뒤 손을 뗀
// 스티어링 휠이 10도 안으로 돌아오면 꺼진다.
private const val WHEEL_LIMIT_DEG = 450.0
private const val WHEEL_RETURN_DEG_PER_S = 420.0
private const val SIGNAL_ARM_DEG = 60.0
private const val SIGNAL_CANCEL_DEG = 10.0

// 스티어링 휠을 돌리면 앞유리 풍경도 그쪽으로 돈다(라디안). 스티어링 휠 각도에 비례하고, 시속 15km까지는 차속에
// 비례해 줄인다.
private const val LOOK_PER_WHEEL_RAD = 0.08
private const val LOOK_LIMIT_RAD = 0.45
private const val LOOK_FULL_KMH = 15.0
private const val LOOK_FOLLOW_PER_S = 3.0

// 앞유리 풍경의 각도와 앞차 거리는 목표값에 조금씩 다가가므로, 이만큼 가까워지면 목표값으로 설정한다. 화면은 값이
// 바뀔 때만 다시 그리는데, 목표값으로 설정하지 않으면 정지한 차의 도로 그림도 계속 다시 그리게 된다.
private const val LOOK_SNAP_RAD = 0.0001
private const val LEAD_SNAP_M = 0.01

private const val DEG_PER_RAD = 180 / PI
private const val LANE_HALF_M = LANE_WIDTH_M / 2

// 차로를 변경할 때 옆으로 움직이는 속도(m/s)다. 차속에 따라 0.6에서 1.2 사이로 정하되 차속의 0.216배를 넘지 않게
// 해서, 차가 정지하면 옆으로도 움직이지 않는다. 0.216은 차로를 변경할 수 있는 가장 낮은 차속(시속 10km)에서 초속
// 0.6m가 되는 비율이다.
private const val LATERAL_BASE_MPS = 0.4
private const val LATERAL_PER_SPEED = 0.035
private const val LATERAL_MIN_MPS = 0.6
private const val LATERAL_MAX_MPS = 1.2
private const val LATERAL_PER_FORWARD = 0.216
private const val LANE_CHANGE_MIN_KMH = 10.0

// 도로교통법 시행령 별표 2: 진로를 바꾸려는 지점 30m 전(고속도로는 100m 전)부터 신호한다. 일반도로 최고속도가 시속
// 80km(시행규칙 제19조)라서 그보다 빠르면 고속도로로 판단하고 100m를 적용한다. 자동차전용도로에도 100m를 적용하게
// 되지만 30m 이상이라는 기준은 지킨다. 합류 장면은 고속도로라서 차속과 관계없이 100m를 적용한다. 화면의 알림 문장도
// 일반도로의 값을 사용한다.
internal const val ROAD_SIGNAL_METERS = 30.0
private const val HIGHWAY_SIGNAL_METERS = 100.0
private const val HIGHWAY_KMH = 80.0

// 좌회전은 시속 15km로 돈다. 반지름 8m의 호를 도는 데 필요한 스티어링 휠 각도는 축간거리 2.9m, 조향비 15:1로
// 계산하면 약 300도이고, 호의 처음과 끝 0.4라디안 동안 스티어링 휠을 돌리고 되돌린다. 0.4라디안은 시속 15km에서
// 스티어링 휠이 초당 약 390도로 돌게 되는 값으로, 손을 뗀 스티어링 휠이 돌아오는 속도(초당 420도)보다 느리다.
private const val TURN_SPEED_MPS = 15 / KMH_PER_MPS
private const val TURN_WHEEL_DEG = 300.0
private const val TURN_WHEEL_RAMP_RAD = 0.4
private const val QUARTER_TURN_RAD = PI / 2

// 1차로에 있는데 왼쪽 방향지시등을 켜지 않았으면, 켜야 하는 자리(교차로 30m 앞)보다 50m 먼저 알린다.
private const val TURN_REMIND_MARGIN_M = 50.0

// 도로교통법 제19조 제1항은 앞차가 갑자기 정지해도 충돌을 피할 수 있는 거리를 두라고만 하고 숫자는 정하지 않아서,
// 같은 차로의 앞차와는 흔히 사용하는 2초 거리를 둔다. 앞차가 다른 차로에 있으면 그 옆을 지나치지 않도록 18m 더
// 둔다.
private const val LEAD_MIN_GAP_M = 12.0
private const val LEAD_GAP_SECONDS = 2.0
private const val LEAD_OTHER_LANE_EXTRA_M = 18.0
private const val LEAD_FOLLOW_PER_S = 0.6

// 차가 거의 정지하면 앞차도 정지해 있는 것으로 판단하고 앞차의 브레이크등을 켠다.
private const val LEAD_STOPPED_MPS = 0.3

// 직접 조작 시연에서 고르는 내비게이션 안내다. 안내를 고르면 그 장면(합류하는 진입로, 좌회전할 교차로)이 도로에 놓인다.
enum class DriveGuide(val turnTypeName: String, val startMeters: Double) {
    MERGE("TURN_DEVIDE_MERGE", 500.0),
    LEFT_TURN("TURN_LEFT", 200.0)
}

// 안내 지점을 지난 뒤에도 장면의 그림이 차 뒤로 지나갈 때까지 장면을 유지하는 거리다. 교차로는 건너편 횡단보도까지,
// 합류 장면은 진입로의 끝까지다.
private val DriveGuide.tailMeters: Double
    get() =
        when (this) {
            DriveGuide.MERGE -> 1.0
            DriveGuide.LEFT_TURN -> CROSS_ENTRY_M + CROSS_EXIT_M + 1
        }

// 조작을 반영하지 않았거나 다음 조작을 알려 줄 때 화면에 잠깐 표시하는 알림이다.
enum class DriveHint {
    STOP_BEFORE_PARK,
    STOP_BEFORE_REVERSE,
    SHIFT_TO_DRIVE,
    NEUTRAL_GEAR,
    REVERSE_STAYS,
    HARD_BRAKE_TOO_SLOW,
    HARD_BRAKE_NO_LEAD,
    SOLID_LINE,
    CITY_SPEED_LIMIT,
    LEFT_TURN_TOO_FAST,
    LEFT_TURN_LANE,
    LEFT_TURN_SIGNAL,
    LEFT_TURN_NEEDS_LANE,
    LEFT_TURN_NEEDS_SIGNAL,
    MERGE_TOO_EARLY,
    MERGE_SIGNAL,
    MERGE_FORCED
}

// 직접 조작 시연에서 화면의 페달, 스티어링 휠, 기어, 방향지시등, 내비게이션 안내 조작으로 움직이는 가상의 차다.
// 차로, 앞차 거리, 앞유리 풍경의 각도는 도로 그림을 위한 값이고, 바쁜 상황은 서비스의 BusyEngine이 이 차의
// 신호(signals)로 판단한다. nowMillis에는 화면의 프레임 시각(밀리초)을 전달한다. 차로 변경과 합류와 좌회전의 규칙은
// 도로교통법에 맞춰 정했다. 화면의 조작마다, 그리고 프레임마다 실행하는 단계마다 함수가 하나씩 있어서 함수 수 기준을
// 넘는다.
@Suppress("TooManyFunctions")
class DriveSim {
    var speedMps = 0.0
        private set
    var gear = Gear.DRIVE
        private set
    var turn = TurnSignal.OFF
        private set

    // 왼쪽으로 돌리면 양수다(시연 장면의 좌회전과 같다).
    var steeringDeg = 0.0
        private set
    var accelHeld = false
        private set
    var brakeHeld = false
        private set

    // 도로에 놓인 장면과 안내 지점까지 남은 거리다. 안내 지점을 지나면 거리가 음수가 되고, 장면의 그림이 차 뒤로
    // 지나가면 장면이 끝난다.
    var guide: DriveGuide? = null
        private set
    var guideMeters = 0.0
        private set

    // 지금 달리는 도로를 따라 달린 거리와, 가운데 차로 중앙에서 오른쪽으로 떨어진 거리(오른쪽이 양수)다.
    var travelledMeters = 0.0
        private set
    var lateralMeters = 0.0
        private set
    var leadGapMeters = LEAD_MIN_GAP_M
        private set

    // 앞차는 가운데 차로를 지킨다. 좌회전하면 가로지르던 도로에도 같은 거리 앞의 가운데 차로에 앞차가 있다.
    val leadLateralMeters = 0.0
    internal var hint: DriveHint? = null
        private set

    private var holdingWheel = false
    private var slowingDown = false
    private var atCityLimit = false
    private var turnArmed = false
    private var signaledMeters = 0.0
    private var requiredSignalMeters = 0.0
    private var laneChangedForSignal = false
    private var laneChangeSide = TurnSignal.OFF
    private var solidWarned = false
    private var laneTarget = 0.0
    private var turningLeft = false
    private var headingRad = 0.0
    private var quarterTurns = 0
    private var lookRad = 0.0
    private var hardBrakeFrom = 0L
    private var hardBrakeUntil = 0L
    private var hintUntil = 0L
    private val reminded = HashSet<DriveHint>()

    val speedKmh: Double get() = speedMps * KMH_PER_MPS

    // 지금 달리는 도로를 기준으로 한 앞유리 풍경의 각도(왼쪽이 양수)다. 좌회전 호를 돈 각도에 스티어링 휠을 따라
    // 도는 각도를 더한다.
    val yawRad: Double get() = headingRad + lookRad

    // 출발할 때의 방향을 기준으로 한 앞유리 풍경의 각도다. 좌회전을 마칠 때마다 90도씩 더해서, 달리는 도로가 바뀌어도
    // 먼 산이 처음 자리로 되돌아가지 않게 한다.
    val sceneryRad: Double get() = quarterTurns * QUARTER_TURN_RAD + yawRad

    // 좌회전 호를 돈 각도다. 좌회전하는 중이 아니면 null이다.
    val turnRad: Double? get() = headingRad.takeIf { turningLeft }

    // 내비게이션이 안내하고 있는 장면이다. 안내 지점을 지나면 안내는 끝나고 장면의 그림만 남는다.
    val navGuide: DriveGuide? get() = guide?.takeIf { guideMeters > 0 }

    // 내비게이션은 안내 지점까지, 그리고 좌회전하는 동안 차로 위에 안내선을 그린다.
    val showsGuideLine: Boolean get() = navGuide != null || turningLeft

    // 좌회전 장면에서 가로지르는 도로를 기준으로 한 차의 자리다. 앞의 값은 그 도로의 가운데 차로 중앙에서 오른쪽으로
    // 떨어진 거리, 뒤의 값은 그 도로를 따라 달린 거리다. 좌회전을 마치면 이 값이 lateralMeters와 travelledMeters가 된다.
    val crossLateralMeters: Double get() = -(guideMeters + TURN_RADIUS_M + LANE_WIDTH_M)
    val crossTravelledMeters: Double get() = -lateralMeters

    private val inSameLaneAsLead: Boolean
        get() = abs(lateralMeters - leadLateralMeters) < LANE_HALF_M

    private val inLeftLane: Boolean
        get() = lateralMeters == LEFT_LANE_M && laneTarget == LEFT_LANE_M

    private val lateralMps: Double
        get() {
            val bySpeed = LATERAL_BASE_MPS + speedMps * LATERAL_PER_SPEED
            return minOf(
                bySpeed.coerceIn(LATERAL_MIN_MPS, LATERAL_MAX_MPS),
                speedMps * LATERAL_PER_FORWARD
            )
        }

    private val signalMetersHere: Double
        get() {
            val onHighway = guide == DriveGuide.MERGE || speedKmh > HIGHWAY_KMH
            return if (onHighway) HIGHWAY_SIGNAL_METERS else ROAD_SIGNAL_METERS
        }

    // 좌회전하려는 차는 왼쪽 방향지시등을 안내 지점 30m 전부터 켜고 있고, 1차로에 있거나 1차로로 옮기고 있다.
    private val headingToLeftTurn: Boolean
        get() =
            guide == DriveGuide.LEFT_TURN && guideMeters > 0 && laneTarget == LEFT_LANE_M &&
                turn == TurnSignal.LEFT && signaledMeters + guideMeters >= ROAD_SIGNAL_METERS

    // 안내 지점에 닿을 때 1차로에 들어와 있어야 좌회전한다.
    private val willTurnLeft: Boolean get() = headingToLeftTurn && inLeftLane

    fun signals(): VehicleSignals = VehicleSignals(
        speedMps = speedMps,
        gear = gear,
        turnSignal = turn,
        steeringAngleDeg = steeringDeg,
        nextManeuver = navGuide?.let { Maneuver(it.turnTypeName, guideMeters) }
    )

    fun isHardBraking(nowMillis: Long) = nowMillis in hardBrakeFrom until hardBrakeUntil

    fun leadBraking(nowMillis: Long): Boolean {
        val weSlowDown = slowingDown || nowMillis < hardBrakeUntil
        return speedMps < LEAD_STOPPED_MPS || (inSameLaneAsLead && weSlowDown)
    }

    fun hintAt(nowMillis: Long): DriveHint? = hint?.takeIf { nowMillis < hintUntil }

    fun step(dtSeconds: Double, nowMillis: Long) {
        val before = speedMps
        speedMps = nextSpeed(dtSeconds, nowMillis)
        slowingDown = speedMps < before - SPEED_EPSILON
        val moved = speedMps * dtSeconds
        if (turningLeft) followTurn(moved) else advance(moved, nowMillis)
        steer(dtSeconds)
        if (!turningLeft) changeLane(dtSeconds, nowMillis)
        followLead(dtSeconds)
        remind(nowMillis)
    }

    fun pressAccel(pressed: Boolean, nowMillis: Long) {
        accelHeld = pressed
        if (!pressed) return
        when (gear) {
            Gear.DRIVE -> Unit
            Gear.PARK -> show(DriveHint.SHIFT_TO_DRIVE, nowMillis)
            Gear.NEUTRAL -> show(DriveHint.NEUTRAL_GEAR, nowMillis)
            Gear.REVERSE -> show(DriveHint.REVERSE_STAYS, nowMillis)
        }
    }

    fun pressBrake(pressed: Boolean) {
        brakeHeld = pressed
    }

    // 시속 5km 이하이면 정지한 것으로 판단해 P와 R을 고를 수 있고, 고르면 차속을 0으로 설정한다. 도로에서는 후진하지
    // 않으므로 R을 골라도 차는 정지해 있다.
    fun chooseGear(next: Gear, nowMillis: Long) {
        if (next == gear) return
        val needsStop = next == Gear.PARK || next == Gear.REVERSE
        if (needsStop && speedKmh > GEAR_CHANGE_MAX_KMH) {
            val stopFirst =
                if (next == Gear.PARK) DriveHint.STOP_BEFORE_PARK else DriveHint.STOP_BEFORE_REVERSE
            show(stopFirst, nowMillis)
            return
        }
        if (needsStop) speedMps = 0.0
        gear = next
    }

    fun toggleTurn(side: TurnSignal) = setTurn(if (turn == side) TurnSignal.OFF else side)

    // 좌회전 장면의 교차로는 도심에 있어서 좌회전 안내는 시속 50km 이하에서만 켠다. 안내를 고르면 장면이 바뀌므로,
    // 차로를 변경하는 동작 없이 차를 장면의 출발 차로에 둔다. 합류 장면은 진입로에서 출발하고, 진입로가 없는 장면을
    // 고르면 진입로에 있거나 진입로에서 본선으로 옮기던 차를 3차로에 둔다. 방향지시등을 켠 채 안내를 고르면 새 장면의
    // 규칙으로 신호 거리를 다시 센다. 좌회전하는 중에는 안내를 골라도 장면을 그대로 둔다.
    fun chooseGuide(next: DriveGuide?, nowMillis: Long) {
        if (turningLeft) return
        if (next == DriveGuide.LEFT_TURN && speedKmh > CITY_LIMIT_KMH) {
            show(DriveHint.LEFT_TURN_TOO_FAST, nowMillis)
            return
        }
        guide = next
        guideMeters = next?.startMeters ?: 0.0
        reminded.clear()
        when {
            next == DriveGuide.MERGE -> placeIn(RAMP_LANE_M)
            lateralMeters > RIGHT_LANE_M -> placeIn(RIGHT_LANE_M)
        }
        resetSignalCount()
    }

    // 급제동은 같은 차로의 앞차가 먼저 급하게 정지할 때만 시연한다(도로교통법 제19조 제4항은 위험방지를 위한 경우가
    // 아니면 급제동을 금지한다). 급제동하는 중에 버튼을 다시 눌러도 감속이 끊기지 않게 한다.
    fun hardBrake(nowMillis: Long) {
        when {
            nowMillis < hardBrakeUntil -> Unit

            speedKmh < HARD_BRAKE_MIN_KMH -> show(DriveHint.HARD_BRAKE_TOO_SLOW, nowMillis)

            !inSameLaneAsLead -> show(DriveHint.HARD_BRAKE_NO_LEAD, nowMillis)

            else -> {
                hardBrakeFrom = nowMillis + HARD_BRAKE_DELAY_MILLIS
                hardBrakeUntil = nowMillis + HARD_BRAKE_END_MILLIS
            }
        }
    }

    fun grabWheel() {
        holdingWheel = true
    }

    // clockwiseRad는 화면에서 스티어링 휠을 시계 방향으로 돌린 각도(라디안)다. 시계 방향은 오른쪽이므로 그만큼
    // 스티어링 휠 각도를 줄인다. 좌회전하는 동안에는 스티어링 휠이 호에 맞춰 스스로 돌므로, 손으로 돌려도 반영하지
    // 않는다.
    fun turnWheel(clockwiseRad: Double) {
        if (turningLeft) return
        val next = steeringDeg - clockwiseRad * DEG_PER_RAD
        steeringDeg = next.coerceIn(-WHEEL_LIMIT_DEG, WHEEL_LIMIT_DEG)
    }

    fun releaseWheel() {
        holdingWheel = false
    }

    private fun show(next: DriveHint, nowMillis: Long) {
        hint = next
        hintUntil = nowMillis + HINT_MILLIS
    }

    private fun setTurn(next: TurnSignal) {
        turn = next
        turnArmed = false
        resetSignalCount()
    }

    private fun resetSignalCount() {
        signaledMeters = 0.0
        requiredSignalMeters = 0.0
        laneChangedForSignal = false
        solidWarned = false
    }

    private fun placeIn(lane: Double) {
        lateralMeters = lane
        laneTarget = lane
    }

    // 좌회전 장면에서는 제한속도에 닿은 순간에만 알려서, 페달을 누르고 있는 동안 다른 알림을 덮지 않게 한다. 좌회전하려는
    // 차는 안내 지점에서 시속 15km가 되도록 미리 감속한다. 엔진이 급제동으로 판단하지 않도록 브레이크 페달과 같은
    // 감속을 사용하므로, 방향지시등을 늦게 켜서 감속할 거리가 모자라면 시속 15km보다 빠르게 돌기 시작해 도는 동안
    // 마저 감속한다.
    private fun nextSpeed(dtSeconds: Double, nowMillis: Long): Double {
        val change =
            when {
                isHardBraking(nowMillis) -> -HARD_BRAKE_MPS2
                brakeHeld -> -BRAKE_MPS2
                accelHeld && gear == Gear.DRIVE -> ACCEL_MPS2
                else -> 0.0
            }
        val inCity = guide == DriveGuide.LEFT_TURN
        val limit = if (inCity) CITY_LIMIT_MPS else MAX_SPEED_MPS
        val pushingLimit = inCity && change > 0 && speedMps >= limit
        if (pushingLimit && !atCityLimit) show(DriveHint.CITY_SPEED_LIMIT, nowMillis)
        atCityLimit = pushingLimit
        val pedalled = (speedMps + change * dtSeconds).coerceIn(0.0, limit)
        val cap = turnSpeedCap()
        if (cap == null || pedalled <= cap) return pedalled
        return maxOf(cap, speedMps - BRAKE_MPS2 * dtSeconds).coerceAtMost(pedalled)
    }

    // 1차로로 옮기는 동안에도 미리 감속한다. 1차로에 닿은 뒤에 감속을 시작하면, 실선 구간 직전에 차로를 변경한 차는
    // 안내 지점 바로 앞에서 1차로에 닿아서 달리던 차속 그대로 돌기 시작한다.
    private fun turnSpeedCap(): Double? = when {
        turningLeft -> TURN_SPEED_MPS
        headingToLeftTurn -> sqrt(TURN_SPEED_MPS * TURN_SPEED_MPS + 2 * BRAKE_MPS2 * guideMeters)
        else -> null
    }

    // 안내 지점에 닿으면 좌회전을 시작하거나, 조건을 갖추지 못했으면 이유를 알리고 직진한다. 좌회전할지는 안내 지점을
    // 지나기 전의 값으로 판단한다.
    private fun advance(moved: Double, nowMillis: Long) {
        travelledMeters += moved
        val scene = guide ?: return
        val arriving = guideMeters > 0 && guideMeters <= moved
        val turns = willTurnLeft
        guideMeters -= moved
        if (arriving && scene == DriveGuide.LEFT_TURN) {
            if (turns) startTurn() else missTurn(nowMillis)
        }
        if (guideMeters < -scene.tailMeters) {
            guide = null
            guideMeters = 0.0
        }
    }

    // 이번 프레임에 안내 지점을 지나친 거리만큼은 호를 따라 돈 것으로 계산한다.
    private fun startTurn() {
        val over = -guideMeters
        travelledMeters -= over
        guideMeters = 0.0
        turningLeft = true
        followTurn(over)
    }

    // 좌회전하지 않고 지나가면 좌회전하려던 행위가 끝난 것이므로, 켜 둔 왼쪽 방향지시등을 끈다.
    private fun missTurn(nowMillis: Long) {
        val reason =
            if (inLeftLane) DriveHint.LEFT_TURN_NEEDS_SIGNAL else DriveHint.LEFT_TURN_NEEDS_LANE
        show(reason, nowMillis)
        if (turn == TurnSignal.LEFT) setTurn(TurnSignal.OFF)
    }

    // 좌회전 호를 따라 돈다. 본선 방향으로 나아간 만큼만 본선을 따라 달린 거리와 안내 거리에 반영한다.
    private fun followTurn(moved: Double) {
        val next = (headingRad + moved / TURN_RADIUS_M).coerceAtMost(QUARTER_TURN_RAD)
        val along = DriveRoad.turnAlong(next) - DriveRoad.turnAlong(headingRad)
        travelledMeters += along
        guideMeters -= along
        headingRad = next
        lateralMeters = LEFT_LANE_M - DriveRoad.turnAcross(next)
        if (next >= QUARTER_TURN_RAD) finishTurn()
    }

    // 좌회전을 마치면 가로지르던 도로가 지금 달리는 도로가 되고 장면이 끝난다. 회전이 끝났으므로 방향지시등을 끈다.
    private fun finishTurn() {
        travelledMeters = crossTravelledMeters
        placeIn(LEFT_LANE_M)
        headingRad = 0.0
        quarterTurns++
        turningLeft = false
        guide = null
        guideMeters = 0.0
        setTurn(TurnSignal.OFF)
    }

    // 좌회전하는 동안에는 스티어링 휠이 호에 맞춰 스스로 돌고, 그 밖에는 손을 떼면 가운데로 돌아온다.
    private fun steer(dtSeconds: Double) {
        val step = WHEEL_RETURN_DEG_PER_S * dtSeconds
        if (turningLeft) {
            steeringDeg = approach(steeringDeg, turnWheelDeg(), step)
        } else if (!holdingWheel) {
            steeringDeg = approach(steeringDeg, 0.0, step)
        }
        if (turn == TurnSignal.LEFT && steeringDeg >= SIGNAL_ARM_DEG) turnArmed = true
        if (turn == TurnSignal.RIGHT && steeringDeg <= -SIGNAL_ARM_DEG) turnArmed = true
        if (turnArmed && !holdingWheel && abs(steeringDeg) < SIGNAL_CANCEL_DEG) {
            setTurn(TurnSignal.OFF)
        }
        look(dtSeconds)
    }

    private fun turnWheelDeg(): Double {
        val toEnd = QUARTER_TURN_RAD - headingRad
        return TURN_WHEEL_DEG * (minOf(headingRad, toEnd) / TURN_WHEEL_RAMP_RAD).coerceAtMost(1.0)
    }

    // 좌회전하는 동안에는 차의 방향이 실제로 돌기 때문에, 스티어링 휠을 따라 풍경을 더 돌리지 않는다.
    private fun look(dtSeconds: Double) {
        val speedShare = (speedKmh / LOOK_FULL_KMH).coerceAtMost(1.0)
        val byWheel = steeringDeg / DEG_PER_RAD * LOOK_PER_WHEEL_RAD * speedShare
        val target = if (turningLeft) 0.0 else byWheel.coerceIn(-LOOK_LIMIT_RAD, LOOK_LIMIT_RAD)
        lookRad += (target - lookRad) * (dtSeconds * LOOK_FOLLOW_PER_S).coerceAtMost(1.0)
        if (abs(target - lookRad) < LOOK_SNAP_RAD) lookRad = target
    }

    // 방향지시등을 켜고 법에서 정한 거리만큼 달리면 그쪽 차로로 한 번 변경한다. 신호는 그 행위가 끝날 때까지
    // 해야 하므로(도로교통법 제38조 제1항), 차로 변경이 끝나면 변경한 쪽의 방향지시등을 끈다. 좌회전하려고 1차로에
    // 들어온 경우에는 회전이 끝날 때까지 켜 둔다. 방향지시등을 켠 동안 한 번이라도 고속도로였으면 필요한 신호 거리를
    // 100m로 유지한다.
    private fun changeLane(dtSeconds: Double, nowMillis: Long) {
        val moving = lateralMeters != laneTarget
        lateralMeters = approach(lateralMeters, laneTarget, lateralStep(dtSeconds))
        val arrived = moving && lateralMeters == laneTarget
        val forLeftTurn = guide == DriveGuide.LEFT_TURN && guideMeters > 0 && inLeftLane
        if (arrived && turn == laneChangeSide && !forLeftTurn) setTurn(TurnSignal.OFF)
        mergeBeforeRampEnds(nowMillis)
        if (turn == TurnSignal.OFF) return
        signaledMeters += speedMps * dtSeconds
        requiredSignalMeters = maxOf(requiredSignalMeters, signalMetersHere)
        if (!laneChangedForSignal) tryLaneChange(nowMillis)
    }

    // 이번 프레임에 옆으로 옮기는 거리다. 진입로에서 본선으로 옮기는 동안 가속하면 다 옮기기 전에 진입로가 끝날 수
    // 있으므로, 합류 지점에 닿기 전에 본선에 들어오도록 남은 거리에 맞춰 옆으로 더 빨리 옮긴다.
    private fun lateralStep(dtSeconds: Double): Double {
        val step = lateralMps * dtSeconds
        val merging =
            guide == DriveGuide.MERGE && laneTarget == RIGHT_LANE_M && lateralMeters > RIGHT_LANE_M
        if (!merging) return step
        val share =
            if (guideMeters > 0) (speedMps * dtSeconds / guideMeters).coerceAtMost(1.0) else 1.0
        return maxOf(step, (lateralMeters - RIGHT_LANE_M) * share)
    }

    // 세 차로 밖으로는 변경하지 않는다. 진입로에서는 본선으로 들어올 수만 있고 본선에서 진입로로 나가지 않는다.
    private fun tryLaneChange(nowMillis: Long) {
        val ready =
            speedKmh >= LANE_CHANGE_MIN_KMH &&
                signaledMeters >= requiredSignalMeters &&
                lateralMeters == laneTarget
        val next = laneTarget + if (turn == TurnSignal.RIGHT) LANE_WIDTH_M else -LANE_WIDTH_M
        if (!ready || abs(next) > LANE_WIDTH_M) return
        val blocked = solidLineHint()
        if (blocked == null) {
            startLaneChange(next)
        } else if (!solidWarned) {
            show(blocked, nowMillis)
            solidWarned = true
        }
    }

    private fun startLaneChange(next: Double) {
        laneChangeSide = if (next < laneTarget) TurnSignal.LEFT else TurnSignal.RIGHT
        laneTarget = next
        laneChangedForSignal = true
    }

    // 이번 차로 변경이 실선을 넘게 되면 그 이유를 반환한다. 합류 장면에서는 합류 구간 전에 진입로에서 본선으로
    // 변경하는 것을 차단한다.
    private fun solidLineHint(): DriveHint? = when (guide) {
        DriveGuide.LEFT_TURN -> DriveHint.SOLID_LINE.takeIf { crossesLineAtCrossing() }

        DriveGuide.MERGE ->
            DriveHint.MERGE_TOO_EARLY.takeIf {
                laneTarget == RAMP_LANE_M && guideMeters > MERGE_ZONE_M
            }

        null -> null
    }

    // 차는 변경을 시작한 뒤 차로 폭의 절반을 옆으로 가야 차선을 넘고 그동안 앞으로도 달린다. 차선을 넘는 자리가
    // 실선 구간의 시작(교차로 36m 앞)부터 건너편 횡단보도의 끝 사이이면 차단한다.
    private fun crossesLineAtCrossing(): Boolean {
        val entry = guideMeters + CROSS_ENTRY_M
        val toLine = speedMps * LANE_HALF_M / lateralMps
        return entry - toLine <= SOLID_FAR_M && entry + CROSS_EXIT_M > 0
    }

    // 진입로는 합류 지점에서 끝난다. 본선으로 다 옮길 수 있는 마지막 자리에 올 때까지 진입로에 있으면 방향지시등과
    // 관계없이 본선으로 옮기고, 방향지시등을 켜지 않았으면 이유를 알린다.
    private fun mergeBeforeRampEnds(nowMillis: Long) {
        val onRamp = guide == DriveGuide.MERGE && laneTarget == RAMP_LANE_M
        if (!onRamp || speedMps == 0.0) return
        val needed = speedMps * LANE_WIDTH_M / lateralMps
        if (guideMeters > needed) return
        if (turn != TurnSignal.LEFT) show(DriveHint.MERGE_FORCED, nowMillis)
        startLaneChange(RIGHT_LANE_M)
    }

    // 다음에 해야 할 조작을 장면마다 한 번씩 알린다. 좌회전 장면에서 1차로가 아니면 장면을 켜자마자 1차로로 옮기라고
    // 알린다. 시속 50km에서는 차로 하나를 변경하는 데 신호 30m와 옆으로 옮기는 약 55m(이 클래스의 값으로 계산)가
    // 필요해서, 일찍 알려야 오른쪽 끝 차로에서도 실선 구간 전에 1차로에 닿는다. 합류 장면은 신호 100m를 채운 뒤에
    // 옆으로 옮기므로, 합류 구간에 들어올 때 알리면 진입로가 끝나기 전에 본선으로 다 옮기지 못한다. 그래서 합류
    // 구간까지 신호 거리만큼 남았을 때 알린다.
    private fun remind(nowMillis: Long) {
        val next =
            when (guide) {
                DriveGuide.LEFT_TURN -> leftTurnReminder()

                DriveGuide.MERGE ->
                    DriveHint.MERGE_SIGNAL.takeIf {
                        laneTarget == RAMP_LANE_M && turn != TurnSignal.LEFT &&
                            guideMeters <= MERGE_ZONE_M + signalMetersHere
                    }

                null -> null
            }
        if (next != null && reminded.add(next)) show(next, nowMillis)
    }

    private fun leftTurnReminder(): DriveHint? = when {
        guideMeters <= 0 -> null

        laneTarget != LEFT_LANE_M ->
            DriveHint.LEFT_TURN_LANE.takeIf { guideMeters + CROSS_ENTRY_M > SOLID_FAR_M }

        turn != TurnSignal.LEFT ->
            DriveHint.LEFT_TURN_SIGNAL.takeIf {
                guideMeters <= ROAD_SIGNAL_METERS + TURN_REMIND_MARGIN_M
            }

        else -> null
    }

    private fun followLead(dtSeconds: Double) {
        val safe = (speedMps * LEAD_GAP_SECONDS).coerceAtLeast(LEAD_MIN_GAP_M)
        val target = if (inSameLaneAsLead) safe else safe + LEAD_OTHER_LANE_EXTRA_M
        val share = (dtSeconds * LEAD_FOLLOW_PER_S).coerceAtMost(1.0)
        leadGapMeters += (target - leadGapMeters) * share
        if (abs(target - leadGapMeters) < LEAD_SNAP_M) leadGapMeters = target
    }
}

// value를 target 쪽으로 최대 step만큼 옮긴다.
private fun approach(value: Double, target: Double, step: Double): Double {
    val gap = target - value
    return if (abs(gap) <= step) target else value + sign(gap) * step
}
