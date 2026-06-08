package com.craftstats.common.gui;

import com.craftstats.common.gui.panel.EntityBrowserPanel;
import com.craftstats.common.gui.panel.StatEditorPanel;
import com.craftstats.common.gui.widget.FooterBar;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public class CraftStatsScreen extends Screen {

    private static final int PANEL_LEFT_W = 200;
    private static final int FOOTER_H     = 24;
    private static final int PADDING      = 4;

    private TargetType activeType     = TargetType.MOB;
    private String     targetId       = "";
    private Object     workingStats;
    private Object     vanillaStats;
    private int        unsavedChanges = 0;

    private UUID       mobInstanceUuid = null;

    private EntityBrowserPanel browserPanel;
    private StatEditorPanel    editorPanel;
    private FooterBar          footerBar;

    private final LivingEntity preselectedMob;
    private final Block        preselectedBlock;
    private final TargetType   preselectedType;
    private final String       preselectedId;

    public CraftStatsScreen(LivingEntity mob) {
        super(Component.literal("CraftStats"));
        this.preselectedMob   = mob;
        this.preselectedBlock = null;
        this.preselectedType  = TargetType.MOB;
        this.preselectedId    = null;
        this.activeType       = TargetType.MOB;
    }

    public CraftStatsScreen(Block block) {
        super(Component.literal("CraftStats"));
        this.preselectedMob   = null;
        this.preselectedBlock = block;
        this.preselectedType  = TargetType.BLOCK;
        this.preselectedId    = null;
        this.activeType       = TargetType.BLOCK;
    }

    public CraftStatsScreen(TargetType type, String id) {
        super(Component.literal("CraftStats"));
        this.preselectedMob   = null;
        this.preselectedBlock = null;
        this.preselectedType  = type;
        this.preselectedId    = id;
        this.activeType       = type;
    }

    public CraftStatsScreen() {
        super(Component.literal("CraftStats"));
        this.preselectedMob   = null;
        this.preselectedBlock = null;
        this.preselectedType  = null;
        this.preselectedId    = null;
    }

    @Override
    protected void init() {
        int leftW  = PANEL_LEFT_W;
        int rightX = leftW + PADDING * 2;
        int rightW = this.width - rightX - PADDING;
        int bodyH  = this.height - FOOTER_H - PADDING * 3;

        browserPanel = new EntityBrowserPanel(this, PADDING, PADDING, leftW, bodyH);
        editorPanel  = new StatEditorPanel(this, rightX, PADDING, rightW, bodyH);
        footerBar    = new FooterBar(this, PADDING, this.height - FOOTER_H - PADDING,
                this.width - PADDING * 2, FOOTER_H);

        browserPanel.init();
        editorPanel.init();
        footerBar.init();

        if (preselectedMob != null) {
            selectMob(preselectedMob);
        } else if (preselectedBlock != null) {
            selectBlock(preselectedBlock);
        } else if (preselectedType != null && preselectedId != null) {
            switch (preselectedType) {
                case MOB    -> selectMobById(preselectedId);
                case BLOCK  -> selectBlockById(preselectedId);
                case ITEM   -> selectItemById(preselectedId);
                default     -> {}
            }
        } else if (workingStats != null && !targetId.isEmpty()) {

            editorPanel.loadTarget(activeType, targetId, workingStats);
            editorPanel.setInstanceMode(activeType == TargetType.MOB && mobInstanceUuid != null);
            footerBar.update(unsavedChanges);
            browserPanel.setSelectedAndScroll(targetId);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {

    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {

        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);

        int bodyH = this.height - FOOTER_H - PADDING * 3;

        g.fill(PADDING,                 PADDING, PADDING + PANEL_LEFT_W,  PADDING + bodyH, 0xFF1A1A2E);
        g.fill(PANEL_LEFT_W + PADDING * 2, PADDING, this.width - PADDING,  PADDING + bodyH, 0xFF16213E);
        g.fill(PADDING, this.height - FOOTER_H - PADDING,
               this.width - PADDING, this.height - PADDING, 0xFF0F3460);

        drawBorder(g, PADDING, PADDING,
                PADDING + PANEL_LEFT_W, PADDING + bodyH, 0xFF3355AA);
        drawBorder(g, PANEL_LEFT_W + PADDING * 2, PADDING,
                this.width - PADDING, PADDING + bodyH, 0xFF3355AA);
        drawBorder(g, PADDING, this.height - FOOTER_H - PADDING,
                this.width - PADDING, this.height - PADDING, 0xFF3355AA);

        browserPanel.render(g, mouseX, mouseY, delta);
        editorPanel.render(g, mouseX, mouseY, delta);
        footerBar.render(g, mouseX, mouseY, delta);

        super.render(g, mouseX, mouseY, delta);
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);
        g.fill(x1,     y2 - 1, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + 1, y2,     color);
        g.fill(x2 - 1, y1,     x2,     y2,     color);
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {

        browserPanel.clearSearchFocus();
        editorPanel.clearAllFocus();
        if (browserPanel.mouseClicked(x, y, btn)) return true;
        if (editorPanel.mouseClicked(x, y, btn))  return true;
        if (footerBar.mouseClicked(x, y, btn))    return true;
        return super.mouseClicked(x, y, btn);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hScroll, double vScroll) {
        if (browserPanel.mouseScrolled(x, y, hScroll, vScroll)) return true;
        if (editorPanel.mouseScrolled(x, y, hScroll, vScroll))  return true;
        return super.mouseScrolled(x, y, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (browserPanel.keyPressed(key, scan, mods)) return true;
        if (editorPanel.keyPressed(key, scan, mods))  return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (browserPanel.charTyped(c, mods)) return true;
        if (editorPanel.charTyped(c, mods))  return true;
        return super.charTyped(c, mods);
    }

    public void selectMob(LivingEntity entity) {
        activeType      = TargetType.MOB;
        mobInstanceUuid = entity.getUUID();
        targetId        = net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString();

        MobStats existing = StatRegistry.getMobUuid(entity.getUUID());
        vanillaStats   = existing != null ? existing.copy() : new MobStats();
        workingStats   = ((MobStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        editorPanel.setInstanceMode(true);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void selectBlock(Block block) {
        activeType = TargetType.BLOCK;
        targetId   = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        BlockStats existing = StatRegistry.getBlock(ResourceLocation.parse(targetId));
        vanillaStats   = existing != null ? existing.copy() : new BlockStats();
        workingStats   = ((BlockStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void selectMobById(String typeId) {
        activeType      = TargetType.MOB;
        mobInstanceUuid = null;
        targetId        = typeId;
        MobStats existing = StatRegistry.getMob(ResourceLocation.parse(targetId));
        vanillaStats   = existing != null ? existing.copy() : new MobStats();
        workingStats   = ((MobStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        editorPanel.setInstanceMode(false);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void selectBlockById(String blockId) {
        activeType = TargetType.BLOCK;
        targetId   = blockId;
        BlockStats existing = StatRegistry.getBlock(ResourceLocation.parse(targetId));
        vanillaStats   = existing != null ? existing.copy() : new BlockStats();
        workingStats   = ((BlockStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void selectItemById(String itemId) {
        activeType = TargetType.ITEM;
        targetId   = itemId;
        ItemStats existing = StatRegistry.getItem(ResourceLocation.parse(targetId));
        vanillaStats   = existing != null ? existing.copy() : new ItemStats();
        workingStats   = ((ItemStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void selectPlayerById(UUID uuid, String displayName) {
        activeType = TargetType.PLAYER;
        targetId   = uuid.toString();
        PlayerStats existing = StatRegistry.getPlayer(uuid);
        vanillaStats   = existing != null ? existing.copy() : new PlayerStats();
        workingStats   = ((PlayerStats) vanillaStats).copy();
        unsavedChanges = 0;
        editorPanel.loadTarget(activeType, targetId, workingStats);
        footerBar.update(unsavedChanges);
        if (browserPanel != null) browserPanel.setSelectedAndScroll(targetId);
    }

    public void onApply() {
        if (targetId.isEmpty()) return;
        switch (activeType) {
            case MOB -> {
                if (mobInstanceUuid != null)
                    CraftStatsNetwork.sendApplyMobUuidStats(mobInstanceUuid, (MobStats) workingStats);
                else
                    CraftStatsNetwork.sendApplyMobStats(targetId, (MobStats) workingStats);
            }
            case BLOCK  -> CraftStatsNetwork.sendApplyBlockStats(targetId, (BlockStats) workingStats);
            case ITEM   -> CraftStatsNetwork.sendApplyItemStats(targetId, (ItemStats) workingStats);
            case PLAYER -> CraftStatsNetwork.sendApplyPlayerStats(
                    UUID.fromString(targetId), (PlayerStats) workingStats);
        }
        unsavedChanges = 0;
        footerBar.update(0);
    }

    public void onReset() {
        if (targetId.isEmpty()) return;
        switch (activeType) {
            case MOB -> {
                if (mobInstanceUuid != null) CraftStatsNetwork.sendResetMobUuid(mobInstanceUuid);
                else CraftStatsNetwork.sendReset("mob", targetId);
                workingStats = new MobStats(); vanillaStats = new MobStats();
            }
            case BLOCK  -> { CraftStatsNetwork.sendReset("block",  targetId); workingStats = new BlockStats();  vanillaStats = new BlockStats(); }
            case ITEM   -> { CraftStatsNetwork.sendReset("item",   targetId); workingStats = new ItemStats();   vanillaStats = new ItemStats(); }
            case PLAYER -> { CraftStatsNetwork.sendReset("player", targetId); workingStats = new PlayerStats(); vanillaStats = new PlayerStats(); }
        }
        editorPanel.loadTarget(activeType, targetId, workingStats);
        unsavedChanges = 0;
        footerBar.update(0);
    }

    public void onResetAll() {
        com.craftstats.common.network.CraftStatsNetwork.sendResetAll();

        workingStats   = null;
        vanillaStats   = null;
        targetId       = "";
        activeType     = TargetType.MOB;
        unsavedChanges = 0;
        footerBar.update(0);

        browserPanel.init();
        editorPanel.loadTarget(null, "", null);
    }

    public void onCopyProfile() {
        if (workingStats == null) return;
        String json = com.craftstats.common.util.JsonUtil.toJson(workingStats);
        net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(json);
    }

    public void onPasteProfile() {
        String clip = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) return;
        try {
            switch (activeType) {
                case MOB    -> workingStats = com.craftstats.common.util.JsonUtil.mobStatsFromJson(clip);
                case BLOCK  -> workingStats = com.craftstats.common.util.JsonUtil.blockStatsFromJson(clip);
                case ITEM   -> workingStats = com.craftstats.common.util.JsonUtil.itemStatsFromJson(clip);
                case PLAYER -> workingStats = com.craftstats.common.util.JsonUtil.playerStatsFromJson(clip);
            }
            editorPanel.loadTarget(activeType, targetId, workingStats);
            markDirty();
        } catch (Exception ignored) {}
    }

    public void markDirty() {
        unsavedChanges++;
        footerBar.update(unsavedChanges);
    }

    public TargetType getActiveType()   { return activeType; }
    public String     getTargetId()     { return targetId; }
    public Object     getWorkingStats() { return workingStats; }
    public Object     getVanillaStats() { return vanillaStats; }
    public int        getUnsaved()      { return unsavedChanges; }

    @Override
    public void tick() {
        if (footerBar != null) footerBar.tick();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
