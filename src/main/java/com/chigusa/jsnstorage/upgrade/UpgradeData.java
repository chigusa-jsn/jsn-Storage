package com.chigusa.jsnstorage.upgrade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** アップグレードの設定（登録したアイテム・ためた量）はアップグレードのアイテムに書いておく */
public final class UpgradeData {
    static CompoundTag tag(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    static void put(ItemStack s, CompoundTag t) {
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** 登録した枠（空は ""） */
    public static List<String> filter(ItemStack s) {
        List<String> out = new ArrayList<>();
        ListTag l = tag(s).getList("filter", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    public static void setFilter(ItemStack s, int cell, String id) {
        CompoundTag t = tag(s);
        ListTag l = t.getList("filter", Tag.TAG_STRING);
        while (l.size() <= cell) l.add(StringTag.valueOf(""));
        l.set(cell, StringTag.valueOf(id));
        t.put("filter", l);
        put(s, t);
    }

    public static boolean matches(ItemStack upgrade, ItemStack item) {
        if (item.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        for (String f : filter(upgrade)) if (f.equals(id)) return true;
        return false;
    }

    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || id.isEmpty() ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    public static double stored(ItemStack s) {
        return tag(s).getDouble("stored");
    }

    public static void setStored(ItemStack s, double v) {
        CompoundTag t = tag(s);
        t.putDouble("stored", Math.max(0, v));
        put(s, t);
    }

    private UpgradeData() {}
}
