package hu.adamka00.momentum;
import hu.adamka00.momentum.movement.MovementState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SprintStateTest {
    private void tick(MovementState state, boolean eligible, boolean restricted) {
        state.tickSprint(eligible, restricted, 20, 60, 1.35, 0.025);
    }
    @Test void activationAndLinearProgression() {
        var s = new MovementState();
        for (int i = 0; i < 20; i++) tick(s, true, false);
        assertEquals(1, s.sprintMultiplier());
        for (int i = 0; i < 30; i++) tick(s, true, false);
        assertEquals(1.175, s.sprintMultiplier(), 1e-9);
    }
    @Test void cappedAfterLongSprintAndCannotStack() {
        var s = new MovementState();
        for (int i = 0; i < 10000; i++) tick(s, true, false);
        assertEquals(1.35, s.sprintMultiplier()); assertEquals(80, s.sprintTicks());
    }
    @Test void deceleratesAndStopsAtOne() {
        var s = new MovementState();
        for (int i = 0; i < 80; i++) tick(s, true, false);
        tick(s, false, false); assertEquals(1.325, s.sprintMultiplier(), 1e-9);
        for (int i = 0; i < 100; i++) tick(s, false, false);
        assertEquals(1, s.sprintMultiplier()); assertEquals(0, s.sprintTicks());
    }
    @Test void restrictedStatesResetImmediately() {
        var s = new MovementState();
        for (int i = 0; i < 80; i++) tick(s, true, false);
        tick(s, true, true); assertEquals(1, s.sprintMultiplier()); assertEquals(0, s.sprintTicks());
    }
    @Test void playersHaveIndependentSprintCurves() {
        var a = new MovementState(); var b = new MovementState();
        for (int i = 0; i < 80; i++) tick(a, true, false);
        assertEquals(1, b.sprintMultiplier()); assertEquals(1.35, a.sprintMultiplier());
    }
    @Test void packetTimingGapsDoNotRestartAcceleration() {
        var s = new MovementState();
        for (int time = 1; time <= 81; time++) {
            s.observeHorizontalMotion(time % 2 == 1, time);
            tick(s, s.hasRecentHorizontalMotion(time), false);
        }
        assertEquals(1.35, s.sprintMultiplier());
    }
    @Test void oldMovementCannotKeepBoostingStationaryPlayers() {
        var s = new MovementState();
        assertFalse(s.hasRecentHorizontalMotion(1));
        s.observeHorizontalMotion(true, 2);
        assertTrue(s.hasRecentHorizontalMotion(5));
        assertFalse(s.hasRecentHorizontalMotion(6));
    }
}
