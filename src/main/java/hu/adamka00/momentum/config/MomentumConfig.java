package hu.adamka00.momentum.config;

import java.util.ArrayList;
import java.util.List;

/** JSON model. validate() repairs every user-controlled numeric value before use. */
public final class MomentumConfig {
    public General general = new General();
    public Jump jump = new Jump();
    public Sprint sprint = new Sprint();
    public Dash dash = new Dash();
    public Slide slide = new Slide();
    public WallJump wallJump = new WallJump();
    public LongJump longJump = new LongJump();
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
        public boolean allowAirDash = true, particles = true, sounds = true;
    }
    /** Speeds are blocks/tick; friction is retained horizontal speed per tick. */
    public static final class Slide {
        public boolean enabled = true, cancelOnCrouchRelease = true, allowSlideJump = true;
        public boolean particles = true, sounds = true;
        public int durationTicks = 20, cooldownTicks = 30;
        public double initialSpeedMultiplier = 1.15, friction = 0.92;
        public double minimumActivationSpeed = 0.15, minimumContinueSpeed = 0.06;
    }
    public static final class WallJump {
        public boolean enabled = true, resetAirJumpsOnWallJump = false, particles = true, sounds = true;
        public double verticalStrength = 0.42, horizontalStrength = 0.45, wallDetectionDistance = 0.15;
        public int maximumWallJumps = 2, sameWallCooldownTicks = 10;
    }
    public static final class LongJump {
        public boolean enabled = true, requireSprint = true, allowFromSlide = true, particles = true, sounds = true;
        public double horizontalStrength = 0.9, verticalStrength = 0.34, minimumActivationSpeed = 0.15;
        public int cooldownTicks = 40;
    }
    /** Read only from the local file on the physical client; never enforced by a server. */
    public static final class Client {
        public boolean showHud = true, particles = true, sounds = true, cooldownFeedback = true;
        public boolean showAirJumps = true, showWallJumps = true, showDash = true;
        public boolean showLongJump = true, showSlide = true, showSprint = true;
        /** Fractions of available screen space; independent of resolution. */
        public double hudX = 0, hudY = 1, hudScale = 1, hudOpacity = 1;
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
        if (slide == null) { slide = new Slide(); errors.add("Missing slide section; using defaults"); }
        if (wallJump == null) { wallJump = new WallJump(); errors.add("Missing wallJump section; using defaults"); }
        if (longJump == null) { longJump = new LongJump(); errors.add("Missing longJump section; using defaults"); }
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
        slide.durationTicks = integer("slide.durationTicks", slide.durationTicks, 1, 200, errors);
        slide.cooldownTicks = integer("slide.cooldownTicks", slide.cooldownTicks, 1, 1200, errors);
        slide.initialSpeedMultiplier = number("slide.initialSpeedMultiplier", slide.initialSpeedMultiplier, 1, 2, 1.15, errors);
        slide.friction = number("slide.friction", slide.friction, 0.1, 0.99, 0.92, errors);
        slide.minimumActivationSpeed = number("slide.minimumActivationSpeed", slide.minimumActivationSpeed, 0.01, 1, 0.15, errors);
        slide.minimumContinueSpeed = number("slide.minimumContinueSpeed", slide.minimumContinueSpeed, 0.001, slide.minimumActivationSpeed, 0.06, errors);
        // The fallback also respects the cross-field constraint after nonfinite input.
        slide.minimumContinueSpeed = Math.min(slide.minimumContinueSpeed, slide.minimumActivationSpeed);
        wallJump.verticalStrength = number("wallJump.verticalStrength", wallJump.verticalStrength, 0.1, 1.5, 0.42, errors);
        wallJump.horizontalStrength = number("wallJump.horizontalStrength", wallJump.horizontalStrength, 0.1, 1.5, 0.45, errors);
        wallJump.wallDetectionDistance = number("wallJump.wallDetectionDistance", wallJump.wallDetectionDistance, 0.01, 0.3, 0.15, errors);
        wallJump.maximumWallJumps = integer("wallJump.maximumWallJumps", wallJump.maximumWallJumps, 0, 8, errors);
        wallJump.sameWallCooldownTicks = integer("wallJump.sameWallCooldownTicks", wallJump.sameWallCooldownTicks, 2, 1200, errors);
        longJump.horizontalStrength = number("longJump.horizontalStrength", longJump.horizontalStrength, 0.1, 3, 0.9, errors);
        longJump.verticalStrength = number("longJump.verticalStrength", longJump.verticalStrength, 0.1, 1.5, 0.34, errors);
        longJump.minimumActivationSpeed = number("longJump.minimumActivationSpeed", longJump.minimumActivationSpeed, 0.01, 1, 0.15, errors);
        longJump.cooldownTicks = integer("longJump.cooldownTicks", longJump.cooldownTicks, 1, 1200, errors);
        client.hudX = number("client.hudX", client.hudX, 0, 1, 0, errors);
        client.hudY = number("client.hudY", client.hudY, 0, 1, 1, errors);
        client.hudScale = number("client.hudScale", client.hudScale, 0.5, 3, 1, errors);
        client.hudOpacity = number("client.hudOpacity", client.hudOpacity, 0.1, 1, 1, errors);
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
