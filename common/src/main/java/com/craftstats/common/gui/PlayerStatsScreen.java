package com.craftstats.common.gui;

import com.craftstats.common.gui.panel.FieldDef;
import com.craftstats.common.gui.panel.FieldRow;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerStatsScreen extends Screen {

    private static final int HEADER_H = 18;
    private static final int PRESET_H = 18;
    private static final int TAB_H    = 18;
    private static final int ROW_H    = 18;
    private static final int FOOTER_H = 22;
    private static final int PAD      = 4;

    private final UUID   targetUUID;
    private final String targetName;

    private PlayerStats workingStats;
    private PlayerStats defaultStats;
    private int         activeTab    = 0;
    private int         scrollOffset = 0;
    private int         unsaved      = 0;

    private final List<FieldRow> rows      = new ArrayList<>();
    private final List<Button>   presetBtns = new ArrayList<>();
    private final List<FieldDef> activeDefs = new ArrayList<>();

    private Button applyBtn, resetBtn, fullEditorBtn;

    private int bodyY, bodyH;

    private static final String[] TAB_NAMES = { "Combat", "Movement", "Hunger", "Flags" };

    private static final Object[][] PRESETS = {
        { "Vanilla",     null    },
        { "Builder",     "builder" },
        { "PvP",         "pvp"     },
        { "Speedrunner", "speed"   },
        { "God Mode",    "god"     }
    };

    public PlayerStatsScreen(UUID uuid, String name) {
        super(Component.literal("Player Stats"));
        this.targetUUID = uuid;
        this.targetName = name;
    }

    @Override
    protected void init() {
        PlayerStats existing = StatRegistry.getPlayer(targetUUID);
        workingStats = existing != null ? existing.copy() : new PlayerStats();
        defaultStats  = new PlayerStats();
        unsaved       = 0;

        bodyY = PAD + HEADER_H + PAD + PRESET_H + PAD + TAB_H + 2;
        bodyH = this.height - bodyY - FOOTER_H - PAD * 2;

        presetBtns.clear();
        int totalW = this.width - PAD * 2;
        int btnW   = totalW / PRESETS.length - 2;
        int presetY = PAD + HEADER_H + PAD;
        for (int i = 0; i < PRESETS.length; i++) {
            final int pi = i;
            presetBtns.add(Button.builder(
                    Component.literal((String) PRESETS[i][0]),
                    b -> applyPreset(pi)
            ).bounds(PAD + i * (btnW + 2), presetY, btnW, PRESET_H).build());
        }

        int fy  = this.height - FOOTER_H - PAD + 2;
        int bh  = FOOTER_H - 6;
        int bwA = 60, bwR = 56, bwF = 50;
        applyBtn      = Button.builder(Component.literal("Apply"),       b -> onApply())
                .bounds(this.width - PAD - bwA,               fy, bwA, bh).build();
        resetBtn      = Button.builder(Component.literal("Reset"),       b -> onReset())
                .bounds(this.width - PAD - bwA - bwR - 3,     fy, bwR, bh).build();
        fullEditorBtn = Button.builder(Component.literal("Full"),        b -> openFullEditor())
                .bounds(this.width - PAD - bwA - bwR - bwF - 6, fy, bwF, bh).build();

        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        activeDefs.clear();
        activeDefs.addAll(getFieldsForTab(activeTab));
        int curY = bodyY + PAD;
        for (FieldDef fd : activeDefs) {
            rows.add(FieldRow.create(fd, workingStats,
                    PAD + 6, curY, this.width - PAD * 2 - 6, this::markDirty));
            curY += ROW_H + 2;
        }
        scrollOffset = 0;
    }

    private void markDirty() { unsaved++; }

    private List<FieldDef> getFieldsForTab(int tab) {
        return switch (tab) {
            case 0 -> List.of(
                    FieldDef.num("maxHealth",           "Max Health"),
                    FieldDef.num("baseDamage",          "Base Damage"),
                    FieldDef.num("attackSpeed",         "Attack Speed"),
                    FieldDef.num("reachDistance",       "Reach Distance"),
                    FieldDef.num("critMultiplier",      "Crit Multiplier"),
                    FieldDef.num("invincibilityFrames", "Invincibility Frames")
            );
            case 1 -> List.of(
                    FieldDef.num("walkSpeed",        "Walk Speed"),
                    FieldDef.num("sprintSpeed",      "Sprint Speed"),
                    FieldDef.num("flySpeed",         "Fly Speed"),
                    FieldDef.num("jumpForce",        "Jump Force"),
                    FieldDef.num("stepHeight",       "Step Height"),
                    FieldDef.num("swimSpeed",        "Swim Speed"),
                    FieldDef.toggle("noFallDamage",  "No Fall Damage"),
                    FieldDef.toggle("noClip",        "No Clip"),
                    FieldDef.toggle("infiniteSprint","Infinite Sprint"),
                    FieldDef.toggle("instantSwim",   "Instant Swim")
            );
            case 2 -> List.of(
                    FieldDef.num("maxFoodLevel",    "Max Food Level"),
                    FieldDef.num("regenThreshold",  "Regen Threshold"),
                    FieldDef.num("hungerDrainRate", "Hunger Drain Rate"),
                    FieldDef.num("exhaustionCap",   "Exhaustion Cap")
            );
            case 3 -> List.of(
                    FieldDef.toggle("keepInventory","Keep Inventory"),
                    FieldDef.toggle("fireImmune",   "Fire Immune"),
                    FieldDef.toggle("drownImmune",  "Drown Immune"),
                    FieldDef.toggle("oneHitKill",   "One Hit Kill"),
                    FieldDef.toggle("godMode",      "God Mode"),
                    FieldDef.toggle("infiniteItems","Infinite Items")
            );
            default -> List.of();
        };
    }

    private void applyPreset(int idx) {
        workingStats = buildPreset(idx);
        scrollOffset = 0;
        rebuildRows();
        markDirty();
    }

    private PlayerStats buildPreset(int idx) {
        PlayerStats p = new PlayerStats();
        switch (idx) {
            case 1 -> {
                p.noFallDamage = true; p.reachDistance = 8.0;
                p.walkSpeed = 0.15; p.maxHealth = 40;
            }
            case 2 -> {
                p.maxHealth = 30; p.baseDamage = 3.0; p.attackSpeed = 6.0;
                p.critMultiplier = 2.5; p.walkSpeed = 0.13; p.sprintSpeed = 0.17;
            }
            case 3 -> {
                p.walkSpeed = 0.2; p.sprintSpeed = 0.3; p.flySpeed = 0.1;
                p.jumpForce = 0.6; p.infiniteSprint = true; p.noFallDamage = true;
                p.stepHeight = 1.0;
            }
            case 4 -> {
                p.maxHealth = 200; p.godMode = true; p.fireImmune = true;
                p.drownImmune = true; p.noFallDamage = true;
                p.keepInventory = true; p.infiniteItems = true;
                p.noClip = false;
            }

        }
        return p;
    }

    private void onApply() {
        CraftStatsNetwork.sendApplyPlayerStats(targetUUID, workingStats);
        unsaved = 0;
    }

    private void onReset() {
        CraftStatsNetwork.sendReset("player", targetUUID.toString());
        workingStats = new PlayerStats();
        defaultStats  = new PlayerStats();
        unsaved       = 0;
        rebuildRows();
    }

    private void openFullEditor() {
        CraftStatsScreen full = new CraftStatsScreen();
        Minecraft.getInstance().setScreen(full);
        full.selectPlayerById(targetUUID, targetName);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {

    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();

        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);

        g.fill(0, 0, this.width, PAD + HEADER_H + 1, 0xFF0D0D22);
        g.fill(0, PAD + HEADER_H, this.width, PAD + HEADER_H + 1, 0xFF3355AA);
        String title = "★ Player Stats  ·  " + targetName;
        g.drawString(mc.font, title, PAD + 4, (PAD + HEADER_H - 8) / 2 + 1, 0xFFCCAAFF, false);
        String hint = unsaved > 0 ? "● " + unsaved + " unsaved" : "● Ready";
        g.drawString(mc.font, hint,
                this.width - PAD - mc.font.width(hint) - 4,
                (PAD + HEADER_H - 8) / 2 + 1,
                unsaved > 0 ? 0xFFFFAA00 : 0xFF44BB44, false);

        int presetY = PAD + HEADER_H + PAD;
        g.drawString(mc.font, "Quick:", PAD, presetY + 5, 0xFF888888, false);
        for (Button b : presetBtns) b.render(g, mx, my, delta);

        int tabY = presetY + PRESET_H + PAD;
        int tabW = (this.width - PAD * 2) / TAB_NAMES.length;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tx     = PAD + i * tabW;
            boolean active = i == activeTab;
            g.fill(tx, tabY, tx + tabW - 1, tabY + TAB_H, active ? 0xFF533483 : 0xFF1A1A2E);
            if (active) g.fill(tx, tabY + TAB_H - 2, tx + tabW - 1, tabY + TAB_H, 0xFF9966FF);
            g.drawCenteredString(mc.font, TAB_NAMES[i], tx + tabW / 2, tabY + 5,
                    active ? 0xFFFFFFFF : 0xFF888888);
        }

        g.fill(PAD, bodyY, this.width - PAD, bodyY + bodyH, 0xFF13192A);
        drawBorder(g, PAD, bodyY, this.width - PAD, bodyY + bodyH, 0xFF3355AA);

        g.enableScissor(PAD + 1, bodyY + 1, this.width - PAD - 1, bodyY + bodyH - 1);
        int visible = bodyH / (ROW_H + 2);
        for (int i = 0; i < rows.size(); i++) {
            FieldRow row = rows.get(i);
            int renderY = row.y - scrollOffset;
            if (renderY + ROW_H < bodyY || renderY > bodyY + bodyH) continue;

            if (i % 2 == 0) g.fill(PAD + 1, renderY, this.width - PAD - 1, renderY + ROW_H, 0x0CFFFFFF);

            if (isChangedFromDefault(i)) {
                g.fill(PAD + 1, renderY, PAD + 4, renderY + ROW_H, 0xFFFFCC00);
            }

            row.render(g, mx, my, delta, scrollOffset);
        }
        g.disableScissor();

        if (rows.size() > visible && visible > 0) {
            int sbH  = Math.max(14, bodyH * visible / rows.size());
            int sbY  = bodyY + (scrollOffset * (bodyH - sbH)) / Math.max(1, rows.size() - visible);
            g.fill(this.width - PAD - 3, bodyY, this.width - PAD - 1, bodyY + bodyH, 0xFF111133);
            g.fill(this.width - PAD - 3, sbY, this.width - PAD - 1, Math.min(bodyY + bodyH, sbY + sbH), 0xFF6677CC);
        }

        g.fill(0, this.height - FOOTER_H - PAD, this.width, this.height, 0xFF0A0A1A);
        g.fill(0, this.height - FOOTER_H - PAD, this.width, this.height - FOOTER_H - PAD + 1, 0xFF3355AA);

        g.fill(PAD + 4, this.height - FOOTER_H / 2 - 2, PAD + 8, this.height - FOOTER_H / 2 + 2, 0xFFFFCC00);
        g.drawString(mc.font, "= changed from vanilla",
                PAD + 12, this.height - FOOTER_H / 2 - 3, 0xFF888888, false);
        applyBtn.render(g, mx, my, delta);
        resetBtn.render(g, mx, my, delta);
        fullEditorBtn.render(g, mx, my, delta);

        super.render(g, mx, my, delta);
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private boolean isChangedFromDefault(int idx) {
        if (idx < 0 || idx >= activeDefs.size()) return false;
        String fieldName = activeDefs.get(idx).fieldName();
        try {
            Field wf = resolveField(workingStats.getClass(), fieldName);
            Field df = resolveField(defaultStats.getClass(), fieldName);
            if (wf == null || df == null) return false;
            wf.setAccessible(true);
            df.setAccessible(true);
            Object wv = wf.get(workingStats);
            Object dv = df.get(defaultStats);
            return wv != null && !wv.equals(dv);
        } catch (Exception ignored) { return false; }
    }

    private static Field resolveField(Class<?> c, String name) {
        try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}

        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char ch : name.toCharArray()) {
            if (ch == '_') { up = true; }
            else if (up)   { sb.append(Character.toUpperCase(ch)); up = false; }
            else           { sb.append(ch); }
        }
        try { return c.getDeclaredField(sb.toString()); } catch (NoSuchFieldException ignored) {}
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (Button b : presetBtns) if (b.mouseClicked(mx, my, btn)) return true;
        if (applyBtn.mouseClicked(mx, my, btn))      return true;
        if (resetBtn.mouseClicked(mx, my, btn))      return true;
        if (fullEditorBtn.mouseClicked(mx, my, btn)) return true;

        int tabY = PAD + HEADER_H + PAD + PRESET_H + PAD;
        if (my >= tabY && my < tabY + TAB_H) {
            int tabW = (this.width - PAD * 2) / TAB_NAMES.length;
            int idx = (int) ((mx - PAD) / tabW);
            if (idx >= 0 && idx < TAB_NAMES.length && idx != activeTab) {
                activeTab = idx;
                rebuildRows();
                return true;
            }
        }

        rows.forEach(FieldRow::clearFocus);
        int adjustedY = (int) my + scrollOffset;
        for (FieldRow row : rows) {
            if (row.mouseClicked(mx, adjustedY, btn)) return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        if (mx >= PAD && mx < this.width - PAD && my >= bodyY && my < bodyY + bodyH) {
            int visible   = bodyH / (ROW_H + 2);
            int maxScroll = Math.max(0, rows.size() * (ROW_H + 2) - bodyH);
            scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * (ROW_H + 2)));
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (FieldRow row : rows) if (row.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (FieldRow row : rows) if (row.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
