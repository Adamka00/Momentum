package hu.adamka00.momentum;

import hu.adamka00.momentum.config.*;
import hu.adamka00.momentum.movement.MovementState;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SettingsMetadataTest {
    @Test void everySettingHasValidDefaultBoundsAndCanBeReset() {
        var config = new MomentumConfig();
        var expected = ConfigManager.GSON.toJsonTree(config);
        for (SettingSpec spec : SettingSpec.all()) {
            assertTrue(spec.set(config, spec.get(config).toString()), spec.path());
            if (!spec.toggle()) {
                assertTrue(spec.bounds(config).minimum() <= spec.bounds(config).maximum(), spec.path());
                assertFalse(spec.set(config, "NaN"), spec.path());
                assertFalse(spec.set(config, "Infinity"), spec.path());
                assertFalse(spec.set(config, "-99999"), spec.path());
            }
            spec.reset(config, new MomentumConfig());
        }
        assertEquals(expected, ConfigManager.GSON.toJsonTree(config));
        assertTrue(config.validate().isEmpty());
    }

    @Test void numericFieldsRejectFractionalIntegersAndHandleSignedImpulse() {
        var config = new MomentumConfig();
        var count = SettingSpec.all().stream().filter(s -> s.path().equals("jump.maximumTotalJumps")).findFirst().orElseThrow();
        assertFalse(count.set(config, "2.5")); assertEquals(3, config.jump.maximumTotalJumps);
        var impulse = SettingSpec.all().stream().filter(s -> s.path().equals("dash.verticalImpulse")).findFirst().orElseThrow();
        assertTrue(impulse.set(config, "-0.5")); assertTrue(impulse.set(config, "0.5"));
        assertFalse(impulse.set(config, "0.51"));
    }

    @Test void bothLanguagesCoverEverySettingTooltipAndCategory() throws Exception {
        var english = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/momentum/lang/en_us.json"))).getAsJsonObject();
        var hungarian = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/momentum/lang/hu_hu.json"))).getAsJsonObject();
        assertEquals(english.keySet(), hungarian.keySet());
        for (var translation : java.util.List.of(english, hungarian)) {
            for (SettingSpec spec : SettingSpec.all()) {
                assertTrue(translation.has(spec.key()), spec.key());
                assertTrue(translation.has(spec.key() + ".tooltip"), spec.key());
            }
            for (String category : SettingSpec.CATEGORIES) assertTrue(translation.has("settings.momentum.category." + category));
            for (var entry : translation.entrySet()) assertFalse(entry.getValue().getAsString().isBlank(), entry.getKey());
        }
    }

    @Test void longJumpReservesGroundJumpAndCannotResetSpentAirJumps() {
        var state = new MovementState();
        state.takeoff(10, false);
        assertEquals(1, state.usedJumps()); assertEquals(2, state.remainingJumps(3));
        state.observeGround(false, true, 11, 2);
        assertEquals(2, state.requestJump(1, true, 12, false, true, 3));
        state.takeoff(13, false); assertEquals(2, state.usedJumps());
        state.requestJump(2, false, 14, false, true, 3);
        assertEquals(3, state.requestJump(3, true, 15, false, true, 3));
        state.takeoff(16, false); assertEquals(0, state.remainingJumps(3));
        state.takeoff(17, true); assertEquals(2, state.remainingJumps(3));
    }

    @Test void takeoffRequiresActualSeparationBeforeSupportedLandingCanReset() {
        var state = new MovementState(); state.takeoff(1, false);
        for (int tick = 2; tick <= 10; tick++) state.observeGround(true, false, tick, 2);
        assertEquals(1, state.usedJumps());
        state.observeGround(false, true, 11, 2);
        state.observeGround(true, false, 12, 2); state.observeGround(true, false, 13, 2);
        assertEquals(0, state.usedJumps());
    }
}
