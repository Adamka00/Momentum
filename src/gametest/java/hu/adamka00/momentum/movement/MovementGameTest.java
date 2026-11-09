package hu.adamka00.momentum.movement;

import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.network.payload.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class MovementGameTest {
    private static ServerPlayer player(GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }
    private static void near(GameTestHelper h, double expected, double actual, String message) {
        h.assertTrue(Math.abs(expected - actual) < 0.000001, message + ": " + actual);
    }

    @GameTest public void jumpsPreserveMomentumAndFallDistance(GameTestHelper h) {
        var a = player(h); var b = player(h);
        a.fallDistance = 12; b.setDeltaMovement(0.1, -0.2, 0.3);
        var observed = new Vec3(0.23, -0.5, -0.18);
        JumpHandler.apply(a, observed, 2, new MomentumConfig());
        near(h, 0.42, a.getDeltaMovement().y, "Second jump strength");
        near(h, observed.x, a.getDeltaMovement().x, "Preserved x");
        near(h, observed.z, a.getDeltaMovement().z, "Preserved z");
        near(h, 12, a.fallDistance, "No fall distance reset");
        JumpHandler.apply(a, observed, 3, new MomentumConfig());
        near(h, 0.46, a.getDeltaMovement().y, "Third jump strength");
        near(h, -0.2, b.getDeltaMovement().y, "Second player unaffected");
        h.succeed();
    }

    @GameTest public void sprintModifiersAreTransientAndIndependent(GameTestHelper h) {
        var a = player(h); var b = player(h);
        var speed = a.getAttribute(Attributes.MOVEMENT_SPEED);
        double base = speed.getValue();
        var otherId = Identifier.fromNamespaceAndPath("momentum-test", "other_speed");
        speed.addTransientModifier(new AttributeModifier(otherId, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        for (int i = 0; i < 100; i++) SprintHandler.apply(a, 1.35);
        near(h, base * 1.2 * 1.35, speed.getValue(), "No stacking; other modifier respected");
        near(h, base, b.getAttributeValue(Attributes.MOVEMENT_SPEED), "Independent attributes");
        h.assertTrue(speed.getPermanentModifiers().isEmpty(), "No saved sprint modifier");
        SprintHandler.clear(a);
        near(h, base * 1.2, speed.getValue(), "Restores original modifiers");
        h.assertTrue(speed.hasModifier(otherId), "Preserves other mod's modifier");
        h.succeed();
    }

    @GameTest public void dashUsesVanillaCollision(GameTestHelper h) {
        for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) h.setBlock(x, 0, z, Blocks.STONE);
        h.setBlock(3, 1, 4, Blocks.STONE); h.setBlock(3, 2, 4, Blocks.STONE);
        var p = player(h);
        p.setPos(h.absoluteVec(new Vec3(2.5, 1, 4.5)));
        p.setYRot(-90); p.fallDistance = 7;
        DashHandler.apply(p, new Vec3(0, -0.1, 0), 1, 0, new MomentumConfig());
        near(h, 1.2, p.getDeltaMovement().x, "Dash strength");
        near(h, -0.1, p.getDeltaMovement().y, "Preserved vertical motion");
        near(h, 7, p.fallDistance, "Dash does not grant fall immunity");
        p.move(MoverType.SELF, new Vec3(p.getDeltaMovement().x, 0, p.getDeltaMovement().z));
        h.assertTrue(p.getBoundingBox().maxX <= h.absolutePos(new BlockPos(3, 1, 4)).getX() + 0.000001, "Wall blocks dash");
        h.succeed();
    }

    @GameTest public void groundValidationRequiresFeetSupport(GameTestHelper h) {
        var p = player(h);
        h.setBlock(2, 0, 2, Blocks.SMOOTH_STONE_SLAB);
        p.setPos(h.absoluteVec(new Vec3(2.5, 0.5, 2.5))); p.setOnGround(true);
        h.assertTrue(MovementManager.validGround(p, p.getY()), "Partial blocks support landing");
        p.setPos(h.absoluteVec(new Vec3(2.5, 2, 2.5))); p.setOnGround(true);
        h.setBlock(3, 2, 2, Blocks.STONE); h.setBlock(2, 4, 2, Blocks.STONE);
        h.assertFalse(MovementManager.validGround(p, p.getY()), "Walls, ceilings and spoofed onGround do not support feet");
        h.succeed();
    }

    @GameTest public void typedPayloadsRoundTrip(GameTestHelper h) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var jump = new JumpRequest(9, 10, true, true);
            JumpRequest.CODEC.encode(buffer, jump); h.assertTrue(jump.equals(JumpRequest.CODEC.decode(buffer)), "Jump codec");
            var dash = new DashRequest(9, 11, -1, 1);
            DashRequest.CODEC.encode(buffer, dash); h.assertTrue(dash.equals(DashRequest.CODEC.decode(buffer)), "Dash codec");
            var state = new StatePayload(9, 100, 2, 3, 40, 1.35, false);
            StatePayload.CODEC.encode(buffer, state); h.assertTrue(state.equals(StatePayload.CODEC.decode(buffer)), "State codec");
            var config = new ConfigPayload(9, "{\"general\":{}}");
            ConfigPayload.CODEC.encode(buffer, config); h.assertTrue(config.equals(ConfigPayload.CODEC.decode(buffer)), "Config codec");
            var action = new ActionPayload(9, 4, 1.2, 0, 0);
            ActionPayload.CODEC.encode(buffer, action); h.assertTrue(action.equals(ActionPayload.CODEC.decode(buffer)), "Action codec");
            var effect = new EffectPayload(1, 3, true, false);
            EffectPayload.CODEC.encode(buffer, effect); h.assertTrue(effect.equals(EffectPayload.CODEC.decode(buffer)), "Effect codec");
        } finally { buffer.release(); }
        h.succeed();
    }
}
