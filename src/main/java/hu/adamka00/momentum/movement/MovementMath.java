package hu.adamka00.momentum.movement;

public final class MovementMath {
    private MovementMath() { }
    public record Horizontal(double x, double z) { }
    public static boolean validInput(int forward, int strafe) {
        return forward >= -1 && forward <= 1 && strafe >= -1 && strafe <= 1;
    }
    /** Positive strafe means left; yaw zero faces positive z, matching Minecraft. */
    public static Horizontal dash(double yawDegrees, int forward, int strafe, double impulse) {
        if (!Double.isFinite(yawDegrees) || !validInput(forward, strafe)) throw new IllegalArgumentException("Invalid direction input");
        if (forward == 0 && strafe == 0) forward = 1;
        double magnitude = Math.hypot(forward, strafe);
        double yaw = Math.toRadians(yawDegrees);
        return new Horizontal((strafe * Math.cos(yaw) - forward * Math.sin(yaw)) / magnitude * impulse,
                              (forward * Math.cos(yaw) + strafe * Math.sin(yaw)) / magnitude * impulse);
    }
}
