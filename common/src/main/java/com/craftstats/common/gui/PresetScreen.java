package com.craftstats.common.gui;

import com.craftstats.common.gui.editor.TargetList;
import com.craftstats.common.preset.Preset;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.stats.StatSchema;
import com.craftstats.common.stats.TargetType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Pick a built-in or saved preset, or save the current stats as a new one. */
public class PresetScreen extends BaseScreen {

    private final Screen parent;
    private final TargetType type;
    private final Object current;
    private final Consumer<Object> onLoad;

    private final TargetList list = new TargetList(e -> selected = e.key());
    private String selected = "";
    private EditBox nameBox;
    private Button loadBtn, deleteBtn, saveBtn;
    private String message = "";

    public PresetScreen(Screen parent, TargetType type, Object current, Consumer<Object> onLoad) {
        super(Component.literal(type.displayName() + " Presets"));
        this.parent = parent;
        this.type = type;
        this.current = current;
        this.onLoad = onLoad;
    }

    @Override
    protected void init() {
        int w = Math.min(300, width - 20), x = (width - w) / 2;
        list.setBounds(x, 34, w, height - 34 - 66);
        refreshList();

        int by = height - 54;
        nameBox = addRenderableWidget(new EditBox(font, x, by, w - 104, 20, Component.literal("Preset name")));
        nameBox.setHint(Component.literal("Name for a new preset").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setMaxLength(40);
        saveBtn = addRenderableWidget(Button.builder(Component.literal("Save Current"), b -> save())
                .bounds(x + w - 100, by, 100, 20).build());

        int bw = (w - 8) / 3;
        loadBtn = addRenderableWidget(Button.builder(Component.literal("Load").withStyle(ChatFormatting.GREEN), b -> load())
                .bounds(x, height - 28, bw, 20).build());
        deleteBtn = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> delete())
                .bounds(x + bw + 4, height - 28, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(x + (bw + 4) * 2, height - 28, bw, 20).build());
    }

    private void refreshList() {
        List<TargetList.Entry> entries = new ArrayList<>();
        for (Preset p : PresetManager.getForType(type)) {
            ItemStack icon = new ItemStack(p.readonly ? Items.BOOK : Items.WRITABLE_BOOK);
            entries.add(new TargetList.Entry(p.name, p.name.replace('_', ' ') + (p.readonly ? "" : "  (yours)"), icon));
        }
        list.setEntries(entries);
        list.select(selected, false);
    }

    private Preset find(String name) {
        for (Preset p : PresetManager.getForType(type)) if (p.name.equals(name)) return p;
        return null;
    }

    private void load() {
        Preset p = find(selected);
        if (p == null) return;
        onLoad.accept(p.copyStats());
        Minecraft.getInstance().setScreen(parent);
    }

    private void delete() {
        Preset p = find(selected);
        if (p == null || p.readonly) return;
        PresetManager.delete(p);
        selected = "";
        message = "Deleted " + p.name;
        refreshList();
    }

    private void save() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) { message = "Type a name first"; return; }
        Preset p = new Preset(name, type, StatSchema.copyAny(current));
        if (PresetManager.save(p)) {
            message = "Saved \"" + name + "\"";
            selected = name;
            nameBox.setValue("");
            refreshList();
        } else {
            message = "Can't use that name";
        }
    }

    @Override
    public void tick() {
        Preset p = find(selected);
        loadBtn.active = p != null;
        deleteBtn.active = p != null && !p.readonly;
        saveBtn.active = !nameBox.getValue().isBlank();
    }

    @Override
    protected boolean onMouseClicked(double x, double y, int button) {
        return button == 0 && list.mouseClicked(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return list.mouseScrolled(x, y, v) || super.mouseScrolled(x, y, h, v);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        Ui.centered(g, title.getString(), width / 2, 10, Ui.WHITE);
        Ui.centered(g, message.isEmpty() ? "Built-in presets can't be deleted. Yours are saved in craftstats/presets/" : message,
                width / 2, 21, message.isEmpty() ? Ui.GRAY : Ui.AQUA);
        list.render(g, mx, my);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
