package com.chigusa.jsnstorage.storage;

/** ランク：マスの数・アップグレードの枠・横の列の数 */
public enum Tier {
    WOOD("wood", "木", "革", 27, 1, 9, 0xFF9A6A38),
    IRON("iron", "鉄", "鉄", 45, 2, 9, 0xFFC8CCD0),
    GOLD("gold", "金", "金", 63, 3, 9, 0xFFF0C030),
    DIAMOND("diamond", "ダイヤ", "ダイヤ", 81, 4, 9, 0xFF50D8E0),
    NETHERITE("netherite", "ネザライト", "ネザライト", 108, 5, 12, 0xFF5A3A5A);

    public final String id, name, backpackName;
    public final int slots, upgrades, cols, color;

    Tier(String id, String name, String backpackName, int slots, int upgrades, int cols, int color) {
        this.id = id;
        this.name = name;
        this.backpackName = backpackName;
        this.slots = slots;
        this.upgrades = upgrades;
        this.cols = cols;
        this.color = color;
    }

    public int rows() {
        return (slots + cols - 1) / cols;
    }

    public Tier next() {
        return this == NETHERITE ? null : values()[ordinal() + 1];
    }
}
