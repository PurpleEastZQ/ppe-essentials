package me.purpleeast.mods.ppe_essentials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

final class PpePlayerDataMigrations {
    static final int CURRENT_SCHEMA_VERSION = 2;
    static final String SCHEMA_VERSION_KEY = "schemaVersion";
    private static final Set<MinecraftServer> CHECKED_SERVERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private PpePlayerDataMigrations() {
    }

    static synchronized boolean backupIfNeeded(MinecraftServer server) {
        Path dataPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(PpeEssentials.MODID + "_player_data.dat");
        if (CHECKED_SERVERS.contains(server)) {
            return false;
        }
        if (!Files.isRegularFile(dataPath)) {
            CHECKED_SERVERS.add(server);
            return false;
        }

        try {
            CompoundTag payload = NbtIo.readCompressed(dataPath, NbtAccounter.unlimitedHeap()).getCompound("data");
            int version = payload.contains(SCHEMA_VERSION_KEY, Tag.TAG_ANY_NUMERIC)
                    ? payload.getInt(SCHEMA_VERSION_KEY)
                    : 0;
            if (version < CURRENT_SCHEMA_VERSION) {
                backup(dataPath);
                CHECKED_SERVERS.add(server);
                return true;
            }
            CHECKED_SERVERS.add(server);
            return false;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to back up PPE player data before migration: " + dataPath, exception);
        }
    }

    static void awaitSaved(MinecraftServer server) {
        Path dataPath = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(PpeEssentials.MODID + "_player_data.dat");
        long deadline = System.nanoTime() + 5_000_000_000L;
        IOException lastException = null;
        while (System.nanoTime() < deadline) {
            try {
                CompoundTag payload = NbtIo.readCompressed(dataPath, NbtAccounter.unlimitedHeap()).getCompound("data");
                int version = payload.contains(SCHEMA_VERSION_KEY, Tag.TAG_ANY_NUMERIC)
                        ? payload.getInt(SCHEMA_VERSION_KEY)
                        : 0;
                if (version == CURRENT_SCHEMA_VERSION) {
                    return;
                }
            } catch (IOException exception) {
                lastException = exception;
            }

            try {
                Thread.sleep(10L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while verifying migrated PPE player data: " + dataPath, exception);
            }
        }
        throw new IllegalStateException("Timed out waiting for migrated PPE player data to be saved: " + dataPath, lastException);
    }

    static boolean migrate(CompoundTag tag) {
        int version = tag.contains(SCHEMA_VERSION_KEY, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(SCHEMA_VERSION_KEY)
                : 0;
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported PPE player data schema version: " + version);
        }

        boolean migrated = false;
        while (version < CURRENT_SCHEMA_VERSION) {
            switch (version) {
                case 0 -> migrateV0ToV1(tag);
                case 1 -> migrateV1ToV2(tag);
                default -> throw new IllegalStateException("Missing PPE player data migration for schema version: " + version);
            }
            version++;
            migrated = true;
        }
        return migrated;
    }

    private static void backup(Path dataPath) throws IOException {
        Path backupPath = dataPath.resolveSibling(dataPath.getFileName() + ".bak");
        Files.copy(
                dataPath,
                backupPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
        );
        PpeEssentials.LOGGER.info("Backed up PPE Essentials player data from {} to {}", dataPath, backupPath);
    }

    private static void migrateV0ToV1(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_KEY, 1);
    }

    private static void migrateV1ToV2(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_KEY, 2);
    }
}
