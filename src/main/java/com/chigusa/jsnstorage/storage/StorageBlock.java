package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.menu.StorageMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** ランクつきのチェスト・シュルカーボックス */
public class StorageBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape CHEST = Block.box(1, 0, 1, 15, 14, 15);
    /** ランクを上げている途中（中身を落とさないように） */
    public static boolean upgrading;
    public final Tier tier;
    public final Kind kind;

    public StorageBlock(Properties p, Tier tier, Kind kind) {
        super(p);
        this.tier = tier;
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new StorageBlock(p, tier, kind));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind == Kind.CHEST ? CHEST : super.getShape(state, level, pos, ctx);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBlockEntity(pos, state);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                                                   net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.chigusa.jsnstorage.JsnStorage.STORAGE_BE.get(), StorageBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof StorageBlockEntity be) {
            StorageInv inv = be.inv();
            Component title = Component.literal(tier.name + "の" + kind.name);
            sp.openMenu(new SimpleMenuProvider((id, pinv, pl) -> new StorageMenu(id, pinv, inv, StorageMenu.Source.block(pos)), title),
                    buf -> StorageMenu.writeOpen(buf, tier, kind, -1));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !upgrading && kind == Kind.CHEST && level instanceof ServerLevel sl
                && level.getBlockEntity(pos) instanceof StorageBlockEntity be) {
            // チェストは、壊すと中身をばらまく
            StorageData d = StorageData.get(sl.getServer());
            StorageInv inv = d.inv(be.id, tier, kind);
            for (int i = 0; i < inv.getSlots(); i++) {
                var s = inv.getStackInSlot(i);
                while (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s.split(s.getMaxStackSize()));
            }
            for (int i = 0; i < inv.upgrades.getSlots(); i++)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.upgrades.getStackInSlot(i).copy());
            d.remove(be.id);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof StorageBlockEntity be)) return 0;
        StorageInv inv = be.inv();
        float f = 0;
        int used = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            var s = inv.getStackInSlot(i);
            if (!s.isEmpty()) {
                f += s.getCount() / (float) inv.getStackLimit(i, s);
                used++;
            }
        }
        f /= inv.getSlots();
        return (int) Math.floor(f * 14) + (used > 0 ? 1 : 0);
    }
}
