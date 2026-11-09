package hu.adamka00.momentum;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/** Two real client JVMs, coordinated only by test files; all abilities use the actual network. */
final class TwoClientGameTest {
    private static final Identifier SPEED = Identifier.fromNamespaceAndPath("momentum", "sprint_boost");
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void mark(Path directory, String player, String phase) {
        try { Files.createDirectories(directory); Files.writeString(directory.resolve(player + "." + phase), "ok\n"); }
        catch (IOException error) { throw new AssertionError(error); }
    }
    private static void await(ClientGameTestContext context, Path directory, String player, String phase) {
        long deadline = System.nanoTime() + 120_000_000_000L;
        while (!Files.exists(directory.resolve(player + "." + phase))) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for " + player + "." + phase);
            context.waitTicks(2);
            try { Thread.sleep(25); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
        }
    }
    static void run(ClientGameTestContext context, String address) {
        String name = context.computeOnClient(c -> c.getUser().getName());
        String other = name.equals("MomentumA") ? "MomentumB" : "MomentumA";
        Path directory = Path.of(System.getProperty("momentum.test.barriers"));
        context.runOnClient(c -> c.options.pauseOnLostFocus = false);
        context.runOnClient(client -> ConnectScreen.startConnecting(new TitleScreen(), client,
            ServerAddress.parseString(address), new ServerData("Momentum test", address, ServerData.Type.OTHER), false, null));
        context.waitFor(c -> c.player != null && c.level != null && c.gui.screen() == null, 2400);
        context.waitFor(c -> c.player.onGround()); context.waitTicks(6);
        mark(directory, name, "ready"); await(context, directory, other, "ready");
        context.waitFor(c -> c.level.players().size() >= 2, 1200);

        context.getInput().holdKeyFor(o -> o.keyJump, 1); context.waitTicks(3);
        context.getInput().holdKeyFor(o -> o.keyJump, 1);
        context.waitFor(c -> MomentumClient.movementState() != null && MomentumClient.movementState().usedJumps() == 2, 120);
        context.waitTicks(3);
        context.getInput().holdKeyFor(o -> o.keyJump, 1);
        context.waitFor(c -> MomentumClient.movementState().usedJumps() == 3, 120);
        context.waitFor(c -> c.player.onGround()); context.waitTicks(4);
        mark(directory, name, "dash-ready"); await(context, directory, other, "dash-ready");
        context.getInput().lookAt(name.equals("MomentumA") ? 0 : 90, 0);
        context.getInput().holdKeyFor(InputConstants.KEY_R, 1);
        context.waitFor(c -> MomentumClient.movementState().dashCooldown() > 0, 120);
        mark(directory, name, "dashed"); await(context, directory, other, "dashed");

        context.getInput().holdKey(o -> o.keyUp); context.getInput().holdKey(o -> o.keySprint);
        context.waitTicks(100);
        context.runOnClient(c -> MomentumMod.LOGGER.info("Two-client sprint probe {}: input={}, sprinting={}, onGround={}, speed={}, position={}",
            name, c.player.input.keyPresses, c.player.isSprinting(), c.player.onGround(),
            c.player.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers(), c.player.position()));
        context.waitFor(c -> {
            var modifier = c.player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED);
            return modifier != null && Math.abs(modifier.amount() - 0.35) < 0.000001;
        }, 300);
        mark(directory, name, "sprint-max"); await(context, directory, other, "sprint-max");
        if (name.equals("MomentumA")) {
            context.getInput().releaseKey(o -> o.keyUp); context.getInput().releaseKey(o -> o.keySprint);
            context.waitTicks(30);
            check(context.computeOnClient(c -> !c.player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(SPEED)), "A sprint modifier cleared");
            mark(directory, name, "stopped");
        } else {
            await(context, directory, other, "stopped");
            check(context.computeOnClient(c -> {
                var modifier = c.player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED);
                return modifier != null && Math.abs(modifier.amount() - 0.35) < 0.000001;
            }), "Stopping A must not change B's sprint modifier");
            context.getInput().releaseKey(o -> o.keyUp); context.getInput().releaseKey(o -> o.keySprint);
        }
        context.takeScreenshot("momentum-two-player-" + name);
        mark(directory, name, "done"); await(context, directory, other, "done");
        MomentumMod.LOGGER.info("Two-client integration checks passed for {}", name);
        context.runOnClient(c -> c.disconnectWithSavingScreen());
        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
        context.restoreDefaultGameOptions();
    }
}
