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

    public void markDirty() {
        dirty.run();
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

    /** 「いらない物を消す」に登録した物は、入れたとたんに消える */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!stack.isEmpty() && voids(stack)) return ItemStack.EMPTY;
        return super.insertItem(slot, stack, simulate);
    }

    public boolean voids(ItemStack stack) {
        ItemStack v = com.chigusa.jsnstorage.upgrade.Upgrades.find(this, com.chigusa.jsnstorage.upgrade.UpgradeType.VOID);
        return !v.isEmpty() && com.chigusa.jsnstorage.upgrade.UpgradeData.matches(v, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64 * stackMultiplier;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize() * stackMultiplier);
    }

    /** このマスに、この物を何こまで入れられるか（画面のマス用） */
    public int limitFor(int slot, ItemStack stack) {
        return getStackLimit(slot, stack);
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

    /**
     * 保存。容量アップで 99 こを超えて入っていると、マイクラ標準の保存（1〜99 まで）では保存できず落ちるので、
     * 種類は 1 こ分で保存して、数は別の欄（Count）に入れる。
     */
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider reg) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s.isEmpty()) continue;
            CompoundTag it = new CompoundTag();
            it.putInt("Slot", i);
            it.putInt("Count", s.getCount());
            it.put("Item", s.copyWithCount(1).save(reg));
            list.add(it);
        }
        CompoundTag t = new CompoundTag();
        t.put("Items", list);
        t.putInt("Size", stacks.size());
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider reg, CompoundTag t) {
        setSize(t.contains("Size", net.minecraft.nbt.Tag.TAG_INT) ? t.getInt("Size") : stacks.size());
        net.minecraft.nbt.ListTag list = t.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag it = list.getCompound(i);
            int slot = it.getInt("Slot");
            if (slot < 0 || slot >= stacks.size()) continue;
            ItemStack s;
            if (it.contains("Item")) {
                // 新しい形：種類は1こ分、数は Count
                s = ItemStack.parse(reg, it.getCompound("Item")).orElse(ItemStack.EMPTY);
                if (!s.isEmpty()) s.setCount(Math.max(1, it.getInt("Count")));
            } else {
                // 前の形（ItemStackHandler の標準）
                s = ItemStack.parse(reg, it).orElse(ItemStack.EMPTY);
            }
            stacks.set(slot, s);
        }
        onLoad();
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
