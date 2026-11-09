package hu.adamka00.momentum.movement;
import hu.adamka00.momentum.config.MomentumConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
public final class DashHandler {
    private DashHandler() { }
    public static MovementMath.Horizontal apply(ServerPlayer player, Vec3 observedMovement,
                                                int forward, int strafe, MomentumConfig config) {
        var horizontal = MovementMath.dash(player.getYRot(), forward, strafe, config.dash.horizontalImpulse);
        // Set a bounded horizontal burst, never multiply accumulated horizontal speed.
        player.setDeltaMovement(horizontal.x(), observedMovement.y + config.dash.verticalImpulse, horizontal.z());
        // No teleport, no manual position update: movement still passes through vanilla collisions.
        return horizontal;
    }
}
