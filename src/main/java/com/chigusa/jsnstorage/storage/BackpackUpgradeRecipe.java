package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.JsnStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 作業台：リュック＋強化キット → 1つ上のランクのリュック（中身はそのまま） */
public class BackpackUpgradeRecipe extends CustomRecipe {
    public BackpackUpgradeRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    private static ItemStack[] find(CraftingInput in) {
        ItemStack bag = ItemStack.EMPTY, kit = ItemStack.EMPTY;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BackpackItem && bag.isEmpty()) bag = s;
            else if (s.getItem() instanceof KitItem && kit.isEmpty()) kit = s;
            else return null;
        }
        if (bag.isEmpty() || kit.isEmpty()) return null;
        if (((BackpackItem) bag.getItem()).tier != ((KitItem) kit.getItem()).from) return null;
        return new ItemStack[]{bag, kit};
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        return find(in) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider reg) {
        ItemStack[] f = find(in);
        if (f == null) return ItemStack.EMPTY;
        Tier to = ((KitItem) f[1].getItem()).to;
        ItemStack out = new ItemStack(JsnStorage.backpack(to));
        out.applyComponents(f[0].getComponentsPatch());
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return JsnStorage.BACKPACK_UPGRADE.get();
    }
}
