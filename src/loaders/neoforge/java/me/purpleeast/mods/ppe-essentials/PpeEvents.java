package me.purpleeast.mods.ppe_essentials;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class PpeEvents {
    private PpeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PpeRuntimeEvents.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !PpeRuntimeEvents.allowsPlayerDamage(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PpeRuntimeEvents.onPlayerRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PpeRuntimeEvents.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PpePlayerData.get(PpeCompat.server(player)).setTeleportBack(
                    player.getUUID(),
                    new PpeLocation(PpeCompat.level(player).dimension(), event.getPrevX(), event.getPrevY(), event.getPrevZ(), player.getYRot(), player.getXRot())
            );
        }
    }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (PpeMobGriefing.shouldPreventBlockGriefing(event.getEntity())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (PpeMobGriefing.shouldPreventCreeperExplosionBlocks(event.getExplosion())) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (PpeConfig.commandEnabled("repeat")
                && event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player
                && PpeCompat.hasPermission(player, PpeConfig.commandPermission("repeat"))) {
            PpeCommands.rememberCommand(player, event.getParseResults().getReader().getString());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        PpeRuntimeEvents.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PpeRuntimeEvents.onServerStopped();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        PpeRuntimeEvents.onServerTick(event.getServer());
    }
}
