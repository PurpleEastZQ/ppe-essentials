package me.purpleeast.mods.ppe_essentials.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.purpleeast.mods.ppe_essentials.PpeMobGriefing;
import net.minecraft.world.entity.monster.Ravager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

@Mixin(Ravager.class)
public class RavagerMixin {
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private boolean ppeEssentials$preventLeafBreaking(boolean original) {
        return original && !PpeMobGriefing.shouldPreventBlockGriefing((Ravager) (Object) this);
    }

    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;getBoolean(Lnet/minecraft/world/level/gamerules/GameRules$Key;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private boolean ppeEssentials$preventLeafBreakingModern(boolean original) {
        return original && !PpeMobGriefing.shouldPreventBlockGriefing((Ravager) (Object) this);
    }

    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private Object ppeEssentials$preventLeafBreakingModernValue(Object original) {
        return PpeMobGriefing.shouldPreventBlockGriefing((Ravager) (Object) this) ? Boolean.FALSE : original;
    }
}
