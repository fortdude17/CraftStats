package com.craftstats.common;

import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.StatPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated check used by CI: start a dedicated server with -Dcraftstats.smokeTest=true and
 * it applies every mixin, exercises the main code paths, logs CRAFTSTATS-SMOKE-PASS or
 * CRAFTSTATS-SMOKE-FAIL, and shuts down. Does nothing in normal play.
 */
public final class SmokeTest {

    private SmokeTest() {}

    public static boolean enabled() {
        return Boolean.getBoolean("craftstats.smokeTest");
    }

    public static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        step(failures, "mixin audit", () -> MixinEnvironment.getCurrentEnvironment().audit());

        step(failures, "item overrides", () -> {
            ItemStats bread = new ItemStats();
            bread.stackSize = 16;
            bread.nutrition = 10;
            StatRegistry.setItem(id("bread"), bread);
            ItemStats sword = new ItemStats();
            sword.maxDurability = 5000;
            sword.attackDamage = 20.0;
            StatRegistry.setItem(id("diamond_sword"), sword);
            check(new ItemStack(Items.BREAD).getMaxStackSize() == 16, "bread stack size not overridden");
            check(new ItemStack(Items.DIAMOND_SWORD).getMaxDamage() == 5000, "sword durability not overridden");
            check(new ItemStack(Items.DIAMOND_SWORD).getMaxStackSize() == 1, "sword must not stack");
            check(VanillaStats.item(Items.DIAMOND_SWORD).attackDamage != null, "vanilla item stats");
        });

        step(failures, "block overrides", () -> {
            BlockStats stone = new BlockStats();
            stone.hardness = 42f;
            stone.lightEmission = 7;
            StatRegistry.setBlock(id("stone"), stone);
            ServerLevel level = server.overworld();
            check(Blocks.STONE.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) == 42f, "hardness not overridden");
            check(Blocks.STONE.defaultBlockState().getLightEmission() == 7, "light not overridden");
            check(VanillaStats.block(Blocks.STONE).hardness == 1.5f, "vanilla hardness should be 1.5");
        });

        step(failures, "mob overrides", () -> {
            MobStats zombieStats = new MobStats();
            zombieStats.maxHealth = 80.0;
            StatRegistry.setMob(id("zombie"), zombieStats);
            ServerLevel level = server.overworld();
            LivingEntity zombie = EntityType.ZOMBIE.create(level
                    //#if MC >= 1.21.2
                    //$ , net.minecraft.world.entity.EntitySpawnReason.COMMAND
                    //#endif
            );
            check(zombie != null, "could not create zombie");
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getMaxHealth() - 80f) < 0.01f, "zombie max health is " + zombie.getMaxHealth());
            check(Math.abs(zombie.getHealth() - 80f) < 0.01f, "fresh zombie should be at full health");
            StatRegistry.removeMob(id("zombie"));
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getMaxHealth() - 20f) < 0.01f, "reset did not restore vanilla health");
            check(VanillaStats.mob(EntityType.ZOMBIE).maxHealth == 20.0, "vanilla zombie health");
        });

        step(failures, "snapshot round trip", () -> {
            var snapshot = StatPersistence.snapshot();
            int items = StatRegistry.allItems().size();
            StatPersistence.restore(snapshot);
            check(StatRegistry.allItems().size() == items, "items lost in snapshot round trip");
            check(StatRegistry.getBlock(id("stone")) != null, "block lost in snapshot round trip");
        });

        step(failures, "legacy migration", () -> {
            MobStats m = StatSchema.mob("{\"max_health\":-1.0,\"attack_damage\":12.0,\"xp_reward\":0}");
            check(m.maxHealth == null && m.attackDamage == 12.0 && m.xpReward == null, "mob migration");
            ItemStats i = StatSchema.item("{\"stack_size\":64,\"attack_damage\":1.0,\"max_durability\":0}");
            check(i.stackSize == null && i.attackDamage == null && i.maxDurability == null, "item migration");
        });

        step(failures, "commands", () -> {
            var source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, "craftstats randomize mob minecraft:skeleton 1234");
            check(StatRegistry.getMob(id("skeleton")) != null, "randomize command did nothing");
            check(StatRegistry.getMob(id("skeleton")).maxHealth > 0, "randomized health must be positive");
            server.getCommands().performPrefixedCommand(source, "craftstats preset load tank mob minecraft:skeleton");
            check(StatRegistry.getMob(id("skeleton")).maxHealth == 200.0, "preset load command");
            server.getCommands().performPrefixedCommand(source, "craftstats reset all");
            check(StatRegistry.allMobs().isEmpty() && StatRegistry.allItems().isEmpty(), "reset all command");
        });

        step(failures, "sync encoding", () -> CraftStatsNetwork.broadcastSync(server));

        if (failures.isEmpty()) {
            CraftStats.LOGGER.info("CRAFTSTATS-SMOKE-PASS");
        } else {
            CraftStats.LOGGER.error("CRAFTSTATS-SMOKE-FAIL: {}", String.join("; ", failures));
        }
        server.halt(false);
        // The dedicated server JVM doesn't always exit by itself after halting (lingering
        // non-daemon threads), which would leave CI waiting for a timeout.
        Thread exit = new Thread(() -> {
            try { Thread.sleep(30_000); } catch (InterruptedException ignored) {}
            Runtime.getRuntime().halt(0);
        }, "craftstats-smoke-exit");
        exit.setDaemon(true);
        exit.start();
    }

    private interface Check { void run() throws Exception; }

    private static void step(List<String> failures, String name, Check check) {
        try {
            check.run();
            CraftStats.LOGGER.info("smoke test: {} ok", name);
        } catch (Throwable t) {
            CraftStats.LOGGER.error("smoke test: {} FAILED", name, t);
            failures.add(name + ": " + t);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
