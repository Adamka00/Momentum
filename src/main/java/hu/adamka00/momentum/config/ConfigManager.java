package hu.adamka00.momentum.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private ConfigManager() { }

    public static MomentumConfig load(Path file, Logger logger) {
        MomentumConfig config;
        try {
            if (!Files.exists(file)) {
                config = new MomentumConfig();
                save(file, config);
            } else {
                config = GSON.fromJson(Files.readString(file), MomentumConfig.class);
                if (config == null) throw new IllegalArgumentException("Configuration is null");
            }
        } catch (IOException | RuntimeException error) {
            logger.error("Cannot load {}; using Momentum defaults", file, error);
            config = new MomentumConfig();
            // Keep malformed input for diagnosis instead of silently destroying it.
            try {
                if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".invalid"), StandardCopyOption.REPLACE_EXISTING);
                save(file, config);
            } catch (IOException backupError) { logger.error("Cannot recover Momentum configuration", backupError); }
        }
        config.validate().forEach(logger::warn);
        return config;
    }

    public static void save(Path file, MomentumConfig config) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(config) + "\n");
        try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
}
