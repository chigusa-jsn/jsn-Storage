package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.storage.Kind;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** アップグレード（入れ物の左の枠に入れる） */
public class UpgradeItem extends Item {
    public final UpgradeType type;
    public final int rank;   // 0=銅 1=金 2=ダイヤ（ランクのないものは0）

    public UpgradeItem(Properties p, UpgradeType type, int rank) {
        super(p);
        this.type = type;
        this.rank = rank;
    }

    public static boolean is(ItemStack s) {
        return s.getItem() instanceof UpgradeItem;
    }

    /** その入れ物に付けられるか */
    public static boolean allowed(ItemStack s, Kind kind) {
        return s.getItem() instanceof UpgradeItem u && u.type.allowed(kind);
    }
}
