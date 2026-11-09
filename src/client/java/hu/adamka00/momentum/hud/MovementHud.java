package hu.adamka00.momentum.hud;

import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.network.payload.StatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class MovementHud {
    private MovementHud() { }
    public static void draw(GuiGraphicsExtractor graphics, StatePayload state,
                            MomentumConfig config, MomentumConfig.Client preferences) {
        var client = Minecraft.getInstance();
        if (state == null || !preferences.showHud || client.player == null || !client.player.isAlive()
            || client.player.isSpectator() || client.gui.hud.isHidden() || client.gui.screen() != null) return;
        int y = graphics.guiHeight() - 40;
        int color = state.restricted() ? 0xFFAAAAAA : 0xFFE0E0E0;
        int remaining = Math.max(0, state.maximumJumps() - Math.max(1, state.usedJumps()));
        graphics.text(client.font, Component.translatable("hud.momentum.jumps", remaining), 6, y, color, true);
        Component cooldown = !config.general.dash ? Component.translatable("hud.momentum.disabled")
            : state.dashCooldown() == 0 ? Component.translatable("hud.momentum.ready")
            : Component.literal(String.format(Locale.ROOT, "%.1fs", state.dashCooldown() / 20.0));
        graphics.text(client.font, Component.translatable("hud.momentum.dash", cooldown), 6, y + 10, color, true);
        graphics.text(client.font, Component.translatable("hud.momentum.sprint",
            String.format(Locale.ROOT, "%.2f", state.sprintMultiplier())), 6, y + 20, color, true);
    }
}
