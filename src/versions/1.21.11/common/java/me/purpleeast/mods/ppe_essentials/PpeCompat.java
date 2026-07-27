package me.purpleeast.mods.ppe_essentials;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;

import java.util.Set;

public final class PpeCompat {
    private PpeCompat() {
    }

    public static MinecraftServer server(ServerPlayer player) {
        return player.level().getServer();
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.level();
    }

    public static ServerLevel spawnLevel(ServerPlayer player) {
        return server(player).findRespawnDimension();
    }

    public static BlockPos spawnPos(ServerPlayer player) {
        return server(player).getRespawnData().pos();
    }

    public static float spawnAngle(ServerPlayer player) {
        return server(player).getRespawnData().yaw();
    }

    public static String profileName(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    public static void playSound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        level(player).playSound(null, player.getX(), player.getY(), player.getZ(), sound, source, volume, pitch);
    }

    public static void kill(ServerPlayer player) {
        player.kill(level(player));
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        player.teleportTo(level, x, y, z, Set.of(), yRot, xRot, false);
    }

    public static CommandSourceStack withPermission(CommandSourceStack source, int level) {
        return source.withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level)));
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return source.permissions().hasPermission(commandPermission(level));
    }

    public static boolean hasPermission(ServerPlayer player, int level) {
        return player.permissions().hasPermission(commandPermission(level));
    }

    public static void setMayFly(ServerPlayer player, boolean mayFly) {
        Abilities abilities = player.getAbilities();
        Abilities.Packed current = abilities.pack();
        abilities.apply(new Abilities.Packed(
                current.invulnerable(),
                mayFly && current.flying(),
                mayFly,
                current.instabuild(),
                current.mayBuild(),
                current.flyingSpeed(),
                current.walkingSpeed()
        ));
        player.onUpdateAbilities();
    }

    public static boolean isFlying(ServerPlayer player) {
        return player.getAbilities().pack().flying();
    }

    public static void restoreFlight(ServerPlayer player, boolean wasFlying) {
        restoreFlight(player, wasFlying, true);
    }

    public static void restoreFlightSilently(ServerPlayer player, boolean wasFlying) {
        restoreFlight(player, wasFlying, false);
    }

    private static void restoreFlight(ServerPlayer player, boolean wasFlying, boolean sync) {
        Abilities abilities = player.getAbilities();
        Abilities.Packed current = abilities.pack();
        abilities.apply(new Abilities.Packed(
                current.invulnerable(),
                wasFlying || current.flying(),
                true,
                current.instabuild(),
                current.mayBuild(),
                current.flyingSpeed(),
                current.walkingSpeed()
        ));
        if (sync) {
            player.onUpdateAbilities();
        }
    }

    public static ClickEvent runCommandClick(String command) {
        return new ClickEvent.RunCommand(command);
    }

    public static ClickEvent suggestCommandClick(String command) {
        return new ClickEvent.SuggestCommand(command);
    }

    public static HoverEvent showTextHover(Component text) {
        return new HoverEvent.ShowText(text);
    }

    private static Permission commandPermission(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }
}
