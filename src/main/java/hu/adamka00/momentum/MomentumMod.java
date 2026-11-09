package hu.adamka00.momentum;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import hu.adamka00.momentum.config.ConfigManager;
import hu.adamka00.momentum.movement.MovementManager;
import hu.adamka00.momentum.network.MomentumNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class MomentumMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("momentum");
    private static MovementManager manager;
    /** No configuration-editing packet exists. Only the actual local server may be edited. */
    public static void updateLocal(net.minecraft.server.MinecraftServer server, hu.adamka00.momentum.config.MomentumConfig config) throws java.io.IOException {
        if (server.isDedicatedServer()) throw new SecurityException("Dedicated configuration is read-only to clients");
        config.validate();
        ConfigManager.save(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), config);
        manager.applyConfig(server, config);
    }
    @Override public void onInitialize() {
        var config = ConfigManager.load(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), LOGGER);
        manager = new MovementManager(config);
        MomentumNetworking.register(manager);
        manager.registerEvents();
        LOGGER.info("Momentum 1.1.0-dev initialized for Minecraft 26.3");
    }
}
