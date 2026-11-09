package hu.adamka00.momentum.movement;

/** Pure, tick-based per-session state. No client coordinates or counters are accepted. */
public final class MovementState {
    private int usedJumps, groundTicks;
    private boolean grounded, jumpHeld;
    private boolean awaitingSeparation;
    private long lastSampleTick = -1, lastJumpSequence = -1, lastJumpTick = Long.MIN_VALUE / 2;
    private long rateTick = -1;
    private int requestsThisTick;
    private int sprintTicks;
    private double sprintMultiplier = 1;
    private long nextDashTick, lastDashTick = Long.MIN_VALUE / 2, lastDashSequence = -1;
    private long lastHorizontalMotionTick = Long.MIN_VALUE / 2;

    public int usedJumps() { return usedJumps; }
    public boolean grounded() { return grounded; }
    public int sprintTicks() { return sprintTicks; }
    public double sprintMultiplier() { return sprintMultiplier; }
    public int dashCooldown(long tick) { return (int) Math.max(0, nextDashTick - tick); }
    public void observeHorizontalMotion(boolean moving, long tick) {
        if (moving) lastHorizontalMotionTick = tick;
    }
    public boolean hasRecentHorizontalMotion(long tick) { return tick - lastHorizontalMotionTick <= 3; }
    public int remainingJumps(int maximum) { return Math.max(0, maximum - Math.max(1, usedJumps)); }

    public void observeGround(boolean supported, long tick, int confirmationTicks) {
        observeGround(supported, !supported, tick, confirmationTicks);
    }

    public void observeGround(boolean supported, boolean physicallySeparated, long tick, int confirmationTicks) {
        if (tick <= lastSampleTick) return;
        lastSampleTick = tick;
        if (physicallySeparated) awaitingSeparation = false;
        if (supported && !awaitingSeparation && tick - lastJumpTick >= confirmationTicks) {
            groundTicks++;
            if (groundTicks >= confirmationTicks) { grounded = true; usedJumps = 0; }
        } else {
            groundTicks = 0;
            grounded = false;
            // Walking off a ledge consumes the ordinary ground jump as well.
            usedJumps = Math.max(1, usedJumps);
        }
    }

    /** Returns the accepted total jump number, or zero. Releases never execute a jump. */
    public int requestJump(long sequence, boolean pressed, long tick, boolean physicallyGrounded,
                           boolean allowed, int maximum) {
        if (sequence < 0 || sequence <= lastJumpSequence) return 0;
        lastJumpSequence = sequence;
        boolean fresh = pressed && !jumpHeld;
        jumpHeld = pressed;
        if (!fresh || physicallyGrounded || !allowed || tick - lastJumpTick < 2) return 0;
        int next = Math.max(1, usedJumps) + 1;
        if (next > maximum) return 0;
        usedJumps = next;
        lastJumpTick = tick;
        awaitingSeparation = true;
        grounded = false;
        groundTicks = 0;
        return next;
    }

    /** Bounds processing and outgoing acknowledgements even for malformed clients. */
    public boolean admitRequest(long tick) {
        if (rateTick != tick) { rateTick = tick; requestsThisTick = 0; }
        return ++requestsThisTick <= 8;
    }

    public void tickSprint(boolean eligible, boolean restricted, int delay, int acceleration, double maximum, double deceleration) {
        if (restricted) { sprintTicks = 0; sprintMultiplier = 1; return; }
        if (eligible) {
            sprintTicks = Math.min(sprintTicks + 1, delay + acceleration);
            double progress = Math.clamp((sprintTicks - delay) / (double) acceleration, 0, 1);
            // Re-entering a sprint while decelerating must not abruptly drop the speed.
            double target = 1 + (maximum - 1) * progress;
            sprintMultiplier = Math.min(maximum, Math.max(target, sprintMultiplier));
        } else {
            sprintTicks = 0;
            sprintMultiplier = Math.max(1, sprintMultiplier - deceleration);
        }
    }

    public boolean requestDash(long sequence, long tick, boolean allowed, int cooldownTicks) {
        if (sequence < 0 || sequence <= lastDashSequence) return false;
        lastDashSequence = sequence;
        if (!allowed || tick < nextDashTick || tick == lastDashTick) return false;
        lastDashTick = tick;
        nextDashTick = tick + cooldownTicks;
        return true;
    }
}
