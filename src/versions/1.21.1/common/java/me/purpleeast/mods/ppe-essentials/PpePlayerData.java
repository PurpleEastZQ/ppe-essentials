package me.purpleeast.mods.ppe_essentials;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PpePlayerData extends PpePlayerDataStore {
    private static final String NAME = PpeEssentials.MODID + "_player_data";

    public static PpePlayerData get(MinecraftServer server) {
        boolean migrationNeeded = PpePlayerDataMigrations.backupIfNeeded(server);
        var storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        PpePlayerData data = storage.computeIfAbsent(
                new SavedData.Factory<>(PpePlayerData::new, PpePlayerData::load, null),
                NAME
        );
        if (migrationNeeded) {
            data.setDirty();
            storage.save();
            PpePlayerDataMigrations.awaitSaved(server);
            PpeEssentials.LOGGER.info(
                    "Migrated PPE Essentials player data to schema version {}",
                    PpePlayerDataMigrations.CURRENT_SCHEMA_VERSION
            );
        }
        return data;
    }

    private static PpePlayerData load(CompoundTag tag, HolderLookup.Provider provider) {
        boolean migrated = PpePlayerDataMigrations.migrate(tag);
        PpePlayerData data = new PpePlayerData();
        data.readLocationMap(tag.getList("homes", Tag.TAG_COMPOUND), data.homes);
        data.readLocationMap(tag.getList("deathBacks", Tag.TAG_COMPOUND), data.deathBacks);
        data.readLocationMap(tag.getList("teleportBacks", Tag.TAG_COMPOUND), data.teleportBacks);
        data.readWarpMap(tag.getList("warps", Tag.TAG_COMPOUND), data.warps);

        data.readUuidSet(tag.getList("tpaAuto", Tag.TAG_COMPOUND), data.tpaAuto);
        data.readUuidSet(tag.getList("fly", Tag.TAG_COMPOUND), data.fly);
        data.readUuidSet(tag.getList("god", Tag.TAG_COMPOUND), data.god);
        data.readUuidSet(tag.getList("backNotice", Tag.TAG_COMPOUND), data.backNotice);
        data.readUuidSet(tag.getList("firstJoinNotice", Tag.TAG_COMPOUND), data.firstJoinNotice);
        if (migrated) {
            data.setDirty();
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(PpePlayerDataMigrations.SCHEMA_VERSION_KEY, PpePlayerDataMigrations.CURRENT_SCHEMA_VERSION);
        tag.put("homes", writeLocationMap(homes));
        tag.put("deathBacks", writeLocationMap(deathBacks));
        tag.put("teleportBacks", writeLocationMap(teleportBacks));
        tag.put("warps", writeWarpMap(warps));

        tag.put("tpaAuto", writeUuidSet(tpaAuto));
        tag.put("fly", writeUuidSet(fly));
        tag.put("god", writeUuidSet(god));
        tag.put("backNotice", writeUuidSet(backNotice));
        tag.put("firstJoinNotice", writeUuidSet(firstJoinNotice));
        return tag;
    }

    private void readLocationMap(ListTag list, Map<UUID, PpeLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            target.put(entry.getUUID("uuid"), PpeLocation.load(entry.getCompound("location")));
        }
    }

    private ListTag writeLocationMap(Map<UUID, PpeLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PpeLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readWarpMap(ListTag list, Map<String, PpeLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            target.put(entry.getString("name"), PpeLocation.load(entry.getCompound("location")));
        }
    }

    private ListTag writeWarpMap(Map<String, PpeLocation> source) {
        ListTag list = new ListTag();
        for (Map.Entry<String, PpeLocation> item : source.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", item.getKey());
            entry.put("location", item.getValue().save());
            list.add(entry);
        }
        return list;
    }

    private void readUuidSet(ListTag list, Set<UUID> target) {
        for (int i = 0; i < list.size(); i++) {
            target.add(list.getCompound(i).getUUID("uuid"));
        }
    }

    private ListTag writeUuidSet(Set<UUID> source) {
        ListTag list = new ListTag();
        for (UUID uuid : source) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            list.add(entry);
        }
        return list;
    }
}
