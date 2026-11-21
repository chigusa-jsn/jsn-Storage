package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.JsnStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 入れ物の中身（チェスト・シュルカー・リュックで共通）。
 * 中身はワールドのデータ（StorageData）に、入れ物ごとのIDで保存する。
 */
public class StorageInv extends ItemStackHandler {
    public Tier tier;
    public final Kind kind;
    public final ItemStackHandler upgrades;
    private Runnable dirty = () -> {};
    /** 容量アップの倍率（アップグレードで変わる） */
    public int stackMultiplier = 1;

    public StorageInv(Tier tier, Kind kind) {
        super(tier.slots);
        this.tier = tier;
        this.kind = kind;
        this.upgrades = new ItemStackHandler(Tier.NETHERITE.upgrades) {
            @Override
            protected void onContentsChanged(int slot) {
                refreshUpgrades();
                dirty.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < StorageInv.this.tier.upgrades && com.chigusa.jsnstorage.upgrade.UpgradeItem.is(stack) && StorageInv.this.canAddUpgrade(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    public void onDirty(Runnable r) {
        this.dirty = r;
    }

    @Override
    protected void onContentsChanged(int slot) {
        dirty.run();
    }

    /** 入れていい物（シュルカーとリュックには、シュルカーやリュックは入れられない） */
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (kind == Kind.CHEST) return true;
        return !isContainer(stack);
    }

    public static boolean isContainer(ItemStack s) {
        if (s.getItem() instanceof BackpackItem) return true;
        if (s.getItem() instanceof BlockItem bi) {
            return bi.getBlock() instanceof ShulkerBoxBlock || (bi.getBlock() instanceof StorageBlock sb && sb.kind == Kind.SHULKER);
        }
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64 * stackMultiplier;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize() * stackMultiplier);
    }

    /** 同じ種類のアップグレードは1つまで */
    public boolean canAddUpgrade(int slot, ItemStack stack) {
        for (int i = 0; i < upgrades.getSlots(); i++)
            if (i != slot && !upgrades.getStackInSlot(i).isEmpty() && upgrades.getStackInSlot(i).getItem() == stack.getItem()) return false;
        return com.chigusa.jsnstorage.upgrade.UpgradeItem.allowed(stack, kind);
    }

    public void refreshUpgrades() {
        stackMultiplier = com.chigusa.jsnstorage.upgrade.Upgrades.stackMultiplier(this);
    }

    /** ランクを上げる（中身はそのまま、マスが増える） */
    public void resize(Tier t) {
        if (t.slots > stacks.size()) {
            var old = stacks;
            setSize(t.slots);
            for (int i = 0; i < old.size(); i++) stacks.set(i, old.get(i));
        }
        tier = t;
        dirty.run();
    }

    public CompoundTag save(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        t.putString("tier", tier.id);
        t.putString("kind", kind.name());
        t.put("items", serializeNBT(reg));
        t.put("upgrades", upgrades.serializeNBT(reg));
        return t;
    }

    public static StorageInv load(CompoundTag t, HolderLookup.Provider reg) {
        Tier tier = Tier.WOOD;
        for (Tier x : Tier.values()) if (x.id.equals(t.getString("tier"))) tier = x;
        Kind kind;
        try {
            kind = Kind.valueOf(t.getString("kind"));
        } catch (Exception e) {
            kind = Kind.CHEST;
        }
        StorageInv inv = new StorageInv(tier, kind);
        inv.deserializeNBT(reg, t.getCompound("items"));
        if (inv.getSlots() < tier.slots) inv.resize(tier);
        inv.upgrades.deserializeNBT(reg, t.getCompound("upgrades"));
        if (inv.upgrades.getSlots() < Tier.NETHERITE.upgrades) {
            JsnStorage.LOG.warn("アップグレードの枠の数がおかしい");
        }
        inv.refreshUpgrades();
        return inv;
    }
}
