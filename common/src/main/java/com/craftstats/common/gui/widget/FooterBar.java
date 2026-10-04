package com.craftstats.common.gui.widget;

import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.UiInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Bottom bar of the main editor: status, Reset All, Reset and Apply. */
public class FooterBar {

    private final CraftStatsScreen parent;
    private final int x, y, w, h;

    private Button resetBtn, applyBtn, resetAllBtn;
    private int unsavedChanges;
    private int confirmTimer;
    private String flash = "";
    private int flashTimer;

    public FooterBar(CraftStatsScreen parent, int x, int y, int w, int h) {
        this.parent = parent;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void init() {
        int bh = 14, by = y + (h - bh) / 2;
        int smallW = 50, dangerW = 76;
        int applyX = x + w - smallW - 4;
        int resetX = applyX - smallW - 4;
        int resetAllX = resetX - dangerW - 4;
        applyBtn    = Button.builder(Component.literal("Apply"), b -> parent.onApply()).bounds(applyX, by, smallW, bh).build();
        resetBtn    = Button.builder(Component.literal("Reset"), b -> parent.onReset()).bounds(resetX, by, smallW, bh).build();
        resetAllBtn = Button.builder(Component.literal("Reset All"), b -> handleResetAll()).bounds(resetAllX, by, dangerW, bh).build();
    }

    private void handleResetAll() {
        if (confirmTimer == 0) {
            confirmTimer = 80;
            resetAllBtn.setMessage(Component.literal("Confirm?").withStyle(ChatFormatting.RED));
        } else {
            confirmTimer = 0;
            resetAllBtn.setMessage(Component.literal("Reset All"));
            parent.onResetAll();
        }
    }

    public void update(int unsaved) {
        this.unsavedChanges = unsaved;
    }

    public void flash(String message) {
        flash = message;
        flashTimer = 60;
    }

    public void tick() {
        if (flashTimer > 0) flashTimer--;
        if (confirmTimer > 0 && --confirmTimer == 0) resetAllBtn.setMessage(Component.literal("Reset All"));
    }

    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        String status; int color;
        if (flashTimer > 0)           { status = flash; color = 0xFF66DDFF; }
        else if (unsavedChanges > 0)  { status = "● " + unsavedChanges + " unapplied change(s)"; color = 0xFFFFAA00; }
        else                          { status = "● Up to date"; color = 0xFF44BB44; }
        if (confirmTimer > 0)         { status = "Click again to remove ALL CraftStats changes in this world"; color = 0xFFFFEE00; }
        g.drawString(mc.font, mc.font.plainSubstrByWidth(status, resetAllBtn.getX() - x - 10), x + 6, y + (h - 8) / 2, color, false);
        for (Button b : List.of(resetAllBtn, resetBtn, applyBtn)) b.render(g, mx, my, delta);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        for (Button b : List.of(resetAllBtn, resetBtn, applyBtn)) if (UiInput.click(b, mx, my, btn)) return true;
        return false;
    }
}
