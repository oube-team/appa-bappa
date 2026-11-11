package studio.oube.appabappa.ui

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.SafetyWarning
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals
import studio.oube.appabappa.status.Reason

// 시연 목록과 재생 화면에 보여 줄 장면 이름과 설명이다. 설명은 앞에 적은 시각(장면을 시작한 뒤의 초)부터 보여 주고,
// 시각은 엔진의 판단 결과가 바뀌는 때(DemoScenes의 주석)에 맞췄다. 설명에는 두 줄 길이를 맞추는 균형 줄바꿈을 사용해서
// "바빠질 수 / 있어서"처럼 구 중간에서 끊길 수 있으므로, 그렇게 끊기는 설명에는 끊을 자리에 줄바꿈(\n)을 추가한다.
// 세로 창의 설명 칸은 두 줄 높이라서 줄바꿈은 한 번만 추가할 수 있다. 가로 창에서 세 줄이 되는 설명은 셋째 줄이 구
// 중간에서 시작하지 않도록, 떨어지면 안 되는 낱말 사이에 줄바꿈하지 않는 공백(\u00A0)을 사용한다.
internal class SceneText(val name: String, val captions: List<Pair<Double, String>>) {
    fun captionAt(seconds: Double): String =
        captions.lastOrNull { (from, _) -> from <= seconds }?.second ?: ""
}

internal val SceneTexts: Map<String, SceneText> =
    mapOf(
        "highway_merge" to SceneText(
            "고속도로 합류",
            listOf(
                0.0 to "시속 90km로 달리고 있어요",
                6.0 to "합류 지점까지 15초 남아서 곧 바빠져요",
                9.0 to "시속 60km 이상에서 왼쪽 방향지시등을 켜고 합류해요",
                21.0 to "합류 지점을 지났어요",
                23.0 to "방향지시등을 껐어요. 다시 바빠질 수 있어서\n10초 동안 주황색을 유지해요",
                33.0 to "바쁜 상황이 끝났어요"
            )
        ),
        "left_turn" to SceneText(
            "교차로 좌회전",
            listOf(
                0.0 to "왼쪽 방향지시등을 켜고 신호를 기다려요.\n정차 중이라 바쁘지 않아요",
                5.0 to "출발해서 스티어링 휠을 180도 돌려 좌회전해요",
                9.0 to "회전을 마쳤어요. 다시 바빠질 수 있어서\n5초 동안 주황색을 유지해요",
                14.0 to "바쁜 상황이 끝났어요"
            )
        ),
        "red_light" to SceneText(
            "신호 대기",
            listOf(0.0 to "좌회전 지점 30m 앞에서 신호를 기다려요.\n정차 중이라 바쁘지 않아요")
        ),
        "hard_brake" to SceneText(
            "급제동",
            listOf(
                0.0 to "시속 50km로 달리고 있어요",
                3.0 to "앞차가 갑자기 감속해서\n1초 만에 시속 35km로 급제동해요",
                4.25 to "다시 바빠질 수 있어서\n10초 동안 주황색을 유지해요",
                14.25 to "바쁜 상황이 끝났어요"
            )
        ),
        "forward_collision" to SceneText(
            "전방 충돌 경고",
            listOf(
                0.0 to "시속 60km로 달리고 있어요",
                3.0 to "앞차와 가까워져서 전방 충돌 경고가 켜졌어요",
                5.0 to "경고가 꺼졌어요. 다시 바빠질 수 있어서\n10초 동안 주황색을 유지해요",
                15.0 to "바쁜 상황이 끝났어요"
            )
        ),
        "blind_spot" to SceneText(
            "후측방 경고",
            listOf(
                0.0 to "시속 50km로 달리며 오른쪽 방향지시등을 켰어요",
                2.0 to "오른쪽 뒤에 차가 있어서 후측방 경고가 켜졌어요",
                5.0 to "차로를 변경하지 않고 방향지시등을 껐어요.\n다시 바빠질 수 있어서 10초\u00A0동안 주황색을 유지해요",
                15.0 to "바쁜 상황이 끝났어요"
            )
        ),
        "stop_and_go" to SceneText(
            "정체",
            listOf(0.0 to "시속 10km 이하로 가다 서다를 반복해요. 방향지시등과 안내가 없어서 바쁘지 않아요")
        ),
        "short_signal" to SceneText(
            "짧은 방향지시등",
            listOf(
                0.0 to "시속 50km로 달리고 있어요",
                2.0 to "방향지시등을 1초만 켰어요",
                3.0 to "다시 바빠질 수 있어서\n5초 동안 주황색을 유지해요",
                8.0 to "바쁜 상황이 끝났어요"
            )
        ),
        "parking" to SceneText(
            "주차",
            listOf(
                0.0 to "후진 기어(R)로 주차하고 있어요",
                3.0 to "기어를 P로 변속해서 바쁜 상황이 끝났어요"
            )
        ),
        "back_to_back" to SceneText(
            "이어지는 바쁜 상황",
            listOf(
                0.0 to "오른쪽 방향지시등을 켰어요",
                2.0 to "방향지시등을 껐어요.\n5초 동안 주황색을 유지해요",
                5.0 to "3초 만에 방향지시등을 다시 켜서\n주황색을 유지해요",
                7.0 to "방향지시등을 껐어요.\n5초 동안 주황색을 유지해요",
                12.0 to "바쁜 상황이 끝났어요"
            )
        )
    )

