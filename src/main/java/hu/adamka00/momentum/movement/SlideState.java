package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.MomentumConfig;

/** Pure slide timer; its caller must supply server-observed eligibility and speed.
 * No pose or velocity is applied here. Speed is always bounded to 3 blocks/tick. */
public final class SlideState {
    private long lastTick = -1, nextSlideTick;
    private boolean crouchHeld;
    private int remainingTicks;
    private double speed;

    public boolean active() { return remainingTicks > 0; }
    public int remainingTicks() { return remainingTicks; }
    public double speed() { return speed; }
    public int cooldown(long tick) { return (int) Math.clamp(nextSlideTick - tick, 0, 1200); }

    /** Exactly one call per consecutive server tick; omitted samples cancel an active slide.
     * Eligibility on activation includes sprinting, actual foot support and unrestricted state.
     * Continuation excludes sprint (vanilla crouching can stop sprinting).
     * The returned speed is a replacement magnitude, never a multiplier of current velocity. */
    public boolean tick(long tick, boolean crouching, boolean canStart, boolean canContinue,
                        double observedSpeed, MomentumConfig.Slide config) {
        if (tick <= lastTick) return false;
        if (lastTick >= 0 && tick != lastTick + 1) cancel();
        lastTick = tick;
        boolean edge = crouching && !crouchHeld;
        crouchHeld = crouching;
        if (!config.enabled || !canContinue || !Double.isFinite(observedSpeed) || observedSpeed < 0) {
            cancel();
            return false;
        }
        if (active()) {
            remainingTicks--;
            speed *= config.friction;
            if (remainingTicks == 0 || speed < config.minimumContinueSpeed
                || observedSpeed < config.minimumContinueSpeed
                || (config.cancelOnCrouchRelease && !crouching)) cancel();
            return false;
        }
        if (!edge || !canStart || tick < nextSlideTick || observedSpeed < config.minimumActivationSpeed) return false;
        speed = Math.min(3, observedSpeed * config.initialSpeedMultiplier);
        remainingTicks = config.durationTicks;
        nextSlideTick = tick + config.cooldownTicks;
        return true;
    }

    /** Invalid states cancel motion without refunding cooldown or fabricating a new key edge. */
    public void cancel() { remainingTicks = 0; speed = 0; }
}
