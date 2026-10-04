package com.craftstats.common.command;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.item.ModItems;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public final class CraftStatsCommands {

    private static final SuggestionProvider<CommandSourceStack> EDITABLE_TYPES = (ctx, b) ->
            SharedSuggestionProvider.suggest(new String[]{"mob", "block", "item"}, b);
    private static final SuggestionProvider<CommandSourceStack> ALL_TYPES = (ctx, b) ->
            SharedSuggestionProvider.suggest(new String[]{"mob", "block", "item", "player"}, b);

    private static final SuggestionProvider<CommandSourceStack> TARGET_IDS = (ctx, b) -> {
        String type = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "mob"    -> SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Object::toString), b);
            case "block"  -> SharedSuggestionProvider.suggest(BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString), b);
            case "item"   -> SharedSuggestionProvider.suggest(BuiltInRegistries.ITEM.keySet().stream().map(Object::toString), b);
            case "player" -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), b);
            default -> b.buildFuture();
        };
    };

    private static final SuggestionProvider<CommandSourceStack> PRESET_NAMES = (ctx, b) ->
            SharedSuggestionProvider.suggest(PresetManager.getAll().stream().map(p -> p.name).distinct(), b);

    private CraftStatsCommands() {}

    private static Predicate<CommandSourceStack> gameMasters() {
        //#if MC >= 1.21.11
        //$ return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        //#else
        return src -> src.hasPermission(2);
        //#endif
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("craftstats")
                .requires(gameMasters())

                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    if (!target.getInventory().add(new ItemStack(ModItems.CRAFT_WAND.get())))
                                        target.drop(new ItemStack(ModItems.CRAFT_WAND.get()), false);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Gave a Craft Wand to " + target.getName().getString()), true);
                                    return 1;
                                })))

                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            CraftStatsConfig.load();
                            PresetManager.reload();
                            ctx.getSource().sendSuccess(() -> Component.literal("CraftStats config and presets reloaded."), true);
                            return 1;
                        }))

                .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                                .executes(ctx -> {
                                    CraftStatsNetwork.resetAll(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Removed every CraftStats override in this world."), true);
                                    return 1;
                                }))
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(ALL_TYPES)
                                .then(Commands.argument("id", StringArgumentType.greedyString()).suggests(TARGET_IDS)
                                        .executes(CraftStatsCommands::reset))))

                .then(Commands.literal("preset")
                        .then(Commands.literal("load")
                                .then(Commands.argument("name", StringArgumentType.string()).suggests(PRESET_NAMES)
                                        .then(Commands.argument("type", StringArgumentType.word()).suggests(ALL_TYPES)
                                                .then(Commands.argument("id", StringArgumentType.greedyString()).suggests(TARGET_IDS)
                                                        .executes(CraftStatsCommands::loadPreset))))))

                // "<id> [seed]" is one greedy argument: plain word arguments can't contain ':'.
                .then(Commands.literal("randomize")
                        .then(Commands.argument("type", StringArgumentType.word()).suggests(EDITABLE_TYPES)
                                .then(Commands.argument("id", StringArgumentType.greedyString()).suggests(TARGET_IDS)
                                        .executes(CraftStatsCommands::randomize))))
        );
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MinecraftServer server = src.getServer();
        String id = StringArgumentType.getString(ctx, "id").trim();
        TargetType type = parseType(src, StringArgumentType.getString(ctx, "type"));
        if (type == null) return 0;
        switch (type) {
            case MOB -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ENTITY_TYPE, id);
                if (rl == null) return 0;
                StatRegistry.removeMob(rl);
                StatApplier.refreshMobs(server, rl);
            }
            case BLOCK -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.BLOCK, id);
                if (rl == null) return 0;
                StatRegistry.removeBlock(rl);
            }
            case ITEM -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ITEM, id);
                if (rl == null) return 0;
                StatRegistry.removeItem(rl);
            }
            case PLAYER -> {
                ServerPlayer p = server.getPlayerList().getPlayerByName(id);
                if (p == null) { src.sendFailure(Component.literal("No online player named " + id)); return 0; }
                StatRegistry.removePlayer(p.getUUID());
                StatApplier.applyPlayer(p);
            }
        }
        CraftStatsNetwork.changed(server);
        src.sendSuccess(() -> Component.literal("Reset " + type.displayName().toLowerCase() + " " + id + " to vanilla."), true);
        return 1;
    }

    private static int loadPreset(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!CraftStatsConfig.get().enablePresets) {
            src.sendFailure(Component.literal("Presets are disabled in the CraftStats config."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        String name = StringArgumentType.getString(ctx, "name");
        String id = StringArgumentType.getString(ctx, "id").trim();
        TargetType type = parseType(src, StringArgumentType.getString(ctx, "type"));
        if (type == null) return 0;
        Optional<Preset> preset = PresetManager.getForType(type).stream().filter(p -> p.name.equals(name)).findFirst();
        if (preset.isEmpty()) {
            src.sendFailure(Component.literal("No " + type.displayName().toLowerCase() + " preset named '" + name + "'."));
            return 0;
        }
        Object stats = preset.get().copyStats();
        switch (type) {
            case MOB -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ENTITY_TYPE, id);
                if (rl == null) return 0;
                StatRegistry.setMob(rl, (MobStats) stats);
                StatApplier.refreshMobs(server, rl);
            }
            case BLOCK -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.BLOCK, id);
                if (rl == null) return 0;
                StatRegistry.setBlock(rl, (BlockStats) stats);
            }
            case ITEM -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ITEM, id);
                if (rl == null) return 0;
                StatRegistry.setItem(rl, (ItemStats) stats);
            }
            case PLAYER -> {
                ServerPlayer p = server.getPlayerList().getPlayerByName(id);
                if (p == null) { src.sendFailure(Component.literal("No online player named " + id)); return 0; }
                StatRegistry.setPlayer(p.getUUID(), (PlayerStats) stats);
                StatApplier.applyPlayer(p);
            }
        }
        CraftStatsNetwork.changed(server);
        src.sendSuccess(() -> Component.literal("Applied preset '" + name + "' to " + id + "."), true);
        return 1;
    }

    private static int randomize(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String[] parts = StringArgumentType.getString(ctx, "id").trim().split("\\s+");
        String id = parts[0];
        long seed;
        try {
            seed = parts.length > 1 ? Long.parseLong(parts[1]) : RandomizeManager.newSeed();
        } catch (NumberFormatException e) {
            src.sendFailure(Component.literal("Seed must be a number: " + parts[1]));
            return 0;
        }
        if (!CraftStatsConfig.get().enableRandomize) {
            src.sendFailure(Component.literal("Randomize is disabled in the CraftStats config."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        TargetType type = parseType(src, StringArgumentType.getString(ctx, "type"));
        if (type == null) return 0;
        switch (type) {
            case MOB -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ENTITY_TYPE, id);
                if (rl == null) return 0;
                EntityType<?> entityType = Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, rl);
                StatRegistry.setMob(rl, RandomizeManager.randomizeMob(VanillaStats.mob(entityType), seed));
                StatApplier.refreshMobs(server, rl);
            }
            case BLOCK -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.BLOCK, id);
                if (rl == null) return 0;
                StatRegistry.setBlock(rl, RandomizeManager.randomizeBlock(VanillaStats.block(Compat.registryValue(BuiltInRegistries.BLOCK, rl)), seed));
            }
            case ITEM -> {
                ResourceLocation rl = parseId(src, BuiltInRegistries.ITEM, id);
                if (rl == null) return 0;
                StatRegistry.setItem(rl, RandomizeManager.randomizeItem(VanillaStats.item(Compat.registryValue(BuiltInRegistries.ITEM, rl)), seed));
            }
            case PLAYER -> {
                src.sendFailure(Component.literal("Use the player editor to randomize players."));
                return 0;
            }
        }
        CraftStatsNetwork.changed(server);
        src.sendSuccess(() -> Component.literal("Randomized " + id + " (" + RandomizeManager.getIntensity().name().toLowerCase()
                + ", seed " + seed + ")."), true);
        return 1;
    }

    private static TargetType parseType(CommandSourceStack src, String raw) {
        try {
            return TargetType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            src.sendFailure(Component.literal("Unknown type '" + raw + "'. Use one of: "
                    + String.join(", ", Arrays.stream(TargetType.values()).map(t -> t.name().toLowerCase()).toList())));
            return null;
        }
    }

    private static <T> ResourceLocation parseId(CommandSourceStack src, Registry<T> registry, String raw) {
        ResourceLocation rl = ResourceLocation.tryParse(raw);
        if (rl == null || !registry.containsKey(rl)) {
            src.sendFailure(Component.literal("Unknown id '" + raw + "'."));
            return null;
        }
        return rl;
    }
}
