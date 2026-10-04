package com.craftstats.common.gui;

import com.craftstats.common.config.CraftStatsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** In-game editor for config/craftstats/config.json (Mod Menu on Fabric, Mods screen on NeoForge). */
public class ConfigScreen extends BaseScreen {

    private static final List<String> INTENSITIES = List.of("mild", "wild", "chaos");

    private final Screen parent;
    private final CraftStatsConfig.ConfigData cfg = CraftStatsConfig.get();

    public ConfigScreen(Screen parent) {
        super(Component.literal("CraftStats Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int gap = 8, rowH = 22;
        int colW = Math.min(180, (this.width - 20 - gap) / 2);
        int left = this.width / 2 - colW - gap / 2, right = this.width / 2 + gap / 2;
        int y = 40;

        toggle(left, y, colW, "Operators only", () -> cfg.requireOp, v -> cfg.requireOp = v);
        toggle(right, y, colW, "Survival players too", () -> cfg.allowSurvival, v -> cfg.allowSurvival = v);
        y += rowH;
        toggle(left, y, colW, "Mob editor", () -> cfg.enableMobEditor, v -> cfg.enableMobEditor = v);
        toggle(right, y, colW, "Block editor", () -> cfg.enableBlockEditor, v -> cfg.enableBlockEditor = v);
        y += rowH;
        toggle(left, y, colW, "Item editor", () -> cfg.enableItemEditor, v -> cfg.enableItemEditor = v);
        toggle(right, y, colW, "Player editor", () -> cfg.enablePlayerEditor, v -> cfg.enablePlayerEditor = v);
        y += rowH;
        toggle(left, y, colW, "Projectile editor", () -> cfg.enableProjectileEditor, v -> cfg.enableProjectileEditor = v);
        toggle(right, y, colW, "Enchantment editor", () -> cfg.enableEnchantmentEditor, v -> cfg.enableEnchantmentEditor = v);
        y += rowH;
        toggle(left, y, colW, "World editor", () -> cfg.enableWorldEditor, v -> cfg.enableWorldEditor = v);
        toggle(right, y, colW, "Randomize", () -> cfg.enableRandomize, v -> cfg.enableRandomize = v);
        y += rowH;
        toggle(left, y, colW, "Presets", () -> cfg.enablePresets, v -> cfg.enablePresets = v);
        addRenderableWidget(Button.builder(intensityLabel(), b -> {
            int i = INTENSITIES.indexOf(cfg.randomizeIntensity.toLowerCase());
            cfg.randomizeIntensity = INTENSITIES.get((i + 1) % INTENSITIES.size());
            b.setMessage(intensityLabel());
        }).bounds(right, y, colW, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> { CraftStatsConfig.save(); onClose(); })
                .bounds(this.width / 2 - 104, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> { CraftStatsConfig.load(); onClose(); })
                .bounds(this.width / 2 + 4, this.height - 30, 100, 20).build());
    }

    private Component intensityLabel() {
        return Component.literal("Randomize: " + cfg.randomizeIntensity);
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
        }).bounds(x, y, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "CraftStats Config", this.width / 2, 12, 0xFFFFFFFF);
        g.drawCenteredString(font, "On a server, the server's config decides who may edit. Blacklist: edit config.json.",
                this.width / 2, 24, 0xFF999999);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
