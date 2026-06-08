package com.craftstats.common.preset;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.*;
import dev.architectury.platform.Platform;
import com.craftstats.common.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PresetManager {

    private static Path presetsDir;
    private static final List<Preset> BUILTIN = new ArrayList<>();
    private static final List<Preset> CUSTOM  = new ArrayList<>();

    public static void init() {
        presetsDir = Platform.getGameFolder().resolve("craftstats").resolve("presets");
        buildBuiltins();
        reload();
    }

    public static void reload() {
        CUSTOM.clear();
        if (!Files.exists(presetsDir)) return;
        try {
            Files.list(presetsDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(PresetManager::loadPresetFile);
        } catch (IOException e) {
            CraftStats.LOGGER.error("Failed to list presets directory", e);
        }
    }

    private static void loadPresetFile(Path path) {
        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            String typeStr = obj.get("target_type").getAsString();
            TargetType type = TargetType.valueOf(typeStr.toUpperCase());
            Object stats = switch (type) {
                case MOB    -> JsonUtil.GSON.fromJson(obj.get("stats"), MobStats.class);
                case BLOCK  -> JsonUtil.GSON.fromJson(obj.get("stats"), BlockStats.class);
                case ITEM   -> JsonUtil.GSON.fromJson(obj.get("stats"), ItemStats.class);
                case PLAYER -> JsonUtil.GSON.fromJson(obj.get("stats"), PlayerStats.class);
            };
            Preset p = new Preset(obj.get("name").getAsString(), type, stats);
            if (obj.has("seed")) p.seed = obj.get("seed").getAsLong();
            CUSTOM.add(p);
        } catch (Exception e) {
            CraftStats.LOGGER.error("Failed to load preset: {}", path, e);
        }
    }

    public static void savePreset(Preset preset) {
        try {
            Files.createDirectories(presetsDir);
            String safeName = preset.name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            Path file = presetsDir.resolve(safeName + ".json");
            try (Writer w = Files.newBufferedWriter(file)) {
                JsonUtil.GSON.toJson(preset, w);
            }
            if (!CUSTOM.contains(preset)) CUSTOM.add(preset);
        } catch (IOException e) {
            CraftStats.LOGGER.error("Failed to save preset '{}'", preset.name, e);
        }
    }

    public static boolean deletePreset(String name) {
        CUSTOM.removeIf(p -> p.name.equals(name));
        String safeName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        Path file = presetsDir.resolve(safeName + ".json");
        try { return Files.deleteIfExists(file); }
        catch (IOException e) {
            CraftStats.LOGGER.error("Failed to delete preset '{}'", name, e);
            return false;
        }
    }

    public static boolean renamePreset(String oldName, String newName) {
        CUSTOM.stream().filter(p -> p.name.equals(oldName)).findFirst().ifPresent(p -> {
            deletePreset(oldName);
            p.name = newName;
            savePreset(p);
        });
        return true;
    }

    public static List<Preset> getBuiltin() { return Collections.unmodifiableList(BUILTIN); }
    public static List<Preset> getCustom()  { return Collections.unmodifiableList(CUSTOM); }

    public static List<Preset> getAll() {
        List<Preset> all = new ArrayList<>(BUILTIN);
        all.addAll(CUSTOM);
        return all;
    }

    public static List<Preset> getForType(TargetType type) {
        return getAll().stream().filter(p -> p.targetType == type).toList();
    }

    private static void buildBuiltins() {

        addBuiltinMob("vanilla_default", makeMobVanilla());
        addBuiltinMob("overpowered",     makeMobOverpowered());
        addBuiltinMob("glass_cannon",    makeMobGlassCannon());
        addBuiltinMob("chaos",           makeMobChaos());

        addBuiltinBlock("vanilla_default", makeBlockVanilla());
        addBuiltinBlock("overpowered",     makeBlockOverpowered());
        addBuiltinBlock("glass_cannon",    makeBlockGlass());
        addBuiltinBlock("chaos",           makeBlockChaos());

        addBuiltinItem("vanilla_default", makeItemVanilla());
        addBuiltinItem("overpowered",     makeItemOverpowered());
        addBuiltinItem("glass_cannon",    makeItemGlass());
        addBuiltinItem("chaos",           makeItemChaos());

        addBuiltinPlayer("vanilla_default", makePlayerVanilla());
        addBuiltinPlayer("overpowered",     makePlayerOverpowered());
        addBuiltinPlayer("glass_cannon",    makePlayerGlass());
        addBuiltinPlayer("chaos",           makePlayerChaos());
    }

    private static void addBuiltinMob(String n, MobStats s) {
        Preset p = new Preset(n, TargetType.MOB, s); p.readonly = true; BUILTIN.add(p);
    }
    private static void addBuiltinBlock(String n, BlockStats s) {
        Preset p = new Preset(n, TargetType.BLOCK, s); p.readonly = true; BUILTIN.add(p);
    }
    private static void addBuiltinItem(String n, ItemStats s) {
        Preset p = new Preset(n, TargetType.ITEM, s); p.readonly = true; BUILTIN.add(p);
    }
    private static void addBuiltinPlayer(String n, PlayerStats s) {
        Preset p = new Preset(n, TargetType.PLAYER, s); p.readonly = true; BUILTIN.add(p);
    }

    private static MobStats makeMobVanilla()      { return new MobStats(); }
    private static MobStats makeMobOverpowered()  { MobStats s = new MobStats(); s.maxHealth = 2000; s.attackDamage = 50; s.moveSpeed = 0.5; s.armor = 20; return s; }
    private static MobStats makeMobGlassCannon()  { MobStats s = new MobStats(); s.maxHealth = 2; s.attackDamage = 100; s.moveSpeed = 0.4; return s; }
    private static MobStats makeMobChaos()        { MobStats s = new MobStats(); s.maxHealth = 500; s.attackDamage = 30; s.knockbackResist = 1.0; s.immuneFire = true; s.immuneExplosion = true; s.burnsDaylight = true; return s; }

    private static BlockStats makeBlockVanilla()     { return new BlockStats(); }
    private static BlockStats makeBlockOverpowered() { BlockStats s = new BlockStats(); s.hardness = 999; s.blastResistance = 3600000; s.lightEmission = 15; return s; }
    private static BlockStats makeBlockGlass()       { BlockStats s = new BlockStats(); s.hardness = 0; s.blastResistance = 0; return s; }
    private static BlockStats makeBlockChaos()       { BlockStats s = new BlockStats(); s.hardness = -1; s.lightEmission = 15; s.slipperiness = 0.98f; s.bounceFactor = 2.0f; return s; }

    private static ItemStats makeItemVanilla()      { return new ItemStats(); }
    private static ItemStats makeItemOverpowered()  { ItemStats s = new ItemStats(); s.attackDamage = 100; s.attackSpeed = 8; s.maxDurability = 100000; return s; }
    private static ItemStats makeItemGlass()        { ItemStats s = new ItemStats(); s.attackDamage = 200; s.maxDurability = 1; return s; }
    private static ItemStats makeItemChaos()        { ItemStats s = new ItemStats(); s.attackDamage = 50; s.throwable = true; s.boomerang = true; s.itemGlow = true; return s; }

    private static PlayerStats makePlayerVanilla()      { return new PlayerStats(); }
    private static PlayerStats makePlayerOverpowered()  { PlayerStats s = new PlayerStats(); s.maxHealth = 200; s.walkSpeed = 0.2; s.flySpeed = 0.2; s.baseDamage = 20; s.godMode = true; return s; }
    private static PlayerStats makePlayerGlass()        { PlayerStats s = new PlayerStats(); s.maxHealth = 1; s.baseDamage = 50; return s; }
    private static PlayerStats makePlayerChaos()        { PlayerStats s = new PlayerStats(); s.maxHealth = 100; s.noFallDamage = true; s.fireImmune = true; s.infiniteSprint = true; s.keepInventory = true; return s; }
}
