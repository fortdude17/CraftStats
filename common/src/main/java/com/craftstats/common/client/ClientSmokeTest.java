package com.craftstats.common.client;

import com.craftstats.common.CraftStats;
import com.craftstats.common.SmokeTest;
import com.craftstats.common.gui.*;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.BooleanSupplier;
import com.craftstats.common.stats.StatCatalog;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.stats.VanillaStats;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Client counterpart of {@link SmokeTest} for CI: with -Dcraftstats.smokeTest=true the client
 * applies its mixins, opens every CraftStats screen, every editor type and every category
 * card, logs CRAFTSTATS-CLIENT-SMOKE-PASS or -FAIL, and quits. With
 * -Dcraftstats.screenshots=true it also asks the CI script for screenshots (by creating
 * {@code screenshot-<name>.req} files and waiting until they are taken).
 */
public final class ClientSmokeTest {

    private record Step(String name, SmokeTest.Check action, String screenshot, BooleanSupplier until, int timeout) {
        Step(String name, SmokeTest.Check action, String screenshot) { this(name, action, screenshot, null, 0); }
    }

    /** Waits (up to {@code timeout} ticks) until {@code condition} holds; fails the step otherwise. */
    private static Step waitUntil(String name, BooleanSupplier condition, int timeout) {
        return new Step(name, () -> {}, null, condition, timeout);
    }

    private static final List<String> FAILURES = new ArrayList<>();
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int waited;
    private static boolean started;
    private static int delay;
    private static Path pendingShot;
    private static int shotWait;
    private static int inWorldTicks;
    private static Step waiting;
    private static int waitTicks;

    private ClientSmokeTest() {}

    public static void register() {
        if (!SmokeTest.enabled()) return;
        ClientTickEvent.CLIENT_POST.register(ClientSmokeTest::tick);
    }

    private static boolean screenshots() {
        return Boolean.getBoolean("craftstats.screenshots");
    }

    /** CI starts the client straight into a world with --quickPlaySingleplayer. */
    private static boolean inWorld() {
        return Boolean.getBoolean("craftstats.inWorld");
    }

    private static void plan(Minecraft mc) {
        STEPS.add(new Step("client mixin audit", () -> MixinEnvironment.getCurrentEnvironment().audit(), null));
        STEPS.add(new Step("main editor", () -> mc.setScreen(new CraftStatsScreen()), null));
        for (TargetType t : TargetType.values()) {
            STEPS.add(new Step(t.plural() + " tab", () -> {
                CraftStatsScreen s = editor(mc);
                if (!s.showFirst(t)) {
                    // Players, enchantments and projectiles need a world; at the title screen the list is empty.
                    if (inWorld() || t == TargetType.MOB || t == TargetType.BLOCK || t == TargetType.ITEM || t == TargetType.WORLD)
                        throw new AssertionError("nothing to select for " + t);
                    return;
                }
                int cats = s.editor().categoryCount();
                SmokeTest.check(cats == StatCatalog.categories(t).size(), t + ": wrong number of cards");
                for (int i = 0; i < cats; i++) {
                    s.editor().openCategory(i);
                    int expected = StatCatalog.categories(t).get(i).stats().size();
                    SmokeTest.check(s.editor().rowCount() == expected,
                            t + " card " + i + " shows " + s.editor().rowCount() + " rows, expected " + expected);
                }
                s.editor().openCategory(-1);
            }, null));
        }
        STEPS.add(new Step("mob cards", () -> { editor(mc).select(TargetType.MOB, "minecraft:zombie"); editor(mc).editor().openCategory(-1); }, "1-mob-cards"));
        STEPS.add(new Step("mob combat", () -> editor(mc).editor().openCategory(0), "2-mob-combat"));
        STEPS.add(new Step("item bonuses", () -> { editor(mc).select(TargetType.ITEM, "minecraft:diamond_sword"); editor(mc).editor().openCategory(1); }, "3-item-bonuses"));
        STEPS.add(new Step("block cards", () -> { editor(mc).select(TargetType.BLOCK, "minecraft:stone"); editor(mc).editor().openCategory(-1); }, "4-block-cards"));
        STEPS.add(new Step("world", () -> editor(mc).showFirst(TargetType.WORLD), "5-world"));
        if (inWorld()) {
            STEPS.add(new Step("players", () -> { editor(mc).showFirst(TargetType.PLAYER); editor(mc).editor().openCategory(-1); }, "6-players"));
            STEPS.add(new Step("enchantments", () -> { editor(mc).select(TargetType.ENCHANTMENT, "minecraft:sharpness"); editor(mc).editor().openCategory(0); }, "7-enchantments"));
            STEPS.add(new Step("projectiles", () -> { editor(mc).select(TargetType.PROJECTILE, "minecraft:arrow"); editor(mc).editor().openCategory(-1); }, "8-projectiles"));
        }
        STEPS.add(new Step("presets", () -> mc.setScreen(new PresetScreen(mc.screen, TargetType.MOB, new MobStats(), s -> {})), "9-presets"));
        STEPS.add(new Step("randomize", () -> mc.setScreen(new RandomizeScreen(null, TargetType.MOB, "Zombie",
                VanillaStats.mob(EntityType.ZOMBIE), s -> {})), null));
        STEPS.add(new Step("config", () -> mc.setScreen(new ConfigScreen(null)), null));
        STEPS.add(new Step("block picker", () -> mc.setScreen(new BlockPickerScreen(null, b -> {})), null));
        if (inWorld()) planInWorld(mc);
        else STEPS.add(new Step("reset screen", () -> mc.setScreen(new StatResetScreen(null)), null));
    }

