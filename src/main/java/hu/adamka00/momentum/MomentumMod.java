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
    @Override public void onInitialize() {
        var config = ConfigManager.load(FabricLoader.getInstance().getConfigDir().resolve("momentum.json"), LOGGER);
        var manager = new MovementManager(config);
        MomentumNetworking.register(manager);
        manager.registerEvents();
        LOGGER.info("Momentum 1.0.0 initialized for Minecraft 26.3");
    }
}
