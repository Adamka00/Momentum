package hu.adamka00.momentum;
import hu.adamka00.momentum.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DashStateTest {
    @Test void deterministicCooldownExpiresAtExactTick() {
        var s = new MovementState(); assertTrue(s.requestDash(1, 10, true, 40));
        assertEquals(40, s.dashCooldown(10)); assertEquals(1, s.dashCooldown(49));
        assertFalse(s.requestDash(2, 49, true, 40));
        assertTrue(s.requestDash(3, 50, true, 40)); assertEquals(40, s.dashCooldown(50));
    }
    @Test void replayAndSameTickAreRejectedEvenAfterCooldown() {
        var s = new MovementState(); assertTrue(s.requestDash(7, 1, true, 1));
        assertFalse(s.requestDash(8, 1, true, 1)); assertFalse(s.requestDash(7, 100, true, 1));
        assertTrue(s.requestDash(9, 100, true, 1));
    }
    @Test void rejectedActionsCannotSpendCooldownOrBeReplayed() {
        var s = new MovementState(); assertFalse(s.requestDash(1, 1, false, 40));
        assertEquals(0, s.dashCooldown(1)); assertFalse(s.requestDash(1, 2, true, 40));
    }
    @Test void dashesDoNotResetJumpAndPlayersHaveIndependentCooldowns() {
        var a = new MovementState(); var b = new MovementState();
        a.observeGround(false, 1, 2); a.requestJump(1, true, 2, false, true, 3);
        assertTrue(a.requestDash(1, 2, true, 40)); assertEquals(2, a.usedJumps());
        assertEquals(0, b.dashCooldown(2)); assertTrue(b.requestDash(1, 2, true, 40));
    }
    @Test void normalizedDiagonalAndForwardFallback() {
        var diagonal = MovementMath.dash(0, 1, 1, 1.2);
        assertEquals(1.2, Math.hypot(diagonal.x(), diagonal.z()), 1e-9);
        var forward = MovementMath.dash(90, 0, 0, 1.2);
        assertEquals(-1.2, forward.x(), 1e-9); assertEquals(0, forward.z(), 1e-9);
        assertEquals(-1.2, MovementMath.dash(0, -1, 0, 1.2).z(), 1e-9);
    }
    @Test void rejectsInvalidDirectionalInputsAndRequestFlood() {
        assertFalse(MovementMath.validInput(2, 0));
        assertThrows(IllegalArgumentException.class, () -> MovementMath.dash(Double.NaN, 1, 0, 1.2));
        assertThrows(IllegalArgumentException.class, () -> MovementMath.dash(0, 100, 0, 1.2));
        var s = new MovementState(); for (int i = 0; i < 8; i++) assertTrue(s.admitRequest(1));
        assertFalse(s.admitRequest(1)); assertTrue(s.admitRequest(2));
    }
}
