package me.purpleeast.mods.ppe_essentials;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Set;
import java.util.UUID;

/**
 * Isolates Minecraft 26.1.2 API calls from the shared command and event code.
 */
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
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                source,
                player.getX(),
                player.getY(),
                player.getZ(),
                volume,
                pitch,
                player.getRandom().nextLong()
        ));
    }

    public static void kill(ServerPlayer player) {
        player.kill(level(player));
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        player.teleportTo(level, x, y, z, Set.of(), yRot, xRot, false);
    }

    static ServerPlayer senderPlayer(CommandSourceStack source, CommandSource sender) {
        ServerPlayer executor = source.getPlayer();
        if (executor != null && executor.commandSource() == sender) {
            return executor;
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (player.commandSource() == sender) {
                return player;
            }
        }
        return null;
    }

    public static CommandSourceStack withPermission(CommandSourceStack source, int level) {
        return source.withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level)));
    }

    public static boolean hasPermission(CommandSourceStack source, int level) {
        return level <= 0 || source.permissions().hasPermission(commandPermission(level));
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

    public static void openTeleportMenu(ServerPlayer player, boolean here, int page) {
        PpeTeleportMenu.open(player, here, page);
    }

    public static Item teleportMenuDivider() {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("gray_stained_glass_pane"));
    }

    public static void setTexturedHeadProfile(ItemStack head, UUID id, String value) {
        var properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", value)));
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(id, "PPE", properties)));
    }

    public static void setPlayerHeadProfile(ItemStack head, ServerPlayer player) {
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
    }

    private static Permission commandPermission(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }
}
