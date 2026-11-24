package me.purpleeast.mods.ppe_essentials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record PpeLocation(ResourceKey<Level> level, double x, double y, double z, float yRot, float xRot) {
    static final Codec<PpeLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter(PpeLocation::level),
            Codec.DOUBLE.fieldOf("x").forGetter(PpeLocation::x),
            Codec.DOUBLE.fieldOf("y").forGetter(PpeLocation::y),
            Codec.DOUBLE.fieldOf("z").forGetter(PpeLocation::z),
            Codec.FLOAT.fieldOf("yRot").forGetter(PpeLocation::yRot),
            Codec.FLOAT.fieldOf("xRot").forGetter(PpeLocation::xRot)
    ).apply(instance, PpeLocation::new));

    public static PpeLocation of(ServerPlayer player) {
        return new PpeLocation(PpeCompat.level(player).dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public Optional<ServerLevel> resolve(MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(level));
    }
}
