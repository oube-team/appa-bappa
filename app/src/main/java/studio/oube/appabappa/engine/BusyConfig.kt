package studio.oube.appabappa.engine

data class BusyConfig(
    val movingSpeedKmh: Double = 5.0,
    val highSpeedKmh: Double = 60.0,
    val turningAngleDeg: Double = 90.0,
    val soonSeconds: Double = 15.0,
    val passingSeconds: Double = 5.0,
    val passingMeters: Double = 50.0,

    // 근처 안내가 사라졌을 때 마지막으로 확인한 거리가 이 값 이하였으면 안내 지점을 지난 것으로 판단한다. 지점을 지날 때
    // 마지막으로 확인한 거리는 한 번 판단하는 사이에 달린 거리(0.25초 동안 시속 140km로 달리면 약 10m, 계산값)보다
    // 작으므로, 판단이 한 번 늦어져도 지난 것으로 판단하도록 20m로 정했다(판단).
    val passedMeters: Double = 20.0,
    val hardBrakeDropMps: Double = 3.5,
    val hardBrakeWindowMillis: Long = 1_000,
    val recoverMillis: Long = 5_000,
    val veryBusyRecoverMillis: Long = 10_000
)
