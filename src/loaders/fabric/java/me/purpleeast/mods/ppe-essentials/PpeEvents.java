package me.purpleeast.mods.ppe_essentials;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class PpeEvents {
    private PpeEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                PpeRuntimeEvents.onPlayerDeath(player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, amount) ->
                !(entity instanceof ServerPlayer player) || PpeRuntimeEvents.allowsPlayerDamage(player)
        );
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PpeRuntimeEvents.onPlayerRespawn(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PpeRuntimeEvents.onPlayerLogin(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(PpeRuntimeEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STARTED.register(PpeRuntimeEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PpeRuntimeEvents.onServerStopped());
    }

}
