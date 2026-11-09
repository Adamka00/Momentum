package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.MomentumConfig;
import java.util.Arrays;

/** Per-airtime counter. Wall normals must come from a server collision query, never a packet. */
public final class WallJumpState {
    public enum Wall { NORTH, SOUTH, EAST, WEST }
    private final long[] nextWallTick = new long[4];
    private long lastSequence = -1, lastJumpTick = -1;
    private int used;
    private boolean held;

    public int used() { return used; }
    public int remaining(int maximum) { return Math.max(0, maximum - used); }

    /** Must be used as part of the same jump press arbitration as ordinary air jumps. */
    public boolean request(long sequence, boolean pressed, long tick, boolean airborneAndAllowed,
                           Wall wall, MomentumConfig.WallJump config) {
        if (sequence < 0 || sequence <= lastSequence) return false;
        lastSequence = sequence;
        boolean edge = pressed && !held;
        held = pressed;
        if (!edge || !config.enabled || !airborneAndAllowed || wall == null || tick <= lastJumpTick
            || used >= config.maximumWallJumps || tick < nextWallTick[wall.ordinal()]) return false;
        used++;
        lastJumpTick = tick;
        nextWallTick[wall.ordinal()] = tick + config.sameWallCooldownTicks;
        return true;
    }

    /** Only a confirmed supported landing resets the airtime budget; releases and wall changes don't.
     * Replay and held-key history survive landings. */
    public void confirmedLanding() { used = 0; Arrays.fill(nextWallTick, 0); }
}
