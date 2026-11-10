package studio.oube.appabappa.vehicle

import ai.pleos.playground.vehicle.api.Seat
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.os.Handler
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.sdkCall

// 에뮬레이터에서 착좌 값을 확인한 세 자리다(2026-11-07). 왼쪽 운전석 기준으로 조수석과 뒷자리 양쪽이고, 2열 가운데와
// 3열은 차가 착좌를 알리는지 확인하지 못해서 포함하지 않는다.
private val passengerSeats =
    listOf(SeatValue.ROW_1_RIGHT, SeatValue.ROW_2_LEFT, SeatValue.ROW_2_RIGHT)

// 조수석과 뒷자리 두 자리의 착좌 여부로 동승자가 있는지 판단한다. 도착 뒤 화면에서 동승자에게 고마움을 전할지 정하는 데
// 사용한다. 좌석에 짐을 올려 두면 차가 착좌로 알릴 수 있다(추정, 실차 확인 못 함).
class PassengerSeats(private val seat: Seat, private val handler: Handler) {
    private val occupied = mutableMapOf<SeatValue, Boolean>()

    val anyOccupied: Boolean get() = occupied.values.any { it }

    // 등록과 해제에 같은 좌석 영역 객체를 사용하도록 좌석마다 한 번만 생성한다.
    private val listeners =
        passengerSeats.associate { value ->
            SeatArea(value) to object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                    handler.post { occupied[value] = occupancy == true }
                }

                override fun onFailed(e: Exception) {
                    AppLog.w("seat $value occupancy listener failed", e)
                }
            }
        }

    fun start() {
        for ((area, listener) in listeners) {
            sdkCall("seat occupancy register") { seat.registerSeatOccupancy(area, listener) }
        }
    }

    fun stop() {
        for ((area, listener) in listeners) {
            sdkCall("seat occupancy unregister") { seat.unregisterSeatOccupancy(area, listener) }
        }
    }
}