// 시연 재생 화면의 신호 표에 들어갈 한 칸이다. wide가 true면 두 칸을 차지한다.
internal class Readout(val label: String, val value: String, val wide: Boolean = false)

internal fun readouts(signals: VehicleSignals, engineState: String): List<Readout> = listOf(
    Readout("차속", "${signals.speedKmh.roundToInt()} km/h"),
    Readout("기어", signals.gear.letter()),
    Readout("방향지시등", turnSignalName(signals.turnSignal)),
    Readout("스티어링 휠", steeringText(signals.steeringAngleDeg)),
    Readout("판단", engineState),
    Readout("경고", warningName(signals.safetyWarning)),
    Readout("다음 안내", signals.nextManeuver?.let(::maneuverText) ?: "없음", wide = true)
)

// 스티어링 휠 각도는 왼쪽이 양수다. 부호 대신 방향을 글자로 표시한다.
internal fun steeringText(steeringDeg: Double): String {
    val angle = abs(steeringDeg).roundToInt()
    return when {
        angle == 0 -> "0도"
        steeringDeg > 0 -> "왼쪽 ${angle}도"
        else -> "오른쪽 ${angle}도"
    }
}

internal fun Gear.letter(): String = when (this) {
    Gear.PARK -> "P"
    Gear.REVERSE -> "R"
    Gear.NEUTRAL -> "N"
    Gear.DRIVE -> "D"
}

private fun turnSignalName(signal: TurnSignal) = when (signal) {
    TurnSignal.OFF -> "꺼짐"
    TurnSignal.LEFT -> "왼쪽"
    TurnSignal.RIGHT -> "오른쪽"
}

private fun warningName(warning: SafetyWarning?) = when (warning) {
    null -> "없음"
    SafetyWarning.FORWARD_COLLISION -> "전방 충돌"
    SafetyWarning.BLIND_SPOT -> "후측방"
}

private fun maneuverText(maneuver: Maneuver): String {
    val name = Reason.maneuverReason(maneuver.turnTypeName)?.maneuverName() ?: "안내"
    val meters = String.format(
        Locale.KOREA,
        "%,d",
        maneuver.distanceMeters.roundToInt().coerceAtLeast(0)
    )
    return "$name ${meters}m"
}

private fun Reason.maneuverName() = when (this) {
    Reason.MERGE -> "합류"
    Reason.HIGHWAY_ENTRY -> "고속도로 진입"
    Reason.HIGHWAY_EXIT -> "나들목 출구"
    Reason.LEFT_TURN -> "좌회전"
    Reason.RIGHT_TURN -> "우회전"
    Reason.U_TURN -> "유턴"
    Reason.ROUNDABOUT -> "회전교차로"
    Reason.JUNCTION -> "갈림길"
    Reason.TOLLGATE -> "요금소"
    Reason.REST_AREA -> "휴게소"
    Reason.DROWSY_REST_AREA -> "졸음쉼터"
    else -> "안내"
}
