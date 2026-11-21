package com.chigusa.jsnstorage;

import com.chigusa.jsnstorage.menu.StorageMenu;
import com.chigusa.jsnstorage.storage.BackpackItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 通信：並べ替え・リュックを開く・アップグレードの設定 */
public final class Net {
    /** 開いている入れ物を並べ替える（mode：0=種類 1=名前 2=数） */
    public record Sort(int mode) implements CustomPacketPayload {
        public static final Type<Sort> TYPE = new Type<>(JsnStorage.id("sort"));
        public static final StreamCodec<ByteBuf, Sort> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Sort::mode, Sort::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Bキー：背中のリュック（なければ持ち物の最初のリュック）を開く */
    public record OpenBackpack() implements CustomPacketPayload {
        public static final Type<OpenBackpack> TYPE = new Type<>(JsnStorage.id("open_backpack"));
        public static final StreamCodec<ByteBuf, OpenBackpack> STREAM_CODEC = StreamCodec.unit(new OpenBackpack());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToServer(Sort.TYPE, Sort.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player().containerMenu instanceof StorageMenu m) m.sort(p.mode());
        }));
        r.playToServer(OpenBackpack.TYPE, OpenBackpack.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (!(c.player() instanceof ServerPlayer sp)) return;
            ItemStack back = CuriosCompat.backpack(sp);
            if (!back.isEmpty()) {
                BackpackItem.open(sp, back, -1);
                return;
            }
            var inv = sp.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++)
                if (inv.getItem(i).getItem() instanceof BackpackItem) {
                    BackpackItem.open(sp, inv.getItem(i), i);
                    return;
                }
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("リュックを持っていないよ"), true);
        }));
        com.chigusa.jsnstorage.upgrade.UpgradeNet.register(r);
    }

    private Net() {}
}
