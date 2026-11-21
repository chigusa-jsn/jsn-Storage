package com.chigusa.jsnstorage.storage;

public enum Kind {
    CHEST("チェスト"), SHULKER("シュルカーボックス"), BACKPACK("リュック");

    public final String name;

    Kind(String name) {
        this.name = name;
    }
}
