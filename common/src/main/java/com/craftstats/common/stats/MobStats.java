package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for a mob type (or a single mob instance). A {@code null} number means
 * "leave the vanilla value alone".
 */
public class MobStats implements HitEffects {

    public static final String LOOT_VANILLA = "vanilla", LOOT_YES = "yes", LOOT_NO = "no";

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    // ---- combat --------------------------------------------------------------------------
    @SerializedName("max_health")        public Double  maxHealth;
    @SerializedName("attack_damage")     public Double  attackDamage;
    @SerializedName("attack_knockback")  public Double  attackKnockback;
    @SerializedName("armor")             public Double  armor;
    @SerializedName("armor_toughness")   public Double  armorToughness;
    @SerializedName("knockback_resist")  public Double  knockbackResist;
    @SerializedName("absorption")        public Double  absorption;
    @SerializedName("regen_per_second")  public Double  regenPerSecond;
    @SerializedName("damage_dealt_multiplier") public Double damageDealtMultiplier;
    @SerializedName("damage_taken_multiplier") public Double damageTakenMultiplier;
    @SerializedName("thorns_percent")    public Double  thornsPercent;
    @SerializedName("lifesteal_percent") public Double  lifestealPercent;
    @SerializedName("fire_on_hit")       public Integer fireOnHit;
    @SerializedName("hit_effect")        public String  hitEffect = "";
    @SerializedName("hit_effect_level")  public Integer hitEffectLevel;
    @SerializedName("hit_effect_seconds") public Integer hitEffectSeconds;
    @SerializedName("lightning_on_hit")  public boolean lightningOnHit;

    // ---- immunities ----------------------------------------------------------------------
    @SerializedName("invincible")        public boolean invincible;
    @SerializedName("immune_fire")       public boolean immuneFire;
    @SerializedName("immune_fall")       public boolean immuneFall;
    @SerializedName("immune_drown")      public boolean immuneDrown;
    @SerializedName("immune_explosion")  public boolean immuneExplosion;
    @SerializedName("immune_poison")     public boolean immunePoison;
    @SerializedName("immune_magic")      public boolean immuneMagic;
    @SerializedName("immune_projectiles") public boolean immuneProjectiles;
    @SerializedName("immune_freezing")   public boolean immuneFreezing;
    @SerializedName("immune_lightning")  public boolean immuneLightning;
    @SerializedName("immune_melee")      public boolean immuneMelee;
    @SerializedName("immune_cactus")     public boolean immuneCactus;
    @SerializedName("immune_effects")    public boolean immuneEffects;
    @SerializedName("immune_suffocation") public boolean immuneSuffocation;

    // ---- movement ------------------------------------------------------------------------
    @SerializedName("move_speed")        public Double  moveSpeed;
    @SerializedName("flying_speed")      public Double  flyingSpeed;
    @SerializedName("jump_force")        public Double  jumpForce;
    @SerializedName("step_height")       public Double  stepHeight;
    @SerializedName("gravity")           public Double  gravity;
    @SerializedName("safe_fall_distance") public Double safeFallDistance;
    @SerializedName("fall_damage_multiplier") public Double fallDamageMultiplier;
    @SerializedName("water_movement")    public Double  waterMovement;
    @SerializedName("movement_efficiency") public Double movementEfficiency;
    @SerializedName("oxygen_bonus")      public Double  oxygenBonus;
    @SerializedName("follow_range")      public Double  followRange;
    @SerializedName("no_gravity")        public boolean noGravity;

    // ---- body ----------------------------------------------------------------------------
    @SerializedName("size_scale")        public Double  sizeScale;
    @SerializedName("burning_time")      public Double  burningTime;
    @SerializedName("explosion_kb_resist") public Double explosionKbResist;
    @SerializedName("invisible")         public boolean invisible;
    @SerializedName("glowing")           public boolean glowing;
    @SerializedName("silent")            public boolean silent;
    @SerializedName("no_push")           public boolean noPush;

    // ---- behaviour -----------------------------------------------------------------------
    @SerializedName("no_ai")             public boolean noAi;
    @SerializedName("hostile")           public boolean hostile;
    @SerializedName("peaceful")          public boolean peaceful;
    @SerializedName("can_despawn")       public boolean canDespawn = true;
    @SerializedName("burns_daylight")    public boolean burnsDaylight;
    @SerializedName("pick_up_loot")      public String  pickUpLoot = LOOT_VANILLA;
    @SerializedName("teleport_when_hurt") public boolean teleportWhenHurt;
    @SerializedName("permanent_effect")  public String  permanentEffect = "";
    @SerializedName("permanent_effect_level") public Integer permanentEffectLevel;

    // ---- drops & death -------------------------------------------------------------------
    @SerializedName("xp_reward")         public Integer xpReward;
    @SerializedName("no_drops")          public boolean noDrops;
    @SerializedName("extra_loot_rolls")  public Integer extraLootRolls;
    @SerializedName("explode_on_death")  public Float   explodeOnDeath;
    @SerializedName("lightning_on_death") public boolean lightningOnDeath;
    @SerializedName("spawn_on_death")    public String  spawnOnDeath = "";
    @SerializedName("spawn_on_death_count") public Integer spawnOnDeathCount;
    @SerializedName("drop_on_death")     public String  dropOnDeath = "";
    @SerializedName("drop_on_death_count") public Integer dropOnDeathCount;

    @Override public Integer fireOnHit()        { return fireOnHit; }
    @Override public String  hitEffect()        { return hitEffect; }
    @Override public Integer hitEffectLevel()   { return hitEffectLevel; }
    @Override public Integer hitEffectSeconds() { return hitEffectSeconds; }
    @Override public Double  lifestealPercent() { return lifestealPercent; }
    @Override public boolean lightningOnHit()   { return lightningOnHit; }

    public MobStats copy() { return StatSchema.copy(this, MobStats.class); }
}
