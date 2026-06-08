package com.craftstats.common.gui;

import com.craftstats.common.gui.panel.FieldDef;
import com.craftstats.common.gui.panel.FieldRow;
import com.craftstats.common.gui.panel.StatEditorPanel;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.BlockStatExtractor;
import com.craftstats.common.util.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockPosScreen extends Screen {

    private static final int HEADER_H = 40;
    private static final int TAB_H    = 18;
    private static final int FOOTER_H = 28;
    private static final int PAD      = 6;
    private static final int ROW_H    = 18;

    private static final String[] TAB_NAMES = {"Physical", "Properties", "Step-On FX", "Rendering"};

    private final String   posKey;
    private final Block    block;
    private final BlockPos pos;
    private final String   blockId;

    private BlockStats working;
    private int        activeTab    = 0;
    private int        scrollOffset = 0;
    private int        unsaved      = 0;

    private final List<FieldRow> rows = new ArrayList<>();

    private Button applyBtn, resetBtn, editTypeBtn, fromBlockBtn, closeBtn;
    private final Button[] tabBtns = new Button[TAB_NAMES.length];

    public BlockPosScreen(String posKey, Block block, BlockPos pos) {
        super(Component.literal("Block Editor"));
        this.posKey  = posKey;
        this.block   = block;
        this.pos     = pos;
        this.blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        BlockStats existing = StatRegistry.getBlockAt(posKey);
        this.working = existing != null ? existing.copy() : new BlockStats();
    }

    @Override
    protected void init() {
        int panelW = Math.min(340, this.width - PAD * 2);
        int panelX = (this.width - panelW) / 2;

        int footerY = this.height - FOOTER_H - PAD;

        int tabW = panelW / TAB_NAMES.length;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int idx = i;
            tabBtns[i] = Button.builder(Component.literal(TAB_NAMES[i]), b -> selectTab(idx))
                    .bounds(panelX + i * tabW, HEADER_H, tabW - 2, TAB_H)
                    .build();
            addRenderableWidget(tabBtns[i]);
        }

        int bh = 18;
        int bw1 = 60, bw2 = 50, bw3 = 76, bw4 = 82, bw5 = 50;
        int totalBW = bw1 + bw2 + bw3 + bw4 + bw5 + PAD * 4;
        int bx = (this.width - totalBW) / 2;
        int by = footerY + (FOOTER_H - bh) / 2;

        applyBtn    = Button.builder(Component.literal("Apply"),          b -> doApply())
                .bounds(bx, by, bw1, bh).build();
        resetBtn    = Button.builder(Component.literal("Reset"),          b -> doReset())
                .bounds(bx + bw1 + PAD, by, bw2, bh).build();
        fromBlockBtn= Button.builder(Component.literal("From Block"),     b -> doFromBlock())
                .bounds(bx + bw1 + bw2 + PAD * 2, by, bw3, bh).build();
        editTypeBtn = Button.builder(Component.literal("Edit All Type"),  b -> doEditType())
                .bounds(bx + bw1 + bw2 + bw3 + PAD * 3, by, bw4, bh).build();
        closeBtn    = Button.builder(Component.literal("Close"),          b -> onClose())
                .bounds(bx + bw1 + bw2 + bw3 + bw4 + PAD * 4, by, bw5, bh).build();

        addRenderableWidget(applyBtn);
        addRenderableWidget(resetBtn);
        addRenderableWidget(fromBlockBtn);
        addRenderableWidget(editTypeBtn);
        addRenderableWidget(closeBtn);

        rebuildRows();
    }

    private void selectTab(int tab) {
        activeTab    = tab;
        scrollOffset = 0;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        int panelW = Math.min(340, this.width - PAD * 2);
        int panelX = (this.width - panelW) / 2;
        List<FieldDef> fields = StatEditorPanel.getBlockFieldsForTab(activeTab);
        int startY = HEADER_H + TAB_H + PAD * 2;
        int curY   = startY;
        for (FieldDef fd : fields) {
            rows.add(FieldRow.create(fd, working, panelX + PAD, curY, panelW - PAD * 2,
                    () -> { unsaved++; updateApplyLabel(); }));
            curY += ROW_H;
        }
    }

    private void updateApplyLabel() {
        if (applyBtn != null)
            applyBtn.setMessage(Component.literal(unsaved > 0 ? "Apply ●" : "Apply"));
    }

    private void doApply() {
        CraftStatsNetwork.sendApplyBlockPosStats(posKey, working);
        unsaved = 0;
        updateApplyLabel();
    }

    private void doReset() {
        CraftStatsNetwork.sendResetBlockPos(posKey);
        working  = new BlockStats();
        unsaved  = 0;
        scrollOffset = 0;
        rebuildRows();
        updateApplyLabel();
    }

    private void doFromBlock() {
        Minecraft.getInstance().setScreen(
            new BlockSourceBrowserScreen(this, extracted -> {

                working.hardness       = extracted.hardness;
                working.blastResistance= extracted.blastResistance;
                working.slipperiness   = extracted.slipperiness;
                working.bounceFactor   = extracted.bounceFactor;
                working.noCollision    = extracted.noCollision;
                working.lightEmission  = extracted.lightEmission;
                working.climbable      = extracted.climbable;
                working.stepDamage     = extracted.stepDamage;
                working.speedModifier  = extracted.speedModifier;
                working.freezeOnStep   = extracted.freezeOnStep;
                unsaved++;
                updateApplyLabel();
                rebuildRows();
            })
        );
    }

    private void doEditType() {
        Minecraft.getInstance().setScreen(new CraftStatsScreen(block));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {

    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(340, this.width - PAD * 2);
        int panelX = (this.width - panelW) / 2;
        int panelH = this.height - HEADER_H - TAB_H - FOOTER_H - PAD * 3;
        int panelY = HEADER_H + TAB_H;
        int footerY = this.height - FOOTER_H - PAD;

        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF16213E);
        drawBorder(g, panelX, panelY, panelX + panelW, panelY + panelH, 0xFF3355AA);

        String typeShort = blockId.contains(":") ? blockId.split(":")[1] : blockId;
        String posStr    = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        g.drawCenteredString(font, "§b§l" + typeShort, this.width / 2, PAD + 2, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7Block at " + posStr + " §8(this block only)", this.width / 2, PAD + 14, 0xFFAAAAAA);
        g.drawCenteredString(font, "§8" + blockId, this.width / 2, PAD + 24, 0xFF555555);

        g.fill(0, footerY, this.width, footerY + FOOTER_H + PAD, 0xFF0F3460);
        drawBorder(g, 0, footerY, this.width, footerY + FOOTER_H + PAD, 0xFF3355AA);

        int tabW = panelW / TAB_NAMES.length;
        int tx   = panelX + activeTab * tabW;
        g.fill(tx, HEADER_H, tx + tabW - 2, HEADER_H + TAB_H, 0xFF2244AA);
        g.fill(tx, HEADER_H + TAB_H - 2, tx + tabW - 2, HEADER_H + TAB_H, 0xFF5588FF);

        int bodyY   = panelY + PAD;
        int bodyBot = panelY + panelH - PAD;

        for (FieldRow row : rows) {
            int screenRowY = row.y - scrollOffset;
            if (screenRowY + ROW_H < bodyY || screenRowY > bodyBot) continue;
            row.render(g, mouseX, mouseY + scrollOffset, delta, scrollOffset);
        }

        super.render(g, mouseX, mouseY, delta);

        if (unsaved > 0) {
            g.drawString(font, "§e" + unsaved + " unsaved change(s)", PAD, footerY + 6, 0xFFFFEEAA, false);
        }
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);
        g.fill(x1,     y2 - 1, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + 1, y2,     color);
        g.fill(x2 - 1, y1,     x2,     y2,     color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (FieldRow row : rows) {
            if (row.mouseClicked(mx, my + scrollOffset, btn)) return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int panelH     = this.height - HEADER_H - TAB_H - FOOTER_H - PAD * 3;
        int totalRowsH = rows.size() * ROW_H;
        int maxScroll  = Math.max(0, totalRowsH - panelH + PAD * 4);
        scrollOffset   = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * ROW_H));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (FieldRow row : rows) {
            if (row.keyPressed(key, scan, mods)) return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (FieldRow row : rows) {
            if (row.charTyped(c, mods)) return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
