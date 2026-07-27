package me.purpleeast.mods.ppe_essentials;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class PpePlayerDataLegacyImporter {
    private static final String LEGACY_FILE_NAME = PpeEssentials.MODID + "_player_data.dat";

    private PpePlayerDataLegacyImporter() {
    }

    static Path currentDataPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.DATA)
                .resolve(PpeEssentials.MODID)
                .resolve("player_data.dat");
    }

    static ImportResult importLegacyData(MinecraftServer server) {
        if (Files.exists(currentDataPath(server))) {
            return null;
        }

        PpePlayerData merged = new PpePlayerData();
        List<Path> importedPaths = new ArrayList<>();
        for (Path legacyPath : legacyPaths(server)) {
            if (!Files.isRegularFile(legacyPath)) {
                continue;
            }

            try {
                CompoundTag root = NbtIo.readCompressed(legacyPath, NbtAccounter.unlimitedHeap());
                CompoundTag payload = root.getCompound("data")
                        .orElseThrow(() -> new IOException("Missing data compound"));
                PpePlayerData legacyData = PpePlayerData.load(payload);
                Path backupPath = backup(legacyPath);
                mergeInto(merged, legacyData);
                importedPaths.add(legacyPath);
                PpeEssentials.LOGGER.info(
                        "Imported PPE Essentials player data from {} after backing it up to {}",
                        legacyPath,
                        backupPath
                );
            } catch (IOException | RuntimeException exception) {
                PpeEssentials.LOGGER.error("Failed to import PPE Essentials player data from {}", legacyPath, exception);
            }
        }

        if (importedPaths.isEmpty()) {
            return null;
        }

        merged.setDirty();
        return new ImportResult(merged, List.copyOf(importedPaths));
    }

    static void deleteLegacyData(ImportResult result) {
        for (Path legacyPath : result.legacyPaths()) {
            try {
                Files.deleteIfExists(legacyPath);
                PpeEssentials.LOGGER.info(
                        "Deleted migrated PPE Essentials player data from {}; backup remains at {}",
                        legacyPath,
                        legacyPath.resolveSibling(legacyPath.getFileName() + ".bak")
                );
            } catch (IOException exception) {
                PpeEssentials.LOGGER.error("Failed to delete migrated PPE Essentials player data from {}", legacyPath, exception);
            }
        }
    }

    private static List<Path> legacyPaths(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        return List.of(
                server.getWorldPath(LevelResource.DATA).resolve(LEGACY_FILE_NAME),
                worldRoot.resolve("dimensions")
                        .resolve("minecraft")
                        .resolve("overworld")
                        .resolve("data")
                        .resolve("minecraft")
                        .resolve(LEGACY_FILE_NAME)
        );
    }

    private static Path backup(Path legacyPath) throws IOException {
        Path backupPath = legacyPath.resolveSibling(legacyPath.getFileName() + ".bak");
        return Files.copy(
                legacyPath,
                backupPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
        );
    }

    private static void mergeInto(PpePlayerData target, PpePlayerData source) {
        target.homes.putAll(source.homes);
        target.deathBacks.putAll(source.deathBacks);
        target.teleportBacks.putAll(source.teleportBacks);
        target.warps.putAll(source.warps);
        target.tpaAuto.addAll(source.tpaAuto);
        target.fly.addAll(source.fly);
        target.god.addAll(source.god);
        target.backNotice.addAll(source.backNotice);
        target.firstJoinNotice.addAll(source.firstJoinNotice);
    }

    record ImportResult(PpePlayerData data, List<Path> legacyPaths) {
    }
}
