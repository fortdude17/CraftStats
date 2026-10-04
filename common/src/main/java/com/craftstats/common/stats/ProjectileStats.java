package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/** Overrides for a projectile type (arrows, tridents, snowballs, fireballs...). Null = vanilla. */
public class ProjectileStats implements HitEffects {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    // ---- flight --------------------------------------------------------------------------
    @SerializedName("damage_multiplier")  public Double  damageMultiplier;
    @SerializedName("extra_damage")       public Double  extraDamage;
    @SerializedName("speed_multiplier")   public Double  speedMultiplier;
    @SerializedName("gravity_multiplier") public Double  gravityMultiplier;
    @SerializedName("no_gravity")         public boolean noGravity;
    @SerializedName("always_crit")        public boolean alwaysCrit;
    @SerializedName("piercing")           public Integer piercing;
    @SerializedName("homing")             public boolean homing;
    @SerializedName("lifetime_seconds")   public Integer lifetimeSeconds;
    @SerializedName("no_pickup")          public boolean noPickup;

    // ---- on hit --------------------------------------------------------------------------
    @SerializedName("explode_on_hit")     public Float   explodeOnHit;
    @SerializedName("lightning_on_hit")   public boolean lightningOnHit;
    @SerializedName("fire_on_hit")        public Integer fireOnHit;
    @SerializedName("knockback_bonus")    public Double  knockbackBonus;
    @SerializedName("hit_effect")         public String  hitEffect = "";
    @SerializedName("hit_effect_level")   public Integer hitEffectLevel;
    @SerializedName("hit_effect_seconds") public Integer hitEffectSeconds;
    @SerializedName("spawn_on_hit")       public String  spawnOnHit = "";
    @SerializedName("teleport_shooter")   public boolean teleportShooter;

    @Override public Integer fireOnHit()        { return fireOnHit; }
    @Override public String  hitEffect()        { return hitEffect; }
    @Override public Integer hitEffectLevel()   { return hitEffectLevel; }
    @Override public Integer hitEffectSeconds() { return hitEffectSeconds; }
    @Override public Double  lifestealPercent() { return null; }
    /** Lightning is handled for every kind of hit by ProjectileHooks, not per damaged entity. */
    @Override public boolean lightningOnHit()   { return false; }

    public ProjectileStats copy() { return StatSchema.copy(this, ProjectileStats.class); }
}
