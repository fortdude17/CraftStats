package com.craftstats.common.gui;

import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class RandomizeScreen extends Screen {

    private final TargetType type;
    private final String     targetId;
    private final Object     vanilla;

    private Object randomized;
    private long   currentSeed;

    private EditBox seedBox;
    private Button  rerollBtn, confirmBtn, rerollAllBtn, savePresetBtn, cancelBtn;

    private static final int COL_W = 160;
    private static final int ROW_H = 12;
    private static final int PAD   = 6;

    public RandomizeScreen(TargetType type, String targetId, Object vanillaStats) {
        super(Component.literal("Randomize - " + targetId));
        this.type      = type;
        this.targetId  = targetId;
        this.vanilla   = vanillaStats;
        this.currentSeed = RandomizeManager.newSeed();
        reroll();
    }

    private void reroll() {
        currentSeed = RandomizeManager.newSeed();
        randomized  = randomizeWith(currentSeed);
    }

    private Object randomizeWith(long seed) {
        return switch (type) {
            case MOB    -> RandomizeManager.randomizeMob((MobStats) vanilla, seed);
            case BLOCK  -> RandomizeManager.randomizeBlock((BlockStats) vanilla, seed);
            case ITEM   -> RandomizeManager.randomizeItem((ItemStats) vanilla, seed);
            case PLAYER -> RandomizeManager.randomizePlayer((PlayerStats) vanilla, seed);
        };
    }

    @Override
    protected void init() {
        int bw = 70, bh = 16;
        int cx = this.width / 2;
        int bottomY = this.height - bh - PAD * 2;

        seedBox = new EditBox(this.font, cx - 60, bottomY - bh - PAD * 2, 120, bh,
                Component.literal("Seed"));
        seedBox.setMaxLength(20);
        seedBox.setValue(String.valueOf(currentSeed));
        seedBox.setResponder(v -> {
            try { currentSeed = Long.parseLong(v); randomized = randomizeWith(currentSeed); }
            catch (NumberFormatException ignored) {}
        });

        rerollBtn    = Button.builder(Component.literal("Reroll All"),  b -> { reroll(); seedBox.setValue(String.valueOf(currentSeed)); })
                .bounds(cx - bw - PAD, bottomY, bw, bh).build();
        confirmBtn   = Button.builder(Component.literal("Confirm"),     b -> apply())
                .bounds(cx + PAD, bottomY, bw, bh).build();
        cancelBtn    = Button.builder(Component.literal("Cancel"),      b -> onClose())
                .bounds(PAD, bottomY, 50, bh).build();
        savePresetBtn= Button.builder(Component.literal("Save Preset"), b -> saveAsPreset())
                .bounds(this.width - 90 - PAD, bottomY, 90, bh).build();

        addRenderableWidget(seedBox);
        addRenderableWidget(rerollBtn);
        addRenderableWidget(confirmBtn);
        addRenderableWidget(cancelBtn);
        addRenderableWidget(savePresetBtn);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        g.drawCenteredString(this.font, "Randomize Preview — " + type.name() + " " + targetId,
                this.width / 2, PAD, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Seed: " + currentSeed,
                this.width / 2, PAD + 12, 0xFFAAAAAA);

        int startY = PAD + 28;
        int leftX  = PAD + 20;
        int rightX = this.width / 2 + PAD;
        g.drawString(this.font, "ORIGINAL",   leftX,  startY, 0xFF888888, false);
        g.drawString(this.font, "RANDOMIZED", rightX, startY, 0xFF44BB44, false);

        if (vanilla != null && randomized != null) {
            renderDiff(g, vanilla, randomized, leftX, rightX, startY + 14);
        }

        super.render(g, mx, my, delta);
    }

    private void renderDiff(GuiGraphics g, Object orig, Object rand, int leftX, int rightX, int startY) {
        java.lang.reflect.Field[] fields = orig.getClass().getFields();
        int rowY = startY;
        int bodyH = this.height - startY - 60;
        for (java.lang.reflect.Field f : fields) {
            if (rowY - startY > bodyH) break;
            f.setAccessible(true);
            try {
                Object ov = f.get(orig);
                Object rv = f.get(rand);
                if (ov == null || rv == null) continue;
                boolean changed = !ov.toString().equals(rv.toString());
                int color = changed ? 0xFFFFAA44 : 0xFF888888;
                String label = f.getName();
                String oval  = formatVal(ov);
                String rval  = formatVal(rv);
                g.drawString(this.font, label + ": " + oval, leftX,  rowY, color, false);
                g.drawString(this.font, rval,                rightX, rowY, changed ? 0xFF44FF44 : 0xFF888888, false);
                rowY += ROW_H;
            } catch (Exception ignored) {}
        }
    }

    private String formatVal(Object v) {
        if (v instanceof Double d)  return String.format("%.4f", d).replaceAll("0+$","").replaceAll("\\.$","");
        if (v instanceof Float  f)  return String.format("%.4f", f).replaceAll("0+$","").replaceAll("\\.$","");
        return String.valueOf(v);
    }

    private void apply() {
        switch (type) {
            case MOB    -> CraftStatsNetwork.sendApplyMobStats(targetId, (MobStats) randomized);
            case BLOCK  -> CraftStatsNetwork.sendApplyBlockStats(targetId, (BlockStats) randomized);
            case ITEM   -> CraftStatsNetwork.sendApplyItemStats(targetId, (ItemStats) randomized);
            case PLAYER -> CraftStatsNetwork.sendApplyPlayerStats(UUID.fromString(targetId), (PlayerStats) randomized);
        }
        onClose();
    }

    private void saveAsPreset() {
        String name = "rand_" + type.name().toLowerCase() + "_" + currentSeed;
        Preset p = new Preset(name, type, randomized);
        p.seed = currentSeed;
        PresetManager.savePreset(p);
        Minecraft.getInstance().player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Saved preset: " + name));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
