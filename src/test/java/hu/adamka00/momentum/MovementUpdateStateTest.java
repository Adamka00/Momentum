package hu.adamka00.momentum;

import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementUpdateStateTest {
    @Test void slideFrictionDurationAndSpeedCap() {
        var state = new SlideState(); var config = new MomentumConfig.Slide();
        assertTrue(state.tick(1, true, true, true, 100, config));
        assertEquals(3, state.speed());
        assertEquals(20, state.remainingTicks());
        assertFalse(state.tick(2, true, true, true, 1, config));
        assertEquals(2.76, state.speed(), 1e-9);
        assertEquals(19, state.remainingTicks());
        for (int tick = 3; tick <= 21; tick++) state.tick(tick, true, true, true, 1, config);
        assertFalse(state.active()); assertEquals(0, state.speed());
    }

    @Test void slideRequiresFreshCrouchAndKeepsCooldownAfterCancel() {
        var state = new SlideState(); var config = new MomentumConfig.Slide();
        assertTrue(state.tick(10, true, true, true, 0.4, config));
        state.tick(11, false, true, true, 0.4, config);
        assertFalse(state.active()); assertEquals(29, state.cooldown(11));
        assertFalse(state.tick(12, true, true, true, 0.4, config));
        for (int tick = 13; tick <= 40; tick++) assertFalse(state.tick(tick, true, true, true, 0.4, config));
        state.tick(41, false, true, true, 0.4, config);
        assertTrue(state.tick(42, true, true, true, 0.4, config));
    }

    @Test void slideRejectsInvalidStatesAndSlowOrNonfiniteMotion() {
        var config = new MomentumConfig.Slide();
        assertFalse(new SlideState().tick(1, true, false, true, 0.5, config));
        assertFalse(new SlideState().tick(1, true, true, false, 0.5, config));
        assertFalse(new SlideState().tick(1, true, true, true, 0.01, config));
        assertFalse(new SlideState().tick(1, true, true, true, Double.NaN, config));
        var state = new SlideState(); state.tick(1, true, true, true, 0.5, config);
        state.tick(2, true, false, false, 0.5, config);
        assertFalse(state.active());
    }

    @Test void slideCanIgnoreReleaseButCannotIgnoreCollisionOrTickGaps() {
        var config = new MomentumConfig.Slide(); config.cancelOnCrouchRelease = false;
        var state = new SlideState(); state.tick(1, true, true, true, 0.5, config);
        state.tick(2, false, false, true, 0.5, config); assertTrue(state.active());
        state.tick(2, false, false, true, 0.5, config); assertEquals(19, state.remainingTicks());
        state.tick(4, false, false, true, 0.5, config); assertFalse(state.active());
        assertTrue(new SlideState().tick(1, true, true, true, 0.5, config));
    }

    @Test void wallJumpLimitsRemainAcrossCornersAndSameWallCooldownIsExact() {
        var config = new MomentumConfig.WallJump(); var state = new WallJumpState();
        assertTrue(state.request(1, true, 1, true, WallJumpState.Wall.NORTH, config));
        state.request(2, false, 2, true, null, config);
        assertFalse(state.request(3, true, 10, true, WallJumpState.Wall.NORTH, config));
        state.request(4, false, 10, true, null, config);
        assertTrue(state.request(5, true, 11, true, WallJumpState.Wall.NORTH, config));
        state.request(6, false, 12, true, null, config);
        assertFalse(state.request(7, true, 12, true, WallJumpState.Wall.EAST, config));
        assertEquals(0, state.remaining(config.maximumWallJumps));
        state.confirmedLanding();
        assertEquals(2, state.remaining(config.maximumWallJumps));
        assertFalse(state.request(8, true, 13, true, WallJumpState.Wall.SOUTH, config), "Landing cannot synthesize a new press");
        state.request(9, false, 14, true, null, config);
        assertTrue(state.request(10, true, 14, true, WallJumpState.Wall.SOUTH, config));
    }

    @Test void wallJumpRejectsHeldKeysReplayGroundAndMissingGeometry() {
        var config = new MomentumConfig.WallJump(); var state = new WallJumpState();
        assertFalse(state.request(1, true, 1, false, WallJumpState.Wall.NORTH, config));
        assertFalse(state.request(2, true, 2, true, WallJumpState.Wall.NORTH, config));
        state.request(3, false, 3, true, null, config);
        assertFalse(state.request(2, true, 4, true, WallJumpState.Wall.NORTH, config));
        assertFalse(state.request(4, true, 4, true, null, config));
        assertEquals(0, state.used());
    }

    @Test void longJumpNeedsSupportForwardSpeedSprintAndFreshPress() {
        var config = new MomentumConfig.LongJump();
        assertFalse(new LongJumpState().request(1, true, 1, false, true, true, false, 0.3, config));
        assertFalse(new LongJumpState().request(1, true, 1, true, false, true, false, 0.3, config));
        assertFalse(new LongJumpState().request(1, true, 1, true, true, false, false, 0.3, config));
        assertFalse(new LongJumpState().request(1, true, 1, true, true, true, false, Double.NaN, config));
        assertFalse(new LongJumpState().request(1, true, 1, true, true, true, false, 0.01, config));
        var state = new LongJumpState();
        assertTrue(state.request(1, true, 1, true, true, true, false, 0.3, config));
        assertFalse(state.request(2, true, 41, true, true, true, false, 0.3, config));
        state.request(3, false, 41, true, true, true, false, 0.3, config);
        assertTrue(state.request(4, true, 41, true, true, true, false, 0.3, config));
    }

    @Test void longJumpCooldownAndSlideRestrictionsDoNotLeakBetweenPlayers() {
        var config = new MomentumConfig.LongJump(); config.allowFromSlide = false;
        var a = new LongJumpState(); var b = new LongJumpState();
        assertFalse(a.request(1, true, 1, true, true, true, true, 0.3, config));
        assertEquals(0, a.cooldown(1));
        a.request(2, false, 2, true, true, true, false, 0.3, config);
        assertTrue(a.request(3, true, 2, true, true, true, false, 0.3, config));
        assertEquals(40, a.cooldown(2)); assertEquals(0, b.cooldown(2));
        a.request(4, false, 41, true, true, true, false, 0.3, config);
        assertFalse(a.request(5, true, 41, true, true, true, false, 0.3, config));
        assertEquals(1, a.cooldown(41));
        a.request(6, false, 42, true, true, true, false, 0.3, config);
        assertTrue(a.request(7, true, 42, true, true, true, false, 0.3, config));
        assertFalse(a.request(7, true, 100, true, true, true, false, 0.3, config));
    }
}
