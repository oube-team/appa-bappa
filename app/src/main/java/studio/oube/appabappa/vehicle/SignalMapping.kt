package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import android.car.VehicleGear
import kotlin.math.abs
import kotlin.math.max
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal

// 승용차 스티어링 휠은 보통 좌우로 1바퀴 반씩 돌아가서(판단), 끝까지 돌린 각도를 540도로 정했다.
private const val FULL_LOCK_WHEEL_DEG = 540.0

// 기어 값을 아직 수신하지 못했거나 수동 변속 단수(1~9단)이면 전진 중으로 간주해 DRIVE로 처리한다.
internal fun gearOf(gearSelection: Int?): Gear = when (gearSelection) {
    VehicleGear.GEAR_PARK -> Gear.PARK
    VehicleGear.GEAR_REVERSE -> Gear.REVERSE
    VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
    else -> Gear.DRIVE
}

internal fun turnSignalOf(value: TurnSignalValue?): TurnSignal = when (value) {
    TurnSignalValue.LEFT -> TurnSignal.LEFT
    TurnSignalValue.RIGHT -> TurnSignal.RIGHT
    TurnSignalValue.OFF, null -> TurnSignal.OFF
}

// RouteStateInfo.isRouting은 공식 문서에 설명이 없고 에뮬레이터에서는 경로 안내 중에도 false여서(2026-11-07 시험),
// 경로 안내 중인지는 안내 모드로 판단한다. 모의 주행(SIMULATION_GUIDING)은 실제로 달리는 것이 아니어서
// 경로 안내로 판단하지 않는다.
internal fun isRouteGuiding(naviMode: NaviMode?): Boolean = naviMode == NaviMode.ROUTE_GUIDING

// 안내 목록을 엔진이 사용하는 형태로 변환한다. 지금 남은 거리는 서비스가 이 목록에서 추정한다(RouteProgress).
internal fun maneuversOf(guidance: List<TBTInfo>): List<Maneuver> =
    guidance.map { Maneuver(it.type.name, it.distance) }

// SDK의 조향각은 앞바퀴 각도여서(SDK 소스 주석) 엔진의 90도 기준(turningAngleDeg)에 닿지 않으므로,
// 조향각 범위의 끝을 FULL_LOCK_WHEEL_DEG로 간주해 스티어링 휠 각도로 변환한다. 범위를 모르면 수신한 값을
// 그대로 사용한다.
internal fun wheelAngleOf(steeringDeg: Double, fullLockDeg: Double?): Double =
    if (fullLockDeg == null) steeringDeg else steeringDeg / fullLockDeg * FULL_LOCK_WHEEL_DEG

// 조향각 범위의 끝은 최솟값과 최댓값 가운데 절댓값이 큰 쪽이다. 0이면 범위를 모르는 것으로 판단한다.
internal fun fullLockOf(minDeg: Float, maxDeg: Float): Double? =
    max(abs(minDeg), abs(maxDeg)).toDouble().takeIf { it > 0.0 }
