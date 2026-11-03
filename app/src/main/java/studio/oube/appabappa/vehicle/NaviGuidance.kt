package studio.oube.appabappa.vehicle

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import studio.oube.appabappa.engine.Maneuver

private const val TAG = "AppaBappa"

class NaviGuidance(context: Context) {
    @Volatile
    var nextManeuver: Maneuver? = null
        private set

    private val navi = NaviHelper(context)
    private var routing = false
    private var guidance: List<TBTInfo> = emptyList()

    private val listener =
        object : NaviHelperEventListener {
            override fun onNavigationStatus(status: NavigationStatus) {
                updateRouting(status == NavigationStatus.ROUTE_DRIVE)
            }

            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                updateRouting(routeStateInfo.isRouting == true)
            }

            override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
                updateGuidance(tbtInfoList)
            }

            override fun onError(errorCode: NaviErrorCode) {
                Log.w(TAG, "navi error: $errorCode")
            }
        }

    fun start() {
        navi.initialize(
            onConnected = {
                navi.addListener(listener)
                // 경로 안내 도중에 앱이 시작될 수 있어서, 연결되면 경로 상태부터 조회한다.
                navi.getRouteStateInfo()
            },
            onFailed = { Log.w(TAG, "navi initialize failed", it) }
        )
    }

    fun stop() {
        navi.removeListener(listener)
        navi.release()
    }

    private fun updateRouting(active: Boolean) {
        synchronized(this) {
            if (active == routing) return
            routing = active
            // 이전 경로의 안내가 남지 않도록 안내 목록을 초기화하고, 경로 안내가 시작되면 다시 조회한다.
            guidance = emptyList()
            refresh()
        }
        // 락을 획득한 상태에서 NaviHelper를 호출하지 않도록 synchronized 블록 밖에서 조회한다.
        if (active) navi.getTBTInfo()
    }

    @Synchronized
    private fun updateGuidance(list: List<TBTInfo>) {
        guidance = list
        refresh()
    }

    private fun refresh() {
        nextManeuver = nextManeuverOf(routing, guidance)
    }
}
