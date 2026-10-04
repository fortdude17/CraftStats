package com.craftstats.common.gui;

import com.craftstats.common.gui.editor.TargetEntries;
import com.craftstats.common.gui.editor.TargetList;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.util.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Pick a block whose real (vanilla) values get copied into the block being edited. */
public class BlockPickerScreen extends BaseScreen {

    private final Screen parent;
    private final Consumer<Block> onPick;
    private final TargetList list = new TargetList(this::pick);

    public BlockPickerScreen(Screen parent, Consumer<Block> onPick) {
        super(Component.literal("Copy From Block"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int w = Math.min(260, width - 20), x = (width - w) / 2;
        EditBox search = addRenderableWidget(new EditBox(font, x, 24, w, 18, Component.literal("Search")));
        search.setHint(Component.literal("Search blocks...").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(list::setQuery);
        setInitialFocus(search);
        list.setBounds(x, 48, w, height - 48 - 34);
        list.setEntries(TargetEntries.get(TargetType.BLOCK));
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    private void pick(TargetList.Entry e) {
        ResourceLocation id = ResourceLocation.tryParse(e.key());
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return;
        Minecraft.getInstance().setScreen(parent);
        onPick.accept(Compat.registryValue(BuiltInRegistries.BLOCK, id));
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
        Ui.centered(g, "Copy the values of which block?", width / 2, 10, Ui.WHITE);
        list.render(g, mx, my);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
