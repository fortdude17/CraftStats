package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for an item type, applied as data components. A {@code null} number means
 * "leave the vanilla value alone".
 */
public class ItemStats {

    public static final String EDIBLE_VANILLA = "vanilla", EDIBLE_YES = "yes", EDIBLE_NO = "no";

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    @SerializedName("attack_damage")   public Double  attackDamage;
    @SerializedName("attack_speed")    public Double  attackSpeed;
    @SerializedName("enchantability")  public Integer enchantability;

    @SerializedName("max_durability")  public Integer maxDurability;
    @SerializedName("stack_size")      public Integer stackSize;
    @SerializedName("mining_speed")    public Float   miningSpeed;
    @SerializedName("fireproof")       public boolean fireproof;
    @SerializedName("unbreakable")     public boolean unbreakable;
    @SerializedName("item_glow")       public boolean itemGlow;

    @SerializedName("edible")          public String  edible = EDIBLE_VANILLA;
    @SerializedName("nutrition")       public Integer nutrition;
    @SerializedName("saturation")      public Float   saturation;
    @SerializedName("eat_seconds")     public Float   eatSeconds;
    @SerializedName("always_edible")   public boolean alwaysEdible;
    @SerializedName("on_eat_effect")   public String  onEatEffect = "";

    public boolean changesFood() {
        return !EDIBLE_VANILLA.equals(edible) || nutrition != null || saturation != null
                || eatSeconds != null || alwaysEdible || !onEatEffect.isEmpty();
    }

    public ItemStats copy() { return StatSchema.copy(this, ItemStats.class); }
}
