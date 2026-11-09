package hu.adamka00.momentum;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Exercises real input, integrated-server networking, accepted impulses, and HUD extraction. */
public final class MomentumClientGameTest implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @Override public void runTest(ClientGameTestContext context) {
        String remote = System.getProperty("momentum.test.remote", "");
        if (!remote.isEmpty()) { TwoClientGameTest.run(context, remote); return; }
        try (var world = context.worldBuilder().create()) {
            var connection = world.getConnection();
            world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(GameType.SURVIVAL));
            connection.waitForClientboundPackets();
            context.waitFor(client -> client.player != null && client.player.onGround());
            context.waitTicks(4);
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            context.waitTicks(3);
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            connection.waitForServerboundPackets(); connection.waitForClientboundPackets();
            context.waitFor(c -> MomentumClient.movementState() != null && MomentumClient.movementState().usedJumps() == 2);
            check(context.computeOnClient(c -> c.player.getDeltaMovement().y) > 0.2, "Second press must give an air jump");
            context.waitTicks(3);
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            connection.waitForServerboundPackets(); connection.waitForClientboundPackets();
            context.waitFor(c -> MomentumClient.movementState().usedJumps() == 3);
            check(context.computeOnClient(c -> c.player.getDeltaMovement().y) > 0.2, "Third press must give an air jump");
            context.waitTicks(2);
            double beforeFourth = context.computeOnClient(c -> c.player.getDeltaMovement().y);
            context.getInput().holdKeyFor(options -> options.keyJump, 1);
            connection.waitForServerboundPackets(); connection.waitForClientboundPackets();
            check(context.computeOnClient(c -> c.player.getDeltaMovement().y) < beforeFourth, "Fourth jump must be rejected");
            context.waitFor(c -> c.player.onGround()); context.waitTicks(4);
            context.getInput().lookAt(0, 0);
            context.getInput().holdKeyFor(InputConstants.KEY_R, 1);
            connection.waitForServerboundPackets(); connection.waitForClientboundPackets();
            check(context.computeOnClient(c -> c.player.getDeltaMovement().horizontalDistance()) > 0.5, "Dash must accelerate horizontally");
            context.waitTicks(6);
            context.getInput().holdKey(options -> options.keyUp);
            context.getInput().holdKey(options -> options.keySprint);
            context.waitTicks(90);
            check(world.getServer().computeOnServer(server -> {
                var modifier = connection.getServerPlayer().getAttribute(Attributes.MOVEMENT_SPEED)
                    .getModifier(Identifier.fromNamespaceAndPath("momentum", "sprint_boost"));
                return modifier != null && Math.abs(modifier.amount() - 0.35) < 0.000001;
            }), "Continuous sprint must reach the configured maximum");
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseKey(options -> options.keySprint);
            context.waitTicks(30);
            check(world.getServer().computeOnServer(server -> !connection.getServerPlayer().getAttribute(Attributes.MOVEMENT_SPEED)
                .hasModifier(Identifier.fromNamespaceAndPath("momentum", "sprint_boost"))), "Stopped sprint must remove the modifier");
            connection.waitForChunksRender();
            context.takeScreenshot("momentum-hud");
        }
    }
}
