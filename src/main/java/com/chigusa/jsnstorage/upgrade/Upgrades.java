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
        ItemStack s = find(inv, UpgradeType.STACK);
        return s.getItem() instanceof UpgradeItem u ? 2 << u.rank : 1;
    }

    private Upgrades() {}
}
