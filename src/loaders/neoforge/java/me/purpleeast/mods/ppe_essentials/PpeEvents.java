package me.purpleeast.mods.ppe_essentials;

import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PpeEvents {
    private static final Map<UUID, Integer> BACK_NOTICE_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FIRST_JOIN_NOTICE_TICKS = new HashMap<>();

    private PpeEvents() {
    }

    public static void clearNoticeQueues() {
        BACK_NOTICE_TICKS.clear();
        FIRST_JOIN_NOTICE_TICKS.clear();
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PpePlayerData.get(PpeCompat.server(player)).setDeathBack(player.getUUID(), PpeLocation.of(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PpePlayerData.get(PpeCompat.server(player)).isGodEnabled(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PpePlayerData.get(PpeCompat.server(player)).markBackNoticeShown(player.getUUID())) {
            BACK_NOTICE_TICKS.put(player.getUUID(), PpeCompat.server(player).getTickCount() + 10);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PpeConfig.firstJoinNotice()
                && !PpePlayerData.get(PpeCompat.server(player)).hasFirstJoinNoticeShown(player.getUUID())) {
            FIRST_JOIN_NOTICE_TICKS.put(player.getUUID(), PpeCompat.server(player).getTickCount() + 40);
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
    public static void onServerStopped(ServerStoppedEvent event) {
        PpeCommands.clearRuntimeState();
        clearNoticeQueues();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Integer>> backIterator = BACK_NOTICE_TICKS.entrySet().iterator();
        while (backIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = backIterator.next();
            if (entry.getValue() > server.getTickCount()) {
                continue;
            }

            backIterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                PpeCompat.playSound(player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendSystemMessage(PpeLang.prefixedComponent(player, "ppe_essentials.back.notice")
                        .withStyle(Style.EMPTY
                                .withClickEvent(PpeCompat.suggestCommandClick("/back"))
                                .withHoverEvent(PpeCompat.showTextHover(PpeLang.component(player, "ppe_essentials.back.notice.tooltip")))));
            }
        }

        Iterator<Map.Entry<UUID, Integer>> joinIterator = FIRST_JOIN_NOTICE_TICKS.entrySet().iterator();
        while (joinIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = joinIterator.next();
            if (entry.getValue() > server.getTickCount()) {
                continue;
            }

            joinIterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && PpePlayerData.get(server).markFirstJoinNoticeShown(player.getUUID())) {
                PpeCompat.playSound(player, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendSystemMessage(PpeLang.prefixedComponent(player, "ppe_essentials.first_join.notice")
                        .withStyle(Style.EMPTY
                                .withClickEvent(PpeCompat.runCommandClick("/ppe-ess help"))
                                .withHoverEvent(PpeCompat.showTextHover(PpeLang.component(player, "ppe_essentials.first_join.notice.tooltip")))));
            }
        }
    }
}
