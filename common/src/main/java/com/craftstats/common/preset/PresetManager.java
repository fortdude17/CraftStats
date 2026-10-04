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
        boss.glowing = true; boss.xpReward = 500;
        add("boss", TargetType.MOB, boss);

        add("vanilla", TargetType.BLOCK, new BlockStats());
        BlockStats reinforced = new BlockStats();
        reinforced.hardness = 50f; reinforced.blastResistance = 1200f; reinforced.pushReaction = "block";
        add("reinforced", TargetType.BLOCK, reinforced);
        BlockStats ice = new BlockStats();
        ice.slipperiness = 0.98f;
        add("slippery", TargetType.BLOCK, ice);
        BlockStats trampoline = new BlockStats();
        trampoline.levitate = true; trampoline.glowOnStep = true;
        add("levitation_pad", TargetType.BLOCK, trampoline);

        add("vanilla", TargetType.ITEM, new ItemStats());
        ItemStats op = new ItemStats();
        op.attackDamage = 50.0; op.attackSpeed = 8.0; op.unbreakable = true; op.fireproof = true; op.itemGlow = true;
        add("overpowered", TargetType.ITEM, op);
        ItemStats snack = new ItemStats();
        snack.edible = ItemStats.EDIBLE_YES; snack.nutrition = 4; snack.saturation = 2.4f; snack.eatSeconds = 0.8f;
        add("quick_snack", TargetType.ITEM, snack);

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
    }

    private static void add(String name, TargetType type, Object stats) {
        Preset p = new Preset(name, type, stats);
        p.readonly = true;
        BUILTIN.add(p);
    }
}
