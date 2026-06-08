package com.craftstats.common.stats;

public enum TargetType {
    MOB, BLOCK, ITEM, PLAYER;

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
