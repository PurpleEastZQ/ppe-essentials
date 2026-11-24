package me.purpleeast.mods.ppe_essentials;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

public final class PpePlayerData extends PpePlayerDataStore {
    private static final SavedDataType<PpePlayerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PpeEssentials.MODID, "player_data"),
            PpePlayerData::new, PpePlayerDataCodec.CODEC, null);

    public static PpePlayerData get(MinecraftServer server) {
        var storage = server.getDataStorage();
        var current = server.getWorldPath(LevelResource.DATA).resolve(PpeEssentials.MODID).resolve("player_data.dat");
        var migrated = PpePlayerDataMigrations.loadForMigration(server, current, PpePlayerDataMigrations.legacyPath(server));
        if (migrated == null) {
            return storage.computeIfAbsent(TYPE);
        }
        storage.set(TYPE, migrated);
        storage.saveAndJoin();
        return migrated;
    }
}
