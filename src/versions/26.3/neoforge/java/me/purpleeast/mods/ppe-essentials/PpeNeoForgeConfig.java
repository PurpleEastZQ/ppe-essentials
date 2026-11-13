package me.purpleeast.mods.ppe_essentials;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

final class PpeNeoForgeConfig {
    private PpeNeoForgeConfig() {
    }

    static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.LOCAL, PpeConfigBackend.SPEC, "ppe_essentials-common.toml");
    }
}
