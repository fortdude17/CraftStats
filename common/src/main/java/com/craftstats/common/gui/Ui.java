package com.craftstats.common.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Drawing helpers that give CraftStats screens the look of vanilla menus. */
public final class Ui {

    public static final int WHITE  = 0xFFFFFFFF;
    public static final int GRAY   = 0xFFA0A0A0;
    public static final int DARK   = 0xFF707070;
    public static final int YELLOW = 0xFFFFFF55;
    public static final int GOLD   = 0xFFFFAA00;
    public static final int GREEN  = 0xFF55FF55;
    public static final int RED    = 0xFFFF5555;
    public static final int AQUA   = 0xFF55FFFF;

    private static final ResourceLocation LIST_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_LIST_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");

    private Ui() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** The darker list background vanilla uses for selection lists, with separators above and below. */
    public static void listPanel(GuiGraphics g, int x, int y, int w, int h) {
        boolean inWorld = Minecraft.getInstance().level != null;
        Screen.renderMenuBackgroundTexture(g, inWorld ? INWORLD_LIST_BACKGROUND : LIST_BACKGROUND, x, y, 0, 0, w, h);
        separator(g, x, y - 2, w);
        separator(g, x, y + h, w);
    }

    /** A two-pixel separator line like the ones under vanilla menu headers. */
    public static void separator(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, 0x99000000);
        g.fill(x, y + 1, x + w, y + 2, 0x40FFFFFF);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Vanilla selection highlight: white outline around a black box. */
    public static void selection(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, focused ? WHITE : GRAY);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
    }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, true);
    }

    /** Text cut to fit {@code maxW}, with "..." when shortened. */
    public static void textFit(GuiGraphics g, String s, int x, int y, int maxW, int color) {
        g.drawString(font(), fit(s, maxW), x, y, color, true);
    }

    public static String fit(String s, int maxW) {
        Font f = font();
        if (f.width(s) <= maxW) return s;
        return f.plainSubstrByWidth(s, Math.max(0, maxW - f.width("..."))) + "...";
    }

    public static void centered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawCenteredString(font(), s, cx, y, color);
    }

    public static void item(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) g.renderItem(stack, x, y);
    }

    public static ItemStack icon(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        Item item = com.craftstats.common.util.Compat.registryValue(BuiltInRegistries.ITEM, id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** A simple vertical scrollbar in the vanilla style. */
    public static void scrollbar(GuiGraphics g, int x, int y, int h, int contentH, int scroll) {
        if (contentH <= h) return;
        int barH = Math.max(16, h * h / contentH);
        int barY = y + (int) ((long) (h - barH) * scroll / Math.max(1, contentH - h));
        g.fill(x, y, x + 6, y + h, 0xFF000000);
        g.fill(x, barY, x + 6, barY + barH, 0xFF808080);
        g.fill(x, barY, x + 5, barY + barH - 1, 0xFFC0C0C0);
    }
}
