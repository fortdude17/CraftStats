package com.craftstats.common.gui;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.util.BlockStatExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class BlockSourceBrowserScreen extends Screen {

    private static final int LIST_W    = 200;
    private static final int PREVIEW_W = 220;
    private static final int ROW_H     = 20;
    private static final int PAD       = 6;
    private static final int SEARCH_H  = 20;
    private static final int FOOTER_H  = 28;

    private final Screen         parent;
    private final Consumer<BlockStats> onApply;

    private final List<Block> allBlocks   = new ArrayList<>();
    private final List<Block> filtered    = new ArrayList<>();

    private Block     selected;
    private BlockStats preview;
    private int       scrollOffset = 0;
    private int       hoveredIndex = -1;

    private EditBox searchBox;
    private Button  applyBtn;
    private Button  cancelBtn;

    public BlockSourceBrowserScreen(Screen parent, Consumer<BlockStats> onApply) {
        super(Component.literal("Block Preset Browser"));
        this.parent  = parent;
        this.onApply = onApply;
    }

    @Override
    protected void init() {

        allBlocks.clear();
        for (Block b : BuiltInRegistries.BLOCK) {
            if (b == Blocks.AIR || b.asItem() == Items.AIR) continue;
            allBlocks.add(b);
        }
        allBlocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));

        filtered.clear();
        filtered.addAll(allBlocks);

        int listX    = PAD;
        int listY    = PAD + SEARCH_H + 4;
        int footerY  = this.height - FOOTER_H - PAD;

        searchBox = new EditBox(font, listX, PAD, LIST_W, SEARCH_H, Component.literal("Search..."));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search blocks...").withStyle(
                net.minecraft.ChatFormatting.DARK_GRAY));
        searchBox.setResponder(this::onSearch);
        addRenderableWidget(searchBox);

        int bw = 120, bh = 18;
        int totalBW = bw * 2 + PAD;
        int bx = (this.width - totalBW) / 2;
        int by = footerY + (FOOTER_H - bh) / 2;

        applyBtn  = Button.builder(Component.literal("Apply These Stats"), b -> doApply())
                .bounds(bx, by, bw, bh).build();
        cancelBtn = Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(bx + bw + PAD, by, bw, bh).build();
        addRenderableWidget(applyBtn);
        addRenderableWidget(cancelBtn);

        if (!filtered.isEmpty()) selectBlock(filtered.get(0));
    }

    private void onSearch(String query) {
        filtered.clear();
        String q = query.toLowerCase().trim();
        for (Block b : allBlocks) {
            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
            if (q.isEmpty() || id.contains(q)) filtered.add(b);
        }
        scrollOffset = 0;
        if (!filtered.isEmpty()) selectBlock(filtered.get(0));
        else { selected = null; preview = null; }
    }

    private void selectBlock(Block b) {
        selected = b;
        preview  = BlockStatExtractor.extractFrom(b);
        applyBtn.active = true;
    }

    private void doApply() {
        if (preview != null && onApply != null) onApply.accept(preview);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int listX   = PAD;
        int listY   = PAD + SEARCH_H + 4;
        int footerY = this.height - FOOTER_H - PAD;
        int listH   = footerY - listY - PAD;

        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);

        g.fill(listX, listY, listX + LIST_W, listY + listH, 0xFF16213E);
        drawBorder(g, listX, listY, listX + LIST_W, listY + listH, 0xFF3355AA);

        int prevX = listX + LIST_W + PAD * 2;
        int prevW = this.width - prevX - PAD;
        g.fill(prevX, listY, prevX + prevW, listY + listH, 0xFF1A1A2E);
        drawBorder(g, prevX, listY, prevX + prevW, listY + listH, 0xFF3355AA);

        g.fill(0, footerY, this.width, footerY + FOOTER_H + PAD, 0xFF0F3460);
        drawBorder(g, 0, footerY, this.width, footerY + FOOTER_H + PAD, 0xFF3355AA);

        g.drawCenteredString(font, "§b§lBlock Preset — copy any block's real stats",
                this.width / 2, listY - 14, 0xFFFFFFFF);

        g.enableScissor(listX, listY, listX + LIST_W, listY + listH);
        hoveredIndex = -1;
        for (int i = 0; i < filtered.size(); i++) {
            int rowY = listY + i * ROW_H - scrollOffset;
            if (rowY + ROW_H < listY || rowY > listY + listH) continue;

            Block b = filtered.get(i);
            boolean sel = b == selected;
            boolean hov = mx >= listX && mx < listX + LIST_W && my >= rowY && my < rowY + ROW_H;
            if (hov) hoveredIndex = i;

            if (sel) g.fill(listX, rowY, listX + LIST_W, rowY + ROW_H, 0xFF2244AA);
            else if (hov) g.fill(listX, rowY, listX + LIST_W, rowY + ROW_H, 0xFF1A2A50);

            ItemStack icon = new ItemStack(b.asItem());
            g.renderItem(icon, listX + 2, rowY + 2);

            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            String name = id.getPath().replace('_', ' ');
            if (name.length() > 18) name = name.substring(0, 17) + "…";
            g.drawString(font, name, listX + 20, rowY + (ROW_H - 8) / 2, sel ? 0xFFFFFFFF : 0xFFCCCCCC, false);
        }
        g.disableScissor();

        if (preview != null && selected != null) {
            ResourceLocation selId = BuiltInRegistries.BLOCK.getKey(selected);
            int px = prevX + PAD, py = listY + PAD;
            int lineH = 11;

            g.drawString(font, "§e§l" + selId.getPath().replace('_', ' '), px, py, 0xFFFFFFFF, false);
            py += lineH + 4;
            g.drawString(font, "§7" + selId, px, py, 0xFF666666, false);
            py += lineH + 8;

            ItemStack icon = new ItemStack(selected.asItem());
            g.renderItem(icon, prevX + prevW - 32, listY + PAD);
            g.renderItemDecorations(font, icon, prevX + prevW - 32, listY + PAD);

            g.drawString(font, "§aPhysical", px, py, 0xFFFFFFFF, false); py += lineH + 2;
            drawStat(g, px, py, "Hardness",        fmt(preview.hardness));    py += lineH;
            drawStat(g, px, py, "Blast Resist",    fmt(preview.blastResistance)); py += lineH;
            drawStat(g, px, py, "Slipperiness",    fmt(preview.slipperiness)); py += lineH;
            drawStat(g, px, py, "No Collision",    preview.noCollision ? "§aYes" : "§7No"); py += lineH;
            drawStat(g, px, py, "Climbable",       preview.climbable   ? "§aYes" : "§7No"); py += lineH;
            py += 6;

            g.drawString(font, "§aProperties", px, py, 0xFFFFFFFF, false); py += lineH + 2;
            drawStat(g, px, py, "Light Emission",  String.valueOf(preview.lightEmission)); py += lineH;
            py += 6;

            g.drawString(font, "§aStep-On Effects", px, py, 0xFFFFFFFF, false); py += lineH + 2;
            drawStat(g, px, py, "Step Damage",  preview.stepDamage  > 0 ? "§c" + fmt(preview.stepDamage) : "§7None"); py += lineH;
            drawStat(g, px, py, "Speed",        preview.speedModifier != 1.0f ? "§e" + fmt(preview.speedModifier) + "×" : "§7Normal"); py += lineH;
            drawStat(g, px, py, "Freeze",       preview.freezeOnStep ? "§bYes" : "§7No"); py += lineH;
            drawStat(g, px, py, "Bounce",       preview.bounceFactor > 0 ? "§d" + fmt(preview.bounceFactor) : "§7No"); py += lineH;
        } else {
            g.drawCenteredString(font, "§7Select a block from the list",
                    prevX + prevW / 2, listY + listH / 2, 0xFFAAAAAA);
        }

        super.render(g, mx, my, delta);
    }

    private void drawStat(GuiGraphics g, int x, int y, String label, String value) {
        g.drawString(font, "§8" + label + ": §f" + value, x, y, 0xFFFFFFFF, false);
    }

    private static String fmt(float v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e6) return String.valueOf((long)v);
        return String.format("%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);
        g.fill(x1,     y2 - 1, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + 1, y2,     color);
        g.fill(x2 - 1, y1,     x2,     y2,     color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int listX  = PAD;
        int listY  = PAD + SEARCH_H + 4;
        int footerY= this.height - FOOTER_H - PAD;
        int listH  = footerY - listY - PAD;

        if (mx >= listX && mx < listX + LIST_W && my >= listY && my < listY + listH) {
            int idx = (int)(my - listY + scrollOffset) / ROW_H;
            if (idx >= 0 && idx < filtered.size()) {
                selectBlock(filtered.get(idx));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int footerY = this.height - FOOTER_H - PAD;
        int listY   = PAD + SEARCH_H + 4;
        int listH   = footerY - listY - PAD;
        int maxScroll = Math.max(0, filtered.size() * ROW_H - listH);
        scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * ROW_H));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (searchBox.isFocused()) return searchBox.keyPressed(key, scan, mods);
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (searchBox.isFocused()) return searchBox.charTyped(c, mods);
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
