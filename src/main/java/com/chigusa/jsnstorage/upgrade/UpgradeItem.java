package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.Kind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** アップグレード（入れ物の左の枠に入れる。右クリックで設定） */
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7" + type.desc));
        StringBuilder where = new StringBuilder("§8つけられる：");
        for (Kind k : Kind.values()) if (type.allowed(k)) where.append(k.name).append(" ");
        tip.add(Component.literal(where.toString()));
        int fs = type.filterSize(rank);
        if (fs > 0) {
            long n = UpgradeData.filter(stack).stream().filter(s -> !s.isEmpty()).count();
            tip.add(Component.literal("§b登録：" + n + " / " + fs + "（入れ物の画面で右クリックして登録）"));
        }
        double cap = type.capacity(rank);
        if (cap > 0) tip.add(Component.literal("§eためている量：" + (int) UpgradeData.stored(stack) + " / " + (int) cap + (type == UpgradeType.XP ? " ポイント" : "")));
        if (type.ranked) tip.add(Component.literal("§8ランク：" + JsnStorage.RANK_NAMES[rank]));
    }
}
