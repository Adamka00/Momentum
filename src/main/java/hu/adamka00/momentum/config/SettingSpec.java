package hu.adamka00.momentum.config;

import java.lang.reflect.Field;
import java.util.*;

/** Shared native-screen metadata; every public config field must have a descriptor. */
public record SettingSpec(String category, String name, Field sectionField, Field valueField) {
    public record Bounds(double minimum, double maximum) { }
    private static final Map<String, Bounds> NUMBERS = Map.ofEntries(
        Map.entry("jump.maximumTotalJumps", new Bounds(1, 3)),
        Map.entry("jump.landingConfirmationTicks", new Bounds(2, 10)),
        Map.entry("jump.secondStrength", new Bounds(0.1, 1.5)),
        Map.entry("jump.thirdStrength", new Bounds(0.1, 1.5)),
        Map.entry("sprint.activationDelayTicks", new Bounds(0, 1200)),
        Map.entry("sprint.accelerationTicks", new Bounds(1, 1200)),
        Map.entry("sprint.maximumMultiplier", new Bounds(1, 2)),
        Map.entry("sprint.decelerationPerTick", new Bounds(0.001, 1)),
        Map.entry("dash.horizontalImpulse", new Bounds(0.1, 3)),
        Map.entry("dash.verticalImpulse", new Bounds(-0.5, 0.5)),
        Map.entry("dash.cooldownTicks", new Bounds(1, 1200)),
        Map.entry("slide.durationTicks", new Bounds(1, 200)),
        Map.entry("slide.cooldownTicks", new Bounds(1, 1200)),
        Map.entry("slide.initialSpeedMultiplier", new Bounds(1, 2)),
        Map.entry("slide.friction", new Bounds(0.1, 0.99)),
        Map.entry("slide.minimumActivationSpeed", new Bounds(0.01, 1)),
        Map.entry("slide.minimumContinueSpeed", new Bounds(0.001, 1)),
        Map.entry("wallJump.verticalStrength", new Bounds(0.1, 1.5)),
        Map.entry("wallJump.horizontalStrength", new Bounds(0.1, 1.5)),
        Map.entry("wallJump.wallDetectionDistance", new Bounds(0.01, 0.3)),
        Map.entry("wallJump.maximumWallJumps", new Bounds(0, 8)),
        Map.entry("wallJump.sameWallCooldownTicks", new Bounds(2, 1200)),
        Map.entry("longJump.horizontalStrength", new Bounds(0.1, 3)),
        Map.entry("longJump.verticalStrength", new Bounds(0.1, 1.5)),
        Map.entry("longJump.minimumActivationSpeed", new Bounds(0.01, 1)),
        Map.entry("longJump.cooldownTicks", new Bounds(1, 1200)),
        Map.entry("client.hudX", new Bounds(0, 1)),
        Map.entry("client.hudY", new Bounds(0, 1)),
        Map.entry("client.hudScale", new Bounds(0.5, 3)),
        Map.entry("client.hudOpacity", new Bounds(0.1, 1)));
    public static final List<String> CATEGORIES = List.of("general", "jump", "sprint", "dash", "slide", "wallJump", "longJump", "client");
    public static List<SettingSpec> all() {
        List<SettingSpec> result = new ArrayList<>();
        try {
            for (String category : CATEGORIES) {
                Field section = MomentumConfig.class.getField(category);
                for (Field field : section.getType().getFields()) {
                    var spec = new SettingSpec(category, field.getName(), section, field);
                    if (!spec.toggle() && !NUMBERS.containsKey(spec.path())) throw new IllegalStateException("Missing numeric bounds: " + spec.path());
                    result.add(spec);
                }
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        return List.copyOf(result);
    }
    public String path() { return category + "." + name; }
    public String key() { return "settings.momentum." + path(); }
    public boolean toggle() { return valueField.getType() == boolean.class; }
    public Bounds bounds(MomentumConfig config) {
        var b = NUMBERS.get(path());
        return path().equals("slide.minimumContinueSpeed") ? new Bounds(b.minimum(), config.slide.minimumActivationSpeed) : b;
    }
    public Object get(MomentumConfig config) {
        try { return valueField.get(sectionField.get(config)); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    public boolean set(MomentumConfig config, String text) {
        Object value;
        if (toggle()) {
            if (!text.equals("true") && !text.equals("false")) return false;
            value = Boolean.parseBoolean(text);
        } else {
            double number;
            try { number = Double.parseDouble(text); } catch (NumberFormatException error) { return false; }
            Bounds b = bounds(config);
            if (!Double.isFinite(number) || number < b.minimum() || number > b.maximum()) return false;
            if (valueField.getType() == int.class) {
                if (number != Math.rint(number)) return false;
                value = (int) number;
            } else value = number;
        }
        try { valueField.set(sectionField.get(config), value); return true; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    public void reset(MomentumConfig config, MomentumConfig defaults) {
        try { valueField.set(sectionField.get(config), get(defaults)); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
}
