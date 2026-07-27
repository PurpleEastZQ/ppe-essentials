package me.purpleeast.mods.ppe_essentials;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Explosion;

public final class PpeMobGriefing {
    private PpeMobGriefing() {
    }

    public static boolean shouldPreventBlockGriefing(Entity entity) {
        return entity instanceof Creeper && PpeConfig.preventCreeperBlockDamage()
                || entity instanceof EnderMan && PpeConfig.preventEndermanBlockDamage()
                || entity instanceof Ravager && PpeConfig.preventRavagerBlockDamage();
    }

    public static boolean shouldPreventCreeperExplosionBlocks(Explosion explosion) {
        return PpeConfig.preventCreeperBlockDamage() && explosion.getDirectSourceEntity() instanceof Creeper;
    }
}
