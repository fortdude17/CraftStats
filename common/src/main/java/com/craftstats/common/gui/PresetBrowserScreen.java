package com.craftstats.common.gui;

import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.JsonUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PresetBrowserScreen extends Screen {

    private final Screen        parent;
    private final TargetType    targetType;
    private final Object        currentStats;
    private final String        targetId;

    private List<Preset> presets;
    private int          selectedIdx = -1;
    private int          scrollOffset = 0;

    private static final int ROW_H = 14;
    private static final int LIST_X = 6;
    private static final int LIST_W = 180;
    private static final int PAD    = 6;

    private Button loadBtn, deleteBtn, saveBtn, importBtn, exportBtn, closeBtn;
    private EditBox importExportBox, nameBox;

    public PresetBrowserScreen(Screen parent, TargetType type, Object currentStats, String targetId) {
        super(Component.literal("Preset Browser — " + type.name()));
        this.parent       = parent;
        this.targetType   = type;
        this.currentStats = currentStats;
        this.targetId     = targetId;
    }

    @Override
    protected void init() {
        presets = PresetManager.getForType(targetType);
        int bw = 60, bh = 14;
        int rightX = LIST_X + LIST_W + PAD;
        int rightY = PAD + 14;

        loadBtn   = Button.builder(Component.literal("Load"),   b -> loadSelected()).bounds(rightX, rightY,       bw, bh).build();
        deleteBtn = Button.builder(Component.literal("Delete"), b -> deleteSelected()).bounds(rightX, rightY + 18, bw, bh).build();
        saveBtn   = Button.builder(Component.literal("Save Current"), b -> saveCurrentAsPreset()).bounds(rightX, rightY + 36, bw + 20, bh).build();
        importBtn = Button.builder(Component.literal("Import"), b -> importFromBox()).bounds(rightX, rightY + 54, bw, bh).build();
        exportBtn = Button.builder(Component.literal("Export"), b -> exportToBox()).bounds(rightX + bw + 4, rightY + 54, bw, bh).build();
        closeBtn  = Button.builder(Component.literal("Close"),  b -> onClose()).bounds(this.width - 60 - PAD, this.height - bh - PAD, 60, bh).build();

        nameBox = new EditBox(this.font, rightX, rightY + 74, 130, bh, Component.literal("Preset name"));
        nameBox.setMaxLength(64);

        importExportBox = new EditBox(this.font, PAD, this.height - bh * 3 - PAD * 2, this.width - PAD * 2, bh * 2,
                Component.literal("Import/Export JSON"));
        importExportBox.setMaxLength(8192);

        addRenderableWidget(loadBtn);
        addRenderableWidget(deleteBtn);
        addRenderableWidget(saveBtn);
        addRenderableWidget(importBtn);
        addRenderableWidget(exportBtn);
        addRenderableWidget(closeBtn);
        addRenderableWidget(nameBox);
        addRenderableWidget(importExportBox);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        g.fill(0, 0, this.width, this.height, 0xCC000000);
        g.fill(LIST_X, PAD + 12, LIST_X + LIST_W, this.height - 70, 0xFF1A1A2E);

        g.drawString(this.font, "Presets — " + targetType.name(), LIST_X, PAD, 0xFFFFFFFF, false);

        int listH = this.height - 70 - PAD - 12;
        int visRows = listH / ROW_H;
        g.enableScissor(LIST_X, PAD + 12, LIST_X + LIST_W, PAD + 12 + listH);
        for (int i = scrollOffset; i < Math.min(presets.size(), scrollOffset + visRows); i++) {
            Preset p  = presets.get(i);
            int ry = PAD + 12 + (i - scrollOffset) * ROW_H;
            boolean sel = i == selectedIdx;
            if (sel) g.fill(LIST_X, ry, LIST_X + LIST_W, ry + ROW_H, 0xFF0F3460);
            String label = (p.readonly ? "[R] " : "     ") + p.name;
            g.drawString(this.font, label, LIST_X + 2, ry + 3, sel ? 0xFFFFFFFF : 0xFFCCCCCC, false);
        }
        g.disableScissor();

        if (selectedIdx >= 0 && selectedIdx < presets.size()) {
            g.drawString(this.font, "Selected: " + presets.get(selectedIdx).name,
                    LIST_X + LIST_W + PAD, PAD, 0xFF44CCFF, false);
        }

        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int listH = this.height - 70 - PAD - 12;
        if (mx >= LIST_X && mx < LIST_X + LIST_W && my >= PAD + 12 && my < PAD + 12 + listH) {
            int row = ((int) my - PAD - 12) / ROW_H + scrollOffset;
            if (row >= 0 && row < presets.size()) { selectedIdx = row; return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int listH = this.height - 70 - PAD - 12;
        if (mx >= LIST_X && mx < LIST_X + LIST_W) {
            int maxScroll = Math.max(0, presets.size() - listH / ROW_H);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - v * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    private void loadSelected() {
        if (selectedIdx < 0 || selectedIdx >= presets.size()) return;
        Preset p = presets.get(selectedIdx);
        if (p.targetType != targetType) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("Type mismatch: preset is " + p.targetType));
            return;
        }

        if (parent instanceof CraftStatsScreen cs) {
            switch (targetType) {
                case MOB    -> cs.selectMobById(targetId);
                case BLOCK  -> cs.selectBlockById(targetId);
                case ITEM   -> cs.selectItemById(targetId);
            }
        }
        onClose();
    }

    private void deleteSelected() {
        if (selectedIdx < 0 || selectedIdx >= presets.size()) return;
        Preset p = presets.get(selectedIdx);
        if (p.readonly) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Cannot delete built-in preset."));
            return;
        }
        PresetManager.deletePreset(p.name);
        presets = PresetManager.getForType(targetType);
        selectedIdx = -1;
    }

    private void saveCurrentAsPreset() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) name = "preset_" + System.currentTimeMillis();
        Preset p = new Preset(name, targetType, currentStats);
        PresetManager.savePreset(p);
        presets = PresetManager.getForType(targetType);
    }

    private void importFromBox() {
        String json = importExportBox.getValue().trim();
        if (json.isEmpty()) return;
        try {
            Preset p = JsonUtil.GSON.fromJson(json, Preset.class);
            if (p != null && p.name != null) PresetManager.savePreset(p);
            presets = PresetManager.getForType(targetType);
        } catch (Exception e) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Invalid preset JSON."));
        }
    }

    private void exportToBox() {
        if (selectedIdx < 0 || selectedIdx >= presets.size()) return;
        importExportBox.setValue(JsonUtil.toJson(presets.get(selectedIdx)));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
