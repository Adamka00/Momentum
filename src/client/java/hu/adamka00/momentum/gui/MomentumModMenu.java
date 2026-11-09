package hu.adamka00.momentum.gui;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;

/** Only instantiated by Mod Menu's optional entrypoint; no common class refers to this adapter. */
public final class MomentumModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MomentumSettingsScreen::new;
    }
}
