package me.purpleeast.mods.ppe_essentials;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PpeEssentials implements ModInitializer {
    public static final String MODID = "ppe_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        PpeConfig.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PpeCommands.registerCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(PpeCommands::onServerTick);
        PpeEvents.register();
    }
}
