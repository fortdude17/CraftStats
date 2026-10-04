package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for a mob type (or a single mob instance). A {@code null} number means
 * "leave the vanilla value alone".
 */
public class MobStats {

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    @SerializedName("max_health")        public Double  maxHealth;
    @SerializedName("attack_damage")     public Double  attackDamage;
    @SerializedName("armor")             public Double  armor;
    @SerializedName("knockback_resist")  public Double  knockbackResist;

    @SerializedName("immune_fire")       public boolean immuneFire;
    @SerializedName("immune_fall")       public boolean immuneFall;
    @SerializedName("immune_drown")      public boolean immuneDrown;
    @SerializedName("immune_explosion")  public boolean immuneExplosion;
    @SerializedName("immune_poison")     public boolean immunePoison;
    @SerializedName("immune_magic")      public boolean immuneMagic;
    @SerializedName("invincible")        public boolean invincible;

    @SerializedName("move_speed")        public Double  moveSpeed;
    @SerializedName("jump_force")        public Double  jumpForce;
    @SerializedName("follow_range")      public Double  followRange;
    @SerializedName("size_scale")        public Double  sizeScale;

    @SerializedName("xp_reward")         public Integer xpReward;
    @SerializedName("burns_daylight")    public boolean burnsDaylight;
    @SerializedName("can_despawn")       public boolean canDespawn = true;
    @SerializedName("silent")            public boolean silent;
    @SerializedName("glowing")           public boolean glowing;

    public MobStats copy() { return StatSchema.copy(this, MobStats.class); }
}
