package com.craftstats.common.gui.panel;

import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.TargetType;

import java.util.List;

/** The editable fields for each target type, grouped into tabs. Every field here is applied in-game. */
public final class StatFields {

    public record Tab(String name, List<FieldDef> fields) {}

    private StatFields() {}

    public static List<Tab> tabs(TargetType type) {
        return switch (type) {
            case MOB -> MOB;
            case BLOCK -> BLOCK;
            case ITEM -> ITEM;
            case PLAYER -> PLAYER;
        };
    }

    /** Block tabs limited to fields that work for a single position. */
    public static List<Tab> blockPositionTabs() {
        return BLOCK.stream()
                .map(t -> new Tab(t.name(), t.fields().stream().filter(FieldDef::perPosition).toList()))
                .filter(t -> !t.fields().isEmpty())
                .toList();
    }

    private static final List<Tab> MOB = List.of(
            new Tab("Combat", List.of(
                    FieldDef.num("maxHealth",       "Max Health"),
                    FieldDef.num("attackDamage",    "Attack Damage"),
                    FieldDef.num("armor",           "Armor"),
                    FieldDef.num("knockbackResist", "Knockback Resist (0-1)"),
                    FieldDef.toggle("invincible",      "Invincible"),
                    FieldDef.toggle("immuneFire",      "Immune: Fire"),
                    FieldDef.toggle("immuneFall",      "Immune: Fall"),
                    FieldDef.toggle("immuneDrown",     "Immune: Drowning"),
                    FieldDef.toggle("immuneExplosion", "Immune: Explosions"),
                    FieldDef.toggle("immunePoison",    "Immune: Poison"),
                    FieldDef.toggle("immuneMagic",     "Immune: Magic"))),
            new Tab("Movement", List.of(
                    FieldDef.num("moveSpeed",   "Move Speed"),
                    FieldDef.num("jumpForce",   "Jump Strength"),
                    FieldDef.num("followRange", "Follow Range"),
                    FieldDef.num("sizeScale",   "Size Scale"))),
            new Tab("Behavior", List.of(
                    FieldDef.num("xpReward",         "XP Reward"),
                    FieldDef.toggle("burnsDaylight", "Burns in Daylight"),
                    FieldDef.toggle("canDespawn",    "Can Despawn"),
                    FieldDef.toggle("silent",        "Silent"),
                    FieldDef.toggle("glowing",       "Glowing"))));

    private static final List<Tab> BLOCK = List.of(
            new Tab("Physical", List.of(
                    FieldDef.num("hardness",        "Hardness (-1 = unbreakable)"),
                    FieldDef.num("blastResistance", "Blast Resistance").typeOnly(),
                    FieldDef.num("slipperiness",    "Slipperiness (ice = 0.98)").typeOnly(),
                    FieldDef.num("lightEmission",   "Light Level (0-15)").typeOnly(),
                    FieldDef.num("dropXp",          "XP Dropped"),
                    FieldDef.toggle("noCollision",         "No Collision"),
                    FieldDef.toggle("climbable",           "Climbable"),
                    FieldDef.toggle("canFall",             "Falls Like Sand").typeOnly(),
                    FieldDef.toggle("requiresCorrectTool", "Needs Correct Tool").typeOnly(),
                    FieldDef.pill("pushReaction", "Piston Reaction",
                            "normal", "destroy", "block", "ignore", "push_only").typeOnly())),
            new Tab("Step-On Effects", List.of(
                    FieldDef.num("stepDamage",           "Damage (every 0.5 s)"),
                    FieldDef.num("speedModifier",        "Speed Multiplier (1 = normal)"),
                    FieldDef.toggle("levitate",          "Levitate"),
                    FieldDef.toggle("glowOnStep",        "Glow"),
                    FieldDef.toggle("freezeOnStep",      "Freeze"),
                    FieldDef.text("onStepPotion",        "Effect ID (e.g. minecraft:speed)"),
                    FieldDef.num("onStepPotionLevel",    "Effect Level"),
                    FieldDef.num("onStepPotionDuration", "Effect Duration (ticks)"))));

