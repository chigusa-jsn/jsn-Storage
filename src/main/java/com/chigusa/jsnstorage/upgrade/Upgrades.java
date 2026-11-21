package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.storage.StorageInv;
import net.minecraft.world.item.ItemStack;

/** 入っているアップグレードを調べる */
public final class Upgrades {
    public static ItemStack find(StorageInv inv, UpgradeType t) {
        for (int i = 0; i < inv.tier.upgrades && i < inv.upgrades.getSlots(); i++) {
            ItemStack s = inv.upgrades.getStackInSlot(i);
            if (s.getItem() instanceof UpgradeItem u && u.type == t) return s;
        }
        return ItemStack.EMPTY;
    }

    /** 容量アップ：銅2倍・金4倍・ダイヤ8倍 */
    public static int stackMultiplier(StorageInv inv) {
        // いくつも付けたら倍率を足し算（ダイヤ2つで16倍）
        int sum = 0;
        for (int i = 0; i < inv.upgrades.getSlots(); i++) {
            ItemStack s = inv.upgrades.getStackInSlot(i);
            if (s.getItem() instanceof UpgradeItem u && u.type == UpgradeType.STACK) sum += UpgradeType.stackFactor(u.rank);
        }
        return Math.max(1, sum);
    }

    private Upgrades() {}
}
