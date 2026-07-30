package me.purpleeast.mods.ppe_essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PpeCommands {
    private static final int INTERNAL_COMMAND_PERMISSION_LEVEL = 4;
    private static final int TELEPORT_SOUND_DELAY_TICKS = 1;
    private static final Map<UUID, Integer> RTP_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> LAST_COMMANDS = new HashMap<>();
    private static final List<PendingSound> PENDING_SOUNDS = new ArrayList<>();

    private PpeCommands() {
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (enabled("tpa")) {
            dispatcher.register(Commands.literal("tpa")
                    .requires(source -> canUse(source, "tpa"))
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPA))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpa(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaa")) {
            dispatcher.register(Commands.literal("tpaa")
                    .requires(source -> canUse(source, "tpaa"))
                    .executes(context -> acceptTpa(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpad")) {
            dispatcher.register(Commands.literal("tpad")
                    .requires(source -> canUse(source, "tpad"))
                    .executes(context -> denyTpa(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpaauto")) {
            dispatcher.register(Commands.literal("tpaauto")
                    .requires(source -> canUse(source, "tpaauto"))
                    .executes(context -> tpaAuto(context.getSource().getPlayerOrException())));
        }

        if (enabled("tpahere")) {
            dispatcher.register(Commands.literal("tpahere")
                    .requires(source -> canUse(source, "tpahere"))
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPAHERE))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpahere(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaherea")) {
            dispatcher.register(Commands.literal("tpaherea")
                    .requires(source -> canUse(source, "tpaherea"))
                    .executes(context -> acceptTpahere(context.getSource().getPlayerOrException())));
        }
        if (enabled("tpahered")) {
            dispatcher.register(Commands.literal("tpahered")
                    .requires(source -> canUse(source, "tpahered"))
                    .executes(context -> denyTpahere(context.getSource().getPlayerOrException())));
        }

        if (enabled("rtp")) {
            dispatcher.register(Commands.literal("rtp")
                    .requires(source -> canUse(source, "rtp"))
                    .executes(context -> rtp(context.getSource().getPlayerOrException())));
        }
        if (enabled("spawn")) {
            dispatcher.register(Commands.literal("spawn")
                    .requires(source -> canUse(source, "spawn"))
                    .executes(context -> spawn(context.getSource().getPlayerOrException())));
        }
        if (enabled("back")) {
            dispatcher.register(Commands.literal("back")
                    .requires(source -> canUse(source, "back"))
                    .executes(context -> back(context.getSource().getPlayerOrException())));
        }
        if (enabled("dback")) {
            dispatcher.register(Commands.literal("dback")
                    .requires(source -> canUse(source, "dback"))
                    .executes(context -> back(context.getSource().getPlayerOrException())));
        }
        if (enabled("tback")) {
            dispatcher.register(Commands.literal("tback")
                    .requires(source -> canUse(source, "tback"))
                    .executes(context -> tback(context.getSource().getPlayerOrException())));
        }
        if (enabled("sethome")) {
            dispatcher.register(Commands.literal("sethome")
                    .requires(source -> canUse(source, "sethome"))
                    .executes(context -> sethome(context.getSource().getPlayerOrException())));
        }
        if (enabled("delhome")) {
            dispatcher.register(Commands.literal("delhome")
                    .requires(source -> canUse(source, "delhome"))
                    .executes(context -> delhome(context.getSource().getPlayerOrException())));
        }
        if (enabled("home")) {
            dispatcher.register(Commands.literal("home")
                    .requires(source -> canUse(source, "home"))
                    .executes(context -> home(context.getSource().getPlayerOrException())));
        }
        if (enabled("suicide")) {
            dispatcher.register(Commands.literal("suicide")
                    .requires(source -> canUse(source, "suicide"))
                    .executes(context -> suicide(context.getSource().getPlayerOrException())));
        }
        if (enabled("trash")) {
            dispatcher.register(Commands.literal("trash")
                    .requires(source -> canUse(source, "trash"))
                    .executes(context -> trash(context.getSource().getPlayerOrException())));
        }

        if (enabled("ppe-ess")) {
            var ppeEssential = Commands.literal("ppe-ess")
                    .requires(source -> canUse(source, "ppe-ess"))
                    .executes(context -> help(context.getSource().getPlayerOrException()))
                    .then(Commands.literal("help")
                            .executes(context -> help(context.getSource().getPlayerOrException())));
            if (enabled("ppe-ess-reset")) {
                ppeEssential.then(Commands.literal("reset")
                        .requires(source -> canUse(source, "ppe-ess-reset"))
                        .then(Commands.literal("all")
                                .executes(context -> resetAll(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("notice")
                                .executes(context -> resetNotice(context.getSource().getPlayerOrException()))));
            }
            dispatcher.register(ppeEssential);
        }

        if (enabled("warp")) {
            dispatcher.register(Commands.literal("warp")
                    .requires(source -> canUse(source, "warp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PpeCommands::suggestWarps)
                            .executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("setwarp")) {
            dispatcher.register(Commands.literal("setwarp")
                    .requires(source -> canUse(source, "setwarp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PpeCommands::suggestWarps)
                            .executes(context -> setWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("delwarp")) {
            dispatcher.register(Commands.literal("delwarp")
                    .requires(source -> canUse(source, "delwarp"))
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PpeCommands::suggestWarps)
                            .executes(context -> delWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))));
        }
        if (enabled("repeat")) {
            dispatcher.register(Commands.literal("repeat")
                    .requires(source -> canUse(source, "repeat"))
                    .then(Commands.argument("times", IntegerArgumentType.integer(1, 99))
                            .executes(context -> repeat(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "times"), null))
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> repeat(
                                            context.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(context, "times"),
                                            StringArgumentType.getString(context, "command")
                                    )))));
        }
        if (enabled("heal")) {
            dispatcher.register(Commands.literal("heal")
                    .requires(source -> canUse(source, "heal"))
                    .executes(context -> heal(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> heal(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("fly")) {
            dispatcher.register(Commands.literal("fly")
                    .requires(source -> canUse(source, "fly"))
                    .executes(context -> fly(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> fly(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("god")) {
            dispatcher.register(Commands.literal("god")
                    .requires(source -> canUse(source, "god"))
                    .executes(context -> god(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> god(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
    }

    public static void onServerTick(MinecraftServer server) {
        int tick = server.getTickCount();
        for (TeleportRequestType type : TeleportRequestType.values()) {
            expireRequests(server, tick, type);
        }
        RTP_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= tick);
        playPendingSounds(server, tick);
        if (tick % 20 == 0) {
            keepFlyEnabled(server);
        }
    }

    public static void rememberCommand(ServerPlayer player, String command) {
        String normalized = normalizeCommand(command);
        if (!normalized.isBlank() && !rootCommand(normalized).equalsIgnoreCase("repeat")) {
            LAST_COMMANDS.put(player.getUUID(), normalized);
        }
    }

    public static boolean isFlyEnabled(ServerPlayer player) {
        return PpePlayerData.get(PpeCompat.server(player)).isFlyEnabled(player.getUUID());
    }

    public static void restoreFly(ServerPlayer player) {
        if (isFlyEnabled(player)) {
            setMayFly(player, true);
        }
    }

    public static void restoreFly(ServerPlayer player, boolean wasFlying) {
        if (isFlyEnabled(player)) {
            PpeCompat.restoreFlight(player, wasFlying);
        }
    }

    public static void clearRuntimeState() {
        for (TeleportRequestType type : TeleportRequestType.values()) {
            type.requests().clear();
        }
        RTP_COOLDOWNS.clear();
        LAST_COMMANDS.clear();
        PENDING_SOUNDS.clear();
    }

    private static int tpa(ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(sender, target, TeleportRequestType.TPA);
    }

    private static int openTeleportMenu(ServerPlayer player, TeleportRequestType type) {
        PpeCompat.openTeleportMenu(player, type == TeleportRequestType.TPAHERE, 0);
        return 1;
    }

    private static int acceptTpa(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPA, true);
    }

    private static int denyTpa(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPA, false);
    }

    private static int tpaAuto(ServerPlayer player) {
        boolean enabled = PpePlayerData.get(PpeCompat.server(player)).toggleTpaAuto(player.getUUID());
        send(player, enabled ? "ppe_essentials.tpaauto.enabled" : "ppe_essentials.tpaauto.disabled");
        return 1;
    }

    private static int tpahere(ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(sender, target, TeleportRequestType.TPAHERE);
    }

    static List<ServerPlayer> teleportMenuTargets(ServerPlayer player) {
        return PpeCompat.server(player).getPlayerList().getPlayers().stream()
                .filter(target -> PpeConfig.allowSelfTeleportRequests() || !target.getUUID().equals(player.getUUID()))
                .sorted(Comparator.comparing(PpeCompat::profileName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static boolean requestTeleportFromMenu(ServerPlayer sender, UUID targetId, boolean here) {
        TeleportRequestType type = here ? TeleportRequestType.TPAHERE : TeleportRequestType.TPA;
        if (!commandAvailable(sender, type.languagePrefix)) {
            return false;
        }

        ServerPlayer target = PpeCompat.server(sender).getPlayerList().getPlayer(targetId);
        if (target == null) {
            send(sender, "ppe_essentials.teleport_menu.player.offline");
            return false;
        }

        return sendTeleportRequest(sender, target, type) > 0;
    }

    static boolean canToggleTpaAutoFromMenu(ServerPlayer player) {
        return commandAvailable(player, "tpaauto");
    }

    static boolean isTpaAutoEnabled(ServerPlayer player) {
        return canToggleTpaAutoFromMenu(player)
                && PpePlayerData.get(PpeCompat.server(player)).isTpaAuto(player.getUUID());
    }

    static void toggleTpaAutoFromMenu(ServerPlayer player) {
        if (commandAvailable(player, "tpaauto")) {
            tpaAuto(player);
        }
    }

    private static int acceptTpahere(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPAHERE, true);
    }

    private static int denyTpahere(ServerPlayer target) {
        return respondToTeleportRequest(target, TeleportRequestType.TPAHERE, false);
    }

    private static int sendTeleportRequest(ServerPlayer sender, ServerPlayer target, TeleportRequestType type) {
        if (!PpeConfig.allowSelfTeleportRequests() && sender.getUUID().equals(target.getUUID())) {
            send(sender, "ppe_essentials.teleport.request.self_disabled");
            return 0;
        }

        if (type == TeleportRequestType.TPA
                && commandAvailable(target, "tpaauto")
                && PpePlayerData.get(PpeCompat.server(sender)).isTpaAuto(target.getUUID())) {
            teleport(sender, PpeCompat.level(target), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            notifyParticipants(
                    target, "ppe_essentials.tpa.auto.target", sender.getName(),
                    sender, "ppe_essentials.tpa.auto.sender", target.getName()
            );
            teleportSounds(target, sender);
            return 1;
        }

        if (!canRespondToTeleportRequest(target, type)) {
            send(sender, "ppe_essentials.teleport.request.response_unavailable", target.getName());
            return 0;
        }

        if (type.requests().containsKey(target.getUUID())) {
            send(sender, type.key("request.busy"));
            return 0;
        }

        int timeoutSeconds = PpeConfig.teleportRequestTimeoutSeconds();
        type.requests().put(
                target.getUUID(),
                new PendingRequest(sender.getUUID(), PpeCompat.server(sender).getTickCount() + secondsToTicks(timeoutSeconds))
        );
        send(target, type.key("request.target"), sender.getName());
        sendRaw(target, prefixedRequestButtons(target, type.acceptCommand(), type.denyCommand()));
        send(target, type.key("request.commands"));
        bell(target);
        if (!sender.getUUID().equals(target.getUUID())) {
            send(sender, type.key("request.sender"), target.getName(), timeoutSeconds);
        }
        return 1;
    }

    private static int respondToTeleportRequest(ServerPlayer target, TeleportRequestType type, boolean accepted) {
        PendingRequest request = type.requests().remove(target.getUUID());
        if (request == null) {
            send(target, type.key(accepted ? "accept.none" : "deny.none"));
            return 0;
        }

        MinecraftServer server = PpeCompat.server(target);
        if (request.expireTick() <= server.getTickCount()) {
            notifyExpiredRequest(server, target.getUUID(), request, type);
            return 0;
        }

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (requester == null) {
            send(target, type.key(accepted ? "accept.none" : "deny.none"));
            return 0;
        }

        String action = accepted ? "accept" : "deny";
        if (accepted) {
            if (type.movesRequester()) {
                teleport(requester, PpeCompat.level(target), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            } else {
                teleport(target, PpeCompat.level(requester), requester.getX(), requester.getY(), requester.getZ(), requester.getYRot(), requester.getXRot());
            }
        }
        notifyParticipants(
                target, type.key(action + ".target"), requester.getName(),
                requester, type.key(action + ".sender"), target.getName()
        );
        if (accepted) {
            teleportSounds(target, requester);
        }
        return 1;
    }

    private static int rtp(ServerPlayer player) {
        int tick = PpeCompat.server(player).getTickCount();
        int cooldownExpireTick = RTP_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        if (cooldownExpireTick > tick) {
            send(player, "ppe_essentials.rtp.cooldown", ticksToSeconds(cooldownExpireTick - tick));
            return 0;
        }

        String name = PpeCompat.profileName(player);
        int minDistance = player.level().dimension() == Level.NETHER ? PpeConfig.rtpNetherMinDistance() : PpeConfig.rtpMinDistance();
        int maxDistance = player.level().dimension() == Level.NETHER ? PpeConfig.rtpNetherMaxDistance() : PpeConfig.rtpMaxDistance();
        String command = player.level().dimension() == Level.NETHER
                ? "execute as " + name + " at " + name + " run spreadplayers ~ ~ " + minDistance + " " + maxDistance + " under 127 false @s"
                : "execute as " + name + " at " + name + " run spreadplayers ~ ~ " + minDistance + " " + maxDistance + " false @s";
        PpeCompat.server(player).getCommands().performPrefixedCommand(PpeCompat.withPermission(player.createCommandSourceStack(), INTERNAL_COMMAND_PERMISSION_LEVEL).withSuppressedOutput(), command);
        send(player, "ppe_essentials.rtp.success");
        title(player, "ppe_essentials.rtp.title", "ppe_essentials.rtp.subtitle");
        teleportSound(player);
        int cooldownSeconds = PpeConfig.rtpCooldownSeconds();
        if (cooldownSeconds > 0) {
            RTP_COOLDOWNS.put(player.getUUID(), tick + secondsToTicks(cooldownSeconds));
        }
        return 1;
    }

    private static int spawn(ServerPlayer player) {
        ServerLevel level = PpeCompat.spawnLevel(player);
        BlockPos spawn = PpeCompat.spawnPos(player);
        teleport(player, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, PpeCompat.spawnAngle(player), 0.0F);
        send(player, "ppe_essentials.spawn.success");
        teleportSound(player);
        return 1;
    }

    private static int back(ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).deathBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "ppe_essentials.back.success");
            teleportSound(player);
            return 1;
        }

        send(player, "ppe_essentials.back.none");
        return 0;
    }

    private static int tback(ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).teleportBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "ppe_essentials.tback.success");
            teleportSound(player);
            return 1;
        }

        send(player, "ppe_essentials.tback.none");
        return 0;
    }

    private static int sethome(ServerPlayer player) {
        PpePlayerData.get(PpeCompat.server(player)).setHome(player.getUUID(), PpeLocation.of(player));
        send(player, "ppe_essentials.sethome.success");
        return 1;
    }

    private static int delhome(ServerPlayer player) {
        if (PpePlayerData.get(PpeCompat.server(player)).deleteHome(player.getUUID())) {
            send(player, "ppe_essentials.delhome.success");
            return 1;
        }

        send(player, "ppe_essentials.delhome.none");
        return 0;
    }

    private static int home(ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).home(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "ppe_essentials.home.success");
            title(player, "ppe_essentials.home.title", "ppe_essentials.home.subtitle", player.getName());
            playSoundLater(player, SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.0F, 1.5F, TELEPORT_SOUND_DELAY_TICKS);
            return 1;
        }

        send(player, "ppe_essentials.home.none");
        return 0;
    }

    private static int suicide(ServerPlayer player) {
        PpeCompat.kill(player);
        for (ServerPlayer target : PpeCompat.server(player).getPlayerList().getPlayers()) {
            send(target, "ppe_essentials.suicide.broadcast", player.getName());
        }
        return 1;
    }

    private static int trash(ServerPlayer player) {
        MenuProvider menu = new SimpleMenuProvider(
                (containerId, inventory, p) -> ChestMenu.sixRows(containerId, inventory),
                PpeLang.component(player, "ppe_essentials.trash.title")
        );
        player.openMenu(menu);
        PpeCompat.playSound(player, SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 1.0F, 1.0F);
        return 1;
    }

    private static int warp(ServerPlayer player, String name) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).warp(name);
        if (location.isPresent()) {
            if (!teleport(player, location.get())) {
                return 0;
            }
            send(player, "ppe_essentials.warp.success", name);
            teleportSound(player);
            return 1;
        }

        send(player, "ppe_essentials.warp.none", name);
        return 0;
    }

    private static int setWarp(ServerPlayer player, String name) {
        PpePlayerData.get(PpeCompat.server(player)).setWarp(name, PpeLocation.of(player));
        send(player, "ppe_essentials.setwarp.success", name);
        return 1;
    }

    private static int delWarp(ServerPlayer player, String name) {
        if (PpePlayerData.get(PpeCompat.server(player)).deleteWarp(name)) {
            send(player, "ppe_essentials.delwarp.success", name);
            return 1;
        }

        send(player, "ppe_essentials.delwarp.none", name);
        return 0;
    }

    private static int help(ServerPlayer player) {
        send(player, "ppe_essentials.help.header");
        CommandSourceStack source = player.createCommandSourceStack();
        sendHelpLine(player, source, "ppe_essentials.help.teleport",
                helpCommand("tpa"), helpCommand("tpahere"), helpCommand("tpaa"), helpCommand("tpad"), helpCommand("tpaauto"),
                helpCommand("tpaherea"), helpCommand("tpahered"),
                helpCommand("back"), helpCommand("dback"), helpCommand("tback"), helpCommand("spawn"), helpCommand("rtp"));
        sendHelpLine(player, source, "ppe_essentials.help.home",
                helpCommand("home"), helpCommand("sethome"), helpCommand("delhome"), helpCommand("warp"));
        sendHelpLine(player, source, "ppe_essentials.help.utility",
                helpCommand("suicide"), helpCommand("trash"));
        sendHelpLine(player, source, "ppe_essentials.help.op",
                helpCommand("setwarp"), helpCommand("delwarp"), helpCommand("heal"), helpCommand("fly"), helpCommand("god"),
                helpCommand("repeat"), new HelpCommand("ppe-ess-reset", "/ppe-ess reset"));
        return 1;
    }

    private static void sendHelpLine(ServerPlayer player, CommandSourceStack source, String titleKey, HelpCommand... commands) {
        MutableComponent line = PpeLang.component(player, titleKey);
        for (int i = 0; i < commands.length; i++) {
            if (i > 0) {
                line.append(Component.literal(" "));
            }

            HelpCommand command = commands[i];
            line.append(Component.literal((canShowAsAvailable(source, command.configKey()) ? "§6" : "§c") + command.display()));
        }
        sendRaw(player, PpeLang.prefixed(line));
    }

    private static HelpCommand helpCommand(String command) {
        return new HelpCommand(command, "/" + command);
    }

    private static boolean canShowAsAvailable(CommandSourceStack source, String command) {
        return enabled(command) && canUse(source, command);
    }

    private static int resetAll(ServerPlayer player) {
        PpePlayerData.get(PpeCompat.server(player)).clearPlayerData();
        clearRuntimeState();
        PpeRuntimeEvents.clearNoticeQueues();
        for (ServerPlayer target : PpeCompat.server(player).getPlayerList().getPlayers()) {
            setMayFly(target, target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        }
        send(player, "ppe_essentials.reset.all.success");
        return 1;
    }

    private static int resetNotice(ServerPlayer player) {
        PpePlayerData.get(PpeCompat.server(player)).clearNoticeData();
        PpeRuntimeEvents.clearNoticeQueues();
        send(player, "ppe_essentials.reset.notice.success");
        return 1;
    }

    private static int repeat(ServerPlayer player, int times, String command) {
        String normalized = command == null ? LAST_COMMANDS.get(player.getUUID()) : normalizeCommand(command);
        if (normalized == null || normalized.isBlank()) {
            send(player, "ppe_essentials.repeat.none");
            return 0;
        }
        if (rootCommand(normalized).equalsIgnoreCase("repeat")) {
            send(player, "ppe_essentials.repeat.blocked");
            return 0;
        }

        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput();
        for (int i = 0; i < times; i++) {
            PpeCompat.server(player).getCommands().performPrefixedCommand(source, normalized);
        }
        return times;
    }

    private static int heal(ServerPlayer sender, ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        FoodData food = target.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        send(sender, "ppe_essentials.heal.success", target.getName());
        if (sender != target) {
            send(target, "ppe_essentials.heal.target", sender.getName());
        }
        return 1;
    }

    private static int fly(ServerPlayer sender, ServerPlayer target) {
        boolean enabled = PpePlayerData.get(PpeCompat.server(sender)).toggleFly(target.getUUID());
        setMayFly(target, enabled || target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        send(sender, enabled ? "ppe_essentials.fly.enabled" : "ppe_essentials.fly.disabled", target.getName());
        if (sender != target) {
            send(target, enabled ? "ppe_essentials.fly.target.enabled" : "ppe_essentials.fly.target.disabled", sender.getName());
        }
        return 1;
    }

    private static int god(ServerPlayer sender, ServerPlayer target) {
        boolean enabled = PpePlayerData.get(PpeCompat.server(sender)).toggleGod(target.getUUID());
        send(sender, enabled ? "ppe_essentials.god.enabled" : "ppe_essentials.god.disabled", target.getName());
        if (sender != target) {
            send(target, enabled ? "ppe_essentials.god.target.enabled" : "ppe_essentials.god.target.disabled", sender.getName());
        }
        return 1;
    }

    private static void expireRequests(MinecraftServer server, int tick, TeleportRequestType type) {
        Iterator<Map.Entry<UUID, PendingRequest>> iterator = type.requests().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRequest> entry = iterator.next();
            if (entry.getValue().expireTick() > tick) {
                continue;
            }

            iterator.remove();
            notifyExpiredRequest(server, entry.getKey(), entry.getValue(), type);
        }
    }

    private static void notifyExpiredRequest(
            MinecraftServer server,
            UUID targetId,
            PendingRequest request,
            TeleportRequestType type
    ) {
        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (target != null) {
            send(target, type.key("timeout.target"));
        }
        if (requester != null && !requester.getUUID().equals(targetId)) {
            send(requester, type.key("timeout.sender"));
        }
    }

    private static boolean teleport(ServerPlayer player, PpeLocation location) {
        Optional<ServerLevel> level = location.resolve(PpeCompat.server(player));
        if (level.isEmpty()) {
            send(player, "ppe_essentials.teleport.missing_dimension");
            return false;
        }

        teleport(player, level.get(), location.x(), location.y(), location.z(), location.yRot(), location.xRot());
        return true;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        PpePlayerData.get(PpeCompat.server(player)).setTeleportBack(player.getUUID(), PpeLocation.of(player));
        PpeCompat.teleport(player, level, x, y, z, yRot, xRot);
    }

    private static MutableComponent prefixedRequestButtons(ServerPlayer player, String acceptCommand, String denyCommand) {
        MutableComponent message = Component.empty();
        message.append(PpeLang.component(player, "ppe_essentials.request.accept")
                .withStyle(Style.EMPTY
                        .withClickEvent(PpeCompat.runCommandClick("/" + acceptCommand))
                        .withHoverEvent(PpeCompat.showTextHover(PpeLang.component(player, "ppe_essentials.request.accept.tooltip")))));
        message.append(Component.literal(" "));
        message.append(PpeLang.component(player, "ppe_essentials.request.deny")
                .withStyle(Style.EMPTY
                        .withClickEvent(PpeCompat.runCommandClick("/" + denyCommand))
                        .withHoverEvent(PpeCompat.showTextHover(PpeLang.component(player, "ppe_essentials.request.deny.tooltip")))));
        return PpeLang.prefixed(message);
    }

    private static boolean canRespondToTeleportRequest(ServerPlayer target, TeleportRequestType type) {
        return commandAvailable(target, type.acceptCommand())
                && commandAvailable(target, type.denyCommand());
    }

    private static boolean commandAvailable(ServerPlayer player, String command) {
        CommandSourceStack source = player.createCommandSourceStack();
        return enabled(command)
                && PpeCompat.server(player).getCommands().getDispatcher().getRoot().getChild(command) != null
                && canUse(source, command);
    }

    private static void notifyParticipants(
            ServerPlayer first,
            String firstKey,
            Object firstArgument,
            ServerPlayer second,
            String secondKey,
            Object secondArgument
    ) {
        send(first, firstKey, firstArgument);
        if (!first.getUUID().equals(second.getUUID())) {
            send(second, secondKey, secondArgument);
        }
    }

    private static void teleportSounds(ServerPlayer first, ServerPlayer second) {
        teleportSound(first);
        if (!first.getUUID().equals(second.getUUID())) {
            teleportSound(second);
        }
    }

    private static void send(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(PpeLang.prefixedComponent(player, key, args));
    }

    private static void sendRaw(ServerPlayer player, Component component) {
        player.sendSystemMessage(component);
    }

    private static void bell(ServerPlayer player) {
        PpeCompat.playSound(player, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void teleportSound(ServerPlayer player) {
        playSoundLater(player, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F, TELEPORT_SOUND_DELAY_TICKS);
    }

    private static void playSoundLater(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch, int delayTicks) {
        PENDING_SOUNDS.add(new PendingSound(
                player.getUUID(),
                sound,
                source,
                volume,
                pitch,
                PpeCompat.server(player).getTickCount() + delayTicks
        ));
    }

    private static void playPendingSounds(MinecraftServer server, int tick) {
        Iterator<PendingSound> iterator = PENDING_SOUNDS.iterator();
        while (iterator.hasNext()) {
            PendingSound sound = iterator.next();
            if (sound.playTick() > tick) {
                continue;
            }

            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(sound.player());
            if (player != null) {
                PpeCompat.playSound(player, sound.sound(), sound.source(), sound.volume(), sound.pitch());
            }
        }
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(PpePlayerData.get(context.getSource().getServer()).warpNames(), builder);
    }

    private static boolean enabled(String command) {
        return PpeConfig.commandEnabled(command);
    }

    private static boolean canUse(CommandSourceStack source, String command) {
        return PpeCompat.hasPermission(source, PpeConfig.commandPermission(command));
    }

    private static void keepFlyEnabled(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            restoreFly(player);
        }
    }

    private static void setMayFly(ServerPlayer player, boolean mayFly) {
        PpeCompat.setMayFly(player, mayFly);
    }

    private static String normalizeCommand(String command) {
        String normalized = command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private static String rootCommand(String command) {
        int space = command.indexOf(' ');
        return space == -1 ? command : command.substring(0, space);
    }

    private static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    private static int ticksToSeconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }

    private static void title(ServerPlayer player, String titleKey, String subtitleKey, Object... subtitleArgs) {
        player.connection.send(new ClientboundSetTitleTextPacket(PpeLang.component(player, titleKey)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(PpeLang.component(player, subtitleKey, subtitleArgs)));
    }

    private record HelpCommand(String configKey, String display) {
    }

    private enum TeleportRequestType {
        TPA("tpa", "tpaa", "tpad", true),
        TPAHERE("tpahere", "tpaherea", "tpahered", false);

        private final String languagePrefix;
        private final String acceptCommand;
        private final String denyCommand;
        private final boolean movesRequester;
        private final Map<UUID, PendingRequest> requests = new HashMap<>();

        TeleportRequestType(String languagePrefix, String acceptCommand, String denyCommand, boolean movesRequester) {
            this.languagePrefix = languagePrefix;
            this.acceptCommand = acceptCommand;
            this.denyCommand = denyCommand;
            this.movesRequester = movesRequester;
        }

        private String key(String suffix) {
            return "ppe_essentials." + languagePrefix + "." + suffix;
        }

        private String acceptCommand() {
            return acceptCommand;
        }

        private String denyCommand() {
            return denyCommand;
        }

        private boolean movesRequester() {
            return movesRequester;
        }

        private Map<UUID, PendingRequest> requests() {
            return requests;
        }
    }

    private record PendingRequest(UUID requester, int expireTick) {
    }

    private record PendingSound(UUID player, SoundEvent sound, SoundSource source, float volume, float pitch, int playTick) {
    }
}
