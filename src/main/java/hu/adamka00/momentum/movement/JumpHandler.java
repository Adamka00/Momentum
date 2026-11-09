package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.MomentumConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class JumpHandler {
    private JumpHandler() { }
    public static double apply(ServerPlayer player, Vec3 observedMovement, int jumpNumber, MomentumConfig config) {
        double strength = jumpNumber == 2 ? config.jump.secondStrength : config.jump.thirdStrength;
        // Match vanilla Jump Boost while preserving x/z and accumulated fall damage.
        double vertical = strength + player.getJumpBoostPower();
        player.setDeltaMovement(observedMovement.x, vertical, observedMovement.z);
        player.setOnGround(false);
        return vertical;
    }
}
