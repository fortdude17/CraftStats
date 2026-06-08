package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

public class MobStats {

    @SerializedName("max_health")       public double maxHealth         = -1.0;
    @SerializedName("attack_damage")    public double attackDamage      = -1.0;
    @SerializedName("armor")            public double armor             = -1.0;
    @SerializedName("knockback_resist") public double knockbackResist   = -1.0;

    @SerializedName("immune_fire")       public boolean immuneFire       = false;
    @SerializedName("immune_fall")       public boolean immuneFall       = false;
    @SerializedName("immune_drown")      public boolean immuneDrown      = false;
    @SerializedName("immune_explosion")  public boolean immuneExplosion  = false;
    @SerializedName("immune_poison")     public boolean immunePoison     = false;
    @SerializedName("immune_magic")      public boolean immuneMagic      = false;

    @SerializedName("move_speed")        public double moveSpeed         = -1.0;
    @SerializedName("jump_force")        public double jumpForce         = 0.42;
    @SerializedName("follow_range")      public double followRange       = -1.0;
    @SerializedName("size_scale")        public double sizeScale         = -1.0;
    @SerializedName("pathfinding_mode")  public String pathfindingMode   = "ground";

    @SerializedName("behavior_mode")     public String behaviorMode      = "passive";
    @SerializedName("flee_threshold")    public double fleeThreshold     = 0.25;
    @SerializedName("attack_cooldown")   public int    attackCooldown    = 20;
    @SerializedName("aggro_range")       public double aggroRange        = 16.0;
    @SerializedName("despawn_range")     public double despawnRange      = 128.0;
    @SerializedName("burns_daylight")    public boolean burnsDaylight    = false;
    @SerializedName("can_despawn")       public boolean canDespawn       = true;
    @SerializedName("tameable")          public boolean tameable         = false;
    @SerializedName("baby_variant")      public boolean babyVariant      = false;

    @SerializedName("silent")            public boolean silent           = false;
    @SerializedName("glowing")           public boolean glowing          = false;
    @SerializedName("invincible")        public boolean invincible       = false;

    @SerializedName("xp_reward")         public int    xpReward         = 0;
    @SerializedName("drop_chance")       public double dropChance        = 0.085;
    @SerializedName("loot_table")        public String lootTable         = "";
    @SerializedName("max_drop_count")    public int    maxDropCount      = 1;

    public MobStats copy() {
        MobStats c = new MobStats();
        c.maxHealth = maxHealth; c.attackDamage = attackDamage; c.armor = armor;
        c.knockbackResist = knockbackResist; c.immuneFire = immuneFire; c.immuneFall = immuneFall;
        c.immuneDrown = immuneDrown; c.immuneExplosion = immuneExplosion; c.immunePoison = immunePoison;
        c.immuneMagic = immuneMagic; c.moveSpeed = moveSpeed; c.jumpForce = jumpForce;
        c.followRange = followRange; c.sizeScale = sizeScale; c.pathfindingMode = pathfindingMode;
        c.behaviorMode = behaviorMode; c.fleeThreshold = fleeThreshold;
        c.attackCooldown = attackCooldown; c.aggroRange = aggroRange; c.despawnRange = despawnRange;
        c.burnsDaylight = burnsDaylight; c.canDespawn = canDespawn; c.tameable = tameable;
        c.babyVariant = babyVariant; c.silent = silent; c.glowing = glowing;
        c.invincible = invincible; c.xpReward = xpReward; c.dropChance = dropChance;
        c.lootTable = lootTable; c.maxDropCount = maxDropCount;
        return c;
    }
}
