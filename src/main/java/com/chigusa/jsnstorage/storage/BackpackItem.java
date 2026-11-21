package com.chigusa.jsnstorage.storage;

import com.chigusa.jsnstorage.menu.StorageMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** リュック：右クリックで開く。Curios の背中の枠につけて Bキーでも開ける */
public class BackpackItem extends Item {
    public final Tier tier;

    public BackpackItem(Properties p, Tier tier) {
        super(p);
        this.tier = tier;
    }

    /** リュックのID（なければ作る） */
    public static UUID id(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d != null && d.copyTag().hasUUID("storage")) return d.copyTag().getUUID("storage");
        UUID id = UUID.randomUUID();
        CompoundTag t = d == null ? new CompoundTag() : d.copyTag();
        t.putUUID("storage", id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return id;
    }

    public static UUID peekId(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().hasUUID("storage") ? d.copyTag().getUUID("storage") : null;
    }

    public static void open(ServerPlayer p, ItemStack s, int slot) {
        if (!(s.getItem() instanceof BackpackItem b)) return;
        UUID id = id(s);
        StorageInv inv = StorageData.get(p.server).inv(id, b.tier, Kind.BACKPACK);
        p.openMenu(new SimpleMenuProvider((cid, pinv, pl) -> new StorageMenu(cid, pinv, inv, StorageMenu.Source.backpack(id, slot)),
                Component.literal(b.tier.backpackName + "のリュック")), buf -> StorageMenu.writeOpen(buf, b.tier, Kind.BACKPACK, slot));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp)
            open(sp, s, hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : 40);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7" + tier.slots + "マス・アップグレード " + tier.upgrades + "つ"));
        tip.add(Component.literal("§8右クリックで開く／Curiosの背中の枠につけてBキー"));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
