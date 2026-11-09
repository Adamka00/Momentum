package hu.adamka00.momentum;
import hu.adamka00.momentum.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ConfigTest {
    @TempDir Path temp;
    @Test void defaultsAndUnifiedModes() {
        var c = new MomentumConfig(); assertTrue(c.validate().isEmpty()); assertEquals(3, c.maximumJumps());
        c.general.tripleJump = false; assertEquals(2, c.maximumJumps());
        c.general.doubleJump = false; assertEquals(1, c.maximumJumps());
        c.general.tripleJump = true; assertEquals(3, c.maximumJumps());
    }
    @Test void clampsRangesAndNonfiniteNumbers() {
        var c = new MomentumConfig(); c.jump.maximumTotalJumps = 99; c.dash.cooldownTicks = -1;
        c.sprint.maximumMultiplier = Double.NaN; c.jump.secondStrength = Double.POSITIVE_INFINITY;
        c.jump.landingConfirmationTicks = 0; c.sprint.accelerationTicks = 0;
        assertFalse(c.validate().isEmpty()); assertEquals(3, c.jump.maximumTotalJumps);
        assertEquals(1, c.dash.cooldownTicks); assertEquals(1.35, c.sprint.maximumMultiplier);
        assertEquals(0.42, c.jump.secondStrength); assertEquals(2, c.jump.landingConfirmationTicks);
    }
    @Test void repairsNullSections() {
        var c = new MomentumConfig(); c.general = null; c.jump = null; c.sprint = null; c.dash = null; c.client = null;
        assertEquals(5, c.validate().size()); assertEquals(3, c.maximumJumps());
    }
    @Test void distributedDefaultsMatchGeneratedDefaults() throws Exception {
        var expected = ConfigManager.GSON.toJsonTree(new MomentumConfig());
        var distributed = com.google.gson.JsonParser.parseString(Files.readString(Path.of("config/momentum.json")));
        assertEquals(expected, distributed);
    }
    @Test void generatesAndRecoversMalformedFile() throws Exception {
        var path = temp.resolve("momentum.json"); var logger = LoggerFactory.getLogger("config-test");
        ConfigManager.load(path, logger); assertTrue(Files.exists(path));
        Files.writeString(path, "{ invalid"); var recovered = ConfigManager.load(path, logger);
        assertEquals(3, recovered.maximumJumps()); assertEquals("{ invalid", Files.readString(temp.resolve("momentum.json.invalid")));
        assertNotNull(ConfigManager.GSON.fromJson(Files.readString(path), MomentumConfig.class));
    }
}