    /** Applies stats through the real network path and checks they take effect. */
    private static void planInWorld(Minecraft mc) {
        STEPS.add(new Step("close screens", () -> mc.setScreen(null), null));
        // Player stats: size 2.
        STEPS.add(new Step("apply player stats", () -> {
            PlayerStats ps = new PlayerStats();
            ps.size = 2.0;
            CraftStatsNetwork.sendApply(CraftStatsNetwork.Kind.PLAYER, mc.player.getUUID().toString(), ps);
        }, null));
        STEPS.add(waitUntil("player is twice as big", () -> Math.abs(mc.player.getAttributeValue(Attributes.SCALE) - 2.0) < 0.01, 100));
        STEPS.add(new Step("in-world screenshot", () -> {}, "10-big-player"));
        STEPS.add(new Step("reset player", () -> CraftStatsNetwork.sendReset(CraftStatsNetwork.Kind.PLAYER, mc.player.getUUID().toString()), null));
        STEPS.add(waitUntil("player is normal size again", () -> Math.abs(mc.player.getAttributeValue(Attributes.SCALE) - 1.0) < 0.01, 100));
        // World stats: half gravity, seen by the client too.
        STEPS.add(new Step("apply world stats", () -> {
            WorldStats w = new WorldStats();
            w.gravityMultiplier = 0.5;
            CraftStatsNetwork.sendApply(CraftStatsNetwork.Kind.WORLD, TargetType.WORLD_KEY, w);
        }, null));
        STEPS.add(waitUntil("half gravity", () -> Math.abs(mc.player.getGravity() - 0.04) < 1.0E-4, 100));
        STEPS.add(new Step("reset world", () -> CraftStatsNetwork.sendReset(CraftStatsNetwork.Kind.WORLD, TargetType.WORLD_KEY), null));
        // Throwable boomerang: thrown stick leaves the hand and comes back.
        STEPS.add(new Step("make sticks boomerangs", () -> {
            ItemStats s = new ItemStats();
            s.throwable = true; s.boomerang = true; s.throwVelocity = 1.0f; s.useCooldown = 0f;
            CraftStatsNetwork.sendApply(CraftStatsNetwork.Kind.ITEM, "minecraft:stick", s);
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            server.execute(() -> {
                var sp = server.getPlayerList().getPlayer(id);
                if (sp != null) sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 5));
            });
            mc.player.setXRot(-15f);
        }, null));
        STEPS.add(waitUntil("holding 5 boomerang sticks", () -> mc.player.getMainHandItem().is(Items.STICK)
                && mc.player.getMainHandItem().getCount() == 5 && StatRegistry.forItem(Items.STICK) != null, 100));
        STEPS.add(new Step("throw", () -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND), null));
        STEPS.add(waitUntil("stick was thrown", () -> mc.player.getMainHandItem().getCount() == 4, 60));
        STEPS.add(waitUntil("boomerang came back", () -> mc.player.getInventory().countItem(Items.STICK) == 5, 400));
        STEPS.add(new Step("reset sticks", () -> CraftStatsNetwork.sendReset(CraftStatsNetwork.Kind.ITEM, "minecraft:stick"), null));
    }

    private static CraftStatsScreen editor(Minecraft mc) {
        if (mc.screen instanceof CraftStatsScreen s) return s;
        throw new AssertionError("editor not open, screen is " + mc.screen);
    }

    private static void tick(Minecraft mc) {
        if (!started && inWorld()) {
            // Wait until the world is loaded and the player is in it, then a little longer.
            if (mc.level == null || mc.player == null || mc.screen != null || mc.getOverlay() != null) {
                if (++waited == 6000) {
                    CraftStats.LOGGER.error("CRAFTSTATS-CLIENT-SMOKE-FAIL: never got into the world");
                    mc.stop();
                }
                return;
            }
            if (++inWorldTicks < 60) return;
            started = true;
            delay = 5;
            plan(mc);
        }
        if (!started) {
            // Wait until loading has finished and a screen is up. A fresh game shows the
            // accessibility onboarding screen instead of the title screen, so accept any.
            if (mc.getOverlay() != null || mc.screen == null) {
                if (++waited == 6000) {
                    CraftStats.LOGGER.error("CRAFTSTATS-CLIENT-SMOKE-FAIL: game never finished loading");
                    mc.stop();
                }
                return;
            }
            started = true;
            delay = 20;
            plan(mc);
        }
        if (pendingShot != null) {
            // Wait for the CI script to take the screenshot (it deletes the request file).
            if (Files.exists(pendingShot) && ++shotWait < 400) return;
            pendingShot = null;
        }
        if (waiting != null) {
            boolean ok;
            try { ok = waiting.until().getAsBoolean(); } catch (Throwable t) { ok = false; }
            if (ok) {
                CraftStats.LOGGER.info("smoke test: {} ok (after {} ticks)", waiting.name(), waitTicks);
                waiting = null;
            } else if (++waitTicks >= waiting.timeout()) {
                CraftStats.LOGGER.error("smoke test: {} FAILED (timed out)", waiting.name());
                FAILURES.add(waiting.name() + ": timed out");
                waiting = null;
            }
            return;
        }
        if (delay > 0) { delay--; return; }
        Step step = STEPS.poll();
        if (step == null) {
            if (FAILURES.isEmpty()) CraftStats.LOGGER.info("CRAFTSTATS-CLIENT-SMOKE-PASS");
            else CraftStats.LOGGER.error("CRAFTSTATS-CLIENT-SMOKE-FAIL: {}", String.join("; ", FAILURES));
            mc.stop();
            return;
        }
        if (step.until() != null) {
            waiting = step;
            waitTicks = 0;
            return;
        }
        SmokeTest.step(FAILURES, step.name(), step.action());
        delay = 5;
        if (step.screenshot() != null && screenshots()) {
            // Give the screen a few frames, then ask for the screenshot.
            STEPS.addFirst(new Step("screenshot " + step.screenshot(), () -> {
                pendingShot = mc.gameDirectory.toPath().resolve("screenshot-" + step.screenshot() + ".req");
                shotWait = 0;
                Files.writeString(pendingShot, step.screenshot());
            }, null));
            delay = 15;
        }
    }
}
