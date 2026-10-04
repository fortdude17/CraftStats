package com.craftstats.common.gui;

import com.craftstats.common.gui.panel.FieldDef;
import com.craftstats.common.gui.panel.FieldRow;
import com.craftstats.common.gui.panel.StatFields;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.TargetType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Compact player editor opened by the Player Stats Book or the wand. */
public class PlayerStatsScreen extends BaseScreen {

    private static final int HEADER_H = 18;
    private static final int PRESET_H = 16;
    private static final int TAB_H    = 18;
    private static final int FOOTER_H = 22;
    private static final int PAD      = 4;
    private static final int ROW_H    = FieldRow.H + 2;

    private final UUID   targetUUID;
    private final String targetName;
    private final List<StatFields.Tab> tabs = StatFields.tabs(TargetType.PLAYER);
    private final PlayerStats vanilla = new PlayerStats();

    private PlayerStats working;
    private int activeTab;
    private int scrollOffset;
    private int unsaved;
    private String status = "";

    private final List<FieldRow> rows = new ArrayList<>();
    private int bodyY, bodyH;

    public PlayerStatsScreen(UUID uuid, String name) {
        super(Component.literal("Player Stats"));
        this.targetUUID = uuid;
        this.targetName = name;
        PlayerStats existing = StatRegistry.getPlayer(uuid);
        this.working = existing != null ? existing.copy() : new PlayerStats();
    }

    @Override
    protected void init() {
        int presetY = PAD + HEADER_H + PAD;
        int tabY = presetY + PRESET_H + PAD;
        bodyY = tabY + TAB_H + PAD;
        bodyH = this.height - bodyY - FOOTER_H - PAD * 2;

        List<Preset> presets = PresetManager.getForType(TargetType.PLAYER);
        int shown = Math.min(presets.size(), 6);
        if (shown > 0) {
            int btnW = (this.width - PAD * 2) / shown - 2;
            for (int i = 0; i < shown; i++) {
                Preset p = presets.get(i);
                addRenderableWidget(Button.builder(Component.literal(p.name.replace('_', ' ')), b -> applyPreset(p))
                        .bounds(PAD + i * (btnW + 2), presetY, btnW, PRESET_H).build());
            }
        }

        int tabW = (this.width - PAD * 2) / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(tabs.get(i).name()), b -> selectTab(idx))
                    .bounds(PAD + i * tabW, tabY, tabW - 2, TAB_H).build());
        }

        int fy = this.height - FOOTER_H - PAD + 2, bh = FOOTER_H - 6;
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> onApply())
                .bounds(this.width - PAD - 60, fy, 60, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> onReset())
                .bounds(this.width - PAD - 60 - 56 - 3, fy, 56, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Full Editor"), b -> openFullEditor())
                .bounds(this.width - PAD - 60 - 56 - 70 - 6, fy, 70, bh).build());
        rebuildRows();
    }

    private void selectTab(int idx) {
        activeTab = idx;
        scrollOffset = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        int curY = bodyY;
        for (FieldDef fd : tabs.get(activeTab).fields()) {
            rows.add(FieldRow.create(fd, working, vanilla, PAD + 6, curY, Math.min(420, this.width - PAD * 2 - 12),
                    () -> unsaved++));
            curY += ROW_H;
        }
    }

    private void applyPreset(Preset preset) {
        working = (PlayerStats) preset.copyStats();
        unsaved++;
        status = "Loaded preset '" + preset.name + "' - press Apply";
        rebuildRows();
    }

    private void onApply() {
        CraftStatsNetwork.sendApply(CraftStatsNetwork.Kind.PLAYER, targetUUID.toString(), working);
        unsaved = 0;
        status = "Applied";
    }

    private void onReset() {
        CraftStatsNetwork.sendReset(CraftStatsNetwork.Kind.PLAYER, targetUUID.toString());
        working = new PlayerStats();
        unsaved = 0;
        status = "Reset to vanilla";
        rebuildRows();
    }

    private void openFullEditor() {
        CraftStatsScreen full = new CraftStatsScreen();
        Minecraft.getInstance().setScreen(full);
        full.selectPlayerById(targetUUID, targetName);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);
        g.fill(0, PAD + HEADER_H, this.width, PAD + HEADER_H + 1, 0xFF3355AA);
        g.drawString(font, "Player Stats - " + targetName, PAD + 4, PAD + 5, 0xFFCCAAFF, false);

        g.enableScissor(0, bodyY, this.width, bodyY + bodyH);
        for (int i = 0; i < rows.size(); i++) {
            FieldRow row = rows.get(i);
            int ry = row.y - scrollOffset;
            if (ry + ROW_H < bodyY || ry > bodyY + bodyH) continue;
            if (i % 2 == 0) g.fill(PAD, ry, this.width - PAD, ry + FieldRow.H, 0x12FFFFFF);
            row.render(g, mx, my, delta, scrollOffset);
        }
        g.disableScissor();

        int fy = this.height - FOOTER_H - PAD;
        g.fill(0, fy, this.width, this.height, 0xFF0F3460);
        String footer = unsaved > 0 ? unsaved + " unapplied change(s)" : status;
        g.drawString(font, footer, PAD + 4, fy + 8, unsaved > 0 ? 0xFFFFAA00 : 0xFF66DDFF, false);
        super.render(g, mx, my, delta);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        rows.forEach(FieldRow::clearFocus);
        if (my < bodyY || my > bodyY + bodyH) return false;
        for (FieldRow row : rows) if (row.mouseClicked(mx, my + scrollOffset, btn)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int maxScroll = Math.max(0, rows.size() * ROW_H - bodyH);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * ROW_H));
        return true;
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        for (FieldRow row : rows) if (row.keyPressed(key, scan, mods)) return true;
        return false;
    }

    @Override
    protected boolean onCharTyped(char c, int mods) {
        for (FieldRow row : rows) if (row.charTyped(c, mods)) return true;
        return false;
    }
}
