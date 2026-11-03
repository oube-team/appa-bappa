package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import android.car.VehicleGear
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.engine.TurnSignal

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

internal fun nextManeuverOf(routing: Boolean, guidance: List<TBTInfo>): Maneuver? {
    val next = guidance.firstOrNull()?.takeIf { routing }
    val distance = next?.distance
    return if (next != null && distance != null) Maneuver(next.type.name, distance) else null
}
