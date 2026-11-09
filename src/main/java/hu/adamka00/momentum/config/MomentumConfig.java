package hu.adamka00.momentum.config;

import java.util.ArrayList;
import java.util.List;

/** JSON model. validate() repairs every user-controlled numeric value before use. */
public final class MomentumConfig {
    public General general = new General();
    public Jump jump = new Jump();
    public Sprint sprint = new Sprint();
    public Dash dash = new Dash();
    public Client client = new Client();

    public static final class General {
        public boolean doubleJump = true, tripleJump = true, sprintBoost = true, dash = true;
        public boolean particles = true, sounds = true, debugLogging = false;
    }
    public static final class Jump {
        public int maximumTotalJumps = 3;
        public double secondStrength = 0.42, thirdStrength = 0.46;
        public boolean allowInCreative = true;
        public int landingConfirmationTicks = 2;
    }
    public static final class Sprint {
        public int activationDelayTicks = 20, accelerationTicks = 60;
        public double maximumMultiplier = 1.35, decelerationPerTick = 0.025;
        public boolean particles = false;
    }
    public static final class Dash {
        public double horizontalImpulse = 1.2, verticalImpulse = 0;
        public int cooldownTicks = 40;
        public boolean allowAirDash = true;
    }
    /** Read only from the local file on the physical client; never enforced by a server. */
    public static final class Client {
        public boolean showHud = true, particles = true, sounds = true, cooldownFeedback = true;
    }

    public int maximumJumps() {
        if (general.tripleJump) return jump.maximumTotalJumps;
        return general.doubleJump ? Math.min(2, jump.maximumTotalJumps) : 1;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (general == null) { general = new General(); errors.add("Missing general section; using defaults"); }
        if (jump == null) { jump = new Jump(); errors.add("Missing jump section; using defaults"); }
        if (sprint == null) { sprint = new Sprint(); errors.add("Missing sprint section; using defaults"); }
        if (dash == null) { dash = new Dash(); errors.add("Missing dash section; using defaults"); }
        if (client == null) { client = new Client(); errors.add("Missing client section; using defaults"); }
        jump.maximumTotalJumps = integer("jump.maximumTotalJumps", jump.maximumTotalJumps, 1, 3, errors);
        jump.landingConfirmationTicks = integer("jump.landingConfirmationTicks", jump.landingConfirmationTicks, 2, 10, errors);
        jump.secondStrength = number("jump.secondStrength", jump.secondStrength, 0.1, 1.5, 0.42, errors);
        jump.thirdStrength = number("jump.thirdStrength", jump.thirdStrength, 0.1, 1.5, 0.46, errors);
        sprint.activationDelayTicks = integer("sprint.activationDelayTicks", sprint.activationDelayTicks, 0, 1200, errors);
        sprint.accelerationTicks = integer("sprint.accelerationTicks", sprint.accelerationTicks, 1, 1200, errors);
        sprint.maximumMultiplier = number("sprint.maximumMultiplier", sprint.maximumMultiplier, 1, 2, 1.35, errors);
        sprint.decelerationPerTick = number("sprint.decelerationPerTick", sprint.decelerationPerTick, 0.001, 1, 0.025, errors);
        dash.horizontalImpulse = number("dash.horizontalImpulse", dash.horizontalImpulse, 0.1, 3, 1.2, errors);
        dash.verticalImpulse = number("dash.verticalImpulse", dash.verticalImpulse, -0.5, 0.5, 0, errors);
        dash.cooldownTicks = integer("dash.cooldownTicks", dash.cooldownTicks, 1, 1200, errors);
        return errors;
    }
    private static int integer(String name, int value, int min, int max, List<String> errors) {
        int fixed = Math.clamp(value, min, max);
        if (fixed != value) errors.add(name + " out of range; clamped to " + fixed);
        return fixed;
    }
    private static double number(String name, double value, double min, double max, double fallback, List<String> errors) {
        double fixed = Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
        if (Double.compare(value, fixed) != 0) errors.add(name + " invalid; repaired to " + fixed);
        return fixed;
    }
}
