package me.purpleeast.mods.ppe_essentials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

final class PpePlayerDataMigrations {
    private static final Set<MinecraftServer> CHECKED = Collections.newSetFromMap(new WeakHashMap<>());

    private PpePlayerDataMigrations() {
    }

    static Path legacyPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(PpeEssentials.MODID + "_player_data.dat");
    }

    static synchronized PpePlayerData loadForMigration(MinecraftServer server, Path current, Path legacy) {
        if (CHECKED.contains(server)) {
            return null;
        }

        boolean importing = Files.notExists(current) && legacy != null;
        Path source = importing ? legacy : current;
        if (Files.notExists(source)) {
            CHECKED.add(server);
            return null;
        }

        try {
            var file = NbtIo.readCompressed(source, NbtAccounter.unlimitedHeap());
            if (!(file.get("data") instanceof CompoundTag payload)) {
                throw new IOException("Missing player data compound");
            }
            PpePlayerData data = PpePlayerDataCodec.decode(payload);
            if (importing || data.isDirty()) {
                if (!importing) {
                    Path backup = source.resolveSibling(source.getFileName() + ".bak");
                    if (Files.notExists(backup)) {
                        Files.copy(source, backup);
                    }
                } else {
                    PpeEssentials.LOGGER.info("Importing PPE player data from {}", source);
                }
                data.setDirty();
                CHECKED.add(server);
                return data;
            }
            CHECKED.add(server);
            return null;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to read PPE player data: " + source, exception);
        }
    }
}
