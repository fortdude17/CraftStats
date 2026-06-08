package com.craftstats.common.command;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.item.ModItems;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class CraftStatsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("craftstats")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ItemStack wand = new ItemStack(ModItems.CRAFT_WAND.get());
                            target.getInventory().add(wand);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Gave Craft Wand to " + target.getName().getString()), true);
                            return 1;
                        })
                    )
                )

                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        CraftStatsConfig.load();
                        PresetManager.reload();
                        ctx.getSource().sendSuccess(() -> Component.literal("CraftStats config reloaded."), true);
                        return 1;
                    })
                )

                .then(Commands.literal("reset")
                    .then(Commands.argument("target_type", StringArgumentType.word())
                        .then(Commands.argument("target_id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String ttype = StringArgumentType.getString(ctx, "target_type");
                                String tid   = StringArgumentType.getString(ctx, "target_id");
                                ResourceLocation rl = ResourceLocation.parse(tid);
                                switch (ttype) {
                                    case "mob"   -> StatRegistry.removeMob(rl);
                                    case "block" -> StatRegistry.removeBlock(rl);
                                    case "item"  -> StatRegistry.removeItem(rl);
                                    default -> {
                                        ctx.getSource().sendFailure(Component.literal("Unknown target type: " + ttype));
                                        return 0;
                                    }
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "Reset " + ttype + " " + tid + " to vanilla defaults."), true);
                                return 1;
                            })
                        )
                    )
                )

                .then(Commands.literal("preset")
                    .then(Commands.literal("load")
                        .then(Commands.argument("preset_name", StringArgumentType.word())
                            .then(Commands.argument("target_type", StringArgumentType.word())
                                .then(Commands.argument("target_id", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        String pname = StringArgumentType.getString(ctx, "preset_name");
                                        String ttype = StringArgumentType.getString(ctx, "target_type");
                                        String tid   = StringArgumentType.getString(ctx, "target_id");
                                        TargetType type = TargetType.valueOf(ttype.toUpperCase());
                                        Optional<Preset> preset = PresetManager.getForType(type).stream()
                                                .filter(p -> p.name.equals(pname)).findFirst();
                                        if (preset.isEmpty()) {
                                            ctx.getSource().sendFailure(Component.literal("Preset not found: " + pname));
                                            return 0;
                                        }
                                        ResourceLocation rl = ResourceLocation.parse(tid);
                                        applyPreset(rl, type, preset.get());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "Applied preset '" + pname + "' to " + tid), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )

                .then(Commands.literal("randomize")
                    .then(Commands.argument("target_type", StringArgumentType.word())
                        .then(Commands.argument("target_id", StringArgumentType.word())
                            .executes(ctx -> doRandomize(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "target_type"),
                                    StringArgumentType.getString(ctx, "target_id"),
                                    RandomizeManager.newSeed()))
                            .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(ctx -> doRandomize(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target_type"),
                                        StringArgumentType.getString(ctx, "target_id"),
                                        LongArgumentType.getLong(ctx, "seed")))
                            )
                        )
                    )
                )
        );
    }

    private static void applyPreset(ResourceLocation rl, TargetType type, Preset preset) {
        switch (type) {
            case MOB   -> StatRegistry.setMob(rl, (MobStats) preset.stats);
            case BLOCK -> StatRegistry.setBlock(rl, (BlockStats) preset.stats);
            case ITEM  -> StatRegistry.setItem(rl, (ItemStats) preset.stats);
        }
    }

    private static int doRandomize(CommandSourceStack src, String ttype, String tid, long seed) {
        ResourceLocation rl = ResourceLocation.parse(tid);
        switch (ttype) {
            case "mob" -> {
                MobStats vanilla = StatRegistry.getMob(rl);
                if (vanilla == null) vanilla = new MobStats();
                StatRegistry.setMob(rl, RandomizeManager.randomizeMob(vanilla, seed));
            }
            case "block" -> {
                BlockStats vanilla = StatRegistry.getBlock(rl);
                if (vanilla == null) vanilla = new BlockStats();
                StatRegistry.setBlock(rl, RandomizeManager.randomizeBlock(vanilla, seed));
            }
            case "item" -> {
                ItemStats vanilla = StatRegistry.getItem(rl);
                if (vanilla == null) vanilla = new ItemStats();
                StatRegistry.setItem(rl, RandomizeManager.randomizeItem(vanilla, seed));
            }
            default -> {
                src.sendFailure(Component.literal("Unknown target type: " + ttype));
                return 0;
            }
        }
        final long s = seed;
        src.sendSuccess(() -> Component.literal(
                "Randomized " + ttype + " " + tid + " with seed " + s), true);
        return 1;
    }
}
