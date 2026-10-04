package com.craftstats.common.gui;

import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Preview of a seeded randomization: original vs. randomized values, then confirm. */
public class RandomizeScreen extends BaseScreen {

    private static final int ROW_H = 11;
    private static final int PAD   = 6;

    private final TargetType type;
    private final String     targetId;
    private final Object     base;

    private Object randomized;
    private long   seed;
    private EditBox seedBox;
    private String status = "";

    public RandomizeScreen(TargetType type, String targetId, Object baseStats) {
        super(Component.literal("Randomize " + targetId));
        this.type = type;
        this.targetId = targetId;
        this.base = baseStats;
        roll(RandomizeManager.newSeed());
    }

    private void roll(long newSeed) {
        seed = newSeed;
        randomized = switch (type) {
            case MOB    -> RandomizeManager.randomizeMob((MobStats) base, seed);
            case BLOCK  -> RandomizeManager.randomizeBlock((BlockStats) base, seed);
            case ITEM   -> RandomizeManager.randomizeItem((ItemStats) base, seed);
            case PLAYER -> RandomizeManager.randomizePlayer((PlayerStats) base, seed);
        };
    }

    @Override
    protected void init() {
        int bw = 80, bh = 16, cx = this.width / 2;
        int bottomY = this.height - bh - PAD;
        seedBox = addRenderableWidget(new EditBox(this.font, cx - 70, bottomY - bh - PAD, 140, bh, Component.literal("Seed")));
        seedBox.setMaxLength(19);
        seedBox.setValue(String.valueOf(seed));
        seedBox.setResponder(v -> {
            try { roll(Long.parseLong(v.trim())); } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(cx - bw * 2 - PAD * 2, bottomY, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Reroll"), b -> {
            roll(RandomizeManager.newSeed());
            seedBox.setValue(String.valueOf(seed));
        }).bounds(cx - bw - PAD, bottomY, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply()).bounds(cx, bottomY, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Save Preset"), b -> saveAsPreset()).bounds(cx + bw + PAD, bottomY, bw, bh).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);
        g.drawCenteredString(font, "Randomize " + type.displayName().toLowerCase() + " " + targetId, this.width / 2, PAD, 0xFFFFFFFF);
        g.drawCenteredString(font, "Intensity: " + RandomizeManager.getIntensity().name().toLowerCase(Locale.ROOT)
                + " (change it in the config)", this.width / 2, PAD + 12, 0xFF888899);

        int leftX = this.width / 2 - 170, midX = this.width / 2 + 10, rightX = this.width / 2 + 100;
        int y = PAD + 32;
        g.drawString(font, "STAT", leftX, y, 0xFF888888, false);
        g.drawString(font, "VANILLA", midX, y, 0xFF888888, false);
        g.drawString(font, "RANDOM", rightX, y, 0xFF44BB44, false);
        y += ROW_H + 2;
        int maxY = this.height - 60;
        for (Field f : base.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getName().equals("schema")) continue;
            try {
                Object before = f.get(base), after = f.get(randomized);
                if (Objects.equals(before, after) || y > maxY) continue;
                g.drawString(font, f.getName(), leftX, y, 0xFFCCCCCC, false);
                g.drawString(font, fmt(before), midX, y, 0xFF999999, false);
                g.drawString(font, fmt(after), rightX, y, 0xFF66FF66, false);
                y += ROW_H;
            } catch (IllegalAccessException ignored) {}
        }
        if (!status.isEmpty()) g.drawCenteredString(font, status, this.width / 2, this.height - 58, 0xFF66DDFF);
        super.render(g, mx, my, delta);
    }

    private static String fmt(Object v) {
        if (v == null) return "-";
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            return String.format(Locale.ROOT, "%.3f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.valueOf(v);
    }

    private void apply() {
        CraftStatsNetwork.Kind kind = switch (type) {
            case MOB -> CraftStatsNetwork.Kind.MOB;
            case BLOCK -> CraftStatsNetwork.Kind.BLOCK;
            case ITEM -> CraftStatsNetwork.Kind.ITEM;
            case PLAYER -> CraftStatsNetwork.Kind.PLAYER;
        };
        String key = type == TargetType.PLAYER ? UUID.fromString(targetId).toString() : targetId;
        CraftStatsNetwork.sendApply(kind, key, randomized);
        onClose();
    }

    private void saveAsPreset() {
        Preset p = new Preset("random_" + seed, type, randomized);
        p.seed = seed;
        status = PresetManager.save(p) ? "Saved preset " + p.name : "Could not save preset";
    }
}
