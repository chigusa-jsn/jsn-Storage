package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.storage.Kind;

/** アップグレードの種類 */
public enum UpgradeType {
    STACK("stack", "容量アップ", true, true, true, true);

    public final String id, name;
    public final boolean ranked;
    private final boolean chest, shulker, backpack;

    UpgradeType(String id, String name, boolean ranked, boolean chest, boolean shulker, boolean backpack) {
        this.id = id;
        this.name = name;
        this.ranked = ranked;
        this.chest = chest;
        this.shulker = shulker;
        this.backpack = backpack;
    }

    public boolean allowed(Kind k) {
        return switch (k) {
            case CHEST -> chest;
            case SHULKER -> shulker;
            case BACKPACK -> backpack;
        };
    }
}
