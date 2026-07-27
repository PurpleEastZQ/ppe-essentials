package me.purpleeast.mods.ppe_essentials;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record PpeLocation(ResourceKey<Level> level, double x, double y, double z, float yRot, float xRot) {
    public static PpeLocation of(ServerPlayer player) {
        return new PpeLocation(PpeCompat.level(player).dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("level", level.location().toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yRot", yRot);
        tag.putFloat("xRot", xRot);
        return tag;
    }

    public static PpeLocation load(CompoundTag tag) {
        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("level")));
        return new PpeLocation(level, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yRot"), tag.getFloat("xRot"));
    }

    public Optional<ServerLevel> resolve(MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(level));
    }
}
