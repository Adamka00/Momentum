package hu.adamka00.momentum.gui;

import hu.adamka00.momentum.MomentumClient;
import hu.adamka00.momentum.config.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Native widgets only. Minecraft-dependent code must be verified against a full 26.3 build. */
public final class MomentumSettingsScreen extends Screen {
    private final Screen parent;
    private final MomentumClient owner = MomentumClient.instance();
    private final List<SettingSpec> specs = SettingSpec.all();
    private MomentumConfig draft;
    private String baseline;
    private final Map<String, String> inputs = new HashMap<>();
    private final Set<String> invalid = new HashSet<>();
    private int category, page;
    private boolean saving;
    private Component feedback = Component.empty();

    public MomentumSettingsScreen(Screen parent) {
        super(Component.translatable("settings.momentum.title"));
        this.parent = parent;
        draft = owner.settings(); baseline = ConfigManager.GSON.toJson(draft);
    }
    private boolean locked(SettingSpec spec) { return owner.gameplayReadOnly() && !spec.category().equals("client"); }
    private boolean dirty() { return !invalid.isEmpty() || !baseline.equals(ConfigManager.GSON.toJson(draft)); }
    private void rebuild() { clearWidgets(); init(); }
    private Button button(Component text, int x, int y, int width, Runnable action) {
        return addRenderableWidget(Button.builder(text, b -> action.run()).pos(x, y).size(width, 20).build());
    }
    private Component text(String key) { return Component.translatable("settings.momentum." + key); }

    @Override protected void init() {
        int margin = 8, contentWidth = Math.max(100, width - 16);
        int rows = Math.max(1, (height - 158) / 24);
        String selected = SettingSpec.CATEGORIES.get(category);
        var fields = specs.stream().filter(s -> s.category().equals(selected)).toList();
        int pages = Math.max(1, (fields.size() + rows - 1) / rows);
        page = Math.clamp(page, 0, pages - 1);
        button(Component.literal("<"), margin, 25, 24, () -> { category = Math.floorMod(category - 1, SettingSpec.CATEGORIES.size()); page = 0; rebuild(); });
        button(text("category." + selected), 36, 25, Math.max(40, width - 72), () -> { category = (category + 1) % SettingSpec.CATEGORIES.size(); page = 0; rebuild(); });
        button(Component.literal(">"), width - 32, 25, 24, () -> { category = (category + 1) % SettingSpec.CATEGORIES.size(); page = 0; rebuild(); });
        for (int row = 0; row < rows && page * rows + row < fields.size(); row++) {
            SettingSpec spec = fields.get(page * rows + row);
            int y = 59 + row * 24;
            Component label = Component.translatable(spec.key());
            Component help = Component.translatable(spec.key() + ".tooltip");
            if (locked(spec)) help = Component.translatable("settings.momentum.controlledTooltip", help);
            if (spec.toggle()) {
                var widget = button(Component.translatable("settings.momentum.toggle", label,
                    text((boolean) spec.get(draft) ? "on" : "off")), margin, y, contentWidth, () -> {
                        spec.set(draft, Boolean.toString(!(boolean) spec.get(draft))); rebuild();
                    });
                widget.active = !locked(spec) && !saving;
                widget.setTooltip(Tooltip.create(help));
            } else {
                var bounds = spec.bounds(draft);
                Component tip = Component.translatable("settings.momentum.range", help, bounds.minimum(), bounds.maximum());
                var labelWidget = button(label, margin, y, Math.max(30, contentWidth - 84), () -> {});
                labelWidget.setTooltip(Tooltip.create(tip));
                var input = new EditBox(font, width - 84, y, 76, 20, label);
                input.setValue(inputs.getOrDefault(spec.path(), spec.get(draft).toString()));
                input.setTooltip(Tooltip.create(tip));
                input.setEditable(!locked(spec) && !saving);
                input.setResponder(value -> {
                    inputs.put(spec.path(), value);
                    if (spec.set(draft, value)) invalid.remove(spec.path());
                    else invalid.add(spec.path());
                    feedback = invalid.isEmpty() ? Component.empty() : text("invalid");
                });
                addRenderableWidget(input);
            }
        }
        var previous = button(Component.literal("<"), margin, height - 80, 24, () -> { page--; rebuild(); });
        previous.active = page > 0;
        var next = button(Component.literal(">"), width - 32, height - 80, 24, () -> { page++; rebuild(); });
        next.active = page + 1 < pages;
        int half = (width - 24) / 2;
        button(text("resetCategory"), 8, height - 55, half, () -> confirmReset(false));
        button(text("resetAll"), 16 + half, height - 55, half, () -> confirmReset(true));
        int third = (width - 32) / 3;
        button(text("apply"), 8, height - 30, third, () -> save(false)).active = !saving;
        button(text("save"), 16 + third, height - 30, third, () -> save(true)).active = !saving;
        button(text("cancel"), 24 + third * 2, height - 30, third, this::onClose).active = !saving;
    }
    private void confirmReset(boolean all) {
        if (saving) return;
        minecraft.gui.setScreen(new ConfirmScreen(accepted -> {
            if (accepted) {
                var defaults = new MomentumConfig();
                for (SettingSpec spec : specs) if (!locked(spec) && (all || spec.category().equals(SettingSpec.CATEGORIES.get(category)))) {
                    spec.reset(draft, defaults); inputs.remove(spec.path()); invalid.remove(spec.path());
                }
            }
            minecraft.gui.setScreen(this);
        }, text("resetTitle"), text("resetPrompt")));
    }
    private void save(boolean close) {
        if (saving) return;
        if (!invalid.isEmpty()) { feedback = text("invalid"); return; }
        // A changed cross-field constraint is also invalid, even when both individual edits parsed.
        var snapshot = ConfigManager.GSON.fromJson(ConfigManager.GSON.toJson(draft), MomentumConfig.class);
        if (!snapshot.validate().isEmpty()) { feedback = text("invalid"); return; }
        saving = true; feedback = text("saving"); rebuild();
        owner.saveSettings(snapshot, success -> {
            saving = false; feedback = text(success ? "saved" : "saveFailed");
            if (success) { baseline = ConfigManager.GSON.toJson(draft); if (close) { minecraft.gui.setScreen(parent); return; } }
            rebuild();
        });
    }
    @Override public void onClose() {
        if (saving) return;
        if (!dirty()) { minecraft.gui.setScreen(parent); return; }
        minecraft.gui.setScreen(new ConfirmScreen(discard -> minecraft.gui.setScreen(discard ? parent : this), text("discardTitle"), text("discardPrompt")));
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical != 0 && !saving) { page = Math.max(0, page + (vertical < 0 ? 1 : -1)); rebuild(); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {
        super.extractRenderState(graphics, x, y, delta);
        graphics.centeredText(font, dirty() ? Component.translatable("settings.momentum.unsaved", title) : title, width / 2, 8, 0xFFFFFFFF);
        if (owner.gameplayReadOnly()) graphics.centeredText(font, text("controlled"), width / 2, 48, 0xFFFFCC66);
        graphics.centeredText(font, feedback, width / 2, height - 93, invalid.isEmpty() ? 0xFFFFFFFF : 0xFFFF6666);
        graphics.centeredText(font, Component.translatable("settings.momentum.page", page + 1), width / 2, height - 75, 0xFFAAAAAA);
    }
}
