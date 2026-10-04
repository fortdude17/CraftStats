package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/** Overrides for an enchantment. Null = vanilla. */
public class EnchantmentStats {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    @SerializedName("max_level")        public Integer maxLevel;
    @SerializedName("level_bonus")      public Integer levelBonus;
    @SerializedName("disabled")         public boolean disabled;
    @SerializedName("anvil_cost")       public Integer anvilCost;
    @SerializedName("weight")           public Integer weight;
    @SerializedName("any_item")         public boolean anyItem;
    @SerializedName("ignore_conflicts") public boolean ignoreConflicts;

    public EnchantmentStats copy() { return StatSchema.copy(this, EnchantmentStats.class); }
}
