package me.purpleeast.mods.ppe_essentials;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class PpePlayerData extends PpePlayerDataStore {
    private static final String NAME = PpeEssentials.MODID + "_player_data";
    private static final SavedData.Factory<PpePlayerData> FACTORY = new SavedData.Factory<>(
            PpePlayerData::new, (tag, provider) -> PpePlayerDataCodec.decode(tag), null);

    public static PpePlayerData get(MinecraftServer server) {
        var storage = server.overworld().getDataStorage();
        var migration = PpePlayerDataMigrations.loadForMigration(server, PpePlayerDataMigrations.legacyPath(server), null);
        if (migration == null) {
            return storage.computeIfAbsent(FACTORY, NAME);
        }
        var migrated = migration.data();
        storage.set(NAME, migrated);
        storage.save();
        return migrated;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return tag.merge((CompoundTag) PpePlayerDataCodec.CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow());
    }
}
