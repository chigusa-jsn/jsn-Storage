package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.menu.StorageMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** アップグレードの設定：登録・経験値やマナの出し入れ */
public final class UpgradeNet {
    public static final int SET_FILTER = 0, XP_IN = 1, XP_OUT = 2, BATTERY_IN = 3, BATTERY_OUT = 4;

    /** slot：何番目のアップグレードか、value：枠の番号やレベル数（-1＝全部） */
    public record Action(int slot, int action, int value, String item) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(JsnStorage.id("upgrade_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Action::slot, ByteBufCodecs.VAR_INT, Action::action,
                ByteBufCodecs.INT, Action::value, ByteBufCodecs.stringUtf8(128), Action::item, Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Action.TYPE, Action.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp && sp.containerMenu instanceof StorageMenu m) handle(sp, m, p);
        }));
    }

    static void handle(ServerPlayer p, StorageMenu m, Action a) {
        if (a.slot() < 0 || a.slot() >= m.tier.upgrades) return;
        ItemStack up = m.inv.upgrades.getStackInSlot(a.slot());
        if (!(up.getItem() instanceof UpgradeItem u)) return;
        switch (a.action()) {
            case SET_FILTER -> {
                if (a.value() < 0 || a.value() >= u.type.filterSize(u.rank)) return;
                UpgradeData.setFilter(up, a.value(), UpgradeData.item(a.item()) == null ? "" : a.item());
            }
            case XP_IN, XP_OUT -> xp(p, up, u, a.action() == XP_IN, a.value());
            case BATTERY_IN, BATTERY_OUT -> {
                if (u.type != UpgradeType.MANA && u.type != UpgradeType.STAMINA) return;
                boolean mana = u.type == UpgradeType.MANA;
                float[] cur = CraftCompat.get(p, mana);
                if (cur == null) {
                    p.displayClientMessage(Component.literal("jsn-Craft が入っていないと使えないよ"), true);
                    return;
                }
                double cap = u.type.capacity(u.rank), st = UpgradeData.stored(up);
                if (a.action() == BATTERY_IN) {
                    double put = Math.min(cur[0], cap - st);
                    CraftCompat.set(p, mana, (float) (cur[0] - put));
                    UpgradeData.setStored(up, st + put);
                } else {
                    double take = Math.min(st, cur[1] - cur[0]);
                    CraftCompat.set(p, mana, (float) (cur[0] + take));
                    UpgradeData.setStored(up, st - take);
                }
                p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            default -> {
                return;
            }
        }
        m.inv.markDirty();
        m.broadcastChanges();
    }

    // ---------------- 経験値 ----------------
    static int need(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    static long total(int level) {
        long t = 0;
        for (int i = 0; i < level; i++) t += need(i);
        return t;
    }

    static long points(ServerPlayer p) {
        return total(p.experienceLevel) + Math.round(p.experienceProgress * need(p.experienceLevel));
    }

    static void xp(ServerPlayer p, ItemStack up, UpgradeItem u, boolean in, int levels) {
        double cap = u.type.capacity(u.rank), st = UpgradeData.stored(up);
        long have = points(p);
        if (in) {
            // 1レベル分：いまのレベルの端数、端数がなければ1つ下のレベルまで
            long amount = levels < 0 ? have
                    : have - (p.experienceProgress > 0 ? total(p.experienceLevel) : total(Math.max(0, p.experienceLevel - 1)));
            amount = (long) Math.min(amount, cap - st);
            if (amount <= 0) return;
            p.giveExperiencePoints((int) -amount);
            UpgradeData.setStored(up, st + amount);
        } else {
            long amount;
            if (levels < 0) amount = (long) st;
            else {
                // 次のレベルまで、を levels 回
                long sim = have, target = total(p.experienceLevel + levels);
                amount = target - sim;
            }
            amount = (long) Math.min(amount, st);
            if (amount <= 0) return;
            p.giveExperiencePoints((int) amount);
            UpgradeData.setStored(up, st - amount);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, in ? 0.8F : 1.2F);
    }

    private UpgradeNet() {}
}
