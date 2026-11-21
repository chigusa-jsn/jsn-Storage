package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.storage.StorageInv;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 圧縮：作業台のレシピで「同じ物9こ（または4こ）→ 1こ」になって、
 * しかもその1こから元にもどせる組み合わせだけ使う（鉄・金・ダイヤ・レッドストーン・小麦→干し草 など）。
 */
public final class Compactor {
    record Rule(int n, ItemStack result) {}

    private static final Map<Item, Rule> CACHE = new HashMap<>();
    private static final Rule NONE = new Rule(0, ItemStack.EMPTY);

    static Rule rule(Level level, Item item) {
        return CACHE.computeIfAbsent(item, it -> {
            for (int size : new int[]{3, 2}) {
                ItemStack r = craft(level, new ItemStack(it), size);
                if (r.isEmpty() || r.getCount() != 1) continue;
                // もどせるか：その1こを作業台に置くと、元のアイテムが size×size こ
                ItemStack back = craft(level, r, 1);
                if (back.getItem() == it && back.getCount() == size * size) return new Rule(size * size, r.copyWithCount(1));
            }
            return NONE;
        });
    }

    static ItemStack craft(Level level, ItemStack s, int size) {
        NonNullList<ItemStack> list = NonNullList.withSize(size * size, ItemStack.EMPTY);
        for (int i = 0; i < size * size; i++) list.set(i, s.copyWithCount(1));
        CraftingInput in = CraftingInput.of(size, size, list);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level)
                .map(h -> h.value().assemble(in, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** 登録した物を圧縮する */
    public static void run(Level level, StorageInv inv) {
        ItemStack up = Upgrades.find(inv, UpgradeType.COMPACT);
        if (up.isEmpty()) return;
        for (String id : UpgradeData.filter(up)) {
            Item it = UpgradeData.item(id);
            if (it == null) continue;
            Rule r = rule(level, it);
            if (r.n() == 0) continue;
            int total = 0;
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.getItem() == it && s.getComponentsPatch().isEmpty()) total += s.getCount();
            }
            int k = total / r.n();
            if (k <= 0) continue;
            // 先に取り出して、ブロックを入れる。入りきらなかったら もどす
            int need = k * r.n();
            for (int i = 0; i < inv.getSlots() && need > 0; i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.getItem() == it && s.getComponentsPatch().isEmpty()) need -= inv.extractItem(i, Math.min(need, s.getCount()), false).getCount();
            }
            ItemStack rest = ItemHandlerHelper.insertItemStacked(inv, r.result().copyWithCount(k), false);
            if (!rest.isEmpty()) {
                // 入らなかった分は、元のアイテムにもどす
                ItemHandlerHelper.insertItemStacked(inv, new ItemStack(it, rest.getCount() * r.n()), false);
            }
        }
    }

    /** 登録の画面用：この物は圧縮できる？（サーバーで確かめるまでは わからない） */
    public static boolean can(Level level, Item it) {
        return rule(level, it).n() > 0;
    }

    private Compactor() {}
}
