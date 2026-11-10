package studio.oube.appabappa.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.BusyState.BUSY
import studio.oube.appabappa.engine.BusyState.FREE
import studio.oube.appabappa.engine.BusyState.RECOVERING
import studio.oube.appabappa.engine.BusyState.SOON
import studio.oube.appabappa.engine.BusyState.VERY_BUSY
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals

class TripRecorderTest {
    private val recorder = TripRecorder()
    private val clockStart = 1_700_000_000_000L

    // 기어가 D이면 시속 36km로 달리고, 그 밖의 기어에서는 정지해 있다.
    private fun tick(
        seconds: Int,
        gear: Gear,
        state: BusyState,
        riders: Boolean = false,
        speedMps: Double = if (gear == Gear.DRIVE) 10.0 else 0.0
    ): TripSummary? {
        val signals = VehicleSignals(speedMps, gear, TurnSignal.OFF, 0.0, null)
        return recorder.update(
            signals,
            state,
            riders,
            seconds * 1_000L,
            clockStart + seconds * 1_000L
        )
    }

    @Test
    fun `기어가 P인 동안에는 운전을 시작하지 않는다`() {
        assertNull(tick(0, Gear.PARK, FREE))
        assertNull(tick(5, Gear.PARK, FREE))
    }

    @Test
    fun `기어가 P를 벗어났다가 다시 P가 되면 운전 기록을 반환한다`() {
        assertNull(tick(0, Gear.DRIVE, FREE))
        assertNull(tick(60, Gear.DRIVE, FREE))
        val trip = tick(120, Gear.PARK, FREE)

        assertEquals(clockStart, trip?.startedAt)
        assertEquals(clockStart + 120_000, trip?.endedAt)
        assertEquals(emptyList<BusySpan>(), trip?.busySpans)
    }

    @Test
    fun `전진하며 달린 적이 없는 운전은 기록하지 않는다`() {
        // 다시 주차하려고 R로 변경했다가 P로 돌아온 경우다.
        tick(0, Gear.REVERSE, BUSY)
        assertNull(tick(6, Gear.PARK, FREE))

        tick(10, Gear.NEUTRAL, FREE)
        assertNull(tick(12, Gear.PARK, FREE))

        // D로 변경했지만 시속 5km를 넘지 않았다.
        tick(20, Gear.DRIVE, FREE, speedMps = 1.0)
        assertNull(tick(25, Gear.PARK, FREE))
    }

    @Test
    fun `운전을 마친 뒤 다시 출발하면 새 운전으로 기록한다`() {
        tick(0, Gear.DRIVE, BUSY)
        tick(5, Gear.DRIVE, FREE)
        assertEquals(1, tick(10, Gear.PARK, FREE)?.busySpans?.size)

        tick(100, Gear.DRIVE, FREE)
        val second = tick(160, Gear.PARK, FREE)
        assertEquals(clockStart + 100_000, second?.startedAt)
        assertEquals(emptyList<BusySpan>(), second?.busySpans)
    }

    @Test
    fun `운전 중에 기기 시각이 조정돼도 출발 시각은 도착 시각에서 운전한 시간을 뺀 값이다`() {
        tick(0, Gear.DRIVE, FREE)
        val signals = VehicleSignals(0.0, Gear.PARK, TurnSignal.OFF, 0.0, null)
        val trip = recorder.update(signals, FREE, false, 60_000L, clockStart + 3_600_000L)

        assertEquals(60_000L, trip?.let { it.endedAt - it.startedAt })
    }

    @Test
    fun `BUSY가 된 때부터 FREE로 돌아올 때까지를 바빴던 순간 하나로 센다`() {
        tick(0, Gear.DRIVE, FREE)
        tick(10, Gear.DRIVE, BUSY)
        tick(12, Gear.DRIVE, VERY_BUSY)
        tick(15, Gear.DRIVE, RECOVERING)
        tick(25, Gear.DRIVE, FREE)
        val trip = tick(40, Gear.PARK, FREE)

        assertEquals(listOf(BusySpan(10_000, 25_000)), trip?.busySpans)
        assertEquals(15_000L, trip?.busyMillis)
    }

    @Test
    fun `RECOVERING에서 다시 바빠지면 바빴던 순간 하나로 이어서 센다`() {
        tick(0, Gear.DRIVE, BUSY)
        tick(5, Gear.DRIVE, RECOVERING)
        tick(8, Gear.DRIVE, BUSY)
        tick(10, Gear.DRIVE, RECOVERING)
        tick(15, Gear.DRIVE, FREE)
        val trip = tick(20, Gear.PARK, FREE)

        assertEquals(listOf(BusySpan(0, 15_000)), trip?.busySpans)
    }

    @Test
    fun `SOON만 있었던 운전은 바빴던 순간이 없다`() {
        tick(0, Gear.DRIVE, FREE)
        tick(5, Gear.DRIVE, SOON)
        tick(10, Gear.DRIVE, FREE)

        assertEquals(emptyList<BusySpan>(), tick(20, Gear.PARK, FREE)?.busySpans)
    }

    @Test
    fun `바쁜 상황 뒤에 SOON이 되면 바빴던 순간을 끝낸다`() {
        tick(0, Gear.DRIVE, BUSY)
        tick(4, Gear.DRIVE, RECOVERING)
        tick(9, Gear.DRIVE, SOON)

        assertEquals(listOf(BusySpan(0, 9_000)), tick(20, Gear.PARK, FREE)?.busySpans)
    }

    @Test
    fun `달린 뒤 후진으로 주차하면 P가 된 때 바빴던 순간을 끝낸다`() {
        tick(0, Gear.DRIVE, FREE)
        tick(50, Gear.REVERSE, BUSY)
        val trip = tick(56, Gear.PARK, FREE)

        assertEquals(listOf(BusySpan(50_000, 56_000)), trip?.busySpans)
    }

    @Test
    fun `P가 된 때 아직 바쁜 상태이면 그때까지를 바빴던 순간으로 센다`() {
        tick(0, Gear.DRIVE, FREE)
        tick(50, Gear.REVERSE, BUSY)
        val trip = tick(56, Gear.PARK, BUSY)

        assertEquals(listOf(BusySpan(50_000, 56_000)), trip?.busySpans)
    }

    @Test
    fun `운전하는 동안 한 번이라도 동승자가 앉아 있었으면 동승자가 있던 운전으로 기록한다`() {
        tick(0, Gear.DRIVE, FREE, riders = true)
        tick(30, Gear.DRIVE, FREE, riders = false)

        assertEquals(true, tick(60, Gear.PARK, FREE)?.withRiders)
    }

    @Test
    fun `encode한 기록을 decode하면 같은 기록을 반환한다`() {
        val trip =
            TripSummary(
                clockStart,
                clockStart + 60_000,
                listOf(BusySpan(1, 2), BusySpan(3, 40)),
                true
            )

        assertEquals(trip, TripSummary.decode(trip.encode()))
        assertEquals(
            trip.copy(busySpans = emptyList()),
            TripSummary.decode(trip.copy(busySpans = emptyList()).encode())
        )
    }

    @Test
    fun `형식이 맞지 않는 기록을 decode하면 null을 반환한다`() {
        assertNull(TripSummary.decode("1;2;true"))
        assertNull(TripSummary.decode("1;2;maybe;"))
        assertNull(TripSummary.decode("1;2;true;3-"))
    }
}
