package com.craftstats.common.gui;

import com.craftstats.common.gui.panel.FieldDef;
import com.craftstats.common.gui.panel.FieldRow;
import com.craftstats.common.gui.panel.StatEditorPanel;
import com.craftstats.common.gui.panel.StatFields;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.VanillaStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** Edits a single block position. Only fields Minecraft can apply per position are shown. */
public class BlockPosScreen extends BaseScreen {

    private static final int HEADER_H = 40;
    private static final int TAB_H    = 18;
    private static final int FOOTER_H = 28;
    private static final int PAD      = 6;
    private static final int ROW_H    = FieldRow.H + 2;

    private final String   posKey;
    private final Block    block;
    private final BlockPos pos;
    private final String   blockId;
    private final BlockStats vanilla;
    private final List<StatFields.Tab> tabs = StatFields.blockPositionTabs();

    private BlockStats working;
    private int activeTab;
    private int scrollOffset;
    private int unsaved;

    private final List<FieldRow> rows = new ArrayList<>();
    private Button applyBtn;

    public BlockPosScreen(String posKey, Block block, BlockPos pos) {
        super(Component.literal("Block Editor"));
        this.posKey = posKey;
        this.block = block;
        this.pos = pos;
        this.blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        this.vanilla = VanillaStats.block(block);
        BlockStats existing = StatRegistry.getBlockAt(posKey);
        this.working = existing != null ? existing.copy() : new BlockStats();
    }

    private int panelW() { return Math.min(360, this.width - PAD * 2); }
    private int panelX() { return (this.width - panelW()) / 2; }
    private int bodyY()  { return HEADER_H + TAB_H + PAD; }
    private int bodyH()  { return this.height - bodyY() - FOOTER_H - PAD * 2; }

    @Override
    protected void init() {
        int tabW = panelW() / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(tabs.get(i).name()), b -> selectTab(idx))
                    .bounds(panelX() + i * tabW, HEADER_H, tabW - 2, TAB_H).build());
        }

        int bh = 18;
        int[] widths = {60, 50, 76, 90, 50};
        int total = PAD * (widths.length - 1);
        for (int bw : widths) total += bw;
        int bx = (this.width - total) / 2;
        int by = this.height - FOOTER_H - PAD + (FOOTER_H - bh) / 2;
        applyBtn = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> doApply()).bounds(bx, by, widths[0], bh).build());
        bx += widths[0] + PAD;
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> doReset()).bounds(bx, by, widths[1], bh).build());
        bx += widths[1] + PAD;
        addRenderableWidget(Button.builder(Component.literal("From Block"), b -> doFromBlock()).bounds(bx, by, widths[2], bh).build());
        bx += widths[2] + PAD;
        addRenderableWidget(Button.builder(Component.literal("Edit Block Type"), b ->
                Minecraft.getInstance().setScreen(new CraftStatsScreen(block))).bounds(bx, by, widths[3], bh).build());
        bx += widths[3] + PAD;
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(bx, by, widths[4], bh).build());
        rebuildRows();
        updateApplyLabel();
    }

    private void selectTab(int tab) {
        activeTab = tab;
        scrollOffset = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        int curY = bodyY();
        for (FieldDef fd : tabs.get(activeTab).fields()) {
            rows.add(FieldRow.create(fd, working, vanilla, panelX() + PAD, curY, panelW() - PAD * 2,
                    () -> { unsaved++; updateApplyLabel(); }));
            curY += ROW_H;
        }
    }

    private void updateApplyLabel() {
        if (applyBtn != null) applyBtn.setMessage(Component.literal(unsaved > 0 ? "Apply *" : "Apply"));
    }

    private void doApply() {
        CraftStatsNetwork.sendApply(CraftStatsNetwork.Kind.BLOCK_POS, posKey, working);
        unsaved = 0;
        updateApplyLabel();
    }

    private void doReset() {
        CraftStatsNetwork.sendReset(CraftStatsNetwork.Kind.BLOCK_POS, posKey);
        working = new BlockStats();
        unsaved = 0;
        scrollOffset = 0;
        rebuildRows();
        updateApplyLabel();
    }

    private void doFromBlock() {
        Minecraft.getInstance().setScreen(new BlockSourceBrowserScreen(this, extracted -> {
            StatEditorPanel.copyBlockValues(extracted, working);
            unsaved++;
            updateApplyLabel();
            rebuildRows();
        }));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int panelX = panelX(), panelW = panelW(), bodyY = bodyY(), bodyH = bodyH();
        int footerY = this.height - FOOTER_H - PAD;
        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);
        g.fill(panelX, HEADER_H + TAB_H, panelX + panelW, bodyY + bodyH + PAD, 0xFF16213E);
        CraftStatsScreen.border(g, panelX, HEADER_H + TAB_H, panelX + panelW, bodyY + bodyH + PAD);

        String name = blockId.startsWith("minecraft:") ? blockId.substring(10) : blockId;
        g.drawCenteredString(font, name, this.width / 2, PAD + 2, 0xFF66DDFF);
        g.drawCenteredString(font, "Only the block at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                this.width / 2, PAD + 14, 0xFFAAAAAA);
        g.drawCenteredString(font, "Removed automatically if a different block is placed here",
                this.width / 2, PAD + 25, 0xFF666688);

        g.fill(0, footerY, this.width, this.height, 0xFF0F3460);
        int tabW = panelW / tabs.size();
        int tx = panelX + activeTab * tabW;
        g.fill(tx, HEADER_H + TAB_H - 2, tx + tabW - 2, HEADER_H + TAB_H, 0xFF5588FF);

        g.enableScissor(panelX, bodyY, panelX + panelW, bodyY + bodyH);
        for (FieldRow row : rows) {
            int ry = row.y - scrollOffset;
            if (ry + ROW_H < bodyY || ry > bodyY + bodyH) continue;
            row.render(g, mouseX, mouseY, delta, scrollOffset);
        }
        g.disableScissor();

        super.render(g, mouseX, mouseY, delta);
        if (unsaved > 0) g.drawString(font, unsaved + " unapplied change(s)", PAD, footerY + 9, 0xFFFFCC44, false);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int btn) {
        rows.forEach(FieldRow::clearFocus);
        if (my < bodyY() || my > bodyY() + bodyH()) return false;
        for (FieldRow row : rows) if (row.mouseClicked(mx, my + scrollOffset, btn)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int maxScroll = Math.max(0, rows.size() * ROW_H - bodyH());
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
