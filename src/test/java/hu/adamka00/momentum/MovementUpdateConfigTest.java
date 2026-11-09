package hu.adamka00.momentum;

import hu.adamka00.momentum.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MovementUpdateConfigTest {
    @TempDir Path directory;

    @Test void originalFileMigratesWithoutRewritingUserPreferences() throws Exception {
        String original = Files.readString(Path.of("src/test/resources/config/momentum-1.0.0.json"));
        var json = com.google.gson.JsonParser.parseString(original).getAsJsonObject();
        json.getAsJsonObject("jump").addProperty("secondStrength", 0.67);
        json.getAsJsonObject("client").addProperty("sounds", false);
        original = json.toString();
        Path file = directory.resolve("momentum.json");
        Files.writeString(file, original);
        var config = ConfigManager.load(file, LoggerFactory.getLogger("migration"));
        assertEquals(0.67, config.jump.secondStrength);
        assertFalse(config.client.sounds);
        assertTrue(config.slide.enabled);
        assertEquals(2, config.wallJump.maximumWallJumps);
        assertFalse(config.wallJump.resetAirJumpsOnWallJump);
        assertEquals(0.9, config.longJump.horizontalStrength);
        assertEquals(original, Files.readString(file));
        assertFalse(Files.exists(file.resolveSibling("momentum.json.invalid")));
    }

    @Test void explicitNullNewSectionsAreRepaired() {
        var config = ConfigManager.GSON.fromJson("{\"slide\":null,\"wallJump\":null,\"longJump\":null}", MomentumConfig.class);
        assertEquals(3, config.validate().size());
        assertNotNull(config.slide); assertNotNull(config.wallJump); assertNotNull(config.longJump);
    }

    @Test void newNumbersAreBoundedAndNonfiniteValuesRepaired() {
        var config = new MomentumConfig();
        config.slide.initialSpeedMultiplier = Double.POSITIVE_INFINITY;
        config.slide.friction = 9;
        config.slide.durationTicks = -4;
        config.slide.minimumActivationSpeed = 0.01;
        config.slide.minimumContinueSpeed = Double.NaN;
        config.wallJump.maximumWallJumps = Integer.MAX_VALUE;
        config.wallJump.wallDetectionDistance = 100;
        config.wallJump.sameWallCooldownTicks = -1;
        config.longJump.horizontalStrength = 1000;
        config.longJump.verticalStrength = Double.NaN;
        config.client.hudOpacity = Double.NaN;
        config.client.hudScale = -1;
        assertFalse(config.validate().isEmpty());
        assertEquals(1.15, config.slide.initialSpeedMultiplier);
        assertEquals(0.99, config.slide.friction);
        assertEquals(1, config.slide.durationTicks);
        assertEquals(0.01, config.slide.minimumContinueSpeed);
        assertEquals(8, config.wallJump.maximumWallJumps);
        assertEquals(0.3, config.wallJump.wallDetectionDistance);
        assertEquals(2, config.wallJump.sameWallCooldownTicks);
        assertEquals(3, config.longJump.horizontalStrength);
        assertEquals(0.34, config.longJump.verticalStrength);
        assertEquals(1, config.client.hudOpacity);
        assertEquals(0.5, config.client.hudScale);
        assertTrue(config.validate().isEmpty(), "Validation must be idempotent");
    }

    @Test void editedConfigurationSurvivesAtomicSaveAndReload() throws Exception {
        var config = new MomentumConfig();
        config.slide.enabled = false; config.wallJump.maximumWallJumps = 4;
        config.longJump.requireSprint = false; config.client.hudX = 0.8;
        config.client.showSlide = false;
        Path file = directory.resolve("config/momentum.json");
        ConfigManager.save(file, config);
        var reloaded = ConfigManager.load(file, LoggerFactory.getLogger("roundtrip"));
        assertEquals(ConfigManager.GSON.toJsonTree(config), ConfigManager.GSON.toJsonTree(reloaded));
        assertFalse(Files.exists(file.resolveSibling("momentum.json.tmp")));
        // Detached edit buffers must not mutate the live configuration before Apply.
        var edited = ConfigManager.GSON.fromJson(ConfigManager.GSON.toJson(config), MomentumConfig.class);
        edited.slide.enabled = true;
        assertFalse(config.slide.enabled);
    }
}
