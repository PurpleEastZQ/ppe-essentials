package me.purpleeast.mods.ppe_essentials.mixin;

import me.purpleeast.mods.ppe_essentials.PpeCompat;
import me.purpleeast.mods.ppe_essentials.PpeLocation;
import me.purpleeast.mods.ppe_essentials.PpePlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"))
    private void ppeEssentials$recordTeleportBack(ServerLevel level, double x, double y, double z, float yRot, float xRot, CallbackInfo callback) {
        ppeEssentials$recordTeleportBack();
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("HEAD"))
    private void ppeEssentials$recordTeleportBack(ServerLevel level, double x, double y, double z, Set<?> relativeMovements, float yRot, float xRot, CallbackInfoReturnable<Boolean> callback) {
        ppeEssentials$recordTeleportBack();
    }

    private void ppeEssentials$recordTeleportBack() {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PpePlayerData.get(PpeCompat.server(player)).setTeleportBack(player.getUUID(), PpeLocation.of(player));
    }
}
