package com.craftstats.common.gui;

import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.stats.StatSchema;
import com.craftstats.common.stats.TargetType;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** Lists presets for one target type; loads one into the editor, saves, deletes, imports/exports JSON. */
public class PresetBrowserScreen extends BaseScreen {

    private static final int ROW_H  = 14;
    private static final int LIST_X = 6;
    private static final int LIST_W = 180;
    private static final int PAD    = 6;

    private final Screen           parent;
    private final TargetType       targetType;
    private final Object           currentStats;
    private final Consumer<Object> onLoad;

    private List<Preset> presets;
    private int selected = -1;
    private int scrollOffset;
    private String message = "";
    private int messageColor = 0xFFAAAAAA;

    private Button loadBtn, deleteBtn, exportBtn;
    private EditBox nameBox;

    public PresetBrowserScreen(Screen parent, TargetType type, Object currentStats, Consumer<Object> onLoad) {
        super(Component.literal("Presets - " + type.displayName()));
        this.parent = parent;
        this.targetType = type;
        this.currentStats = currentStats;
        this.onLoad = onLoad;
    }

    private int listTop()    { return PAD + 14; }
    private int listBottom() { return this.height - 30; }

    @Override
    protected void init() {
        presets = PresetManager.getForType(targetType);
        int bw = 90, bh = 16;
        int rx = LIST_X + LIST_W + PAD * 2;
        int ry = listTop();
        loadBtn   = addRenderableWidget(Button.builder(Component.literal("Load into Editor"), b -> loadSelected()).bounds(rx, ry, bw + 30, bh).build());
        deleteBtn = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteSelected()).bounds(rx, ry + 20, bw, bh).build());
        exportBtn = addRenderableWidget(Button.builder(Component.literal("Copy as JSON"), b -> exportSelected()).bounds(rx, ry + 40, bw, bh).build());

        nameBox = addRenderableWidget(new EditBox(this.font, rx, ry + 80, 150, bh, Component.literal("Preset name")));
        nameBox.setMaxLength(48);
        nameBox.setHint(Component.literal("name for new preset").withStyle(s -> s.withColor(0x707080)));
        addRenderableWidget(Button.builder(Component.literal("Save Current Values"), b -> saveCurrent()).bounds(rx, ry + 100, bw + 30, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Paste JSON as Preset"), b -> importClipboard()).bounds(rx, ry + 130, bw + 30, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(this.width - 60 - PAD, this.height - 22, 60, bh).build());
        updateButtons();
    }

    private void updateButtons() {
        boolean has = selected >= 0 && selected < presets.size();
        loadBtn.active = has;
        exportBtn.active = has;
        deleteBtn.active = has && !presets.get(selected).readonly;
    }

    private void say(String text, int color) {
        message = text;
        messageColor = color;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);
        g.drawString(font, "Presets - " + targetType.displayName(), LIST_X, PAD, 0xFFFFFFFF, false);
        g.fill(LIST_X, listTop(), LIST_X + LIST_W, listBottom(), 0xFF1A1A2E);
        CraftStatsScreen.border(g, LIST_X, listTop(), LIST_X + LIST_W, listBottom());

        int visible = (listBottom() - listTop()) / ROW_H;
        g.enableScissor(LIST_X, listTop(), LIST_X + LIST_W, listBottom());
        for (int i = scrollOffset; i < Math.min(presets.size(), scrollOffset + visible + 1); i++) {
            Preset p = presets.get(i);
            int ry = listTop() + (i - scrollOffset) * ROW_H;
            if (i == selected) g.fill(LIST_X, ry, LIST_X + LIST_W, ry + ROW_H, 0xFF3344BB);
            String label = (p.readonly ? "[built-in] " : "") + p.name.replace('_', ' ');
            g.drawString(font, font.plainSubstrByWidth(label, LIST_W - 6), LIST_X + 3, ry + 3,
                    p.readonly ? 0xFF99AACC : 0xFFFFFFFF, false);
        }
        g.disableScissor();

        int rx = LIST_X + LIST_W + PAD * 2;
        g.drawString(font, "New preset from the values in the editor:", rx, listTop() + 68, 0xFFAAAAAA, false);
        if (!message.isEmpty()) g.drawString(font, message, rx, listTop() + 155, messageColor, false);
        super.render(g, mx, my, delta);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        if (mx >= LIST_X && mx < LIST_X + LIST_W && my >= listTop() && my < listBottom()) {
            int row = ((int) my - listTop()) / ROW_H + scrollOffset;
            if (row >= 0 && row < presets.size()) {
                selected = row;
                updateButtons();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int visible = (listBottom() - listTop()) / ROW_H;
        scrollOffset = (int) Math.max(0, Math.min(Math.max(0, presets.size() - visible), scrollOffset - v * 3));
        return true;
    }

    private void loadSelected() {
        if (selected < 0 || selected >= presets.size()) return;
        onLoad.accept(presets.get(selected).copyStats());
        onClose();
    }

    private void deleteSelected() {
        if (selected < 0 || selected >= presets.size()) return;
        Preset p = presets.get(selected);
        if (PresetManager.delete(p)) say("Deleted '" + p.name + "'", 0xFF66DD66);
        presets = PresetManager.getForType(targetType);
        selected = -1;
        updateButtons();
    }

    private void exportSelected() {
        if (selected < 0 || selected >= presets.size()) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(StatSchema.GSON.toJson(presets.get(selected).toJson()));
        say("Copied preset JSON to clipboard", 0xFF66DD66);
    }

    private void saveCurrent() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) { say("Enter a name first", 0xFFFF6666); return; }
        Preset p = new Preset(name, targetType, StatSchema.parse(targetType, StatSchema.GSON_COMPACT.toJsonTree(currentStats)));
        if (!PresetManager.save(p)) { say("That name is taken by a built-in preset", 0xFFFF6666); return; }
        presets = PresetManager.getForType(targetType);
        selected = presets.indexOf(p);
        nameBox.setValue("");
        updateButtons();
        say("Saved '" + name + "'", 0xFF66DD66);
    }

    private void importClipboard() {
        try {
            Preset p = Preset.fromJson(JsonParser.parseString(Minecraft.getInstance().keyboardHandler.getClipboard()));
            if (p.targetType != targetType) {
                say("That is a " + p.targetType.displayName().toLowerCase() + " preset, not " + targetType.displayName().toLowerCase(), 0xFFFF6666);
                return;
            }
            if (!PresetManager.save(p)) { say("That name is taken by a built-in preset", 0xFFFF6666); return; }
            presets = PresetManager.getForType(targetType);
            say("Imported '" + p.name + "'", 0xFF66DD66);
        } catch (Exception e) {
            say("Clipboard doesn't contain a CraftStats preset", 0xFFFF6666);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
