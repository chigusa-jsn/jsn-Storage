package com.chigusa.jsnstorage.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** jsn-Craft のマナとスタミナ（リフレクションで。jsn-Craft がなくても動く） */
public final class CraftCompat {
    private static boolean tried, ok;
    private static Method get, maxMana, maxStamina, set;
    private static Field mana, stamina;

    static synchronized boolean init() {
        if (tried) return ok;
        tried = true;
        if (!ModList.get().isLoaded("jsn_craft")) return false;
        try {
            Class<?> rs = Class.forName("com.chigusa.jsncraft.stat.ResourceService");
            Class<?> pr = Class.forName("com.chigusa.jsncraft.stat.PlayerResources");
            get = rs.getMethod("get", ServerPlayer.class);
            maxMana = rs.getMethod("maxMana", ServerPlayer.class);
            maxStamina = rs.getMethod("maxStamina", ServerPlayer.class);
            set = rs.getMethod("set", ServerPlayer.class, float.class, float.class);
            mana = pr.getField("mana");
            stamina = pr.getField("stamina");
            ok = true;
        } catch (Throwable t) {
            com.chigusa.jsnstorage.JsnStorage.LOG.warn("jsn-Craft のマナ・スタミナにつながらなかった: {}", t.toString());
        }
        return ok;
    }

    public static boolean ready() {
        return init();
    }

    /** [いま, 最大] */
    public static float[] get(ServerPlayer p, boolean isMana) {
        if (!init()) return null;
        try {
            Object r = get.invoke(null, p);
            float cur = (float) (isMana ? mana : stamina).get(r);
            float max = (float) (isMana ? maxMana : maxStamina).invoke(null, p);
            return new float[]{Math.max(0, cur), max};
        } catch (Throwable t) {
            return null;
        }
    }

    public static void set(ServerPlayer p, boolean isMana, float v) {
        if (!init()) return;
        try {
            if (isMana) set.invoke(null, p, v, -1F);
            else set.invoke(null, p, -1F, v);
        } catch (Throwable ignored) {
        }
    }

    private CraftCompat() {}
}
