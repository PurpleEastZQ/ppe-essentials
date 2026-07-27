package me.purpleeast.mods.ppe_essentials;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(PpeEssentials.MODID)
public class PpeEssentials {
    public static final String MODID = "ppe_essentials";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PpeEssentials(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(PpeNeoForgeCommands.class);
        NeoForge.EVENT_BUS.register(PpeEvents.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, PpeConfigBackend.SPEC);
    }
}
