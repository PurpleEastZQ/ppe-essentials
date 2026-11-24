package me.purpleeast.mods.ppe_essentials;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PpePlayerDataCodec {
    private static final int SCHEMA_VERSION = 2;
    private static final Codec<Integer> SCHEMA = Codec.INT.validate(version -> version >= 0 && version <= SCHEMA_VERSION
            ? DataResult.success(version)
            : DataResult.error(() -> "Unsupported PPE player data schema: " + version));
    private static final Codec<Map<UUID, PpeLocation>> PLAYER_LOCATIONS = locations(UUIDUtil.LENIENT_CODEC, "uuid");
    private static final Codec<Map<String, PpeLocation>> WARPS = locations(Codec.STRING, "name");
    private static final Codec<Set<UUID>> PLAYERS = UUIDUtil.LENIENT_CODEC.fieldOf("uuid").codec().listOf()
            .xmap(HashSet::new, List::copyOf);

    static final Codec<PpePlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SCHEMA.optionalFieldOf("schemaVersion", 0).forGetter(data -> SCHEMA_VERSION),
            PLAYER_LOCATIONS.optionalFieldOf("homes", Map.of()).forGetter(data -> data.homes),
            PLAYER_LOCATIONS.optionalFieldOf("deathBacks", Map.of()).forGetter(data -> data.deathBacks),
            PLAYER_LOCATIONS.optionalFieldOf("teleportBacks", Map.of()).forGetter(data -> data.teleportBacks),
            WARPS.optionalFieldOf("warps", Map.of()).forGetter(data -> data.warps),
            PLAYERS.optionalFieldOf("tpaAuto", Set.of()).forGetter(data -> data.tpaAuto),
            PLAYERS.optionalFieldOf("fly", Set.of()).forGetter(data -> data.fly),
            PLAYERS.optionalFieldOf("god", Set.of()).forGetter(data -> data.god),
            PLAYERS.optionalFieldOf("backNotice", Set.of()).forGetter(data -> data.backNotice),
            PLAYERS.optionalFieldOf("firstJoinNotice", Set.of()).forGetter(data -> data.firstJoinNotice)
    ).apply(instance, (version, homes, deathBacks, teleportBacks, warps, tpaAuto, fly, god, backNotice, firstJoinNotice) -> {
        var data = new PpePlayerData();
        data.homes.putAll(homes);
        data.deathBacks.putAll(deathBacks);
        data.teleportBacks.putAll(teleportBacks);
        data.warps.putAll(warps);
        data.tpaAuto.addAll(tpaAuto);
        data.fly.addAll(fly);
        data.god.addAll(god);
        data.backNotice.addAll(backNotice);
        data.firstJoinNotice.addAll(firstJoinNotice);
        data.setDirty(version < SCHEMA_VERSION);
        return data;
    }));

    private PpePlayerDataCodec() {
    }

    static PpePlayerData decode(Tag payload) {
        return CODEC.parse(NbtOps.INSTANCE, payload).getOrThrow();
    }

    private static <K> Codec<Map<K, PpeLocation>> locations(Codec<K> key, String keyName) {
        return Codec.mapPair(key.fieldOf(keyName), PpeLocation.CODEC.fieldOf("location")).codec().listOf()
                .xmap(entries -> {
                    Map<K, PpeLocation> locations = new HashMap<>();
                    entries.forEach(entry -> locations.put(entry.getFirst(), entry.getSecond()));
                    return locations;
                }, locations -> locations.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList());
    }
}
