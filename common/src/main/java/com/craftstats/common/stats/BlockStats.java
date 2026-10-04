package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for a block type or a single block position. A {@code null} number means
 * "leave the vanilla value alone"; effect fields default to "off".
 */
public class BlockStats {

    public static final String VANILLA = "vanilla", OFF = "off";

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    /** For per-position overrides: the block the override was made for. Ignored once the block changes. */
    @SerializedName("block")                 public String  block = "";

    // ---- physical ------------------------------------------------------------------------
    @SerializedName("hardness")              public Float   hardness;
    @SerializedName("blast_resistance")      public Float   blastResistance;
    @SerializedName("slipperiness")          public Float   slipperiness;
    @SerializedName("jump_factor")           public Float   jumpFactor;
    @SerializedName("speed_factor")          public Float   speedFactor;
    @SerializedName("bounciness")            public Float   bounciness;
    @SerializedName("landing_damage_multiplier") public Float landingDamageMultiplier;
    @SerializedName("no_collision")          public boolean noCollision;
    @SerializedName("can_fall")              public boolean canFall;
    @SerializedName("climbable")             public boolean climbable;
    @SerializedName("requires_correct_tool") public boolean requiresCorrectTool;
    @SerializedName("replaceable")           public boolean replaceable;
    @SerializedName("push_reaction")         public String  pushReaction = "normal";

    // ---- light, sound, redstone ----------------------------------------------------------
    @SerializedName("light_emission")        public Integer lightEmission;
    @SerializedName("invisible")             public boolean invisible;
    @SerializedName("sound_type")            public String  soundType = VANILLA;
    @SerializedName("redstone_power")        public Integer redstonePower;
    @SerializedName("random_tick_multiplier") public Integer randomTickMultiplier;
    @SerializedName("mob_spawning")          public String  mobSpawning = VANILLA;

    // ---- step-on -------------------------------------------------------------------------
    @SerializedName("step_damage")           public float   stepDamage;
    @SerializedName("speed_modifier")        public float   speedModifier = 1.0f;
    @SerializedName("levitate")              public boolean levitate;
    @SerializedName("glow_on_step")          public boolean glowOnStep;
    @SerializedName("freeze_on_step")        public boolean freezeOnStep;
    @SerializedName("on_step_potion")        public String  onStepPotion = "";
    @SerializedName("on_step_potion_level")  public int     onStepPotionLevel = 1;
    @SerializedName("on_step_potion_duration") public int   onStepPotionDuration = 60;
    @SerializedName("launch_power")          public float   launchPower;
    @SerializedName("conveyor_direction")    public String  conveyorDirection = OFF;
    @SerializedName("conveyor_speed")        public float   conveyorSpeed = 0.2f;
    @SerializedName("heal_on_step")          public float   healOnStep;
    @SerializedName("teleport_on_step")      public int     teleportOnStep;
    @SerializedName("fire_on_step")          public int     fireOnStep;
    @SerializedName("hunger_on_step")        public float   hungerOnStep;
    @SerializedName("feed_on_step")          public int     feedOnStep;
    @SerializedName("xp_on_step")            public int     xpOnStep;
    @SerializedName("extinguish_on_step")    public boolean extinguishOnStep;
    @SerializedName("knockback_on_step")     public float   knockbackOnStep;
    @SerializedName("sticky_inside")         public boolean stickyInside;
    @SerializedName("item_void")             public boolean itemVoid;

    // ---- breaking ------------------------------------------------------------------------
    @SerializedName("drop_xp")               public Integer dropXp;
    @SerializedName("explode_on_break")      public Float   explodeOnBreak;
    @SerializedName("lightning_on_break")    public boolean lightningOnBreak;
    @SerializedName("spawn_on_break")        public String  spawnOnBreak = "";
    @SerializedName("spawn_on_break_count")  public Integer spawnOnBreakCount;
    @SerializedName("drop_multiplier")       public Integer dropMultiplier;
    @SerializedName("no_drops")              public boolean noDrops;
    @SerializedName("drop_override")         public String  dropOverride = "";
    @SerializedName("drop_override_count")   public Integer dropOverrideCount;
    @SerializedName("silk_touch_only")       public boolean silkTouchOnly;
    @SerializedName("damage_breaker")        public Float   damageBreaker;
    @SerializedName("regen_seconds")         public Integer regenSeconds;

    /** True if standing on this block does anything (checked every tick, so keep it cheap). */
    public boolean hasStepEffects() {
        return stepDamage > 0 || speedModifier != 1.0f || levitate || glowOnStep || freezeOnStep
                || !onStepPotion.isEmpty() || launchPower > 0 || !OFF.equals(conveyorDirection)
                || healOnStep > 0 || teleportOnStep > 0 || fireOnStep > 0 || hungerOnStep > 0
                || feedOnStep > 0 || xpOnStep > 0 || extinguishOnStep || knockbackOnStep > 0;
    }

    /** True if breaking this block does anything special. */
    public boolean hasBreakEffects() {
        return explodeOnBreak != null || lightningOnBreak || !spawnOnBreak.isEmpty() || dropMultiplier != null
                || noDrops || !dropOverride.isEmpty() || silkTouchOnly || damageBreaker != null || regenSeconds != null;
    }

    public BlockStats copy() { return StatSchema.copy(this, BlockStats.class); }
}
