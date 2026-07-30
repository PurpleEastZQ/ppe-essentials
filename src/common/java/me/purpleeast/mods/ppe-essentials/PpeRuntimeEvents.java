package me.purpleeast.mods.ppe_essentials;

import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PpeRuntimeEvents {
    private static final Map<UUID, Integer> BACK_NOTICE_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> FIRST_JOIN_NOTICE_TICKS = new HashMap<>();

    private PpeRuntimeEvents() {
    }

    public static void onPlayerDeath(ServerPlayer player) {
        PpePlayerData.get(PpeCompat.server(player)).setDeathBack(player.getUUID(), PpeLocation.of(player));
    }

    public static boolean allowsPlayerDamage(ServerPlayer player) {
        return !PpePlayerData.get(PpeCompat.server(player)).isGodEnabled(player.getUUID());
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        PpeCommands.restoreFly(player);
        if (PpePlayerData.get(PpeCompat.server(player)).markBackNoticeShown(player.getUUID())) {
            BACK_NOTICE_TICKS.put(player.getUUID(), PpeCompat.server(player).getTickCount() + 10);
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        MinecraftServer server = PpeCompat.server(player);
        PpeCommands.restoreFly(player);
        if (PpeConfig.firstJoinNotice() && !PpePlayerData.get(server).hasFirstJoinNoticeShown(player.getUUID())) {
            FIRST_JOIN_NOTICE_TICKS.put(player.getUUID(), server.getTickCount() + 40);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        PpePlayerData.get(server);
    }

    public static void onServerStopped() {
        PpeCommands.clearRuntimeState();
        clearNoticeQueues();
    }

    public static void clearNoticeQueues() {
        BACK_NOTICE_TICKS.clear();
        FIRST_JOIN_NOTICE_TICKS.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        PpeCommands.onServerTick(server);

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
