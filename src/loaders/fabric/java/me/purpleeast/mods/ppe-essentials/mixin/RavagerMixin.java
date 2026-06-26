package me.purpleeast.mods.ppe_essentials.mixin;

import me.purpleeast.mods.ppe_essentials.PpeMobGriefing;
import net.minecraft.world.entity.monster.Ravager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ravager.class)
public class RavagerMixin {
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private boolean ppeEssentials$preventLeafBreaking(@Coerce Object gameRules, @Coerce Object key) {
        return ppeEssentials$mobGriefingBoolean(gameRules, key);
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;getBoolean(Lnet/minecraft/world/level/gamerules/GameRules$Key;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private boolean ppeEssentials$preventLeafBreakingModern(@Coerce Object gameRules, @Coerce Object key) {
        return ppeEssentials$mobGriefingBoolean(gameRules, key);
    }

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerLeafBreaking", min = 1)
    private Object ppeEssentials$preventLeafBreakingModernValue(@Coerce Object gameRules, @Coerce Object key) {
        if (PpeMobGriefing.shouldPreventBlockGriefing((Ravager) (Object) this)) {
            return Boolean.FALSE;
        }
        return ppeEssentials$gameRuleValue(gameRules, "get", key);
    }

    private boolean ppeEssentials$mobGriefingBoolean(Object gameRules, Object key) {
        if (PpeMobGriefing.shouldPreventBlockGriefing((Ravager) (Object) this)) {
            return false;
        }
        return (Boolean) ppeEssentials$gameRuleValue(gameRules, "getBoolean", key);
    }

    private Object ppeEssentials$gameRuleValue(Object gameRules, String methodName, Object key) {
        try {
            return gameRules.getClass().getMethod(methodName, key.getClass()).invoke(gameRules, key);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to call GameRules#" + methodName, exception);
        }
    }
}
