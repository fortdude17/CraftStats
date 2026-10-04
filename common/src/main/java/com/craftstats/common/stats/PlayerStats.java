package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/** A full player profile. Defaults are the vanilla values; only differences are applied. */
public class PlayerStats implements HitEffects {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    // ---- combat --------------------------------------------------------------------------
    @SerializedName("max_health")              public double  maxHealth = 20.0;
    @SerializedName("base_damage")             public double  baseDamage = 1.0;
    @SerializedName("attack_speed")            public double  attackSpeed = 4.0;
    @SerializedName("reach_distance")          public double  reachDistance = 4.5;
    @SerializedName("entity_reach")            public double  entityReach = 3.0;
    @SerializedName("crit_multiplier")         public double  critMultiplier = 1.5;
    @SerializedName("invincibility_frames")    public int     invincibilityFrames = 20;
    @SerializedName("attack_knockback")        public double  attackKnockback = 0.0;
    @SerializedName("sweeping_damage_ratio")   public double  sweepingDamageRatio = 0.0;
    @SerializedName("damage_dealt_multiplier") public double  damageDealtMultiplier = 1.0;
    @SerializedName("damage_taken_multiplier") public double  damageTakenMultiplier = 1.0;
    @SerializedName("lifesteal_percent")       public double  lifestealPercent = 0.0;
    @SerializedName("thorns_percent")          public double  thornsPercent = 0.0;
    @SerializedName("fire_on_hit")             public int     fireOnHit = 0;
    @SerializedName("hit_effect")              public String  hitEffect = "";
    @SerializedName("hit_effect_level")        public int     hitEffectLevel = 1;
    @SerializedName("hit_effect_seconds")      public int     hitEffectSeconds = 5;
    @SerializedName("lightning_on_hit")        public boolean lightningOnHit;
    @SerializedName("one_hit_kill")            public boolean oneHitKill;

    // ---- movement ------------------------------------------------------------------------
    @SerializedName("walk_speed")              public double  walkSpeed = 0.1;
    @SerializedName("fly_speed")               public double  flySpeed = 0.05;
    @SerializedName("jump_force")              public double  jumpForce = 0.42;
    @SerializedName("step_height")             public double  stepHeight = 0.6;
    @SerializedName("gravity")                 public double  gravity = 0.08;
    @SerializedName("sneaking_speed")          public double  sneakingSpeed = 0.3;
    @SerializedName("mining_efficiency")       public double  miningEfficiency = 0.0;
    @SerializedName("block_break_speed")       public double  blockBreakSpeed = 1.0;
    @SerializedName("movement_efficiency")     public double  movementEfficiency = 0.0;
    @SerializedName("submerged_mining_speed")  public double  submergedMiningSpeed = 0.2;
    @SerializedName("water_movement_efficiency") public double waterMovementEfficiency = 0.0;
    @SerializedName("oxygen_bonus")            public double  oxygenBonus = 0.0;
    @SerializedName("size")                    public double  size = 1.0;
    @SerializedName("can_fly")                 public boolean canFly;
    @SerializedName("no_fall_damage")          public boolean noFallDamage;
    @SerializedName("no_clip")                 public boolean noClip;
    @SerializedName("infinite_sprint")         public boolean infiniteSprint;

    // ---- defense -------------------------------------------------------------------------
    @SerializedName("armor")                   public double  armor = 0.0;
    @SerializedName("armor_toughness")         public double  armorToughness = 0.0;
    @SerializedName("knockback_resistance")    public double  knockbackResistance = 0.0;
    @SerializedName("max_absorption")          public double  maxAbsorption = 0.0;
    @SerializedName("regen_per_second")        public double  regenPerSecond = 0.0;
    @SerializedName("luck")                    public double  luck = 0.0;
    @SerializedName("burning_time")            public double  burningTime = 1.0;
    @SerializedName("fall_damage_multiplier")  public double  fallDamageMultiplier = 1.0;
    @SerializedName("safe_fall_distance")      public double  safeFallDistance = 3.0;
    @SerializedName("explosion_kb_resistance") public double  explosionKbResistance = 0.0;
    @SerializedName("god_mode")                public boolean godMode;
    @SerializedName("fire_immune")             public boolean fireImmune;
    @SerializedName("drown_immune")            public boolean drownImmune;
    @SerializedName("no_poison")               public boolean noPoison;
    @SerializedName("no_magic")                public boolean noMagic;
    @SerializedName("explosion_immune")        public boolean explosionImmune;
    @SerializedName("projectile_immune")       public boolean projectileImmune;
    @SerializedName("freeze_immune")           public boolean freezeImmune;
    @SerializedName("lightning_immune")        public boolean lightningImmune;
    @SerializedName("negative_effect_immune")  public boolean negativeEffectImmune;

    // ---- survival ------------------------------------------------------------------------
    @SerializedName("max_food_level")          public int     maxFoodLevel = 20;
    @SerializedName("regen_threshold")         public int     regenThreshold = 18;
    @SerializedName("hunger_drain_rate")       public double  hungerDrainRate = 1.0;
    @SerializedName("xp_multiplier")           public double  xpMultiplier = 1.0;
    @SerializedName("keep_inventory")          public boolean keepInventory;
    @SerializedName("keep_xp")                 public boolean keepXp;

    // ---- permanent effects ---------------------------------------------------------------
    @SerializedName("fx_night_vision")         public boolean fxNightVision;
    @SerializedName("fx_water_breath")         public boolean fxWaterBreath;
    @SerializedName("fx_fire_resist")          public boolean fxFireResist;
    @SerializedName("fx_regen")                public boolean fxRegen;
    @SerializedName("fx_glowing")              public boolean fxGlowing;
    @SerializedName("fx_invisibility")         public boolean fxInvisibility;
    @SerializedName("fx_slow_falling")         public boolean fxSlowFalling;
    @SerializedName("fx_dolphins_grace")       public boolean fxDolphinsGrace;
    @SerializedName("fx_conduit_power")        public boolean fxConduitPower;
    @SerializedName("fx_saturation")           public boolean fxSaturation;
    @SerializedName("fx_haste")                public int     fxHaste;
    @SerializedName("fx_strength")             public int     fxStrength;
    @SerializedName("fx_speed")                public int     fxSpeed;
    @SerializedName("fx_jump_boost")           public int     fxJumpBoost;
    @SerializedName("fx_resistance")           public int     fxResistance;

    @Override public Integer fireOnHit()        { return fireOnHit; }
    @Override public String  hitEffect()        { return hitEffect; }
    @Override public Integer hitEffectLevel()   { return hitEffectLevel; }
    @Override public Integer hitEffectSeconds() { return hitEffectSeconds; }
    @Override public Double  lifestealPercent() { return lifestealPercent; }
    @Override public boolean lightningOnHit()   { return lightningOnHit; }

    public PlayerStats copy() { return StatSchema.copy(this, PlayerStats.class); }
}
