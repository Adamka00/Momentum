package hu.adamka00.momentum.hud;

import hu.adamka00.momentum.config.MomentumConfig;
import hu.adamka00.momentum.network.payload.StatePayload;
import hu.adamka00.momentum.network.payload.AbilityStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class MovementHud {
    private MovementHud() { }
    public static void draw(GuiGraphicsExtractor graphics, StatePayload state, AbilityStatePayload abilities,
                            MomentumConfig config, MomentumConfig.Client preferences) {
        var client = Minecraft.getInstance();
        if (state == null || !preferences.showHud || client.player == null || !client.player.isAlive()
            || client.player.isSpectator() || client.gui.hud.isHidden() || client.gui.screen() != null) return;
        java.util.List<Component> lines = new java.util.ArrayList<>(6);
        int remaining = Math.max(0, state.maximumJumps() - Math.max(1, state.usedJumps()));
        if (preferences.showAirJumps) lines.add(Component.translatable("hud.momentum.jumps", remaining));
        if (preferences.showDash) lines.add(Component.translatable("hud.momentum.dash", !config.general.dash ? Component.translatable("hud.momentum.disabled") : cooldown(state.dashCooldown())));
        // New counters are never guessed while awaiting the authoritative snapshot.
        if (abilities != null && abilities.epoch() == state.epoch()) {
            if (preferences.showWallJumps) lines.add(Component.translatable("hud.momentum.wallJump", abilities.wallJumps()));
            if (preferences.showLongJump) lines.add(Component.translatable("hud.momentum.longJump", !config.longJump.enabled ? Component.translatable("hud.momentum.disabled") : cooldown(abilities.longCooldown())));
            if (preferences.showSlide) lines.add(Component.translatable("hud.momentum.slide", !config.slide.enabled ? Component.translatable("hud.momentum.disabled") : abilities.slideTicks() > 0
                ? Component.translatable("hud.momentum.sliding", abilities.slideTicks()) : cooldown(abilities.slideCooldown())));
        }
        if (preferences.showSprint) lines.add(Component.translatable("hud.momentum.sprint", String.format(Locale.ROOT, "%.2f", state.sprintMultiplier())));
        float scale = (float) preferences.hudScale;
        int alpha = (int) Math.round(preferences.hudOpacity * 255);
        int color = (alpha << 24) | (state.restricted() ? 0xAAAAAA : 0xE0E0E0);
        int widest = 0;
        for (Component line : lines) widest = Math.max(widest, client.font.width(line));
        int x = 6 + (int) (Math.max(0, graphics.guiWidth() - widest * scale - 12) * preferences.hudX);
        int y = 6 + (int) (Math.max(0, graphics.guiHeight() - lines.size() * 10 * scale - 12) * preferences.hudY);
        // 26.3 matrix-stack calls still require full client compilation/runtime verification.
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (int i = 0; i < lines.size(); i++) graphics.text(client.font, lines.get(i), 0, i * 10, color, true);
        graphics.pose().popMatrix();
    }
    private static Component cooldown(int ticks) {
        return ticks == 0 ? Component.translatable("hud.momentum.ready")
            : Component.translatable("hud.momentum.seconds", String.format(Locale.ROOT, "%.1f", ticks / 20.0));
    }
}
