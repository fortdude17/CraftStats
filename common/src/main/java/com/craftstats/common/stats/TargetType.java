package com.craftstats.common.stats;

public enum TargetType {
    MOB("Mobs"), BLOCK("Blocks"), ITEM("Items"), PLAYER("Players"),
    PROJECTILE("Projectiles"), ENCHANTMENT("Enchants"), WORLD("World");

    /** The one key used for {@link #WORLD}. */
    public static final String WORLD_KEY = "world";

    private final String plural;

    TargetType(String plural) { this.plural = plural; }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    /** Short plural for tabs and buttons. */
    public String plural() { return plural; }

    public String id() { return name().toLowerCase(); }
}
