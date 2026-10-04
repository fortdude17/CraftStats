package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

/**
 * Overrides for an item type, applied as data components and gameplay hooks. A {@code null}
 * number means "leave the vanilla value alone".
 */
public class ItemStats implements HitEffects {

    public static final String EDIBLE_VANILLA = "vanilla", EDIBLE_YES = "yes", EDIBLE_NO = "no";
    public static final String RARITY_VANILLA = "vanilla";
    public static final String SLOT_MAINHAND = "mainhand";

    @SerializedName("v") public int schema = StatSchema.CURRENT;

    // ---- combat --------------------------------------------------------------------------
    @SerializedName("attack_damage")   public Double  attackDamage;
    @SerializedName("attack_speed")    public Double  attackSpeed;
    @SerializedName("enchantability")  public Integer enchantability;
    @SerializedName("fire_on_hit")     public Integer fireOnHit;
    @SerializedName("hit_effect")      public String  hitEffect = "";
    @SerializedName("hit_effect_level") public Integer hitEffectLevel;
    @SerializedName("hit_effect_seconds") public Integer hitEffectSeconds;
    @SerializedName("lifesteal_percent") public Double lifestealPercent;
    @SerializedName("lightning_on_hit") public boolean lightningOnHit;

    // ---- bonuses while held or worn ------------------------------------------------------
    @SerializedName("bonus_slot")              public String bonusSlot = SLOT_MAINHAND;
    @SerializedName("bonus_max_health")        public Double bonusMaxHealth;
    @SerializedName("bonus_armor")             public Double bonusArmor;
    @SerializedName("bonus_armor_toughness")   public Double bonusArmorToughness;
    @SerializedName("bonus_knockback_resist")  public Double bonusKnockbackResist;
    @SerializedName("bonus_move_speed")        public Double bonusMoveSpeed;
    @SerializedName("bonus_jump")              public Double bonusJump;
    @SerializedName("bonus_step_height")       public Double bonusStepHeight;
    @SerializedName("bonus_gravity")           public Double bonusGravity;
    @SerializedName("bonus_scale")             public Double bonusScale;
    @SerializedName("bonus_block_reach")       public Double bonusBlockReach;
    @SerializedName("bonus_entity_reach")      public Double bonusEntityReach;
    @SerializedName("bonus_luck")              public Double bonusLuck;
    @SerializedName("bonus_safe_fall")         public Double bonusSafeFall;
    @SerializedName("bonus_mining_efficiency") public Double bonusMiningEfficiency;
    @SerializedName("bonus_attack_knockback")  public Double bonusAttackKnockback;
    @SerializedName("bonus_sweeping")          public Double bonusSweeping;
    @SerializedName("bonus_oxygen")            public Double bonusOxygen;

    // ---- properties ----------------------------------------------------------------------
    @SerializedName("max_durability")  public Integer maxDurability;
    @SerializedName("stack_size")      public Integer stackSize;
    @SerializedName("mining_speed")    public Float   miningSpeed;
    @SerializedName("fireproof")       public boolean fireproof;
    @SerializedName("unbreakable")     public boolean unbreakable;
    @SerializedName("item_glow")       public boolean itemGlow;
    @SerializedName("rarity")          public String  rarity = RARITY_VANILLA;
    @SerializedName("display_name")    public String  displayName = "";
    @SerializedName("repair_material") public String  repairMaterial = "";
    @SerializedName("use_cooldown")    public Float   useCooldown;
    @SerializedName("never_despawns")  public boolean neverDespawns;
    @SerializedName("soulbound")       public boolean soulbound;

    // ---- throwing ------------------------------------------------------------------------
    @SerializedName("throwable")       public boolean throwable;
    @SerializedName("boomerang")       public boolean boomerang;
    @SerializedName("throw_damage")    public Float   throwDamage;
    @SerializedName("throw_velocity")  public Float   throwVelocity;
    @SerializedName("explode_on_impact") public Float explodeOnImpact;
    @SerializedName("lightning_on_impact") public boolean lightningOnImpact;

    // ---- food ----------------------------------------------------------------------------
    @SerializedName("edible")          public String  edible = EDIBLE_VANILLA;
    @SerializedName("nutrition")       public Integer nutrition;
    @SerializedName("saturation")      public Float   saturation;
    @SerializedName("eat_seconds")     public Float   eatSeconds;
    @SerializedName("always_edible")   public boolean alwaysEdible;
    @SerializedName("on_eat_effect")   public String  onEatEffect = "";
    @SerializedName("on_eat_effect_level") public Integer onEatEffectLevel;
    @SerializedName("on_eat_effect_seconds") public Integer onEatEffectSeconds;
    @SerializedName("eat_heal")        public Float   eatHeal;
    @SerializedName("eat_xp")          public Integer eatXp;
    @SerializedName("wolf_food")       public boolean wolfFood;

    public boolean changesFood() {
        return !EDIBLE_VANILLA.equals(edible) || nutrition != null || saturation != null
                || eatSeconds != null || alwaysEdible || !onEatEffect.isEmpty();
    }

    public boolean hasBonuses() {
        return bonusMaxHealth != null || bonusArmor != null || bonusArmorToughness != null
                || bonusKnockbackResist != null || bonusMoveSpeed != null || bonusJump != null
                || bonusStepHeight != null || bonusGravity != null || bonusScale != null
                || bonusBlockReach != null || bonusEntityReach != null || bonusLuck != null
                || bonusSafeFall != null || bonusMiningEfficiency != null || bonusAttackKnockback != null
                || bonusSweeping != null || bonusOxygen != null;
    }

    @Override public Integer fireOnHit()        { return fireOnHit; }
    @Override public String  hitEffect()        { return hitEffect; }
    @Override public Integer hitEffectLevel()   { return hitEffectLevel; }
    @Override public Integer hitEffectSeconds() { return hitEffectSeconds; }
    @Override public Double  lifestealPercent() { return lifestealPercent; }
    @Override public boolean lightningOnHit()   { return lightningOnHit; }

    public ItemStats copy() { return StatSchema.copy(this, ItemStats.class); }
}
