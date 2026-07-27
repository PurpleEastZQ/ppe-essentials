package me.purpleeast.mods.ppe_essentials.mixin;

import me.purpleeast.mods.ppe_essentials.PpeMobGriefing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CropBlock.class)
public class CropBlockMixin {
    @Redirect(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerCropBreaking", min = 1)
    private boolean ppeEssentials$preventRavagerCropBreaking(Level level, BlockPos pos, boolean drop, Entity entity) {
        if (ppeEssentials$shouldKeepCrop(entity)) {
            return false;
        }
        return level.destroyBlock(pos, drop, entity);
    }

    @Redirect(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"
            ),
            require = 0
    )
    @Group(name = "ppeEssentials$preventRavagerCropBreaking", min = 1)
    private boolean ppeEssentials$preventRavagerCropBreakingModern(ServerLevel level, BlockPos pos, boolean drop, Entity entity) {
        if (ppeEssentials$shouldKeepCrop(entity)) {
            return false;
        }
        return level.destroyBlock(pos, drop, entity);
    }

    private boolean ppeEssentials$shouldKeepCrop(Entity entity) {
        return entity instanceof Ravager && PpeMobGriefing.shouldPreventBlockGriefing(entity);
    }
}
