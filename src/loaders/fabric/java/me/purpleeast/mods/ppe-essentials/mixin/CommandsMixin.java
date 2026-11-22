package me.purpleeast.mods.ppe_essentials.mixin;

import com.mojang.brigadier.ParseResults;
import me.purpleeast.mods.ppe_essentials.PpeCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(method = "performCommand", at = @At("HEAD"))
    private void ppeEssentials$rememberCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo callback) {
        PpeCommands.rememberCommand(parseResults);
    }
}
