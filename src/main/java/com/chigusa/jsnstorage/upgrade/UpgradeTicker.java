package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.CuriosCompat;
import com.chigusa.jsnstorage.JsnStorage;
import com.chigusa.jsnstorage.storage.BackpackItem;
import com.chigusa.jsnstorage.storage.Kind;
import com.chigusa.jsnstorage.storage.StorageData;
import com.chigusa.jsnstorage.storage.StorageInv;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** リュックのアップグレードを動かす（背中の枠と、持ち物に入っているリュック） */
@EventBusSubscriber(modid = JsnStorage.MODID)
public final class UpgradeTicker {
    private static final Map<UUID, ItemStack> LAST_HAND = new HashMap<>();

    /** 持っているリュックの中身（背中が先） */
    static List<StorageInv> backpacks(ServerPlayer p) {
        List<StorageInv> out = new ArrayList<>();
        StorageData d = StorageData.get(p.server);
        ItemStack back = CuriosCompat.backpack(p);
        if (back.getItem() instanceof BackpackItem b) out.add(d.inv(BackpackItem.id(back), b.tier, Kind.BACKPACK));
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BackpackItem b && BackpackItem.peekId(s) != null) out.add(d.inv(BackpackItem.peekId(s), b.tier, Kind.BACKPACK));
        }
        return out;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        int t = p.tickCount;
        boolean refillTick = true, slow = t % 10 == 0;
        if (!slow && !refillTick) return;
        List<StorageInv> bags = backpacks(p);
        if (bags.isEmpty()) {
            LAST_HAND.put(p.getUUID(), p.getMainHandItem().copy());
            return;
        }
        refill(p, bags);
        if (!slow) return;
        for (StorageInv inv : bags) {
            ItemStack pick = Upgrades.find(inv, UpgradeType.PICKUP);
            if (!pick.isEmpty()) pickup(p, inv, pick);
            if (t % 20 == 0) Compactor.run(p.level(), inv);
            if (t % 40 == 0) {
                ItemStack feed = Upgrades.find(inv, UpgradeType.FEEDING);
                if (!feed.isEmpty() && feed(p, inv, feed)) break;
            }
            if (t % 20 == 0) {
                ItemStack m = Upgrades.find(inv, UpgradeType.MANA);
                if (!m.isEmpty()) battery(p, m, true);
                ItemStack s = Upgrades.find(inv, UpgradeType.STAMINA);
                if (!s.isEmpty()) battery(p, s, false);
                if (!m.isEmpty() || !s.isEmpty()) inv.markDirty();
            }
        }
    }

    // ---------------- 自動で拾う ----------------
    static void pickup(ServerPlayer p, StorageInv inv, ItemStack up) {
        int rank = ((UpgradeItem) up.getItem()).rank;
        double r = new double[]{3, 6, 10}[rank];
        for (ItemEntity it : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(r))) {
            if (!it.isAlive() || it.hasPickUpDelay()) continue;
            Entity owner = it.getOwner();
            if (owner instanceof Player && owner != p) continue;   // ほかの人が落とした物は拾わない
            ItemStack s = it.getItem();
            if (!UpgradeData.matches(up, s)) continue;
            int before = s.getCount();
            ItemStack rest = ItemHandlerHelper.insertItemStacked(inv, s.copy(), false);
            int taken = before - rest.getCount();
            if (taken <= 0) continue;
            p.take(it, taken);
            if (rest.isEmpty()) it.discard();
            else it.setItem(rest);
        }
    }

    // ---------------- 自動で食べる ----------------
    static boolean feed(ServerPlayer p, StorageInv inv, ItemStack up) {
        int rank = ((UpgradeItem) up.getItem()).rank;
        int hunger = 20 - p.getFoodData().getFoodLevel();
        boolean lowHp = rank >= 2 && p.getHealth() < p.getMaxHealth() * 0.4F;
        if (hunger <= 0 && !lowHp) return false;
        int best = -1, bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty() || !UpgradeData.matches(up, s)) continue;
            FoodProperties f = s.getFoodProperties(p);
            if (f == null) continue;
            int n = f.nutrition();
            boolean fits = n <= hunger;
            if (!fits && !lowHp) continue;
            int score = rank >= 1 ? n : 0;   // 金から：いちばん合うもの
            if (lowHp && !f.effects().isEmpty()) score += 100;
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
            if (rank == 0) break;
        }
        if (best < 0) return false;
        ItemStack one = inv.extractItem(best, 1, false);
        ItemStack rest = one.finishUsingItem(p.level(), p);
        if (!rest.isEmpty() && !ItemStack.isSameItem(rest, one)) {
            ItemStack r2 = ItemHandlerHelper.insertItemStacked(inv, rest, false);
            if (!r2.isEmpty()) p.getInventory().placeItemBackInInventory(r2);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.6F, 1F);
        return true;
    }

    // ---------------- 補充 ----------------
    static void refill(ServerPlayer p, List<StorageInv> bags) {
        ItemStack last = LAST_HAND.getOrDefault(p.getUUID(), ItemStack.EMPTY);
        ItemStack hand = p.getMainHandItem();
        LAST_HAND.put(p.getUUID(), hand.copy());
        for (StorageInv inv : bags) {
            ItemStack up = Upgrades.find(inv, UpgradeType.REFILL);
            if (up.isEmpty()) continue;
            int rank = ((UpgradeItem) up.getItem()).rank;
            // ブロックを使いきったら（または残りわずか）
            if (hand.isEmpty() && last.getItem() instanceof BlockItem) {
                ItemStack got = take(inv, last, last.getMaxStackSize());
                if (!got.isEmpty()) {
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, got);
                    LAST_HAND.put(p.getUUID(), got.copy());
                    return;
                }
            } else if (hand.getItem() instanceof BlockItem && hand.getCount() <= 2) {
                ItemStack got = take(inv, hand, hand.getMaxStackSize() - hand.getCount());
                if (!got.isEmpty()) {
                    hand.grow(got.getCount());
                    return;
                }
            }
            // 金：弓・クロスボウを持っていて、矢が持ち物にない
            if (rank >= 1 && p.tickCount % 20 == 0 && (hand.getItem() instanceof BowItem || hand.getItem() instanceof CrossbowItem)
                    && p.getProjectile(hand).isEmpty()) {
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (s.getItem() instanceof ArrowItem) {
                        p.getInventory().placeItemBackInInventory(inv.extractItem(i, 64, false));
                        return;
                    }
                }
            }
            // ダイヤ：こわれそうな道具を、同じ種類の元気な道具と交換
            if (rank >= 2 && hand.isDamageableItem() && hand.getMaxDamage() - hand.getDamageValue() <= 5) {
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (s.getItem() == hand.getItem() && s.getMaxDamage() - s.getDamageValue() > 5) {
                        ItemStack fresh = inv.extractItem(i, 1, false);
                        ItemStack old = hand.copy();
                        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fresh);
                        ItemStack r = ItemHandlerHelper.insertItemStacked(inv, old, false);
                        if (!r.isEmpty()) p.getInventory().placeItemBackInInventory(r);
                        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
                        LAST_HAND.put(p.getUUID(), fresh.copy());
                        return;
                    }
                }
            }
        }
    }

    /** 同じアイテムを、最大 n こ取り出す */
    static ItemStack take(StorageInv inv, ItemStack like, int n) {
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSlots() && n > 0; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!ItemStack.isSameItemSameComponents(s, like)) continue;
            ItemStack got = inv.extractItem(i, Math.min(n, s.getMaxStackSize()), false);
            if (out.isEmpty()) out = got;
            else out.grow(got.getCount());
            n -= got.getCount();
        }
        return out;
    }

    // ---------------- マナ・スタミナ ----------------
    static void battery(ServerPlayer p, ItemStack up, boolean mana) {
        float[] cur = CraftCompat.get(p, mana);
        if (cur == null || cur[1] <= 0) return;
        UpgradeItem u = (UpgradeItem) up.getItem();
        double cap = u.type.capacity(u.rank), stored = UpgradeData.stored(up);
        double rate = new double[]{2, 4, 8}[u.rank] * (mana ? 1 : 1.5);
        if (cur[0] >= cur[1] - 0.5F && stored < cap) {
            // 満タン：あふれる分をためる
            UpgradeData.setStored(up, Math.min(cap, stored + rate));
        } else if (cur[0] < cur[1] * 0.25F && stored > 0) {
            // 少なくなった：ためた分から補充
            double give = Math.min(stored, Math.min(rate * 5, cur[1] * 0.5F - cur[0]));
            if (give > 0) {
                CraftCompat.set(p, mana, (float) (cur[0] + give));
                UpgradeData.setStored(up, stored - give);
            }
        }
    }

    private UpgradeTicker() {}
}
