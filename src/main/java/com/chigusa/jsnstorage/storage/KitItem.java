package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.JsnStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.List;
import java.util.UUID;

/**
 * 強化キット：置いてあるチェスト・シュルカーを右クリックすると、中身はそのままランクが1つ上がる。
 * 鉄のキットは、ふつうのチェスト・シュルカーボックスにも使える。リュックは作業台でキットといっしょに。
 */
public class KitItem extends Item {
    public final Tier from, to;

    public KitItem(Properties p, Tier from) {
        super(p);
        this.from = from;
        this.to = from.next();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState st = level.getBlockState(pos);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel sl = (ServerLevel) level;
        if (st.getBlock() instanceof StorageBlock sb && level.getBlockEntity(pos) instanceof StorageBlockEntity be) {
            if (sb.tier != from) {
                msg(ctx, "このキットは「" + from.name + "」から「" + to.name + "」へ上げるためのものだよ");
                return InteractionResult.FAIL;
            }
            UUID id = be.id;
            Block next = JsnStorage.block(sb.kind, to);
            StorageBlock.upgrading = true;
            try {
                level.setBlock(pos, next.defaultBlockState().setValue(StorageBlock.FACING, st.getValue(StorageBlock.FACING)), 3);
            } finally {
                StorageBlock.upgrading = false;
            }
            if (level.getBlockEntity(pos) instanceof StorageBlockEntity nb) {
                nb.id = id;
                nb.setChanged();
                StorageData.get(sl.getServer()).inv(id, to, sb.kind);
            }
            done(ctx, pos);
            return InteractionResult.CONSUME;
        }
        // ふつうのチェスト・シュルカーボックス → 鉄のランクへ
        if (from == Tier.WOOD && (st.getBlock() instanceof ChestBlock || st.getBlock() instanceof ShulkerBoxBlock)) {
            boolean shulker = st.getBlock() instanceof ShulkerBoxBlock;
            if (!shulker && st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                msg(ctx, "大きなチェストには使えないよ（1つずつにしてね）");
                return InteractionResult.FAIL;
            }
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            var obe = level.getBlockEntity(pos);
            if (obe instanceof ChestBlockEntity c) for (int i = 0; i < Math.min(27, c.getContainerSize()); i++) items.set(i, c.getItem(i).copy());
            else if (obe instanceof ShulkerBoxBlockEntity c) for (int i = 0; i < Math.min(27, c.getContainerSize()); i++) items.set(i, c.getItem(i).copy());
            else return InteractionResult.PASS;
            if (obe instanceof net.minecraft.world.Container c) c.clearContent();
            Kind kind = shulker ? Kind.SHULKER : Kind.CHEST;
            var facing = st.hasProperty(ChestBlock.FACING) ? st.getValue(ChestBlock.FACING) : ctx.getHorizontalDirection().getOpposite();
            level.setBlock(pos, JsnStorage.block(kind, to).defaultBlockState().setValue(StorageBlock.FACING, facing), 3);
            if (level.getBlockEntity(pos) instanceof StorageBlockEntity nb) {
                StorageInv inv = StorageData.get(sl.getServer()).inv(nb.id, to, kind);
                for (int i = 0; i < 27; i++) inv.setStackInSlot(i, items.get(i));
            }
            done(ctx, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void done(UseOnContext ctx, BlockPos pos) {
        ctx.getLevel().playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1F, 1.2F);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        msg(ctx, "「" + to.name + "」に強化したよ！（中身はそのまま）");
    }

    private static void msg(UseOnContext ctx, String s) {
        if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.literal(s), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext c, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7" + from.name + " → " + to.name + "（中身はそのまま）"));
        tip.add(Component.literal("§8置いてあるチェスト・シュルカーを右クリック／リュックは作業台でいっしょに"));
        if (from == Tier.WOOD) tip.add(Component.literal("§8ふつうのチェスト・シュルカーボックスにも使える"));
    }
}
