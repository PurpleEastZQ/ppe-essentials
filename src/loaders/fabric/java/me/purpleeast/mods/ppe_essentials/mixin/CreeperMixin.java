package me.purpleeast.mods.ppe_essentials.mixin;

import me.purpleeast.mods.ppe_essentials.PpeMobGriefing;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public class CreeperMixin {
    @ModifyArg(
            method = "explodeCreeper",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
            ),
            index = 5,
            require = 0
    )
    @Group(name = "ppeEssentials$preventCreeperBlockDamage", min = 1)
    private Level.ExplosionInteraction ppeEssentials$preventBlockDamage(Level.ExplosionInteraction interaction) {
        return ppeEssentials$blockDamageInteraction(interaction);
    }

    @ModifyArg(
            method = "explodeCreeper",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            ),
            index = 5,
            require = 0
    )
    @Group(name = "ppeEssentials$preventCreeperBlockDamage", min = 1)
    private Level.ExplosionInteraction ppeEssentials$preventBlockDamageModern(Level.ExplosionInteraction interaction) {
        return ppeEssentials$blockDamageInteraction(interaction);
    }

    private Level.ExplosionInteraction ppeEssentials$blockDamageInteraction(Level.ExplosionInteraction interaction) {
        if (PpeMobGriefing.shouldPreventBlockGriefing((Creeper) (Object) this)) {
            return Level.ExplosionInteraction.NONE;
        }
        return interaction;
    }
}
