package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.JsnStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** 置いたチェスト・シュルカー：中身のID */
public class StorageBlockEntity extends BlockEntity {
    public UUID id = UUID.randomUUID();

    public StorageBlockEntity(BlockPos pos, BlockState state) {
        super(JsnStorage.STORAGE_BE.get(), pos, state);
    }

    public StorageBlock block() {
        return (StorageBlock) getBlockState().getBlock();
    }

    public StorageInv inv() {
        if (!(level instanceof ServerLevel sl)) return null;
        return StorageData.get(sl.getServer()).inv(id, block().tier, block().kind);
    }

    /** チェスト・シュルカーの「自動で拾う」：近くに落ちている登録した物を吸いこむ（だれかが落とした物は拾わない） */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, StorageBlockEntity be) {
        if (level.getGameTime() % 20 != (pos.asLong() & 15)) return;
        StorageInv inv = be.inv();
        if (inv == null) return;
        com.chigusa.jsnstorage.upgrade.Compactor.run(level, inv);
        var up = com.chigusa.jsnstorage.upgrade.Upgrades.find(inv, com.chigusa.jsnstorage.upgrade.UpgradeType.PICKUP);
        if (up.isEmpty()) return;
        int rank = ((com.chigusa.jsnstorage.upgrade.UpgradeItem) up.getItem()).rank;
        double r = new double[]{3, 6, 10}[rank];
        for (var it : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(r))) {
            if (!it.isAlive() || it.hasPickUpDelay() || it.getOwner() instanceof net.minecraft.world.entity.player.Player) continue;
            var s = it.getItem();
            if (!com.chigusa.jsnstorage.upgrade.UpgradeData.matches(up, s)) continue;
            var rest = net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(inv, s.copy(), false);
            if (rest.isEmpty()) it.discard();
            else it.setItem(rest);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putUUID("storage", id);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        if (t.hasUUID("storage")) id = t.getUUID("storage");
    }

    // シュルカーを壊したとき：IDをアイテムに残す／置いたとき：アイテムのIDを使う
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        CompoundTag t = new CompoundTag();
        t.putUUID("storage", id);
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        CustomData d = input.get(DataComponents.CUSTOM_DATA);
        if (d != null && d.copyTag().hasUUID("storage")) id = d.copyTag().getUUID("storage");
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("storage");
    }
}
