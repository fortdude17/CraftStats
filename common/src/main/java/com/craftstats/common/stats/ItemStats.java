package com.craftstats.common.stats;

import com.google.gson.annotations.SerializedName;

public class ItemStats {

    @SerializedName("attack_damage")     public double  attackDamage     = 1.0;
    @SerializedName("attack_speed")      public double  attackSpeed      = 4.0;
    @SerializedName("enchantability")    public int     enchantability   = 0;
    @SerializedName("sweep_multiplier")  public double  sweepMultiplier  = 0.0;

    @SerializedName("max_durability")    public int     maxDurability    = 0;
    @SerializedName("stack_size")        public int     stackSize        = 64;
    @SerializedName("mining_speed")      public float   miningSpeed      = 1.0f;
    @SerializedName("repair_material")   public String  repairMaterial   = "";
    @SerializedName("fireproof")         public boolean fireproof        = false;
    @SerializedName("unbreakable")       public boolean unbreakable      = false;
    @SerializedName("consumed_on_use")   public boolean consumedOnUse    = false;
    @SerializedName("item_glow")         public boolean itemGlow         = false;

    @SerializedName("nutrition")         public int     nutrition        = 0;
    @SerializedName("saturation")        public float   saturation       = 0.0f;
    @SerializedName("eat_duration")      public int     eatDuration      = 32;
    @SerializedName("on_eat_effect")     public String  onEatEffect      = "";
    @SerializedName("is_food")           public boolean isFood           = false;
    @SerializedName("always_edible")     public boolean alwaysEdible     = false;
    @SerializedName("fast_eat")          public boolean fastEat          = false;
    @SerializedName("is_meat")           public boolean isMeat           = false;

    @SerializedName("is_weapon")         public boolean isWeapon         = false;
    @SerializedName("is_tool")           public boolean isTool           = false;
    @SerializedName("is_armor")          public boolean isArmor          = false;
    @SerializedName("throwable")         public boolean throwable        = false;
    @SerializedName("boomerang")         public boolean boomerang        = false;
    @SerializedName("projectile")        public boolean projectile       = false;

    public ItemStats copy() {
        ItemStats c = new ItemStats();
        c.attackDamage = attackDamage; c.attackSpeed = attackSpeed; c.enchantability = enchantability;
        c.sweepMultiplier = sweepMultiplier; c.maxDurability = maxDurability; c.stackSize = stackSize;
        c.miningSpeed = miningSpeed; c.repairMaterial = repairMaterial; c.fireproof = fireproof;
        c.unbreakable = unbreakable; c.consumedOnUse = consumedOnUse; c.itemGlow = itemGlow;
        c.nutrition = nutrition; c.saturation = saturation; c.eatDuration = eatDuration;
        c.onEatEffect = onEatEffect; c.isFood = isFood; c.alwaysEdible = alwaysEdible;
        c.fastEat = fastEat; c.isMeat = isMeat; c.isWeapon = isWeapon; c.isTool = isTool;
        c.isArmor = isArmor; c.throwable = throwable; c.boomerang = boomerang; c.projectile = projectile;
        return c;
    }
}
