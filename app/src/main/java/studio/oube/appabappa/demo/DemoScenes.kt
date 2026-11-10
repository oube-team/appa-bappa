// 장면마다 차속, 거리, 시간 값이 달라서 상수로 분리하면 장면을 한눈에 파악하기 어려워지므로, 숫자를 그대로 사용한다.
@file:Suppress("MagicNumber")

package studio.oube.appabappa.demo

import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.SafetyWarning
import studio.oube.appabappa.engine.TurnSignal

internal object DemoScenes {
    val all =
        listOf(
            highwayMerge,
            leftTurn,
            redLight,
            hardBrake,
            forwardCollision,
            blindSpot,
            stopAndGo,
            shortSignal,
            parking,
            backToBack
        )

    fun find(id: String): DemoScene? = all.firstOrNull { it.id == id }
}

private val nextExit = Maneuver("TURN_EXIT_IC_R", 5_000.0)

private fun merge(meters: Double) = Maneuver("TURN_DEVIDE_MERGE", meters)

private fun turnLeft(meters: Double) = Maneuver("TURN_LEFT", meters)

// 고속도로 합류: 90 km/h로 주행하다가 합류 안내 15초 전에 SOON이 되고, 방향지시등을 켜면 VERY_BUSY가 된다.
// 오른쪽 진입로에서 왼쪽 본선 차로로 들어가므로 왼쪽 방향지시등을 켠다.
private val highwayMerge =
    DemoScene(
        "highway_merge",
        listOf(
            DemoStep(9.0) { t -> driving(90.0, nextManeuver = merge(525 - 25 * t)) },
            DemoStep(12.0) { t ->
                driving(90.0, TurnSignal.LEFT, mergeSteeringDeg(t), merge(300 - 25 * t))
            },
            DemoStep(2.0, driving(90.0, TurnSignal.LEFT, nextManeuver = nextExit)),
            DemoStep(15.0, driving(90.0, nextManeuver = nextExit))
        )
    )

// 합류 지점 150m 앞(방향지시등을 켠 지 6초 뒤)부터 본선으로 옮긴다. 90 km/h에서 스티어링 휠을 4도 돌리면 옆으로
// 1초에 초속 약 1m씩 빨라지므로(축간거리 2.9m, 조향비 15:1로 계산), 왼쪽으로 1.9초, 오른쪽으로 1.9초 돌리면 차로
// 하나(3.5m)를 옮긴다.
private fun mergeSteeringDeg(seconds: Double): Double = when {
    seconds < 6.0 -> 0.0
    seconds < 7.9 -> 4.0
    seconds < 9.8 -> -4.0
    else -> 0.0
}

// 교차로 좌회전: 신호를 기다리는 동안에는 FREE를 유지하고, 출발해 스티어링 휠을 돌리면 BUSY가 된다.
private val leftTurn =
    DemoScene(
        "left_turn",
        listOf(
            DemoStep(5.0, driving(0.0, TurnSignal.LEFT, nextManeuver = turnLeft(15.0))),
            DemoStep(4.0) { t ->
                driving(
                    15.0,
                    TurnSignal.LEFT,
                    steeringAngleDeg = 180.0,
                    nextManeuver = turnLeft(15 - 4 * t)
                )
            },
            DemoStep(10.0, driving(30.0, nextManeuver = Maneuver("TURN_RIGHT", 800.0)))
        )
    )

// 신호 대기: 좌회전 안내 지점 30m 앞에 정차해 방향지시등을 켜도 FREE를 유지한다.
private val redLight =
    DemoScene(
        "red_light",
        listOf(DemoStep(10.0, driving(0.0, TurnSignal.LEFT, nextManeuver = turnLeft(30.0))))
    )

// 급제동: 50 km/h에서 1초 만에 35 km/h로 감속하면 VERY_BUSY가 된다. 도로교통법 제19조 제4항은 위험방지를 위한
// 경우가 아니면 급제동을 금지하므로, 설명(ui/DemoTexts.kt)에서 앞차가 갑자기 감속한 상황이라고 알린다.
private val hardBrake =
    DemoScene(
        "hard_brake",
        listOf(
            DemoStep(3.0, driving(50.0)),
            DemoStep(1.0) { t -> driving(50 - 15 * t) },
            DemoStep(15.0, driving(35.0))
        )
    )

// 전방 충돌 경고: 60 km/h로 달리다가 경고가 켜지면 VERY_BUSY가 되고, 2초 뒤에 꺼지면 RECOVERING을 거쳐 FREE로
// 돌아온다.
private val forwardCollision =
    DemoScene(
        "forward_collision",
        listOf(
            DemoStep(3.0, driving(60.0)),
            DemoStep(2.0, driving(60.0).copy(safetyWarning = SafetyWarning.FORWARD_COLLISION)),
            DemoStep(12.0, driving(60.0))
        )
    )

// 후측방 경고: 50 km/h에서 오른쪽 방향지시등을 켜면 BUSY가 된다. 그 상태로 경고가 켜지면 VERY_BUSY가 된다.
// 3초 뒤 차로를 바꾸지 않은 채 경고와 방향지시등이 함께 꺼지면 RECOVERING을 거쳐 FREE로 돌아온다.
private val blindSpot =
    DemoScene(
        "blind_spot",
        listOf(
            DemoStep(2.0, driving(50.0, TurnSignal.RIGHT)),
            DemoStep(
                3.0,
                driving(50.0, TurnSignal.RIGHT).copy(safetyWarning = SafetyWarning.BLIND_SPOT)
            ),
            DemoStep(13.0, driving(50.0))
        )
    )

// 정체: 10 km/h 이하로 가다 서다를 반복하면 FREE를 유지한다.
private val stopAndGo =
    DemoScene(
        "stop_and_go",
        List(5) {
            listOf(
                DemoStep(2.0) { t -> driving(5 * t) },
                DemoStep(2.0) { t -> driving(10 - 5 * t) }
            )
        }.flatten()
    )

// 짧은 방향지시등: 50 km/h에서 방향지시등을 1초만 켜도 BUSY와 RECOVERING을 거쳐 FREE로 돌아온다.
private val shortSignal =
    DemoScene(
        "short_signal",
        listOf(
            DemoStep(2.0, driving(50.0)),
            DemoStep(1.0, driving(50.0, TurnSignal.RIGHT)),
            DemoStep(10.0, driving(50.0))
        )
    )

// 주차: 기어를 R로 변경하면 BUSY가 되고, P로 변경하면 바로 FREE가 된다.
private val parking =
    DemoScene(
        "parking",
        listOf(
            DemoStep(3.0, driving(0.0, gear = Gear.REVERSE)),
            DemoStep(4.0, driving(0.0, gear = Gear.PARK))
        )
    )

// 이어지는 바쁜 상황: RECOVERING 중에 방향지시등을 다시 켜면 FREE를 거치지 않고 BUSY가 된다.
private val backToBack =
    DemoScene(
        "back_to_back",
        listOf(
            DemoStep(2.0, driving(50.0, TurnSignal.RIGHT)),
            DemoStep(3.0, driving(50.0)),
            DemoStep(2.0, driving(50.0, TurnSignal.RIGHT)),
            DemoStep(10.0, driving(50.0))
        )
    )
