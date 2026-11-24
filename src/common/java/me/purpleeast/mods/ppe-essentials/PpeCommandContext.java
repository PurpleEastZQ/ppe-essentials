package me.purpleeast.mods.ppe_essentials;

import net.minecraft.commands.CommandSourceStack;

final class PpeCommandContext {
    private PpeCommandContext() {
    }

    static void reply(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> PpeLang.prefixedComponent(source.getPlayer(), key, args), false);
    }

    static int fail(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(PpeLang.prefixedComponent(source.getPlayer(), key, args));
        return 0;
    }
}
