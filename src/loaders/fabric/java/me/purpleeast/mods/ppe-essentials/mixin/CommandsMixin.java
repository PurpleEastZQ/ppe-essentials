package me.purpleeast.mods.ppe_essentials.mixin;

import com.mojang.brigadier.ParseResults;
import me.purpleeast.mods.ppe_essentials.PpeCommands;
import me.purpleeast.mods.ppe_essentials.PpeCompat;
import me.purpleeast.mods.ppe_essentials.PpeConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(method = "performCommand", at = @At("HEAD"))
    private void ppeEssentials$rememberCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo callback) {
        CommandSourceStack source = parseResults.getContext().getSource();
        if (PpeConfig.commandEnabled("repeat")
                && source.getEntity() instanceof ServerPlayer player
                && PpeCompat.hasPermission(player, PpeConfig.commandPermission("repeat"))) {
            PpeCommands.rememberCommand(player, command);
        }
    }
}
