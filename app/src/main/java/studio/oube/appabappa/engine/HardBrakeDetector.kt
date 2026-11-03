package studio.oube.appabappa.engine

internal class HardBrakeDetector(private val dropMps: Double, private val windowMillis: Long) {
    private val samples = ArrayDeque<SpeedSample>()

    fun update(speedMps: Double, nowMillis: Long): Boolean {
        samples.addLast(SpeedSample(nowMillis, speedMps))
        while (samples.size >= 2 && nowMillis - samples[1].timeMillis >= windowMillis) {
            samples.removeFirst()
        }
        val oldest = samples.first()
        return nowMillis - oldest.timeMillis >= windowMillis &&
            oldest.speedMps - speedMps >= dropMps
    }

    private data class SpeedSample(val timeMillis: Long, val speedMps: Double)
}
