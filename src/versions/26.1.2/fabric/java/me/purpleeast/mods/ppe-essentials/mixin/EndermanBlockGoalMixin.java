package me.purpleeast.mods.ppe_essentials.mixin;

import me.purpleeast.mods.ppe_essentials.PpeConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal",
        "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal",
        "net.minecraft.world.entity.monster.Enderman$EndermanLeaveBlockGoal",
        "net.minecraft.world.entity.monster.Enderman$EndermanTakeBlockGoal"
}, remap = false)
public class EndermanBlockGoalMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void ppeEssentials$preventBlockGriefing(CallbackInfoReturnable<Boolean> cir) {
        if (PpeConfig.preventEndermanBlockDamage()) {
            cir.setReturnValue(false);
        }
    }
}
