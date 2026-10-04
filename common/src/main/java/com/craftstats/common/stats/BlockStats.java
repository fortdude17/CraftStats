package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for a block type or a single block position. A {@code null} number means
 * "leave the vanilla value alone".
 */
public class BlockStats {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    /** For per-position overrides: the block the override was made for. Ignored once the block changes. */
    @SerializedName("block")                 public String  block = "";

    @SerializedName("hardness")              public Float   hardness;
    @SerializedName("blast_resistance")      public Float   blastResistance;
    @SerializedName("slipperiness")          public Float   slipperiness;
    @SerializedName("light_emission")        public Integer lightEmission;
    @SerializedName("drop_xp")               public Integer dropXp;

    @SerializedName("no_collision")          public boolean noCollision;
    @SerializedName("can_fall")              public boolean canFall;
    @SerializedName("climbable")             public boolean climbable;
    @SerializedName("requires_correct_tool") public boolean requiresCorrectTool;
    @SerializedName("push_reaction")         public String  pushReaction = "normal";

    @SerializedName("step_damage")           public float   stepDamage;
    @SerializedName("speed_modifier")        public float   speedModifier = 1.0f;
    @SerializedName("levitate")              public boolean levitate;
    @SerializedName("glow_on_step")          public boolean glowOnStep;
    @SerializedName("freeze_on_step")        public boolean freezeOnStep;
    @SerializedName("on_step_potion")        public String  onStepPotion = "";
    @SerializedName("on_step_potion_level")  public int     onStepPotionLevel = 1;
    @SerializedName("on_step_potion_duration") public int   onStepPotionDuration = 60;

    public boolean hasStepEffects() {
        return stepDamage > 0 || speedModifier != 1.0f || levitate || glowOnStep || freezeOnStep
                || !onStepPotion.isEmpty();
    }

    public BlockStats copy() { return StatSchema.copy(this, BlockStats.class); }
}
