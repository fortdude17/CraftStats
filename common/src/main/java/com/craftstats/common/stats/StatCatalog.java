package com.craftstats.common.stats;

import java.util.ArrayList;
import java.util.List;

import static com.craftstats.common.stats.StatDef.*;

/**
 * Every editable stat, grouped into the categories shown as cards in the editor. The
 * single source of truth for labels, value ranges, help text and randomizing.
 */
public final class StatCatalog {

    /** A card in the editor. {@code icon} is an item id. */
    public record Category(String name, String icon, List<StatDef> stats) {}

    private StatCatalog() {}

    public static List<Category> categories(TargetType type) {
        return switch (type) {
            case MOB         -> MOB;
            case BLOCK       -> BLOCK;
            case ITEM        -> ITEM;
            case PLAYER      -> PLAYER;
            case PROJECTILE  -> PROJECTILE;
            case ENCHANTMENT -> ENCHANTMENT;
            case WORLD       -> WORLD;
        };
    }

    public static List<StatDef> all(TargetType type) {
        List<StatDef> out = new ArrayList<>();
        for (Category c : categories(type)) out.addAll(c.stats());
        return out;
    }

    public static int count(TargetType type) {
        int n = 0;
        for (Category c : categories(type)) n += c.stats().size();
        return n;
    }

    /** Categories limited to the stats that work for a single block position. */
    public static List<Category> blockPositionCategories() {
        List<Category> out = new ArrayList<>();
        for (Category c : BLOCK) {
            List<StatDef> stats = c.stats().stream().filter(StatDef::perPosition).toList();
            if (!stats.isEmpty()) out.add(new Category(c.name(), c.icon(), stats));
        }
        return out;
    }

    private static Category cat(String name, String icon, StatDef... stats) {
        return new Category(name, "minecraft:" + icon, List.of(stats));
    }

    // Shared definitions ------------------------------------------------------------------

    private static StatDef[] hitEffects(String who) {
        return new StatDef[]{
                num("fireOnHit", "Sets Target on Fire (s)", 0, 600).def(0).help("Sets whatever " + who + " hits on fire for this many seconds."),
                text("hitEffect", "Hit Effect", ID_EFFECT).help("Effect given to whatever " + who + " hits, e.g. minecraft:poison."),
                num("hitEffectLevel", "Hit Effect Level", 1, 10).def(1).help("Level of the hit effect (1 = I)."),
                num("hitEffectSeconds", "Hit Effect Duration (s)", 1, 600).def(5).help("How long the hit effect lasts."),
                num("lifestealPercent", "Lifesteal (%)", 0, 1000).def(0).help("Heals the attacker by this percentage of the damage dealt."),
                toggle("lightningOnHit", "Lightning on Hit").help("Strikes lightning on whatever gets hit."),
        };
    }

