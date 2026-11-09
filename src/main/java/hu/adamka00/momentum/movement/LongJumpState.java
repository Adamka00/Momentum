package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.MomentumConfig;

/** Pure long-jump cooldown/press guard; the server caller validates physical support and direction. */
public final class LongJumpState {
    private long lastSequence = -1, lastActionTick = -1, nextTick;
    private boolean held;
    public int cooldown(long tick) { return (int) Math.clamp(nextTick - tick, 0, 1200); }

    public boolean request(long sequence, boolean pressed, long tick, boolean groundedAndAllowed,
                           boolean sprinting, boolean forward, boolean sliding, double observedSpeed,
                           MomentumConfig.LongJump config) {
        if (sequence < 0 || sequence <= lastSequence) return false;
        lastSequence = sequence;
        boolean edge = pressed && !held;
        held = pressed;
        if (!edge || !config.enabled || !groundedAndAllowed || !forward
            || (config.requireSprint && !sprinting) || (sliding && !config.allowFromSlide)
            || !Double.isFinite(observedSpeed) || observedSpeed < config.minimumActivationSpeed
            || tick <= lastActionTick || tick < nextTick) return false;
        lastActionTick = tick;
        nextTick = tick + config.cooldownTicks;
        return true;
    }
}
