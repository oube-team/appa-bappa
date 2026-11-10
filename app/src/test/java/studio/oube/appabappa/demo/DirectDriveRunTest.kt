package studio.oube.appabappa.demo

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import studio.oube.appabappa.engine.BusyState
import studio.oube.appabappa.engine.Gear
import studio.oube.appabappa.engine.TurnSignal
import studio.oube.appabappa.engine.VehicleSignals

class DirectDriveRunTest {
    private val run = DirectDriveRun(log = {})

    private val signaling =
        VehicleSignals(
            speedMps = 10.0,
            gear = Gear.DRIVE,
            turnSignal = TurnSignal.RIGHT,
            steeringAngleDeg = 0.0,
            nextManeuver = null
        )

    private var session = 0

    @Before
    fun startScreen() {
        session = DirectDrive.restart()
    }

    @After
    fun closeScreen() {
        DirectDrive.end()
    }

    @Test
    fun `화면이 신호를 보내지 않으면 판단하지 않는다`() {
        assertNull(run.update(Gear.PARK, nowMillis = 0))
        assertNull(run.progress)
    }

    @Test
    fun `실제 기어가 P이면 화면이 보낸 신호로 판단한다`() {
        DirectDrive.send(session, signaling, 0)
        assertEquals(
            BusyState.BUSY,
            run.update(Gear.PARK, nowMillis = 100)?.state
        )
        assertEquals(BusyState.BUSY, run.progress?.status?.state)
    }

    @Test
    fun `실제 기어가 P가 아니거나 기어 값을 수신하기 전이면 판단하지 않는다`() {
        DirectDrive.send(session, signaling, 0)
        assertNull(run.update(Gear.DRIVE, nowMillis = 100))
        assertNull(run.update(null, nowMillis = 100))
    }

    @Test
    fun `1초 넘게 신호를 수신하지 못하면 시연이 끝난 것으로 판단한다`() {
        DirectDrive.send(session, signaling, 0)
        run.update(Gear.PARK, nowMillis = 500)

        assertNull(run.update(Gear.PARK, nowMillis = 1_001))
        assertNull(run.progress)
    }

    @Test
    fun `처음부터 다시 하면 새 엔진으로 판단한다`() {
        DirectDrive.send(session, signaling, 0)
        run.update(Gear.PARK, nowMillis = 0)
        val signalOff = signaling.copy(turnSignal = TurnSignal.OFF)
        DirectDrive.send(session, signalOff, 250)
        assertEquals(
            BusyState.RECOVERING,
            run.update(Gear.PARK, nowMillis = 250)?.state
        )

        val next = DirectDrive.restart()
        DirectDrive.send(next, signalOff, 500)
        assertEquals(
            BusyState.FREE,
            run.update(Gear.PARK, nowMillis = 500)?.state
        )
    }

    @Test
    fun `처음부터 다시 한 뒤에 이전 시연 번호로 보낸 신호는 사용하지 않는다`() {
        DirectDrive.send(session, signaling, 0)
        run.update(Gear.PARK, nowMillis = 0)

        DirectDrive.restart()
        DirectDrive.send(session, signaling, 100)
        assertNull(run.update(Gear.PARK, nowMillis = 100))
    }
}
