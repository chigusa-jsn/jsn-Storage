package com.chigusa.jsnstorage.upgrade;

import com.chigusa.jsnstorage.storage.Kind;

/** アップグレードの種類 */
public enum UpgradeType {
    STACK("stack", "容量アップ", true, true, true, true, "1マスに入る数が 2倍・4倍・8倍 になる"),
    PICKUP("pickup", "自動で拾う", true, true, true, true, "登録したアイテムだけ、近くから拾う（半径3・6・10マス）。ほかの人が落とした物は拾わない"),
    VOID("void", "いらない物を消す", false, true, true, true, "登録したアイテムは、入れたとたんに消える"),
    FEEDING("feeding", "自動で食べる", true, false, false, true, "おなかがすいたら、登録した食べ物を食べる（金：むだなく選ぶ・ダイヤ：HPが減ったら回復の食べ物も）"),
    REFILL("refill", "補充", true, false, false, true, "手のブロックがなくなったら補充（金：矢も・ダイヤ：こわれそうな道具を交換）"),
    COMPACT("compact", "圧縮", true, true, true, true, "登録した物がたまると、ブロックにまとめる（鉄インゴット9こ→鉄ブロック、など）。もどせる組み合わせだけ"),
    XP("xp", "経験値貯蔵", true, true, true, true, "経験値を預けたり引き出したりできる"),
    MANA("mana", "マナ貯蔵", true, true, true, true, "マナをためておける（jsn-Craft）。リュックなら満タンのときにため、減ったら自動で補充"),
    STAMINA("stamina", "スタミナ貯蔵", true, true, true, true, "スタミナをためておける（jsn-Craft）。リュックなら満タンのときにため、減ったら自動で補充");

    public final String id, name, desc;
    public final boolean ranked;
    private final boolean chest, shulker, backpack;

    UpgradeType(String id, String name, boolean ranked, boolean chest, boolean shulker, boolean backpack, String desc) {
        this.id = id;
        this.name = name;
        this.ranked = ranked;
        this.chest = chest;
        this.shulker = shulker;
        this.backpack = backpack;
        this.desc = desc;
    }

    public boolean allowed(Kind k) {
        return switch (k) {
            case CHEST -> chest;
            case SHULKER -> shulker;
            case BACKPACK -> backpack;
        };
    }

    /** 登録する枠の数（0なら登録なし） */
    public int filterSize(int rank) {
        return switch (this) {
            case PICKUP -> 9 * (rank + 1);
            case VOID -> 18;
            case FEEDING -> 9;
            case COMPACT -> new int[]{3, 9, 18}[rank];
            default -> 0;
        };
    }

    /** ためられる量（経験値はポイント） */
    public double capacity(int rank) {
        return switch (this) {
            case XP -> new double[]{1395, 8670, 30970}[rank];
            case MANA -> new double[]{200, 600, 2000}[rank];
            case STAMINA -> new double[]{300, 900, 3000}[rank];
            default -> 0;
        };
    }
}
