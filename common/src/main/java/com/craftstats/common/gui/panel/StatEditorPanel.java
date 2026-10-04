package com.craftstats.common.gui.panel;

import com.craftstats.common.gui.BlockSourceBrowserScreen;
import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.PresetBrowserScreen;
import com.craftstats.common.gui.UiInput;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.TargetType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Right-hand side of the main editor: header buttons, tabs and the field rows. */
public class StatEditorPanel {

    private static final int HEADER_H = 22;
    private static final int TAB_H    = 18;
    private static final int ROW_GAP  = 2;
    private static final int PAD      = 4;

    private final CraftStatsScreen parent;
    private final int x, y, w, h;

    private TargetType activeType;
    private String     targetLabel = "";
    private Object     stats;
    private Object     vanilla;

    private int     activeTab;
    private int     scrollOffset;
    private boolean instanceMode;

    private List<StatFields.Tab> tabs = List.of();
    private final List<FieldRow> rows = new ArrayList<>();

    private Button copyBtn, pasteBtn, fromBlockBtn, presetBtn;

    public StatEditorPanel(CraftStatsScreen parent, int x, int y, int w, int h) {
        this.parent = parent;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void init() {
        int bh = 14, by = y + (HEADER_H - bh) / 2;
        int presetW = 50, fromW = 66, smallW = 40;
        int px = x + w - presetW - PAD;
        int fx = px - fromW - PAD;
        int vx = fx - smallW - PAD;
        int cx = vx - smallW - PAD;
        copyBtn      = Button.builder(Component.literal("Copy"), b -> parent.onCopyProfile()).bounds(cx, by, smallW, bh).build();
        pasteBtn     = Button.builder(Component.literal("Paste"), b -> parent.onPasteProfile()).bounds(vx, by, smallW, bh).build();
        fromBlockBtn = Button.builder(Component.literal("From Block"), b -> openFromBlock()).bounds(fx, by, fromW, bh).build();
        presetBtn    = Button.builder(Component.literal("Presets"), b -> openPresets()).bounds(px, by, presetW, bh).build();
        updateButtons();
    }

    private List<Button> buttons() {
        return List.of(copyBtn, pasteBtn, fromBlockBtn, presetBtn);
    }

    private void updateButtons() {
        boolean has = stats != null;
        copyBtn.active = has;
        pasteBtn.active = has;
        presetBtn.active = has && com.craftstats.common.config.CraftStatsConfig.get().enablePresets;
        fromBlockBtn.active = has && activeType == TargetType.BLOCK;
    }

    public void setInstanceMode(boolean instance) { this.instanceMode = instance; }

    /** Shows a target. {@code vanilla} provides the default-value hints (may be null). */
    public void loadTarget(TargetType type, String label, Object statsObj, Object vanilla) {
        boolean sameType = type == activeType;
        this.activeType = type;
        this.targetLabel = label;
        this.stats = statsObj;
        this.vanilla = vanilla;
        this.tabs = type == null ? List.of() : StatFields.tabs(type);
        if (!sameType || activeTab >= tabs.size()) activeTab = 0;
        this.scrollOffset = 0;
        if (copyBtn != null) updateButtons();
        rebuildRows();
    }

    public void reload() {
        rebuildRows();
    }

    private int bodyY() { return y + HEADER_H + TAB_H + PAD; }
    private int bodyH() { return h - HEADER_H - TAB_H - PAD; }

    private void rebuildRows() {
        rows.clear();
        if (stats == null || tabs.isEmpty()) return;
        int curY = bodyY();
        for (FieldDef fd : tabs.get(activeTab).fields()) {
            rows.add(FieldRow.create(fd, stats, vanilla, x + PAD, curY, w - PAD * 2, parent::markDirty));
            curY += FieldRow.H + ROW_GAP;
        }
    }

    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();
        g.fill(x, y, x + w, y + HEADER_H, 0xFF0F3460);
        String typeLabel = activeType == null ? "" : instanceMode ? "MOB (this one)" : activeType.name();
        String header = stats == null ? "Pick something on the left" : "[" + typeLabel + "] " + targetLabel;
        int maxHeaderW = copyBtn.getX() - x - PAD * 2;
        g.drawString(mc.font, mc.font.plainSubstrByWidth(header, maxHeaderW), x + PAD, y + 7, 0xFFFFFFFF, false);
        for (Button b : buttons()) b.render(g, mx, my, delta);

        if (!tabs.isEmpty()) {
            int tabW = w / tabs.size();
            for (int i = 0; i < tabs.size(); i++) {
                int tx = x + i * tabW, ty = y + HEADER_H;
                boolean active = i == activeTab;
                g.fill(tx, ty, tx + tabW - 1, ty + TAB_H, active ? 0xFF533483 : 0xFF2D2D4E);
                g.drawCenteredString(mc.font, tabs.get(i).name(), tx + tabW / 2, ty + 5, active ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }

        int bodyY = bodyY(), bodyH = bodyH();
        g.enableScissor(x, bodyY, x + w, bodyY + bodyH);
        for (int i = 0; i < rows.size(); i++) {
            FieldRow row = rows.get(i);
            int ry = row.y - scrollOffset;
            if (ry + FieldRow.H < bodyY || ry > bodyY + bodyH) continue;
            if (i % 2 == 0) g.fill(x + 1, ry, x + w - 1, ry + FieldRow.H, 0x12FFFFFF);
            row.render(g, mx, my, delta, scrollOffset);
        }
        g.disableScissor();

        int contentH = rows.size() * (FieldRow.H + ROW_GAP);
        if (contentH > bodyH) {
            int barH = Math.max(12, bodyH * bodyH / contentH);
            int barY = bodyY + (bodyH - barH) * scrollOffset / Math.max(1, contentH - bodyH);
            g.fill(x + w - 3, bodyY, x + w - 1, bodyY + bodyH, 0xFF111133);
            g.fill(x + w - 3, barY, x + w - 1, barY + barH, 0xFF6677CC);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        for (Button b : buttons()) if (UiInput.click(b, mx, my, btn)) return true;

        if (!tabs.isEmpty() && my >= y + HEADER_H && my < y + HEADER_H + TAB_H && mx >= x && mx < x + w) {
            int idx = (int) ((mx - x) / (w / tabs.size()));
            if (idx >= 0 && idx < tabs.size() && idx != activeTab) {
                activeTab = idx;
                scrollOffset = 0;
                rebuildRows();
            }
            return true;
        }

        if (mx < x || mx > x + w || my < bodyY() || my > bodyY() + bodyH()) return false;
        clearAllFocus();
        for (FieldRow row : rows) if (row.mouseClicked(mx, my + scrollOffset, btn)) return true;
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double vScroll) {
        if (mx < x || mx >= x + w || my < bodyY() || my >= bodyY() + bodyH()) return false;
        int maxScroll = Math.max(0, rows.size() * (FieldRow.H + ROW_GAP) - bodyH());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * FieldRow.H));
        return true;
    }

    public void clearAllFocus() {
        rows.forEach(FieldRow::clearFocus);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        for (FieldRow row : rows) if (row.keyPressed(key, scan, mods)) return true;
        return false;
    }

    public boolean charTyped(char c, int mods) {
        for (FieldRow row : rows) if (row.charTyped(c, mods)) return true;
        return false;
    }

    private void openFromBlock() {
        if (!(stats instanceof BlockStats target)) return;
        Minecraft.getInstance().setScreen(new BlockSourceBrowserScreen(parent, extracted -> {
            copyBlockValues(extracted, target);
            parent.markDirty();
            rebuildRows();
        }));
    }

    /** Copies the physical and step-on values of another block. */
    public static void copyBlockValues(BlockStats from, BlockStats to) {
        to.hardness        = from.hardness;
        to.blastResistance = from.blastResistance;
        to.slipperiness    = from.slipperiness;
        to.lightEmission   = from.lightEmission;
        to.noCollision     = from.noCollision;
        to.climbable       = from.climbable;
        to.stepDamage      = from.stepDamage;
        to.speedModifier   = from.speedModifier;
        to.freezeOnStep    = from.freezeOnStep;
    }

    private void openPresets() {
        if (stats == null) return;
        Minecraft.getInstance().setScreen(new PresetBrowserScreen(parent, activeType, stats, parent::loadStats));
    }
}
