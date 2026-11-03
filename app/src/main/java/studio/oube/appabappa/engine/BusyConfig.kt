package studio.oube.appabappa.engine

data class BusyConfig(
    val movingSpeedKmh: Double = 5.0,
    val highSpeedKmh: Double = 60.0,
    val turningAngleDeg: Double = 90.0,
    val soonSeconds: Double = 15.0,
    val passingSeconds: Double = 5.0,
    val passingMeters: Double = 50.0,
    val hardBrakeDropMps: Double = 3.5,
    val hardBrakeWindowMillis: Long = 1_000,
    val recoverMillis: Long = 5_000,
    val veryBusyRecoverMillis: Long = 10_000
)
