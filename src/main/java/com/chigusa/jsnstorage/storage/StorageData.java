package com.chigusa.jsnstorage.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** すべての入れ物の中身（ワールドに保存） */
public class StorageData extends SavedData {
    private final Map<UUID, StorageInv> map = new HashMap<>();
    private CompoundTag pending;
    private HolderLookup.Provider reg;

    public static StorageData get(MinecraftServer s) {
        StorageData d = s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(StorageData::new, StorageData::load), "jsn_storage");
        d.reg = s.registryAccess();
        return d;
    }

    /** 中身をとり出す（なければ作る。ランクが上がっていたら広げる） */
    public StorageInv inv(UUID id, Tier tier, Kind kind) {
        StorageInv inv = map.get(id);
        if (inv == null && pending != null && pending.contains(id.toString())) {
            inv = StorageInv.load(pending.getCompound(id.toString()), reg);
            pending.remove(id.toString());
            map.put(id, inv);
        }
        if (inv == null) {
            inv = new StorageInv(tier, kind);
            map.put(id, inv);
            setDirty();
        }
        if (inv.tier.ordinal() < tier.ordinal()) inv.resize(tier);
        inv.onDirty(this::setDirty);
        return inv;
    }

    public boolean has(UUID id) {
        return map.containsKey(id) || (pending != null && pending.contains(id.toString()));
    }

    public void remove(UUID id) {
        map.remove(id);
        if (pending != null) pending.remove(id.toString());
        setDirty();
    }

    public Map<UUID, StorageInv> loaded() {
        return map;
    }

    static StorageData load(CompoundTag t, HolderLookup.Provider reg) {
        StorageData d = new StorageData();
        d.pending = t.copy();   // 使うときに読みこむ（はじめに全部読むと重いので）
        d.reg = reg;
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
        if (pending != null) for (String k : pending.getAllKeys()) t.put(k, pending.get(k));
        map.forEach((k, v) -> t.put(k.toString(), v.save(reg)));
        return t;
    }
}
