package hu.adamka00.momentum;
import hu.adamka00.momentum.movement.MovementState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JumpStateTest {
    @Test void initialCounter() { assertEquals(0, new MovementState().usedJumps()); }
    private MovementState airborne() { var s = new MovementState(); s.observeGround(false, 1, 2); return s; }
    private int press(MovementState s, int seq, long tick, int max) {
        s.requestJump(seq, false, tick, false, true, max);
        return s.requestJump(seq + 1, true, tick, false, true, max);
    }
    @Test void doubleJumpLimit() {
        var s = airborne(); assertEquals(2, press(s, 1, 2, 2)); assertEquals(0, press(s, 3, 4, 2));
    }
    @Test void tripleJumpLimit() {
        var s = airborne(); assertEquals(2, press(s, 1, 2, 3)); assertEquals(3, press(s, 3, 4, 3)); assertEquals(0, press(s, 5, 6, 3));
    }
    @Test void landingNeedsConsecutiveConfirmedSamples() {
        var s = airborne(); press(s, 1, 2, 3);
        s.observeGround(false, 3, 2);
        s.observeGround(true, 5, 2); assertEquals(2, s.usedJumps());
        s.observeGround(true, 5, 2); assertEquals(2, s.usedJumps());
        s.observeGround(true, 6, 2); assertEquals(0, s.usedJumps()); assertTrue(s.grounded());
    }
    @Test void noResetAirborneOrBriefGroundReport() {
        var s = airborne(); press(s, 1, 2, 3);
        s.observeGround(true, 5, 2); s.observeGround(false, 6, 2); s.observeGround(true, 7, 2);
        assertEquals(2, s.usedJumps());
    }
    @Test void holdingAndReplayCannotJumpAgain() {
        var s = airborne(); assertEquals(2, press(s, 1, 2, 3));
        assertEquals(0, s.requestJump(3, true, 4, false, true, 3));
        assertEquals(0, s.requestJump(1, false, 4, false, true, 3));
        assertEquals(0, s.requestJump(4, true, 5, false, true, 3));
    }
    @Test void delayedVelocityCannotCreateFalseLanding() {
        var s = airborne(); press(s, 1, 2, 3);
        for (int tick = 3; tick < 20; tick++) s.observeGround(true, false, tick, 2);
        assertEquals(2, s.usedJumps());
        s.observeGround(false, true, 20, 2);
        s.observeGround(true, false, 21, 2); s.observeGround(true, false, 22, 2);
        assertEquals(0, s.usedJumps());
    }
    @Test void sameTickCannotExecuteTwoJumps() {
        var s = airborne(); assertEquals(2, press(s, 1, 2, 3)); assertEquals(0, press(s, 3, 2, 3));
    }
    @Test void groundPressUsesVanillaAndLedgeReservesFirstJump() {
        var s = new MovementState(); s.observeGround(true, 1, 2); s.observeGround(true, 2, 2);
        assertEquals(0, s.requestJump(1, true, 3, true, true, 3));
        s.observeGround(false, 4, 2); assertEquals(1, s.usedJumps());
        assertEquals(0, s.requestJump(2, true, 5, false, true, 3));
        assertEquals(2, press(s, 3, 6, 3));
    }
    @Test void restrictedRequestsFailAndPlayersAreIndependent() {
        var a = airborne(); var b = airborne(); press(a, 1, 2, 3);
        assertEquals(1, b.usedJumps()); assertEquals(2, a.usedJumps());
        assertEquals(0, b.requestJump(1, true, 3, false, false, 3));
    }
}
