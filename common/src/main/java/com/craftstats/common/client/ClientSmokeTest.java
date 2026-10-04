package com.craftstats.common.client;

import com.craftstats.common.CraftStats;
import com.craftstats.common.SmokeTest;
import com.craftstats.common.gui.*;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.stats.VanillaStats;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client counterpart of {@link SmokeTest} for CI: with -Dcraftstats.smokeTest=true the client
 * applies its mixins, opens every CraftStats screen (each is rendered for a few frames),
 * logs CRAFTSTATS-CLIENT-SMOKE-PASS or -FAIL, and quits.
 */
public final class ClientSmokeTest {

    private static final List<String> FAILURES = new ArrayList<>();
    private static int ticks;
    private static boolean started;

    private ClientSmokeTest() {}

    public static void register() {
        if (!SmokeTest.enabled()) return;
        ClientTickEvent.CLIENT_POST.register(ClientSmokeTest::tick);
    }

    private static void tick(Minecraft mc) {
        if (!started) {
            // Wait until the game has finished loading and shows the title screen.
            if (!(mc.screen instanceof TitleScreen)) return;
            started = true;
        }
        ticks++;
        switch (ticks) {
            case 20  -> SmokeTest.step(FAILURES, "client mixin audit", () -> MixinEnvironment.getCurrentEnvironment().audit());
            case 30  -> open(mc, "main editor", () -> new CraftStatsScreen());
            case 40  -> SmokeTest.step(FAILURES, "mob tab", () -> ((CraftStatsScreen) mc.screen).selectMobById("minecraft:zombie"));
            case 50  -> SmokeTest.step(FAILURES, "block tab", () -> ((CraftStatsScreen) mc.screen).selectBlockById("minecraft:stone"));
            case 60  -> SmokeTest.step(FAILURES, "item tab", () -> ((CraftStatsScreen) mc.screen).selectItemById("minecraft:bread"));
            case 70  -> SmokeTest.step(FAILURES, "player tab", () -> ((CraftStatsScreen) mc.screen).selectPlayerById(UUID.randomUUID(), "Tester"));
            case 80  -> open(mc, "player editor", () -> new PlayerStatsScreen(UUID.randomUUID(), "Tester"));
            case 90  -> open(mc, "preset browser", () -> new PresetBrowserScreen(null, TargetType.MOB, new MobStats(), s -> {}));
            case 100 -> open(mc, "randomize", () -> new RandomizeScreen(TargetType.MOB, "minecraft:zombie", VanillaStats.mob(EntityType.ZOMBIE)));
            case 110 -> open(mc, "block browser", () -> new BlockSourceBrowserScreen(null, s -> {}));
            case 120 -> open(mc, "config", () -> new ConfigScreen(null));
            case 130 -> open(mc, "reset screen", () -> new StatResetScreen(null));
            case 150 -> {
                if (FAILURES.isEmpty()) CraftStats.LOGGER.info("CRAFTSTATS-CLIENT-SMOKE-PASS");
                else CraftStats.LOGGER.error("CRAFTSTATS-CLIENT-SMOKE-FAIL: {}", String.join("; ", FAILURES));
                mc.stop();
            }
            default -> {}
        }
    }

    private interface ScreenFactory { net.minecraft.client.gui.screens.Screen create(); }

    private static void open(Minecraft mc, String name, ScreenFactory factory) {
        SmokeTest.step(FAILURES, name, () -> mc.setScreen(factory.create()));
    }
}