    private static StatDef[] concat(StatDef[] a, StatDef... b) {
        StatDef[] out = new StatDef[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    // Mobs ----------------------------------------------------------------------------------

    private static final List<Category> MOB = List.of(
            cat("Combat", "iron_sword", concat(new StatDef[]{
                    num("maxHealth", "Max Health", 1, 1024).random().help("Health points (2 = one heart)."),
                    num("attackDamage", "Attack Damage", 0, 2048).random().help("Melee damage. Only mobs that attack use it."),
                    num("attackKnockback", "Attack Knockback", 0, 5).help("Extra knockback on melee hits."),
                    num("armor", "Armor", 0, 30).random().help("Armor points (like wearing armor)."),
                    num("armorToughness", "Armor Toughness", 0, 20).help("Reduces damage from strong hits."),
                    num("knockbackResist", "Knockback Resistance", 0, 1).random().help("0 = normal, 1 = can't be knocked back."),
                    num("absorption", "Absorption Hearts", 0, 2048).def(0).help("Extra yellow health points given when the mob spawns."),
                    num("regenPerSecond", "Regeneration (HP/s)", 0, 100).def(0).help("Heals this much every second."),
                    num("damageDealtMultiplier", "Damage Dealt x", 0, 100).def(1).help("Multiplies all damage this mob deals."),
                    num("damageTakenMultiplier", "Damage Taken x", 0, 100).def(1).help("Multiplies all damage this mob takes. 0.5 = half damage."),
                    num("thornsPercent", "Thorns (%)", 0, 1000).def(0).help("Reflects this percentage of melee damage back at the attacker."),
            }, hitEffects("it"))),
            cat("Immunities", "totem_of_undying",
                    toggle("invincible", "Invincible").random().help("Takes no damage at all (except /kill and the void)."),
                    toggle("immuneFire", "Immune: Fire & Lava").random(),
                    toggle("immuneFall", "Immune: Fall Damage").random(),
                    toggle("immuneDrown", "Immune: Drowning"),
                    toggle("immuneExplosion", "Immune: Explosions").random(),
                    toggle("immunePoison", "Immune: Poison"),
                    toggle("immuneMagic", "Immune: Magic & Wither"),
                    toggle("immuneProjectiles", "Immune: Projectiles").help("Arrows, tridents, fireballs and other projectiles."),
                    toggle("immuneFreezing", "Immune: Freezing"),
                    toggle("immuneLightning", "Immune: Lightning"),
                    toggle("immuneMelee", "Immune: Melee").help("Direct hits from players and mobs."),
                    toggle("immuneCactus", "Immune: Cactus & Thorns"),
                    toggle("immuneEffects", "Immune: Potion Effects").help("No effect can be applied, good or bad."),
                    toggle("immuneSuffocation", "Immune: Suffocation")),
            cat("Movement", "feather",
                    num("moveSpeed", "Move Speed", 0, 10).random().help("Walking speed. Zombies are 0.23, horses about 0.3."),
                    num("flyingSpeed", "Flying Speed", 0, 10).help("Only for flying mobs (bees, parrots, allays...)."),
                    num("jumpForce", "Jump Strength", 0, 32).random(),
                    num("stepHeight", "Step Height", 0, 10).help("How tall a block it can walk up without jumping."),
                    num("gravity", "Gravity", -1, 1).help("0.08 is normal. Negative values make it float up."),
                    num("safeFallDistance", "Safe Fall Distance", -1024, 1024).help("Blocks it can fall before taking damage."),
                    num("fallDamageMultiplier", "Fall Damage x", 0, 100),
                    num("waterMovement", "Water Movement", 0, 1).help("1 = moves in water like on land."),
                    num("movementEfficiency", "Terrain Speed", 0, 1).help("1 = soul sand and similar blocks don't slow it."),
                    num("oxygenBonus", "Oxygen Bonus", 0, 1024).help("Higher = holds its breath longer."),
                    num("followRange", "Follow Range", 0, 2048).random().help("How far away it notices targets (blocks)."),
                    toggle("noGravity", "No Gravity").help("Floats in place instead of falling.")),
            cat("Body", "armor_stand",
                    num("sizeScale", "Size", 0.0625, 16).random().help("1 = normal size. Also changes its reach and hitbox."),
                    num("burningTime", "Burn Time x", 0, 1024).help("How long it keeps burning once set on fire."),
                    num("explosionKbResist", "Explosion Knockback Resistance", 0, 1),
                    toggle("invisible", "Invisible"),
                    toggle("glowing", "Glowing").random().help("Visible through walls."),
                    toggle("silent", "Silent"),
                    toggle("noPush", "Can't Be Pushed").help("Other mobs and players can't push it around.")),
            cat("Behaviour", "zombie_head",
                    toggle("noAi", "No AI").help("Doesn't move, attack or react, like a statue."),
                    toggle("hostile", "Hostile").help("Attacks players on sight, even if it is normally passive."),
                    toggle("peaceful", "Peaceful").help("Never attacks players, even if it is normally hostile."),
                    toggle("canDespawn", "Can Despawn").help("Off = never despawns when you walk away."),
                    toggle("burnsDaylight", "Burns in Daylight").random(),
                    choice("pickUpLoot", "Picks Up Items", MobStats.LOOT_VANILLA, MobStats.LOOT_YES, MobStats.LOOT_NO),
                    toggle("teleportWhenHurt", "Teleports When Hurt").help("Jumps somewhere nearby when hit, like an enderman."),
                    text("permanentEffect", "Permanent Effect", ID_EFFECT).help("An effect it always has, e.g. minecraft:speed."),
                    num("permanentEffectLevel", "Permanent Effect Level", 1, 10).def(1)),
            cat("Drops & Death", "chest",
                    num("xpReward", "XP Dropped", 0, 100000).help("Experience points dropped when killed."),
                    toggle("noDrops", "No Drops").help("Drops nothing when killed."),
                    num("extraLootRolls", "Extra Loot Rolls", 0, 32).def(0).help("Drops its loot this many extra times."),
                    num("explodeOnDeath", "Explodes on Death (power)", 0, 20).def(0).help("4 = a TNT blast."),
                    toggle("lightningOnDeath", "Lightning on Death"),
                    text("spawnOnDeath", "Spawns on Death", ID_ENTITY).help("Mob that appears when this one dies, e.g. minecraft:silverfish."),
                    num("spawnOnDeathCount", "Spawn Count", 1, 16).def(1),
                    text("dropOnDeath", "Extra Item Drop", ID_ITEM).help("Item dropped when killed, e.g. minecraft:diamond."),
                    num("dropOnDeathCount", "Extra Item Count", 1, 64).def(1))
    );

    // Players -------------------------------------------------------------------------------

    private static final List<Category> PLAYER = List.of(
            cat("Combat", "diamond_sword", concat(new StatDef[]{
                    num("maxHealth", "Max Health", 1, 1024).random(),
                    num("baseDamage", "Base Damage", 0, 2048).random().help("Damage of an empty hand. Weapons add to this."),
                    num("attackSpeed", "Attack Speed", 0, 1024).random().help("Attacks per second at full strength."),
                    num("critMultiplier", "Critical Hit x", 0, 100).help("Damage multiplier of jump attacks (1.5 is normal)."),
                    num("attackKnockback", "Attack Knockback", 0, 5),
                    num("sweepingDamageRatio", "Sweep Damage Ratio", 0, 1).help("Share of damage dealt to mobs around the target."),
                    num("reachDistance", "Block Reach", 0, 64).random(),
                    num("entityReach", "Attack Reach", 0, 64),
                    num("invincibilityFrames", "Invulnerability (ticks)", 0, 200).help("Time after a hit before you can be hurt again (20 = 1 s)."),
                    num("damageDealtMultiplier", "Damage Dealt x", 0, 100),
                    num("damageTakenMultiplier", "Damage Taken x", 0, 100),
                    num("thornsPercent", "Thorns (%)", 0, 1000).help("Reflects this percentage of melee damage back."),
                    toggle("oneHitKill", "One Hit Kill"),
            }, hitEffects("you"))),
            cat("Movement", "leather_boots",
                    num("walkSpeed", "Walk Speed", 0, 10).random().help("Also affects sprinting."),
                    num("flySpeed", "Fly Speed", 0, 10).help("Creative flight speed (0.05 is normal)."),
                    num("jumpForce", "Jump Strength", 0, 32).random(),
                    num("stepHeight", "Step Height", 0, 10).random(),
                    num("gravity", "Gravity", -1, 1).random(),
                    num("sneakingSpeed", "Sneaking Speed", 0, 1),
                    num("size", "Size", 0.0625, 16).help("1 = normal. Changes reach and hitbox too."),
                    num("movementEfficiency", "Terrain Speed", 0, 1),
                    num("waterMovementEfficiency", "Water Movement", 0, 1),
                    num("oxygenBonus", "Oxygen Bonus", 0, 1024),
                    num("miningEfficiency", "Mining Efficiency", 0, 1024),
                    num("blockBreakSpeed", "Block Break Speed x", 0, 1024),
                    num("submergedMiningSpeed", "Underwater Mining", 0, 20),
                    toggle("canFly", "Can Fly").help("Double-tap jump to fly, like in creative."),
                    toggle("noFallDamage", "No Fall Damage"),
                    toggle("noClip", "No Clip").help("Walk through walls (also lets you fly)."),
                    toggle("infiniteSprint", "No Hunger From Actions")),
            cat("Defense", "shield",
                    num("armor", "Armor", 0, 30),
                    num("armorToughness", "Armor Toughness", 0, 20),
                    num("knockbackResistance", "Knockback Resistance", 0, 1),
                    num("maxAbsorption", "Max Absorption", 0, 2048),
                    num("regenPerSecond", "Regeneration (HP/s)", 0, 100),
                    num("luck", "Luck", -1024, 1024).help("Better fishing and chest loot."),
                    num("burningTime", "Burn Time x", 0, 1024),
                    num("fallDamageMultiplier", "Fall Damage x", 0, 100),
                    num("safeFallDistance", "Safe Fall Distance", -1024, 1024),
                    num("explosionKbResistance", "Explosion Knockback Resistance", 0, 1),
                    toggle("godMode", "God Mode").help("Takes no damage (except /kill and the void)."),
                    toggle("fireImmune", "Immune: Fire & Lava"),
                    toggle("drownImmune", "Immune: Drowning"),
                    toggle("noPoison", "Immune: Poison"),
                    toggle("noMagic", "Immune: Magic & Wither"),
                    toggle("explosionImmune", "Immune: Explosions"),
                    toggle("projectileImmune", "Immune: Projectiles"),
                    toggle("freezeImmune", "Immune: Freezing"),
                    toggle("lightningImmune", "Immune: Lightning"),
                    toggle("negativeEffectImmune", "Immune: Bad Effects").help("Harmful effects like poison or slowness can't be applied.")),
            cat("Survival", "cooked_beef",
                    num("maxFoodLevel", "Max Food Level", 0, 20),
                    num("regenThreshold", "Regen From Food Level", 0, 20).help("Health regenerates while food is at least this (18 is normal)."),
                    num("hungerDrainRate", "Hunger Drain x", 0, 100),
                    num("xpMultiplier", "XP Gain x", 0, 1000),
                    toggle("keepInventory", "Keep Inventory"),
                    toggle("keepXp", "Keep XP on Death")),
            cat("Effects", "potion",
                    toggle("fxNightVision", "Night Vision"),
                    toggle("fxWaterBreath", "Water Breathing"),
                    toggle("fxFireResist", "Fire Resistance"),
                    toggle("fxRegen", "Regeneration"),
                    toggle("fxGlowing", "Glowing"),
                    toggle("fxInvisibility", "Invisibility"),
                    toggle("fxSlowFalling", "Slow Falling"),
                    toggle("fxDolphinsGrace", "Dolphin's Grace"),
                    toggle("fxConduitPower", "Conduit Power"),
                    toggle("fxSaturation", "Saturation").help("Never get hungry."),
                    num("fxHaste", "Haste Level", 0, 10).help("0 = off."),
                    num("fxStrength", "Strength Level", 0, 10).help("0 = off."),
                    num("fxSpeed", "Speed Level", 0, 10).help("0 = off."),
                    num("fxJumpBoost", "Jump Boost Level", 0, 10).help("0 = off."),
                    num("fxResistance", "Resistance Level", 0, 4).help("0 = off. 4 = no damage at all."))
    );

    // Items ---------------------------------------------------------------------------------

    private static final List<Category> ITEM = List.of(
            cat("Combat", "golden_sword", concat(new StatDef[]{
                    num("attackDamage", "Attack Damage", 0, 2048).random().help("Total damage per hit with this item."),
                    num("attackSpeed", "Attack Speed", 0, 1024).random().help("Attacks per second at full strength (swords 1.6)."),
                    num("enchantability", "Enchantability", 0, 100).help("Higher = better enchantments from the table. 0 = can't be enchanted."),
            }, hitEffects("it"))),
            cat("Bonuses", "golden_apple",
                    choice("bonusSlot", "Bonuses Work When", "mainhand", "offhand", "hand", "armor", "any")
                            .help("Where the item must be for the bonuses below to apply."),
                    num("bonusMaxHealth", "+ Max Health", -1024, 1024).def(0),
                    num("bonusArmor", "+ Armor", -30, 30).def(0),
                    num("bonusArmorToughness", "+ Armor Toughness", -20, 20).def(0),
                    num("bonusKnockbackResist", "+ Knockback Resistance", -1, 1).def(0),
                    num("bonusMoveSpeed", "+ Move Speed", -1, 1).def(0).help("0.1 = double walking speed."),
                    num("bonusJump", "+ Jump Strength", -1, 4).def(0),
                    num("bonusStepHeight", "+ Step Height", -1, 10).def(0),
                    num("bonusGravity", "+ Gravity", -1, 1).def(0).help("-0.07 = almost floating."),
                    num("bonusScale", "+ Size", -0.9, 15).def(0).help("1 = twice as big."),
                    num("bonusBlockReach", "+ Block Reach", -4, 60).def(0),
                    num("bonusEntityReach", "+ Attack Reach", -3, 60).def(0),
                    num("bonusLuck", "+ Luck", -1024, 1024).def(0),
                    num("bonusSafeFall", "+ Safe Fall Distance", -1024, 1024).def(0),
                    num("bonusMiningEfficiency", "+ Mining Efficiency", -1024, 1024).def(0),
                    num("bonusAttackKnockback", "+ Attack Knockback", -5, 5).def(0),
                    num("bonusSweeping", "+ Sweep Damage Ratio", -1, 1).def(0),
                    num("bonusOxygen", "+ Oxygen", -1024, 1024).def(0)),
            cat("Properties", "anvil",
                    num("maxDurability", "Max Durability", 1, 1000000).random().help("Uses before it breaks. Makes the item damageable."),
                    num("stackSize", "Stack Size", 1, 99).help("Tools and armor always stack to 1."),
                    num("miningSpeed", "Mining Speed", 0, 1000).random(),
                    toggle("fireproof", "Fireproof").help("Survives lava and fire when dropped."),
                    toggle("unbreakable", "Unbreakable"),
                    toggle("itemGlow", "Enchantment Glint"),
                    choice("rarity", "Name Color (Rarity)", ItemStats.RARITY_VANILLA, "common", "uncommon", "rare", "epic"),
                    text("displayName", "Display Name", 0).help("Default name of the item. Renamed items keep their own name."),
                    text("repairMaterial", "Repaired With", ID_ITEM).help("Item that repairs it in an anvil, e.g. minecraft:diamond."),
                    num("useCooldown", "Use Cooldown (s)", 0, 600).def(0).help("Wait time after using it (right-click)."),
                    toggle("neverDespawns", "Never Despawns").help("Dropped items of this type stay forever."),
                    toggle("soulbound", "Soulbound").help("Stays in your inventory when you die.")),
            cat("Throwing", "snowball",
                    toggle("throwable", "Throwable").help("Right-click throws the item. It deals damage where it lands."),
                    toggle("boomerang", "Boomerang").help("Thrown items fly back to you."),
                    num("throwDamage", "Throw Damage", 0, 1000).def(4),
                    num("throwVelocity", "Throw Speed", 0.1, 10).def(1.5),
                    num("explodeOnImpact", "Explodes on Impact (power)", 0, 20).def(0),
                    toggle("lightningOnImpact", "Lightning on Impact")),
            cat("Food", "bread",
                    choice("edible", "Edible", ItemStats.EDIBLE_VANILLA, ItemStats.EDIBLE_YES, ItemStats.EDIBLE_NO),
                    num("nutrition", "Hunger Restored", 0, 100).help("2 = one drumstick."),
                    num("saturation", "Saturation", 0, 100),
                    num("eatSeconds", "Eating Time (s)", 0.05, 60),
                    toggle("alwaysEdible", "Edible When Full"),
                    text("onEatEffect", "Effect on Eating", ID_EFFECT).help("e.g. minecraft:regeneration"),
                    num("onEatEffectLevel", "Effect Level", 1, 10).def(1),
                    num("onEatEffectSeconds", "Effect Duration (s)", 1, 3600).def(30),
                    num("eatHeal", "Heals on Eating (HP)", 0, 1024).def(0),
                    num("eatXp", "XP on Eating", 0, 100000).def(0),
                    toggle("wolfFood", "Wolves Eat It").help("Tamed wolves can be fed and healed with it."))
    );

    // Blocks --------------------------------------------------------------------------------

    private static final List<Category> BLOCK = List.of(
            cat("Physical", "stone",
                    num("hardness", "Hardness", -1, 1000).random().help("How long it takes to mine. -1 = unbreakable, 0 = instant."),
                    num("blastResistance", "Blast Resistance", 0, 3600000).random().typeOnly().help("Obsidian is 1200."),
                    num("slipperiness", "Slipperiness", 0, 1.2).random().typeOnly().help("0.6 is normal, ice is 0.98."),
                    num("jumpFactor", "Jump Height x", 0, 10).random().typeOnly().help("Honey is 0.5."),
                    num("speedFactor", "Walk Speed x", 0, 10).random().typeOnly().help("Soul sand is 0.4."),
                    num("bounciness", "Bounciness", 0, 2).def(0).typeOnly().help("Slime blocks are 1 (bounce back as high as you fell)."),
                    num("landingDamageMultiplier", "Landing Damage x", 0, 10).def(1).help("Fall damage when landing on it. Hay bales are 0.2."),
                    toggle("noCollision", "No Collision").help("You can walk through it."),
                    toggle("canFall", "Falls Like Sand").typeOnly(),
                    toggle("climbable", "Climbable").help("Climb it like a ladder."),
                    toggle("requiresCorrectTool", "Needs Correct Tool").typeOnly().help("Drops nothing unless mined with the right tool."),
                    toggle("replaceable", "Replaceable").typeOnly().help("Other blocks can be placed into it, like tall grass."),
                    choice("pushReaction", "Piston Reaction", "normal", "destroy", "block", "ignore", "push_only").typeOnly()),
            cat("Light & Sound", "lantern",
                    num("lightEmission", "Light Level", 0, 15).random().typeOnly(),
                    toggle("invisible", "Invisible").typeOnly().help("Not drawn at all (still solid)."),
                    choice("soundType", "Sound", BlockStats.VANILLA, "stone", "wood", "grass", "gravel", "sand", "snow",
                            "wool", "glass", "metal", "slime", "honey", "amethyst", "bone", "netherrack").typeOnly()),
            cat("Redstone & Growth", "redstone",
                    num("redstonePower", "Redstone Power", 0, 15).def(0).typeOnly().help("Powers redstone around it like a redstone block."),
                    num("randomTickMultiplier", "Growth Speed x", 0, 64).def(1).typeOnly().help("Crops, saplings and other growing blocks. 0 = never grows."),
                    choice("mobSpawning", "Mobs Spawn On It", BlockStats.VANILLA, "always", "never")),
            cat("Step-On Effects", "magma_block",
                    num("stepDamage", "Damage (every 0.5 s)", 0, 1000).def(0),
                    num("speedModifier", "Speed x", 0, 10).def(1),
                    num("healOnStep", "Heals (HP/s)", 0, 100).def(0),
                    num("fireOnStep", "Sets on Fire (s)", 0, 600).def(0),
                    toggle("extinguishOnStep", "Puts Out Fire"),
                    toggle("levitate", "Levitation"),
                    toggle("glowOnStep", "Glowing"),
                    toggle("freezeOnStep", "Freezing"),
                    text("onStepPotion", "Effect", ID_EFFECT).help("Any effect id, e.g. minecraft:speed."),
                    num("onStepPotionLevel", "Effect Level", 1, 10).def(1),
                    num("onStepPotionDuration", "Effect Duration (ticks)", 25, 72000).def(60).help("20 ticks = 1 second.")),
            cat("Step-On Fun", "slime_block",
                    num("launchPower", "Launch Pad Power", 0, 10).def(0).help("Throws you into the air. 1 = about 10 blocks."),
                    choice("conveyorDirection", "Conveyor Belt", BlockStats.OFF, "north", "south", "east", "west"),
                    num("conveyorSpeed", "Conveyor Speed", 0, 2).def(0.2),
                    num("teleportOnStep", "Random Teleport (radius)", 0, 64).def(0),
                    num("knockbackOnStep", "Bounces You Away", 0, 10).def(0),
                    num("hungerOnStep", "Drains Hunger", 0, 40).def(0).help("Exhaustion per second (4 = one hunger point)."),
                    num("feedOnStep", "Feeds (food/s)", 0, 20).def(0),
                    num("xpOnStep", "Gives XP (per s)", 0, 1000).def(0),
                    toggle("stickyInside", "Sticky Like Cobweb").help("Slows you down while inside it. Combine with No Collision."),
                    toggle("itemVoid", "Deletes Dropped Items").help("Items that land on it disappear.")),
            cat("Breaking", "diamond_pickaxe",
                    num("dropXp", "XP Dropped", 0, 100000),
                    toggle("noDrops", "Drops Nothing"),
                    num("dropMultiplier", "Drop Multiplier", 1, 64).def(1).help("Drops its items this many times."),
                    text("dropOverride", "Drops Instead", ID_ITEM).help("Item dropped instead of the normal drops."),
                    num("dropOverrideCount", "Drop Count", 1, 64).def(1),
                    toggle("silkTouchOnly", "Needs Silk Touch").help("Drops nothing unless mined with Silk Touch."),
                    num("explodeOnBreak", "Explodes When Broken (power)", 0, 20).def(0),
                    toggle("lightningOnBreak", "Lightning When Broken"),
                    text("spawnOnBreak", "Mob Spawns When Broken", ID_ENTITY).help("e.g. minecraft:silverfish"),
                    num("spawnOnBreakCount", "Mob Count", 1, 16).def(1),
                    num("damageBreaker", "Hurts the Breaker (HP)", 0, 1024).def(0),
                    num("regenSeconds", "Grows Back After (s)", 1, 3600).help("The block comes back by itself after this many seconds."))
    );

    // Projectiles ---------------------------------------------------------------------------

    private static final List<Category> PROJECTILE = List.of(
            cat("Flight", "arrow",
                    num("damageMultiplier", "Damage x", 0, 1000).def(1).random(),
                    num("extraDamage", "Extra Damage", 0, 1000).def(0),
                    num("speedMultiplier", "Speed x", 0.05, 10).def(1).random(),
                    num("gravityMultiplier", "Gravity x", -5, 5).def(1).random(),
                    toggle("noGravity", "No Gravity").help("Flies in a straight line."),
                    toggle("homing", "Homing").help("Steers towards the nearest mob."),
                    toggle("alwaysCrit", "Always Critical").help("Arrows only."),
                    num("piercing", "Piercing", 0, 10).def(0).help("Goes through this many mobs. Arrows and tridents only."),
                    num("lifetimeSeconds", "Disappears After (s)", 1, 600).help("Removed this many seconds after being fired."),
                    toggle("noPickup", "Can't Be Picked Up").help("Arrows and tridents only.")),
            cat("On Hit", "tnt",
                    num("explodeOnHit", "Explodes on Hit (power)", 0, 20).def(0),
                    toggle("lightningOnHit", "Lightning on Hit"),
                    num("fireOnHit", "Sets Target on Fire (s)", 0, 600).def(0),
                    num("knockbackBonus", "Extra Knockback", 0, 10).def(0),
                    text("hitEffect", "Hit Effect", ID_EFFECT),
                    num("hitEffectLevel", "Hit Effect Level", 1, 10).def(1),
                    num("hitEffectSeconds", "Hit Effect Duration (s)", 1, 600).def(5),
                    text("spawnOnHit", "Spawns Mob on Hit", ID_ENTITY).help("e.g. minecraft:chicken"),
                    toggle("teleportShooter", "Teleports the Shooter").help("Like an ender pearl."))
    );

    // Enchantments --------------------------------------------------------------------------

    private static final List<Category> ENCHANTMENT = List.of(
            cat("Enchantment", "enchanted_book",
                    num("maxLevel", "Max Level", 1, 255).help("Highest level from the table and anvils."),
                    num("levelBonus", "Level Bonus", -10, 50).def(0).help("Every item with this enchantment acts as if the level was this much higher."),
                    toggle("disabled", "Disabled").help("The enchantment does nothing."),
                    num("weight", "Table Chance (weight)", 1, 1024).help("How often it shows up in the enchanting table. Higher = more often."),
                    num("anvilCost", "Anvil Cost", 0, 100).help("Base level cost when combining in an anvil."),
                    toggle("anyItem", "Works on Any Item").help("Can be put on any item with an anvil."),
                    toggle("ignoreConflicts", "Ignores Conflicts").help("Can be combined with enchantments it normally conflicts with."))
    );

    // World ---------------------------------------------------------------------------------

    private static final List<Category> WORLD = List.of(
            cat("Time & Weather", "clock",
                    num("dayLengthMultiplier", "Day Length x", 0.05, 20).def(1).help("2 = days last twice as long."),
                    toggle("alwaysClear", "Always Clear Weather")),
            cat("Physics", "sand",
                    num("gravityMultiplier", "Gravity x", -2, 5).def(1).help("Affects every mob, player and item."),
                    num("fallDamageMultiplier", "Fall Damage x", 0, 100).def(1),
                    num("explosionPowerMultiplier", "Explosion Power x", 0, 10).def(1)),
            cat("Mobs", "zombie_head",
                    num("mobHealthMultiplier", "Mob Health x", 0.05, 100).def(1),
                    num("mobDamageMultiplier", "Mob Damage x", 0, 100).def(1),
                    num("mobSpeedMultiplier", "Mob Speed x", 0, 10).def(1),
                    num("spawnCapMultiplier", "Mob Spawn Amount x", 0, 20).def(1).help("How many mobs can exist at once. 0 = no natural spawning."),
                    toggle("mobsNeverDespawn", "Mobs Never Despawn"),
                    toggle("mobsIgnorePlayers", "Mobs Ignore Players").help("No mob ever attacks a player.")),
            cat("Players", "player_head",
                    num("playerDamageTakenMultiplier", "Player Damage Taken x", 0, 100).def(1),
                    num("hungerMultiplier", "Hunger x", 0, 100).def(1),
                    num("xpMultiplier", "XP Orbs x", 0, 1000).def(1),
                    num("itemDespawnSeconds", "Item Despawn Time (s)", 10, 36000).def(300).help("How long dropped items stay (300 = 5 minutes)."))
    );
}
