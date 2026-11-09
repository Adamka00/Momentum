package hu.adamka00.momentum.movement;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One replaceable transient modifier. Never edits base speed or another mod's modifiers. */
public final class SprintHandler {
    private static final Identifier MODIFIER = Identifier.fromNamespaceAndPath("momentum", "sprint_boost");
    private SprintHandler() { }
    public static void apply(ServerPlayer player, double multiplier) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        var previous = speed.getModifier(MODIFIER);
        if (multiplier <= 1.000001) {
            if (previous != null) speed.removeModifier(MODIFIER);
        } else if (previous == null || Math.abs(previous.amount() - (multiplier - 1)) > 0.000001) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, multiplier - 1,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    public static void clear(ServerPlayer player) { apply(player, 1); }
}
