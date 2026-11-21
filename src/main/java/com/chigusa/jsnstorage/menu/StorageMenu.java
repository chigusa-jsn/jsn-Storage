package com.chigusa.jsnstorage.menu;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.BackpackItem;
import com.chigusa.jsnstorage.storage.Kind;
import com.chigusa.jsnstorage.storage.StorageBlockEntity;
import com.chigusa.jsnstorage.storage.StorageInv;
import com.chigusa.jsnstorage.storage.Tier;
import com.chigusa.jsnstorage.upgrade.UpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 入れ物の画面：左にアップグレードの枠、まんなかに中身、下に持ち物 */
public class StorageMenu extends AbstractContainerMenu {
    public record Source(BlockPos pos, UUID backpack, int slot) {
        public static Source block(BlockPos p) {
            return new Source(p, null, -1);
        }

        public static Source backpack(UUID id, int slot) {
            return new Source(null, id, slot);
        }
    }

    public final StorageInv inv;
    public final Tier tier;
    public final Kind kind;
    public final Source source;
    public final int lockedSlot;
    public final int storageX = 34, storageY = 18, invX, invY;
    public final int width, height;

    public static void writeOpen(RegistryFriendlyByteBuf buf, Tier t, Kind k, int lockedSlot) {
        buf.writeVarInt(t.ordinal());
        buf.writeVarInt(k.ordinal());
        buf.writeVarInt(lockedSlot + 1);
    }

    /** 画面側 */
    public static StorageMenu client(int id, Inventory pinv, RegistryFriendlyByteBuf buf) {
        Tier t = Tier.values()[buf.readVarInt()];
        Kind k = Kind.values()[buf.readVarInt()];
        int locked = buf.readVarInt() - 1;
        return new StorageMenu(id, pinv, new StorageInv(t, k), k == Kind.BACKPACK ? Source.backpack(null, locked) : Source.block(BlockPos.ZERO));
    }

    public StorageMenu(int id, Inventory pinv, StorageInv inv, Source source) {
        super(JsnStorage.STORAGE_MENU.get(), id);
        this.inv = inv;
        this.tier = inv.tier;
        this.kind = inv.kind;
        this.source = source;
        this.lockedSlot = source.slot();
        int cols = tier.cols, rows = tier.rows();
        for (int i = 0; i < tier.slots; i++)
            addSlot(new StorageSlot(inv, i, storageX + (i % cols) * 18, storageY + (i / cols) * 18));
        for (int j = 0; j < tier.upgrades; j++)
            addSlot(new SlotItemHandler(inv.upgrades, j, 8, storageY + j * 22));
        invX = storageX + (cols * 18 - 162) / 2;
        invY = storageY + rows * 18 + 14;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new LockSlot(pinv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new LockSlot(pinv, c, invX + c * 18, invY + 58));
        width = storageX + cols * 18 + 8;
        height = invY + 58 + 18 + 8;
        if (source.pos() != null && !pinv.player.level().isClientSide
                && pinv.player.level().getBlockEntity(source.pos()) instanceof StorageBlockEntity be) {
            be.changeOpeners(1);
        }
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (source.pos() != null && !p.level().isClientSide
                && p.level().getBlockEntity(source.pos()) instanceof StorageBlockEntity be) {
            be.changeOpeners(-1);
        }
    }

    /**
     * 入れ物のマス。容量アップで 64 こより多く重ねられるようにする
     * （マイクラ標準のマスは、手で置くときも Shift クリックのときも 64 こで止まってしまう）
     */
    static class StorageSlot extends SlotItemHandler {
        private final StorageInv inv;

        StorageSlot(StorageInv inv, int index, int x, int y) {
            super(inv, index, x, y);
            this.inv = inv;
        }

        @Override
        public int getMaxStackSize() {
            return inv.getSlotLimit(getSlotIndex());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return inv.limitFor(getSlotIndex(), stack);
        }

        /** 取り出すときは、ふつうの1スタック分まで（手に 64 こより多く持たないように） */
        @Override
        public java.util.Optional<ItemStack> tryRemove(int count, int decrement, Player p) {
            return super.tryRemove(Math.min(count, getItem().getMaxStackSize()), Math.min(decrement, getItem().getMaxStackSize()), p);
        }
    }

    /** 開いているリュックは動かせない */
    class LockSlot extends Slot {
        LockSlot(Inventory inv, int idx, int x, int y) {
            super(inv, idx, x, y);
        }

        @Override
        public boolean mayPickup(Player p) {
            return getContainerSlot() != lockedSlot && super.mayPickup(p);
        }

        @Override
        public boolean mayPlace(ItemStack s) {
            return getContainerSlot() != lockedSlot && super.mayPlace(s);
        }
    }

    int storageEnd() {
        return tier.slots;
    }

    int upgradeEnd() {
        return tier.slots + tier.upgrades;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        // 数字キーで、開いているリュックを動かさない
        if (type == ClickType.SWAP && button == lockedSlot) return;
        super.clicked(slot, button, type, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem(), copy = s.copy();
        int end = slots.size();
        if (index < upgradeEnd()) {
            if (!moveItemStackTo(s, upgradeEnd(), end, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = UpgradeItem.is(s) && moveItemStackTo(s, storageEnd(), upgradeEnd(), false);
            if (!moved) {
                // 同じ物があるマスに先に重ねて（64 こを超えてOK）、あふれた分を空いたマスへ
                int before = s.getCount();
                ItemStack rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(inv, s.copy(), false);
                if (rest.getCount() == before) return ItemStack.EMPTY;
                s.setCount(rest.getCount());
            }
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        if (source.pos() != null) {
            return p.level().getBlockEntity(source.pos()) instanceof StorageBlockEntity && p.distanceToSqr(source.pos().getCenter()) < 64;
        }
        if (source.backpack() == null) return true;   // 画面側
        if (source.slot() >= 0 && source.slot() < p.getInventory().getContainerSize()) {
            ItemStack s = p.getInventory().getItem(source.slot());
            if (s.getItem() instanceof BackpackItem && source.backpack().equals(BackpackItem.peekId(s))) return true;
        }
        return com.chigusa.jsnstorage.CuriosCompat.hasBackpack(p, source.backpack());
    }

    /** 並べ替え：同じ物をまとめて、種類の順に */
    public void sort(int mode) {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) continue;
            boolean merged = false;
            for (ItemStack a : all) {
                if (ItemStack.isSameItemSameComponents(a, s)) {
                    a.grow(s.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) all.add(s.copy());
        }
        Comparator<ItemStack> byId = Comparator.comparing(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
        Comparator<ItemStack> cmp = switch (mode) {
            case 1 -> Comparator.comparing((ItemStack s) -> s.getHoverName().getString()).thenComparing(byId);
            case 2 -> Comparator.comparingInt((ItemStack s) -> -s.getCount()).thenComparing(byId);
            default -> byId.thenComparing(s -> s.getHoverName().getString());
        };
        all.sort(cmp);
        int slot = 0;
        for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
        for (ItemStack a : all) {
            while (!a.isEmpty() && slot < inv.getSlots()) {
                int lim = Math.min(inv.getSlotLimit(slot), a.getMaxStackSize() * inv.stackMultiplier);
                inv.setStackInSlot(slot++, a.split(lim));
            }
        }
        broadcastChanges();
    }
}
