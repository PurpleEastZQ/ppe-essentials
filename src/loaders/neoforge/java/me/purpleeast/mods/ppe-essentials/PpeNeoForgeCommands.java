package me.purpleeast.mods.ppe_essentials;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PpeNeoForgeCommands {
    private PpeNeoForgeCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        PpeCommands.registerCommands(event.getDispatcher());
    }
}
