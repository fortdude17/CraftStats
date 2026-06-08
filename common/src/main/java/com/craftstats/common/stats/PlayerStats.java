package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

public class PlayerStats {

    @SerializedName("max_health")              public double  maxHealth              = 20.0;
    @SerializedName("base_damage")             public double  baseDamage             = 1.0;
    @SerializedName("attack_speed")            public double  attackSpeed            = 4.0;
    @SerializedName("reach_distance")          public double  reachDistance          = 4.5;
    @SerializedName("crit_multiplier")         public double  critMultiplier         = 1.5;
    @SerializedName("invincibility_frames")    public int     invincibilityFrames     = 20;
    @SerializedName("attack_knockback")        public double  attackKnockback         = 0.0;
    @SerializedName("sweeping_damage_ratio")   public double  sweepingDamageRatio     = 1.0;

    @SerializedName("walk_speed")              public double  walkSpeed              = 0.1;
    @SerializedName("sprint_speed")            public double  sprintSpeed            = 0.13;
    @SerializedName("fly_speed")               public double  flySpeed               = 0.05;
    @SerializedName("jump_force")              public double  jumpForce              = 0.42;
    @SerializedName("step_height")             public double  stepHeight             = 0.6;
    @SerializedName("swim_speed")              public double  swimSpeed              = 0.02;
    @SerializedName("gravity")                 public double  gravity                = 0.08;
    @SerializedName("sneaking_speed")          public double  sneakingSpeed          = 0.3;
    @SerializedName("mining_efficiency")       public double  miningEfficiency       = 0.0;
    @SerializedName("movement_efficiency")     public double  movementEfficiency     = 0.0;
    @SerializedName("submerged_mining_speed")  public double  submergedMiningSpeed   = 0.2;
    @SerializedName("water_movement_efficiency") public double waterMovementEfficiency = 0.0;
    @SerializedName("no_fall_damage")          public boolean noFallDamage           = false;
    @SerializedName("no_clip")                 public boolean noClip                 = false;
    @SerializedName("infinite_sprint")         public boolean infiniteSprint         = false;
    @SerializedName("instant_swim")            public boolean instantSwim            = false;

    @SerializedName("armor")                   public double  armor                  = 0.0;
    @SerializedName("armor_toughness")         public double  armorToughness         = 0.0;
    @SerializedName("knockback_resistance")    public double  knockbackResistance     = 0.0;
    @SerializedName("max_absorption")          public double  maxAbsorption          = 0.0;
    @SerializedName("luck")                    public double  luck                   = 0.0;
    @SerializedName("burning_time")            public double  burningTime            = 8.0;
    @SerializedName("fall_damage_multiplier")  public double  fallDamageMultiplier   = 1.0;
    @SerializedName("safe_fall_distance")      public double  safeFallDistance       = 3.0;
    @SerializedName("explosion_kb_resistance") public double  explosionKbResistance  = 0.0;

    @SerializedName("max_food_level")          public int     maxFoodLevel           = 20;
    @SerializedName("regen_threshold")         public double  regenThreshold         = 18.0;
    @SerializedName("hunger_drain_rate")       public double  hungerDrainRate        = 1.0;
    @SerializedName("exhaustion_cap")          public double  exhaustionCap          = 4.0;
    @SerializedName("xp_multiplier")           public double  xpMultiplier           = 1.0;

    @SerializedName("fx_night_vision")         public boolean fxNightVision          = false;
    @SerializedName("fx_water_breath")         public boolean fxWaterBreath          = false;
    @SerializedName("fx_fire_resist")          public boolean fxFireResist           = false;
    @SerializedName("fx_haste")               public int     fxHaste               = 0;
    @SerializedName("fx_strength")            public int     fxStrength            = 0;
    @SerializedName("fx_speed")               public int     fxSpeed               = 0;
    @SerializedName("fx_regen")               public boolean fxRegen               = false;
    @SerializedName("fx_glowing")             public boolean fxGlowing             = false;
    @SerializedName("fx_invisibility")        public boolean fxInvisibility        = false;

    @SerializedName("keep_inventory")          public boolean keepInventory          = false;
    @SerializedName("fire_immune")             public boolean fireImmune             = false;
    @SerializedName("drown_immune")            public boolean drownImmune            = false;
    @SerializedName("no_poison")              public boolean noPoison              = false;
    @SerializedName("no_magic")              public boolean noMagic               = false;
    @SerializedName("one_hit_kill")           public boolean oneHitKill            = false;
    @SerializedName("god_mode")              public boolean godMode               = false;
    @SerializedName("infinite_items")        public boolean infiniteItems         = false;

    public PlayerStats copy() {
        PlayerStats c = new PlayerStats();
        c.maxHealth = maxHealth; c.baseDamage = baseDamage; c.attackSpeed = attackSpeed;
        c.reachDistance = reachDistance; c.critMultiplier = critMultiplier;
        c.invincibilityFrames = invincibilityFrames; c.attackKnockback = attackKnockback;
        c.sweepingDamageRatio = sweepingDamageRatio;
        c.walkSpeed = walkSpeed; c.sprintSpeed = sprintSpeed; c.flySpeed = flySpeed;
        c.jumpForce = jumpForce; c.stepHeight = stepHeight; c.swimSpeed = swimSpeed;
        c.gravity = gravity; c.sneakingSpeed = sneakingSpeed;
        c.miningEfficiency = miningEfficiency; c.movementEfficiency = movementEfficiency;
        c.submergedMiningSpeed = submergedMiningSpeed;
        c.waterMovementEfficiency = waterMovementEfficiency;
        c.noFallDamage = noFallDamage; c.noClip = noClip;
        c.infiniteSprint = infiniteSprint; c.instantSwim = instantSwim;
        c.armor = armor; c.armorToughness = armorToughness;
        c.knockbackResistance = knockbackResistance; c.maxAbsorption = maxAbsorption;
        c.luck = luck; c.burningTime = burningTime; c.fallDamageMultiplier = fallDamageMultiplier;
        c.safeFallDistance = safeFallDistance; c.explosionKbResistance = explosionKbResistance;
        c.maxFoodLevel = maxFoodLevel; c.regenThreshold = regenThreshold;
        c.hungerDrainRate = hungerDrainRate; c.exhaustionCap = exhaustionCap;
        c.xpMultiplier = xpMultiplier;
        c.fxNightVision = fxNightVision; c.fxWaterBreath = fxWaterBreath;
        c.fxFireResist = fxFireResist; c.fxHaste = fxHaste; c.fxStrength = fxStrength;
        c.fxSpeed = fxSpeed; c.fxRegen = fxRegen; c.fxGlowing = fxGlowing;
        c.fxInvisibility = fxInvisibility;
        c.keepInventory = keepInventory; c.fireImmune = fireImmune; c.drownImmune = drownImmune;
        c.noPoison = noPoison; c.noMagic = noMagic;
        c.oneHitKill = oneHitKill; c.godMode = godMode; c.infiniteItems = infiniteItems;
        return c;
    }
}