    private static final List<Tab> ITEM = List.of(
            new Tab("Combat", List.of(
                    FieldDef.num("attackDamage",   "Attack Damage"),
                    FieldDef.num("attackSpeed",    "Attack Speed"),
                    FieldDef.num("enchantability", "Enchantability"))),
            new Tab("Properties", List.of(
                    FieldDef.num("maxDurability", "Max Durability"),
                    FieldDef.num("stackSize",     "Stack Size (1-99)"),
                    FieldDef.num("miningSpeed",   "Mining Speed"),
                    FieldDef.toggle("fireproof",   "Fireproof"),
                    FieldDef.toggle("unbreakable", "Unbreakable"),
                    FieldDef.toggle("itemGlow",    "Enchantment Glint"))),
            new Tab("Food", List.of(
                    FieldDef.pill("edible", "Edible", ItemStats.EDIBLE_VANILLA, ItemStats.EDIBLE_YES, ItemStats.EDIBLE_NO),
                    FieldDef.num("nutrition",    "Nutrition"),
                    FieldDef.num("saturation",   "Saturation"),
                    FieldDef.num("eatSeconds",   "Eating Time (seconds)"),
                    FieldDef.toggle("alwaysEdible", "Edible When Full"),
                    FieldDef.text("onEatEffect", "Effect ID (30 s)"))));

    private static final List<Tab> PLAYER = List.of(
            new Tab("Combat", List.of(
                    FieldDef.num("maxHealth",           "Max Health"),
                    FieldDef.num("baseDamage",          "Base Damage"),
                    FieldDef.num("attackSpeed",         "Attack Speed"),
                    FieldDef.num("critMultiplier",      "Crit Multiplier"),
                    FieldDef.num("attackKnockback",     "Attack Knockback"),
                    FieldDef.num("sweepingDamageRatio", "Sweep Damage Ratio"),
                    FieldDef.num("invincibilityFrames", "Invulnerability (ticks)"),
                    FieldDef.num("reachDistance",       "Block Reach"),
                    FieldDef.num("entityReach",         "Entity Reach"),
                    FieldDef.toggle("oneHitKill",       "One Hit Kill"))),
            new Tab("Movement", List.of(
                    FieldDef.num("walkSpeed",               "Walk Speed"),
                    FieldDef.num("flySpeed",                "Fly Speed"),
                    FieldDef.num("jumpForce",               "Jump Strength"),
                    FieldDef.num("stepHeight",              "Step Height"),
                    FieldDef.num("gravity",                 "Gravity"),
                    FieldDef.num("sneakingSpeed",           "Sneaking Speed"),
                    FieldDef.num("waterMovementEfficiency", "Water Movement"),
                    FieldDef.num("movementEfficiency",      "Movement Efficiency"),
                    FieldDef.num("miningEfficiency",        "Mining Efficiency"),
                    FieldDef.num("submergedMiningSpeed",    "Underwater Mining"),
                    FieldDef.toggle("noFallDamage",   "No Fall Damage"),
                    FieldDef.toggle("noClip",         "No Clip (grants flight)"),
                    FieldDef.toggle("infiniteSprint", "No Hunger From Actions"))),
            new Tab("Defense", List.of(
                    FieldDef.num("armor",                 "Armor"),
                    FieldDef.num("armorToughness",        "Armor Toughness"),
                    FieldDef.num("knockbackResistance",   "Knockback Resist (0-1)"),
                    FieldDef.num("maxAbsorption",         "Max Absorption"),
                    FieldDef.num("burningTime",           "Burn Time Multiplier"),
                    FieldDef.num("fallDamageMultiplier",  "Fall Damage Multiplier"),
                    FieldDef.num("safeFallDistance",      "Safe Fall Distance"),
                    FieldDef.num("explosionKbResistance", "Explosion KB Resist"),
                    FieldDef.num("luck",                  "Luck"),
                    FieldDef.toggle("godMode",     "God Mode"),
                    FieldDef.toggle("fireImmune",  "Fire Immune"),
                    FieldDef.toggle("drownImmune", "Drown Immune"),
                    FieldDef.toggle("noPoison",    "Poison Immune"),
                    FieldDef.toggle("noMagic",     "Magic Immune"))),
            new Tab("Survival", List.of(
                    FieldDef.num("maxFoodLevel",    "Max Food Level"),
                    FieldDef.num("regenThreshold",  "Regen From Food Level"),
                    FieldDef.num("hungerDrainRate", "Hunger Drain Multiplier"),
                    FieldDef.num("xpMultiplier",    "XP Multiplier"),
                    FieldDef.toggle("keepInventory", "Keep Inventory"))),
            new Tab("Effects", List.of(
                    FieldDef.toggle("fxNightVision",  "Night Vision"),
                    FieldDef.toggle("fxWaterBreath",  "Water Breathing"),
                    FieldDef.toggle("fxFireResist",   "Fire Resistance"),
                    FieldDef.toggle("fxRegen",        "Regeneration"),
                    FieldDef.toggle("fxGlowing",      "Glowing"),
                    FieldDef.toggle("fxInvisibility", "Invisibility"),
                    FieldDef.num("fxHaste",           "Haste Level (0 = off)"),
                    FieldDef.num("fxStrength",        "Strength Level (0 = off)"),
                    FieldDef.num("fxSpeed",           "Speed Level (0 = off)"))));
}
