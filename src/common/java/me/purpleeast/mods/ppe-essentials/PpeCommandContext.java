package me.purpleeast.mods.ppe_essentials;

import me.purpleeast.mods.ppe_essentials.mixin.CommandSourceStackAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class PpeCommandContext {
    private PpeCommandContext() {
    }

    // The command source survives execute as, which replaces only the execution entity.
    static ServerPlayer senderPlayer(CommandSourceStack source) {
        return PpeCompat.senderPlayer(source, ((CommandSourceStackAccessor) source).ppe$getSender());
    }

    static boolean isSilent(CommandSourceStack source) {
        return ((CommandSourceStackAccessor) source).ppe$isSilent();
    }

    static String language(CommandSourceStack source) {
        return PpeLang.languageFor(senderPlayer(source));
    }

    static void reply(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> message(source, key, args), false);
    }

    static int fail(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(message(source, key, args));
        return 0;
    }

    private static Component message(CommandSourceStack source, String key, Object... args) {
        return PpeLang.prefixed(PpeLang.componentForLanguage(language(source), key, args));
    }
}
