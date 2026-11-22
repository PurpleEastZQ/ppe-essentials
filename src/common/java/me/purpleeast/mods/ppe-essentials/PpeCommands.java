package me.purpleeast.mods.ppe_essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.purpleeast.mods.ppe_essentials.PpeCommandContext.fail;
import static me.purpleeast.mods.ppe_essentials.PpeCommandContext.reply;

/**
 * Registers PPE commands and owns their loader-independent runtime behavior.
 */
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
            dispatcher.register(command("tpa")
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPA))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpa(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaa")) {
            dispatcher.register(command("tpaa")
                    .executes(context -> acceptTpa(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("tpad")) {
            dispatcher.register(command("tpad")
                    .executes(context -> denyTpa(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("tpaauto")) {
            dispatcher.register(command("tpaauto")
                    .executes(context -> tpaAuto(context.getSource(), context.getSource().getPlayerOrException())));
        }

        if (enabled("tpahere")) {
            dispatcher.register(command("tpahere")
                    .executes(context -> openTeleportMenu(context.getSource().getPlayerOrException(), TeleportRequestType.TPAHERE))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> tpahere(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))));
        }
        if (enabled("tpaherea")) {
            dispatcher.register(command("tpaherea")
                    .executes(context -> acceptTpahere(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("tpahered")) {
            dispatcher.register(command("tpahered")
                    .executes(context -> denyTpahere(context.getSource(), context.getSource().getPlayerOrException())));
        }

        if (enabled("rtp")) {
            dispatcher.register(command("rtp")
                    .executes(context -> rtp(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("spawn")) {
            dispatcher.register(command("spawn")
                    .executes(context -> spawn(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("back")) {
            dispatcher.register(command("back")
                    .executes(context -> back(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("dback")) {
            dispatcher.register(command("dback")
                    .executes(context -> back(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("tback")) {
            dispatcher.register(command("tback")
                    .executes(context -> tback(context.getSource(), context.getSource().getPlayerOrException())));
        }
        registerHome(dispatcher);
        if (enabled("suicide")) {
            dispatcher.register(command("suicide")
                    .executes(context -> suicide(context.getSource().getPlayerOrException())));
        }
        if (enabled("trash")) {
            dispatcher.register(command("trash")
                    .executes(context -> trash(context.getSource().getPlayerOrException())));
        }

        if (enabled("ppe-ess")) {
            var ppeEssential = command("ppe-ess")
                    .executes(context -> help(context.getSource()))
                    .then(Commands.literal("help")
                            .executes(context -> help(context.getSource())));
            if (enabled("ppe-ess-reset")) {
                ppeEssential.then(Commands.literal("reset")
                        .requires(source -> canUse(source, "ppe-ess-reset"))
                        .then(Commands.literal("all")
                                .executes(context -> resetAll(context.getSource())))
                        .then(Commands.literal("notice")
                                .executes(context -> resetNotice(context.getSource()))));
            }
            dispatcher.register(ppeEssential);
        }

        registerWarp(dispatcher);
        if (enabled("repeat")) {
            dispatcher.register(command("repeat")
                    .then(Commands.argument("times", IntegerArgumentType.integer(1, 99))
                            .executes(context -> repeat(context.getSource(), IntegerArgumentType.getInteger(context, "times"), null))
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> repeat(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "times"),
                                            StringArgumentType.getString(context, "command")
                                    )))));
        }
        registerTargetCommand(dispatcher, "heal", PpeCommands::heal);
        registerTargetCommand(dispatcher, "fly", PpeCommands::fly);
        registerTargetCommand(dispatcher, "god", PpeCommands::god);
    }

    private static void registerHome(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!enabled("home")) {
            return;
        }
        var root = command("home")
                .executes(context -> home(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("tp")
                        .executes(context -> home(context.getSource(), context.getSource().getPlayerOrException())));
        if (enabled("sethome")) {
            root.then(Commands.literal("set")
                    .requires(source -> canUse(source, "sethome"))
                    .executes(context -> sethome(context.getSource(), context.getSource().getPlayerOrException())));
        }
        if (enabled("delhome")) {
            root.then(Commands.literal("del")
                    .requires(source -> canUse(source, "delhome"))
                    .executes(context -> delhome(context.getSource(), context.getSource().getPlayerOrException())));
        }
        dispatcher.register(root);
    }

    private static void registerWarp(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!enabled("warp")) {
            return;
        }
        var root = command("warp").then(warpTeleportArgument())
                .then(Commands.literal("tp")
                        .executes(context -> warp(context.getSource(), context.getSource().getPlayerOrException(), "tp"))
                        .then(warpTeleportArgument()));
        if (enabled("setwarp")) {
            root.then(Commands.literal("set")
                    .executes(context -> warp(context.getSource(), context.getSource().getPlayerOrException(), "set"))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                            .requires(source -> canUse(source, "setwarp"))
                            .suggests(PpeCommands::suggestWarps)
                            .executes(context -> setWarp(context.getSource(), context.getSource().getPlayerOrException(),
                                    warpName(context)))));
        }
        if (enabled("delwarp")) {
            root.then(Commands.literal("delete")
                    .executes(context -> warp(context.getSource(), context.getSource().getPlayerOrException(), "delete"))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                            .requires(source -> canUse(source, "delwarp"))
                            .suggests(PpeCommands::suggestWarps)
                            .executes(context -> delWarp(context.getSource(), warpName(context)))));
        }
        dispatcher.register(root);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> warpTeleportArgument() {
        return Commands.argument("name", StringArgumentType.greedyString())
                .suggests(PpeCommands::suggestWarps)
                .executes(context -> warp(context.getSource(), context.getSource().getPlayerOrException(), warpName(context)));
    }

    private static String warpName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, "name").strip();
        if (value.startsWith("\"")) {
            var reader = new StringReader(value);
            value = reader.readQuotedString();
            if (reader.canRead()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
        }
        if (value.isBlank()) {
            throw new SimpleCommandExceptionType(Component.literal("Specify a warp name.")).create();
        }
        return value;
    }

    private static void registerTargetCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                                              TargetCommand action) {
        if (enabled(name)) {
            dispatcher.register(command(name)
                    .executes(context -> action.run(context.getSource(), context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> action.run(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name).requires(source -> canUse(source, name));
    }

    @FunctionalInterface
    private interface TargetCommand {
        int run(CommandSourceStack source, ServerPlayer target);
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

    public static void rememberCommand(ParseResults<CommandSourceStack> command) {
        var source = command.getContext().getSource();
        ServerPlayer sender = PpeCommandContext.senderPlayer(source);
        if (sender == null || PpeCommandContext.isSilent(source)
                || !enabled("repeat") || !canUse(source, "repeat") || containsRepeat(command)) {
            return;
        }

        String original = normalizeCommand(command.getReader().getString());
        if (!original.isBlank() && !command.getReader().canRead() && command.getContext().getLastChild().getCommand() != null) {
            LAST_COMMANDS.put(sender.getUUID(), original);
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
        // These collections contain session-only state and must not survive a server restart.
        for (TeleportRequestType type : TeleportRequestType.values()) {
            type.requests().clear();
        }
        RTP_COOLDOWNS.clear();
        LAST_COMMANDS.clear();
        PENDING_SOUNDS.clear();
    }

    private static int tpa(CommandSourceStack source, ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(source, sender, target, TeleportRequestType.TPA);
    }

    private static int openTeleportMenu(ServerPlayer player, TeleportRequestType type) {
        PpeCompat.openTeleportMenu(player, type == TeleportRequestType.TPAHERE, 0);
        return 1;
    }

    private static int acceptTpa(CommandSourceStack source, ServerPlayer target) {
        return respondToTeleportRequest(source, target, TeleportRequestType.TPA, true);
    }

    private static int denyTpa(CommandSourceStack source, ServerPlayer target) {
        return respondToTeleportRequest(source, target, TeleportRequestType.TPA, false);
    }

    private static int tpaAuto(CommandSourceStack source, ServerPlayer player) {
        boolean enabled = PpePlayerData.get(PpeCompat.server(player)).toggleTpaAuto(player.getUUID());
        reply(source, enabled ? "ppe_essentials.tpaauto.enabled" : "ppe_essentials.tpaauto.disabled");
        return 1;
    }

    private static int tpahere(CommandSourceStack source, ServerPlayer sender, ServerPlayer target) {
        return sendTeleportRequest(source, sender, target, TeleportRequestType.TPAHERE);
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

        return sendTeleportRequest(sender.createCommandSourceStack(), sender, target, type) > 0;
    }

    static boolean canToggleTpaAutoFromMenu(ServerPlayer player) {
        return commandAvailable(player, "tpaauto");
    }

    static boolean isTpaAutoEnabled(ServerPlayer player) {
        return canToggleTpaAutoFromMenu(player)
                && PpePlayerData.get(PpeCompat.server(player)).isTpaAuto(player.getUUID());
    }

    static boolean canAutoAcceptTpa(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR && isTpaAutoEnabled(player);
    }

    static void toggleTpaAutoFromMenu(ServerPlayer player) {
        if (commandAvailable(player, "tpaauto")) {
            tpaAuto(player.createCommandSourceStack(), player);
        }
    }

    private static int acceptTpahere(CommandSourceStack source, ServerPlayer target) {
        return respondToTeleportRequest(source, target, TeleportRequestType.TPAHERE, true);
    }

    private static int denyTpahere(CommandSourceStack source, ServerPlayer target) {
        return respondToTeleportRequest(source, target, TeleportRequestType.TPAHERE, false);
    }

    private static int sendTeleportRequest(CommandSourceStack source, ServerPlayer requester, ServerPlayer target, TeleportRequestType type) {
        if (!PpeConfig.allowSelfTeleportRequests() && requester.getUUID().equals(target.getUUID())) {
            return fail(source, "ppe_essentials.teleport.request.self_disabled");
        }

        if (type == TeleportRequestType.TPA
                && canAutoAcceptTpa(target)) {
            teleport(requester, PpeCompat.level(target), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            notifyParticipants(
                    target, "ppe_essentials.tpa.auto.target", requester.getName(),
                    requester, "ppe_essentials.tpa.auto.sender", target.getName()
            );
            replyToOtherSender(source, requester, target, "ppe_essentials.tpa.auto.sender", target.getName());
            teleportSounds(target, requester);
            return 1;
        }

        if (!canRespondToTeleportRequest(target, type)) {
            return fail(source, "ppe_essentials.teleport.request.response_unavailable", target.getName());
        }

        if (type.requests().containsKey(target.getUUID())) {
            return fail(source, type.key("request.busy"));
        }

        int timeoutSeconds = PpeConfig.teleportRequestTimeoutSeconds();
        type.requests().put(
                target.getUUID(),
                new PendingRequest(requester.getUUID(), PpeCompat.server(requester).getTickCount() + secondsToTicks(timeoutSeconds))
        );
        send(target, type.key("request.target"), requester.getName());
        sendRaw(target, prefixedRequestButtons(target, type.acceptCommand(), type.denyCommand()));
        send(target, type.key("request.commands"));
        bell(target);
        if (!requester.getUUID().equals(target.getUUID()) || PpeCommandContext.senderPlayer(source) != target) {
            reply(source, type.key("request.sender"), target.getName(), timeoutSeconds);
        }
        return 1;
    }

    private static int respondToTeleportRequest(CommandSourceStack source, ServerPlayer target, TeleportRequestType type, boolean accepted) {
        PendingRequest request = type.requests().remove(target.getUUID());
        if (request == null) {
            return fail(source, type.key(accepted ? "accept.none" : "deny.none"));
        }

        MinecraftServer server = PpeCompat.server(target);
        if (request.expireTick() <= server.getTickCount()) {
            notifyExpiredRequest(server, target.getUUID(), request, type);
            return 0;
        }

        ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (requester == null) {
            return fail(source, type.key(accepted ? "accept.none" : "deny.none"));
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
        replyToOtherSender(source, target, requester, type.key(action + ".target"), requester.getName());
        if (accepted) {
            teleportSounds(target, requester);
        }
        return 1;
    }

    private static int rtp(CommandSourceStack source, ServerPlayer player) {
        int tick = PpeCompat.server(player).getTickCount();
        int cooldownExpireTick = RTP_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        if (cooldownExpireTick > tick) {
            return fail(source, "ppe_essentials.rtp.cooldown", ticksToSeconds(cooldownExpireTick - tick));
        }

        String name = PpeCompat.profileName(player);
        int minDistance = player.level().dimension() == Level.NETHER ? PpeConfig.rtpNetherMinDistance() : PpeConfig.rtpMinDistance();
        int maxDistance = player.level().dimension() == Level.NETHER ? PpeConfig.rtpNetherMaxDistance() : PpeConfig.rtpMaxDistance();
        String command = player.level().dimension() == Level.NETHER
                ? "execute as " + name + " at " + name + " run spreadplayers ~ ~ " + minDistance + " " + maxDistance + " under 127 false @s"
                : "execute as " + name + " at " + name + " run spreadplayers ~ ~ " + minDistance + " " + maxDistance + " false @s";
        PpeCompat.server(player).getCommands().performPrefixedCommand(PpeCompat.withPermission(player.createCommandSourceStack(), INTERNAL_COMMAND_PERMISSION_LEVEL).withSuppressedOutput(), command);
        reply(source, "ppe_essentials.rtp.success");
        title(player, "ppe_essentials.rtp.title", "ppe_essentials.rtp.subtitle");
        teleportSound(player);
        int cooldownSeconds = PpeConfig.rtpCooldownSeconds();
        if (cooldownSeconds > 0) {
            RTP_COOLDOWNS.put(player.getUUID(), tick + secondsToTicks(cooldownSeconds));
        }
        return 1;
    }

    private static int spawn(CommandSourceStack source, ServerPlayer player) {
        ServerLevel level = PpeCompat.spawnLevel(player);
        BlockPos spawn = PpeCompat.spawnPos(player);
        teleport(player, level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, PpeCompat.spawnAngle(player), 0.0F);
        reply(source, "ppe_essentials.spawn.success");
        teleportSound(player);
        return 1;
    }

    private static int back(CommandSourceStack source, ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).deathBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(source, player, location.get())) {
                return 0;
            }
            reply(source, "ppe_essentials.back.success");
            teleportSound(player);
            return 1;
        }

        return fail(source, "ppe_essentials.back.none");
    }

    private static int tback(CommandSourceStack source, ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).teleportBack(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(source, player, location.get())) {
                return 0;
            }
            reply(source, "ppe_essentials.tback.success");
            teleportSound(player);
            return 1;
        }

        return fail(source, "ppe_essentials.tback.none");
    }

    private static int sethome(CommandSourceStack source, ServerPlayer player) {
        PpePlayerData.get(PpeCompat.server(player)).setHome(player.getUUID(), PpeLocation.of(player));
        reply(source, "ppe_essentials.sethome.success");
        return 1;
    }

    private static int delhome(CommandSourceStack source, ServerPlayer player) {
        if (PpePlayerData.get(PpeCompat.server(player)).deleteHome(player.getUUID())) {
            reply(source, "ppe_essentials.delhome.success");
            return 1;
        }

        return fail(source, "ppe_essentials.delhome.none");
    }

    private static int home(CommandSourceStack source, ServerPlayer player) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).home(player.getUUID());
        if (location.isPresent()) {
            if (!teleport(source, player, location.get())) {
                return 0;
            }
            reply(source, "ppe_essentials.home.success");
            title(player, "ppe_essentials.home.title", "ppe_essentials.home.subtitle", player.getName());
            playSoundLater(player, SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.0F, 1.5F, TELEPORT_SOUND_DELAY_TICKS);
            return 1;
        }

        return fail(source, "ppe_essentials.home.none");
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

    private static int warp(CommandSourceStack source, ServerPlayer player, String name) {
        Optional<PpeLocation> location = PpePlayerData.get(PpeCompat.server(player)).warp(name);
        if (location.isPresent()) {
            if (!teleport(source, player, location.get())) {
                return 0;
            }
            reply(source, "ppe_essentials.warp.success", name);
            teleportSound(player);
            return 1;
        }

        return fail(source, "ppe_essentials.warp.none", name);
    }

    private static int setWarp(CommandSourceStack source, ServerPlayer player, String name) {
        PpePlayerData.get(source.getServer()).setWarp(name, PpeLocation.of(player));
        reply(source, "ppe_essentials.setwarp.success", name);
        return 1;
    }

    private static int delWarp(CommandSourceStack source, String name) {
        if (PpePlayerData.get(source.getServer()).deleteWarp(name)) {
            reply(source, "ppe_essentials.delwarp.success", name);
            return 1;
        }

        return fail(source, "ppe_essentials.delwarp.none", name);
    }

    private static int help(CommandSourceStack source) {
        reply(source, "ppe_essentials.help.header");
        sendHelpLine(source, "ppe_essentials.help.teleport",
                playerHelpCommand("tpa"), playerHelpCommand("tpahere"), playerHelpCommand("tpaa"), playerHelpCommand("tpad"), playerHelpCommand("tpaauto"),
                playerHelpCommand("tpaherea"), playerHelpCommand("tpahered"),
                playerHelpCommand("back"), playerHelpCommand("dback"), playerHelpCommand("tback"), playerHelpCommand("spawn"), playerHelpCommand("rtp"));
        sendHelpLine(source, "ppe_essentials.help.home",
                playerHelpCommand("home"), new HelpCommand("sethome", "/home set", true),
                new HelpCommand("delhome", "/home del", true), new HelpCommand("warp", "/warp tp", true));
        sendHelpLine(source, "ppe_essentials.help.utility",
                playerHelpCommand("suicide"), playerHelpCommand("trash"));
        sendHelpLine(source, "ppe_essentials.help.op",
                new HelpCommand("setwarp", "/warp set", true), new HelpCommand("delwarp", "/warp delete", false), helpCommand("heal"), helpCommand("fly"), helpCommand("god"),
                helpCommand("repeat"), new HelpCommand("ppe-ess-reset", "/ppe-ess reset", false));
        return 1;
    }

    private static void sendHelpLine(CommandSourceStack source, String titleKey, HelpCommand... commands) {
        MutableComponent line = PpeLang.componentForLanguage(PpeCommandContext.language(source), titleKey);
        for (int i = 0; i < commands.length; i++) {
            if (i > 0) {
                line.append(Component.literal(" "));
            }

            HelpCommand command = commands[i];
            line.append(Component.literal((canShowAsAvailable(source, command) ? "§6" : "§c") + command.display()));
        }
        source.sendSuccess(() -> PpeLang.prefixed(line), false);
    }

    private static HelpCommand helpCommand(String command) {
        return new HelpCommand(command, "/" + command, false);
    }

    private static HelpCommand playerHelpCommand(String command) {
        return new HelpCommand(command, "/" + command, true);
    }

    private static boolean canShowAsAvailable(CommandSourceStack source, HelpCommand command) {
        boolean homeAction = command.configKey().equals("sethome") || command.configKey().equals("delhome");
        boolean warpAction = command.configKey().equals("setwarp") || command.configKey().equals("delwarp");
        return (!homeAction || (enabled("home") && canUse(source, "home")))
                && (!warpAction || (enabled("warp") && canUse(source, "warp")))
                && enabled(command.configKey()) && canUse(source, command.configKey())
                && (!command.playerOnly() || source.getPlayer() != null);
    }

    private static int resetAll(CommandSourceStack source) {
        PpePlayerData.get(source.getServer()).clearPlayerData();
        clearRuntimeState();
        PpeRuntimeEvents.clearNoticeQueues();
        for (ServerPlayer target : source.getServer().getPlayerList().getPlayers()) {
            setMayFly(target, target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        }
        reply(source, "ppe_essentials.reset.all.success");
        return 1;
    }

    private static int resetNotice(CommandSourceStack source) {
        PpePlayerData.get(source.getServer()).clearNoticeData();
        PpeRuntimeEvents.clearNoticeQueues();
        reply(source, "ppe_essentials.reset.notice.success");
        return 1;
    }

    private static int repeat(CommandSourceStack source, int times, String command) throws CommandSyntaxException {
        ServerPlayer sender = PpeCommandContext.senderPlayer(source);
        String normalized = command == null
                ? (sender == null ? null : LAST_COMMANDS.get(sender.getUUID()))
                : normalizeCommand(command);
        if (normalized == null || normalized.isBlank()) {
            return fail(source, "ppe_essentials.repeat.none");
        }

        var commands = source.getServer().getCommands();
        var replay = source.withSuppressedOutput().withCallback(CommandResultCallback.EMPTY);
        var parsed = commands.getDispatcher().parse(normalized, replay);
        if (containsRepeat(parsed)) {
            return fail(source, "ppe_essentials.repeat.blocked");
        }
        Commands.validateParseResults(parsed);
        for (int i = 0; i < times; i++) {
            commands.performCommand(parsed, normalized);
        }
        return times;
    }

    private static int heal(CommandSourceStack source, ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        FoodData food = target.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        reply(source, "ppe_essentials.heal.success", target.getName());
        if (PpeCommandContext.senderPlayer(source) != target) {
            send(target, "ppe_essentials.heal.target", senderName(source));
        }
        return 1;
    }

    private static int fly(CommandSourceStack source, ServerPlayer target) {
        boolean enabled = PpePlayerData.get(source.getServer()).toggleFly(target.getUUID());
        setMayFly(target, enabled || target.gameMode.getGameModeForPlayer() == GameType.CREATIVE);
        reply(source, enabled ? "ppe_essentials.fly.enabled" : "ppe_essentials.fly.disabled", target.getName());
        if (PpeCommandContext.senderPlayer(source) != target) {
            send(target, enabled ? "ppe_essentials.fly.target.enabled" : "ppe_essentials.fly.target.disabled", senderName(source));
        }
        return 1;
    }

    private static int god(CommandSourceStack source, ServerPlayer target) {
        boolean enabled = PpePlayerData.get(source.getServer()).toggleGod(target.getUUID());
        reply(source, enabled ? "ppe_essentials.god.enabled" : "ppe_essentials.god.disabled", target.getName());
        if (PpeCommandContext.senderPlayer(source) != target) {
            send(target, enabled ? "ppe_essentials.god.target.enabled" : "ppe_essentials.god.target.disabled", senderName(source));
        }
        return 1;
    }

    private static Component senderName(CommandSourceStack source) {
        ServerPlayer sender = PpeCommandContext.senderPlayer(source);
        return sender == null ? Component.literal("Server") : sender.getName();
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

    private static boolean teleport(CommandSourceStack source, ServerPlayer player, PpeLocation location) {
        Optional<ServerLevel> level = location.resolve(PpeCompat.server(player));
        if (level.isEmpty()) {
            fail(source, "ppe_essentials.teleport.missing_dimension");
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

    private static void replyToOtherSender(CommandSourceStack source, ServerPlayer first, ServerPlayer second,
                                           String key, Object argument) {
        ServerPlayer sender = PpeCommandContext.senderPlayer(source);
        if (sender != first && sender != second) {
            reply(source, key, argument);
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
        String prefix = builder.getRemainingLowerCase();
        for (String name : PpePlayerData.get(context.getSource().getServer()).warpNames()) {
            String quoted = "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix) || quoted.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(quoted);
            }
        }
        return builder.buildFuture();
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

    private static boolean containsRepeat(ParseResults<CommandSourceStack> parsed) {
        var context = parsed.getContext();
        var repeatNode = context.getSource().getServer().getCommands().getDispatcher().getRoot().getChild("repeat");
        for (var branch = context; branch != null; branch = branch.getChild()) {
            for (var node : branch.getNodes()) {
                if (node.getNode() == repeatNode) {
                    return true;
                }
            }
        }
        return false;
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

    private record HelpCommand(String configKey, String display, boolean playerOnly) {
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
