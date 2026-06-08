package com.craftstats.common.gui.widget;

import com.craftstats.common.gui.CraftStatsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FooterBar {

    private final CraftStatsScreen parent;
    private final int x, y, w, h;

    private Button resetBtn, applyBtn, resetAllBtn;
    private int    unsavedChanges = 0;

    private boolean confirmingResetAll = false;
    private int     confirmTimer       = 0;

    public FooterBar(CraftStatsScreen parent, int x, int y, int w, int h) {
        this.parent = parent;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void init() {
        int bh = 14;
        int by = y + (h - bh) / 2;

        int bwSmall  = 50;
        int bwDanger = 76;

        int applyX    = x + w - bwSmall - 4;
        int resetX    = applyX - bwSmall - 4;
        int resetAllX = resetX - bwDanger - 4;

        applyBtn = Button.builder(Component.literal("Apply"),
                b -> parent.onApply())
                .bounds(applyX, by, bwSmall, bh).build();
        resetBtn = Button.builder(Component.literal("Reset"),
                b -> parent.onReset())
                .bounds(resetX, by, bwSmall, bh).build();
        resetAllBtn = Button.builder(Component.literal("Reset All"),
                b -> handleResetAll())
                .bounds(resetAllX, by, bwDanger, bh).build();
    }

    private void handleResetAll() {
        if (!confirmingResetAll) {

            confirmingResetAll = true;
            confirmTimer       = 80;
            resetAllBtn.setMessage(Component.literal("§cConfirm?"));
        } else {

            confirmingResetAll = false;
            confirmTimer       = 0;
            resetAllBtn.setMessage(Component.literal("Reset All"));
            parent.onResetAll();
        }
    }

    public void update(int unsaved) {
        this.unsavedChanges = unsaved;
    }

    public void tick() {
        if (confirmTimer > 0) {
            confirmTimer--;
            if (confirmTimer == 0) {
                confirmingResetAll = false;
                if (resetAllBtn != null) resetAllBtn.setMessage(Component.literal("Reset All"));
            }
        }
    }

    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();

        String status = unsavedChanges > 0
                ? "● " + unsavedChanges + " unsaved change(s)"
                : "● Connected";
        int color = unsavedChanges > 0 ? 0xFFFFAA00 : 0xFF44BB44;
        g.drawString(mc.font, status, x + 6, y + (h - 8) / 2, color, false);

        if (confirmingResetAll) {
            String hint = "§eClick again to wipe ALL mod stats from this world!";
            g.drawString(mc.font, hint,
                    resetAllBtn.getX() - mc.font.width(hint) - 6,
                    y + (h - 8) / 2, 0xFFFFEE00, false);
        }

        resetAllBtn.render(g, mx, my, delta);
        resetBtn.render(g, mx, my, delta);
        applyBtn.render(g, mx, my, delta);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (resetAllBtn.mouseClicked(mx, my, btn)) return true;
        if (resetBtn.mouseClicked(mx, my, btn))    return true;
        if (applyBtn.mouseClicked(mx, my, btn))    return true;
        return false;
    }
}
