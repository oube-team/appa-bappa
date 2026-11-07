package studio.oube.appabappa.vehicle

import ai.pleos.playground.vehicle.api.Light
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.listener.LightStateListener
import android.os.Handler
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.sdkCall

// 자동 전조등은 밤이나 터널처럼 바깥이 어두울 때 하향등을 켜서, 하향등 상태로 어두운지 판단한다.
// 운전자가 낮에 하향등을 직접 켜도 어두운 것으로 판단한다. Android 야간 모드는 가상 콘솔에서 하향등을 켜도 바뀌지 않아서
// 사용하지 않는다(2026-11-07 에뮬레이터).
class LowBeam(
    private val light: Light,
    private val handler: Handler,
    private val onChanged: () -> Unit
) {
    var isOn = false
        private set

    private val listener =
        object : LightStateListener {
            override fun onLightStateUpdated(state: LightState?) {
                handler.post { update(state == LightState.ON) }
            }

            override fun onFailed(e: Exception) {
                AppLog.w("low beam listener failed", e)
            }
        }

    fun start() {
        sdkCall("low beam register") { light.registerLowBeamLightState(listener) }
    }

    fun stop() {
        sdkCall("low beam unregister") { light.unregisterLowBeamLightState(listener) }
    }

    private fun update(on: Boolean) {
        if (on == isOn) return
        isOn = on
        AppLog.i("low beam ${if (on) "on" else "off"}")
        onChanged()
    }
}
