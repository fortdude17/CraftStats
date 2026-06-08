package com.craftstats.fabric.config;

import com.craftstats.common.config.CraftStatsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CraftStatsConfigScreen extends Screen {

    private final Screen parent;
    private CraftStatsConfig.ConfigData cfg;

    private EditBox intensityBox;
    private Button  requireOpBtn, allowSurvivalBtn, saveBtn, cancelBtn;

    public CraftStatsConfigScreen(Screen parent) {
        super(Component.literal("CraftStats Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cfg = CraftStatsConfig.get();
        int cx = this.width / 2;
        int rowY = 40;
        int bw = 160, bh = 16;

        requireOpBtn = Button.builder(
                Component.literal("Require OP: " + cfg.requireOp),
                b -> { cfg.requireOp = !cfg.requireOp;
                    b.setMessage(Component.literal("Require OP: " + cfg.requireOp)); })
                .bounds(cx - bw / 2, rowY, bw, bh).build();
        rowY += 22;

        allowSurvivalBtn = Button.builder(
                Component.literal("Allow Survival: " + cfg.allowSurvival),
                b -> { cfg.allowSurvival = !cfg.allowSurvival;
                    b.setMessage(Component.literal("Allow Survival: " + cfg.allowSurvival)); })
                .bounds(cx - bw / 2, rowY, bw, bh).build();
        rowY += 22;

        intensityBox = new EditBox(this.font, cx - 60, rowY, 120, bh,
                Component.literal("Intensity"));
        intensityBox.setValue(cfg.randomizeIntensity);
        rowY += 22;

        saveBtn = Button.builder(Component.literal("Save"),
                b -> { cfg.randomizeIntensity = intensityBox.getValue();
                    CraftStatsConfig.save(); onClose(); })
                .bounds(cx - 40, rowY + 10, 80, bh).build();
        cancelBtn = Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 40, rowY + 32, 80, bh).build();

        addRenderableWidget(requireOpBtn);
        addRenderableWidget(allowSurvivalBtn);
        addRenderableWidget(intensityBox);
        addRenderableWidget(saveBtn);
        addRenderableWidget(cancelBtn);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        g.drawCenteredString(this.font, "CraftStats Config", this.width / 2, 14, 0xFFFFFFFF);
        g.drawString(this.font, "Randomize Intensity:", this.width / 2 - 60, 85, 0xFFCCCCCC, false);
        super.render(g, mx, my, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
