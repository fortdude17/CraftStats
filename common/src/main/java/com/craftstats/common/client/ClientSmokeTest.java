package com.craftstats.common.client;

import com.craftstats.common.CraftStats;
import com.craftstats.common.SmokeTest;
import com.craftstats.common.gui.*;
import com.craftstats.common.stats.MobStats;
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

    private record Step(String name, SmokeTest.Check action, String screenshot) {}

    private static final List<String> FAILURES = new ArrayList<>();
    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int waited;
    private static boolean started;
    private static int delay;
    private static Path pendingShot;
    private static int shotWait;

    private ClientSmokeTest() {}

    public static void register() {
        if (!SmokeTest.enabled()) return;
        ClientTickEvent.CLIENT_POST.register(ClientSmokeTest::tick);
    }

    private static boolean screenshots() {
        return Boolean.getBoolean("craftstats.screenshots");
    }

    private static void plan(Minecraft mc) {
        STEPS.add(new Step("client mixin audit", () -> MixinEnvironment.getCurrentEnvironment().audit(), null));
        STEPS.add(new Step("main editor", () -> mc.setScreen(new CraftStatsScreen()), null));
        for (TargetType t : TargetType.values()) {
            STEPS.add(new Step(t.plural() + " tab", () -> {
                CraftStatsScreen s = editor(mc);
                if (!s.showFirst(t)) {
                    // Players, enchantments and projectiles need a world; at the title screen the list is empty.
                    if (t == TargetType.MOB || t == TargetType.BLOCK || t == TargetType.ITEM || t == TargetType.WORLD)
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
        STEPS.add(new Step("presets", () -> mc.setScreen(new PresetScreen(mc.screen, TargetType.MOB, new MobStats(), s -> {})), "6-presets"));
        STEPS.add(new Step("randomize", () -> mc.setScreen(new RandomizeScreen(null, TargetType.MOB, "Zombie",
                VanillaStats.mob(EntityType.ZOMBIE), s -> {})), "7-randomize"));
        STEPS.add(new Step("config", () -> mc.setScreen(new ConfigScreen(null)), "8-config"));
        STEPS.add(new Step("block picker", () -> mc.setScreen(new BlockPickerScreen(null, b -> {})), null));
        STEPS.add(new Step("reset screen", () -> mc.setScreen(new StatResetScreen(null)), null));
    }

    private static CraftStatsScreen editor(Minecraft mc) {
        if (mc.screen instanceof CraftStatsScreen s) return s;
        throw new AssertionError("editor not open, screen is " + mc.screen);
    }

    private static void tick(Minecraft mc) {
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
        if (delay > 0) { delay--; return; }
        Step step = STEPS.poll();
        if (step == null) {
            if (FAILURES.isEmpty()) CraftStats.LOGGER.info("CRAFTSTATS-CLIENT-SMOKE-PASS");
            else CraftStats.LOGGER.error("CRAFTSTATS-CLIENT-SMOKE-FAIL: {}", String.join("; ", FAILURES));
            mc.stop();
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
