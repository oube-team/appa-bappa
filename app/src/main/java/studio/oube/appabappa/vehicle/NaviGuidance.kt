package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.SystemClock
import studio.oube.appabappa.AppLog
import studio.oube.appabappa.engine.Maneuver
import studio.oube.appabappa.sdkCall

// 경로 안내 중의 다음 안내 목록과 수신 시각이다. 목록이 바뀌거나 경로 안내가 시작될 때마다 새로 만든다.
// 서비스는 이 객체가 바뀌면 새 목록으로 판단한다.
class RouteGuidance(val maneuvers: List<Maneuver>, val receivedAtMillis: Long)

class NaviGuidance(context: Context) {
    // 경로 안내 중이 아니면 null이다.
    @Volatile
    var guidance: RouteGuidance? = null
        private set

    private val navi = NaviHelper(context)
    private var routing = false
    private var maneuvers: List<Maneuver> = emptyList()

    private val listener =
        object : NaviHelperEventListener {
            // 경로 안내 여부는 안내 모드 하나로 판단한다(onRouteStateInfo). 상태 알림으로도 경로 안내 여부를 변경하면,
            // 모의 주행처럼 두 값이 다를 때 안내 목록이 계속 초기화되어 달린 거리를 다시 측정한다.
            // 상태가 ROUTE_DRIVE가 되면 안내 모드를 조회한다. ROUTE_DRIVE가 아니면 안내 모드를 기다리지 않고
            // 경로 안내가 끝난 것으로 처리한다.
            override fun onNavigationStatus(status: NavigationStatus) {
                if (status == NavigationStatus.ROUTE_DRIVE) {
                    sdkCall("navi route state") { navi.getRouteStateInfo() }
                } else {
                    updateRouting(false)
                }
            }

            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                updateRouting(isRouteGuiding(routeStateInfo.naviMode))
            }

            override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
                updateGuidance(tbtInfoList)
            }

            override fun onError(errorCode: NaviErrorCode) {
                AppLog.w("navi error: $errorCode")
            }
        }

    fun start() {
        sdkCall("navi initialize") {
            navi.initialize(
                onConnected = {
                    sdkCall("navi listener add") { navi.addListener(listener) }
                    // 경로 안내 도중에 앱이 시작될 수 있어서, 연결되면 경로 상태부터 조회한다.
                    sdkCall("navi route state") { navi.getRouteStateInfo() }
                },
                onFailed = { AppLog.w("navi initialize failed", it) }
            )
        }
    }

    fun stop() {
        sdkCall("navi listener remove") { navi.removeListener(listener) }
        sdkCall("navi release") { navi.release() }
    }

    private fun updateRouting(active: Boolean) {
        synchronized(this) {
            if (active == routing) return
            routing = active
            AppLog.i("navi routing $active")
            // 이전 경로의 안내가 남지 않도록 안내 목록을 초기화하고, 경로 안내가 시작되면 다시 조회한다.
            maneuvers = emptyList()
            refresh()
        }
        // 락을 획득한 상태에서 NaviHelper를 호출하지 않도록 synchronized 블록 밖에서 조회한다.
        if (active) sdkCall("navi TBT") { navi.getTBTInfo() }
    }

    // 내비게이션이 같은 목록을 다시 보내기도 해서(에뮬레이터 실측 5분마다), 안내와 거리가 같은 목록은 무시한다.
    // 새 목록으로 처리하면 서비스가 그동안 달린 거리를 초기화해서, 이미 지난 안내를 다시 다가오는 안내로 판단한다.
    @Synchronized
    private fun updateGuidance(list: List<TBTInfo>) {
        val received = maneuversOf(list)
        if (received == maneuvers) {
            AppLog.i("navi guidance unchanged, ${received.size} maneuvers")
            return
        }
        AppLog.i("navi guidance ${received.size} maneuvers, first ${received.firstOrNull()}")
        maneuvers = received
        refresh()
    }

    private fun refresh() {
        guidance = if (routing) RouteGuidance(maneuvers, SystemClock.elapsedRealtime()) else null
    }
}
