package com.chigusa.jsnstorage;

import com.chigusa.jsnstorage.storage.BackpackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.UUID;

/** Curios（背中の枠）。入っていなければ何もしない */
public final class CuriosCompat {
    public static boolean loaded() {
        return ModList.get().isLoaded("curios");
    }

    /** 背中の枠のリュック */
    public static ItemStack backpack(Player p) {
        if (!loaded()) return ItemStack.EMPTY;
        return Inner.find(p);
    }

    public static boolean hasBackpack(Player p, UUID id) {
        ItemStack s = backpack(p);
        return !s.isEmpty() && id.equals(BackpackItem.peekId(s));
    }

    private static final class Inner {
        static ItemStack find(Player p) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                    .flatMap(h -> h.findFirstCurio(s -> s.getItem() instanceof BackpackItem)).map(r -> r.stack()).orElse(ItemStack.EMPTY);
        }
    }

    private CuriosCompat() {}
}
