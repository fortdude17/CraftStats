package com.craftstats.common.preset;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.*;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Built-in presets plus user presets stored as JSON in {@code <game dir>/craftstats/presets/}. */
public final class PresetManager {

    private static Path presetsDir;
    private static final List<Preset> BUILTIN = new ArrayList<>();
    private static final List<Preset> CUSTOM  = Collections.synchronizedList(new ArrayList<>());

    private PresetManager() {}

    public static void init() {
        presetsDir = Platform.getGameFolder().resolve("craftstats").resolve("presets");
        buildBuiltins();
        reload();
    }

    public static void reload() {
        CUSTOM.clear();
        if (!Files.isDirectory(presetsDir)) return;
        try (Stream<Path> files = Files.list(presetsDir)) {
            files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(PresetManager::loadFile);
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: failed to list presets in {}", presetsDir, e);
        }
    }

    private static void loadFile(Path path) {
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CUSTOM.add(Preset.fromJson(JsonParser.parseReader(r)));
        } catch (Exception e) {
            CraftStats.LOGGER.warn("CraftStats: skipping invalid preset {}: {}", path.getFileName(), e.toString());
        }
    }

    /** Saves (or overwrites) a custom preset. Returns false if the name is taken by a built-in. */
    public static boolean save(Preset preset) {
        if (isBuiltinName(preset.name, preset.targetType)) return false;
        try {
            Files.createDirectories(presetsDir);
            Files.writeString(fileFor(preset.name, preset.targetType),
                    StatSchema.GSON.toJson(preset.toJson()), StandardCharsets.UTF_8);
            CUSTOM.removeIf(p -> p.name.equals(preset.name) && p.targetType == preset.targetType);
            CUSTOM.add(preset);
            return true;
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: failed to save preset '{}'", preset.name, e);
            return false;
        }
    }

    public static boolean delete(Preset preset) {
        if (preset.readonly) return false;
        CUSTOM.remove(preset);
        try {
            return Files.deleteIfExists(fileFor(preset.name, preset.targetType));
        } catch (IOException e) {
            CraftStats.LOGGER.error("CraftStats: failed to delete preset '{}'", preset.name, e);
            return false;
        }
    }

    public static List<Preset> getAll() {
        List<Preset> all = new ArrayList<>(BUILTIN);
        synchronized (CUSTOM) { all.addAll(CUSTOM); }
        return all;
    }

    public static List<Preset> getForType(TargetType type) {
        return getAll().stream().filter(p -> p.targetType == type).toList();
    }

    private static boolean isBuiltinName(String name, TargetType type) {
        return BUILTIN.stream().anyMatch(p -> p.name.equals(name) && p.targetType == type);
    }

    private static Path fileFor(String name, TargetType type) {
        String safe = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return presetsDir.resolve(type.name().toLowerCase() + "_" + safe + ".json");
    }

    // ---- built-ins ---------------------------------------------------------------------------

    private static void buildBuiltins() {
        BUILTIN.clear();
        add("vanilla", TargetType.MOB, new MobStats());
        MobStats tank = new MobStats();
        tank.maxHealth = 200.0; tank.armor = 20.0; tank.knockbackResist = 1.0; tank.moveSpeed = 0.2;
        add("tank", TargetType.MOB, tank);
        MobStats glass = new MobStats();
        glass.maxHealth = 2.0; glass.attackDamage = 20.0; glass.moveSpeed = 0.35;
        add("glass_cannon", TargetType.MOB, glass);
        MobStats boss = new MobStats();
        boss.maxHealth = 500.0; boss.attackDamage = 15.0; boss.sizeScale = 2.5; boss.knockbackResist = 1.0;
        boss.immuneFire = true; boss.immuneFall = true; boss.immuneExplosion = true; boss.canDespawn = false;
        boss.glowing = true; boss.xpReward = 500; boss.regenPerSecond = 1.0; boss.thornsPercent = 25.0;
        add("boss", TargetType.MOB, boss);
        MobStats statue = new MobStats();
        statue.noAi = true; statue.invincible = true; statue.silent = true; statue.noPush = true; statue.canDespawn = false;
        add("statue", TargetType.MOB, statue);
        MobStats berserker = new MobStats();
        berserker.hostile = true; berserker.moveSpeed = 0.35; berserker.damageDealtMultiplier = 2.0; berserker.lifestealPercent = 50.0;
        add("berserker", TargetType.MOB, berserker);
        MobStats creeperish = new MobStats();
        creeperish.explodeOnDeath = 3f; creeperish.lightningOnDeath = true;
        add("explodes_on_death", TargetType.MOB, creeperish);

        add("vanilla", TargetType.BLOCK, new BlockStats());
        BlockStats reinforced = new BlockStats();
        reinforced.hardness = 50f; reinforced.blastResistance = 1200f; reinforced.pushReaction = "block";
        add("reinforced", TargetType.BLOCK, reinforced);
        BlockStats ice = new BlockStats();
        ice.slipperiness = 0.98f;
        add("slippery", TargetType.BLOCK, ice);
        BlockStats levitation = new BlockStats();
        levitation.levitate = true; levitation.glowOnStep = true;
        add("levitation_pad", TargetType.BLOCK, levitation);
        BlockStats trampoline = new BlockStats();
        trampoline.bounciness = 1.0f; trampoline.landingDamageMultiplier = 0f;
        add("trampoline", TargetType.BLOCK, trampoline);
        BlockStats launch = new BlockStats();
        launch.launchPower = 1.5f; launch.landingDamageMultiplier = 0f;
        add("launch_pad", TargetType.BLOCK, launch);
        BlockStats landmine = new BlockStats();
        landmine.explodeOnBreak = 3f; landmine.noDrops = true;
        add("booby_trap", TargetType.BLOCK, landmine);

        add("vanilla", TargetType.ITEM, new ItemStats());
        ItemStats op = new ItemStats();
        op.attackDamage = 50.0; op.attackSpeed = 8.0; op.unbreakable = true; op.fireproof = true; op.itemGlow = true;
        add("overpowered", TargetType.ITEM, op);
        ItemStats snack = new ItemStats();
        snack.edible = ItemStats.EDIBLE_YES; snack.nutrition = 4; snack.saturation = 2.4f; snack.eatSeconds = 0.8f;
        add("quick_snack", TargetType.ITEM, snack);
        ItemStats boomerang = new ItemStats();
        boomerang.throwable = true; boomerang.boomerang = true; boomerang.throwDamage = 6f;
        add("boomerang", TargetType.ITEM, boomerang);
        ItemStats grenade = new ItemStats();
        grenade.throwable = true; grenade.explodeOnImpact = 2.5f; grenade.throwDamage = 0f; grenade.useCooldown = 1f;
        add("grenade", TargetType.ITEM, grenade);
        ItemStats charm = new ItemStats();
        charm.bonusSlot = "any"; charm.bonusMoveSpeed = 0.05; charm.bonusJump = 0.2; charm.bonusSafeFall = 10.0;
        add("speed_charm", TargetType.ITEM, charm);

        add("vanilla", TargetType.PLAYER, new PlayerStats());
        PlayerStats builder = new PlayerStats();
        builder.reachDistance = 8.0; builder.noFallDamage = true; builder.walkSpeed = 0.13; builder.flySpeed = 0.1;
        add("builder", TargetType.PLAYER, builder);
        PlayerStats pvp = new PlayerStats();
        pvp.maxHealth = 30.0; pvp.baseDamage = 3.0; pvp.attackSpeed = 6.0; pvp.critMultiplier = 2.0;
        add("pvp", TargetType.PLAYER, pvp);
        PlayerStats runner = new PlayerStats();
        runner.walkSpeed = 0.18; runner.jumpForce = 0.6; runner.stepHeight = 1.0; runner.infiniteSprint = true;
        runner.noFallDamage = true;
        add("speedrunner", TargetType.PLAYER, runner);
        PlayerStats god = new PlayerStats();
        god.maxHealth = 100.0; god.godMode = true; god.keepInventory = true; god.fxNightVision = true;
        add("god_mode", TargetType.PLAYER, god);
        PlayerStats giant = new PlayerStats();
        giant.size = 3.0; giant.reachDistance = 9.0; giant.entityReach = 6.0; giant.maxHealth = 60.0;
        add("giant", TargetType.PLAYER, giant);

        add("vanilla", TargetType.PROJECTILE, new ProjectileStats());
        ProjectileStats explosive = new ProjectileStats();
        explosive.explodeOnHit = 2.0f;
        add("explosive", TargetType.PROJECTILE, explosive);
        ProjectileStats homing = new ProjectileStats();
        homing.homing = true; homing.speedMultiplier = 1.5;
        add("homing", TargetType.PROJECTILE, homing);
        ProjectileStats laser = new ProjectileStats();
        laser.noGravity = true; laser.speedMultiplier = 3.0; laser.piercing = 5; laser.lifetimeSeconds = 5;
        add("laser", TargetType.PROJECTILE, laser);

        add("vanilla", TargetType.ENCHANTMENT, new EnchantmentStats());
        EnchantmentStats overcharged = new EnchantmentStats();
        overcharged.levelBonus = 3; overcharged.maxLevel = 10;
        add("overcharged", TargetType.ENCHANTMENT, overcharged);
        EnchantmentStats universal = new EnchantmentStats();
        universal.anyItem = true; universal.ignoreConflicts = true;
        add("universal", TargetType.ENCHANTMENT, universal);

        add("vanilla", TargetType.WORLD, new WorldStats());
        WorldStats moon = new WorldStats();
        moon.gravityMultiplier = 0.3; moon.fallDamageMultiplier = 0.2;
        add("moon_gravity", TargetType.WORLD, moon);
        WorldStats hard = new WorldStats();
        hard.mobHealthMultiplier = 2.0; hard.mobDamageMultiplier = 2.0; hard.spawnCapMultiplier = 2.0;
        add("hard_mode", TargetType.WORLD, hard);
        WorldStats chill = new WorldStats();
        chill.mobsIgnorePlayers = true; chill.alwaysClear = true; chill.dayLengthMultiplier = 3.0;
        add("chill", TargetType.WORLD, chill);
    }

    private static void add(String name, TargetType type, Object stats) {
        Preset p = new Preset(name, type, stats);
        p.readonly = true;
        BUILTIN.add(p);
    }
}
