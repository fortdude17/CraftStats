package com.craftstats.common.gui;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.gui.panel.EntityBrowserPanel;
import com.craftstats.common.gui.panel.StatEditorPanel;
import com.craftstats.common.gui.widget.FooterBar;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.network.CraftStatsNetwork.Kind;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

/** The main editor: browser on the left, fields on the right, footer at the bottom. */
public class CraftStatsScreen extends BaseScreen {

    private static final int PANEL_LEFT_W = 200;
    private static final int FOOTER_H     = 24;
    private static final int PADDING      = 4;

    private TargetType activeType = TargetType.MOB;
    private String     targetId = "";
    private String     targetLabel = "";
    private UUID       mobInstance;
    private Object     workingStats;
    private Object     vanillaStats;
    private int        unsavedChanges;
    private int        seenRevision = -1;

    private EntityBrowserPanel browserPanel;
    private StatEditorPanel    editorPanel;
    private FooterBar          footerBar;

    /** Selection to restore when the screen is (re)initialised. */
    private Runnable pendingSelection;

    public CraftStatsScreen() {
        super(Component.literal("CraftStats"));
    }

    public CraftStatsScreen(LivingEntity mob) {
        this();
        pendingSelection = () -> selectMobInstance(mob);
    }

    public CraftStatsScreen(Block block) {
        this();
        pendingSelection = () -> selectBlockById(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    public CraftStatsScreen(TargetType type, String id) {
        this();
        pendingSelection = switch (type) {
            case MOB   -> () -> selectMobById(id);
            case BLOCK -> () -> selectBlockById(id);
            case ITEM  -> () -> selectItemById(id);
            case PLAYER -> null;
        };
    }

    @Override
    protected void init() {
        int rightX = PANEL_LEFT_W + PADDING * 2;
        int bodyH = this.height - FOOTER_H - PADDING * 3;
        browserPanel = new EntityBrowserPanel(this, PADDING, PADDING, PANEL_LEFT_W, bodyH);
        editorPanel  = new StatEditorPanel(this, rightX, PADDING, this.width - rightX - PADDING, bodyH);
        footerBar    = new FooterBar(this, PADDING, this.height - FOOTER_H - PADDING, this.width - PADDING * 2, FOOTER_H);
        browserPanel.init();
        editorPanel.init();
        footerBar.init();
        seenRevision = StatRegistry.revision();

        if (pendingSelection != null) {
            Runnable r = pendingSelection;
            pendingSelection = null;
            r.run();
        } else if (workingStats != null) {
            // Window resized: keep the current edit.
            editorPanel.loadTarget(activeType, targetLabel, workingStats, vanillaStats);
            editorPanel.setInstanceMode(mobInstance != null);
            browserPanel.setSelectedAndScroll(targetId);
        }
        footerBar.update(unsavedChanges);
        if (!ClientHooks.mayEdit()) footerBar.flash("View only: you need operator permissions to apply changes");
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Opaque custom background drawn in render().
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);
        int bodyH = this.height - FOOTER_H - PADDING * 3;
        int rightX = PANEL_LEFT_W + PADDING * 2;
        g.fill(PADDING, PADDING, PADDING + PANEL_LEFT_W, PADDING + bodyH, 0xFF1A1A2E);
        g.fill(rightX, PADDING, this.width - PADDING, PADDING + bodyH, 0xFF16213E);
        g.fill(PADDING, this.height - FOOTER_H - PADDING, this.width - PADDING, this.height - PADDING, 0xFF0F3460);
        border(g, PADDING, PADDING, PADDING + PANEL_LEFT_W, PADDING + bodyH);
        border(g, rightX, PADDING, this.width - PADDING, PADDING + bodyH);
        border(g, PADDING, this.height - FOOTER_H - PADDING, this.width - PADDING, this.height - PADDING);

        browserPanel.render(g, mouseX, mouseY, delta);
        editorPanel.render(g, mouseX, mouseY, delta);
        footerBar.render(g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);
    }

    static void border(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int c = 0xFF3355AA;
        g.fill(x1, y1, x2, y1 + 1, c);
        g.fill(x1, y2 - 1, x2, y2, c);
        g.fill(x1, y1, x1 + 1, y2, c);
        g.fill(x2 - 1, y1, x2, y2, c);
    }

    @Override
    protected boolean onMouseClicked(double x, double y, int btn) {
        browserPanel.clearSearchFocus();
        editorPanel.clearAllFocus();
        return browserPanel.mouseClicked(x, y, btn) || editorPanel.mouseClicked(x, y, btn) || footerBar.mouseClicked(x, y, btn);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hScroll, double vScroll) {
        return browserPanel.mouseScrolled(x, y, vScroll) || editorPanel.mouseScrolled(x, y, vScroll)
                || super.mouseScrolled(x, y, hScroll, vScroll);
    }

    @Override
    protected boolean onKeyPressed(int key, int scan, int mods) {
        return browserPanel.keyPressed(key, scan, mods) || editorPanel.keyPressed(key, scan, mods);
    }

    @Override
    protected boolean onCharTyped(char c, int mods) {
        return browserPanel.charTyped(c, mods) || editorPanel.charTyped(c, mods);
    }

    @Override
    public void tick() {
        footerBar.tick();
        // The server sent new data (or another editor applied changes): refresh the markers.
        if (StatRegistry.revision() != seenRevision) {
            seenRevision = StatRegistry.revision();
            browserPanel.refresh();
        }
    }

    // ---- selection ---------------------------------------------------------------------------

    private void show(TargetType type, String id, String label, Object stats, Object vanilla, UUID instance) {
        activeType = type;
        targetId = id;
        targetLabel = label;
        mobInstance = instance;
        workingStats = stats;
        vanillaStats = vanilla;
        unsavedChanges = 0;
        editorPanel.loadTarget(type, label, stats, vanilla);
        editorPanel.setInstanceMode(instance != null);
        footerBar.update(0);
        browserPanel.setSelectedAndScroll(id);
    }

    public void selectMobInstance(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        MobStats existing = StatRegistry.getMobInstance(entity.getUUID());
        if (existing == null) existing = StatRegistry.getMob(EntityType.getKey(type));
        show(TargetType.MOB, EntityType.getKey(type).toString(), entity.getName().getString(),
                existing != null ? existing.copy() : new MobStats(), VanillaStats.mob(type), entity.getUUID());
    }

    public void selectMobById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) return;
        MobStats existing = StatRegistry.getMob(rl);
        show(TargetType.MOB, id, id, existing != null ? existing.copy() : new MobStats(),
                VanillaStats.mob(Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, rl)), null);
    }

    public void selectBlockById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.BLOCK.containsKey(rl)) return;
        BlockStats existing = StatRegistry.getBlock(rl);
        show(TargetType.BLOCK, id, id, existing != null ? existing.copy() : new BlockStats(),
                VanillaStats.block(Compat.registryValue(BuiltInRegistries.BLOCK, rl)), null);
    }

    public void selectItemById(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return;
        ItemStats existing = StatRegistry.getItem(rl);
        show(TargetType.ITEM, id, id, existing != null ? existing.copy() : new ItemStats(),
                VanillaStats.item(Compat.registryValue(BuiltInRegistries.ITEM, rl)), null);
    }

    public void selectPlayerById(UUID uuid, String name) {
        PlayerStats existing = StatRegistry.getPlayer(uuid);
        show(TargetType.PLAYER, uuid.toString(), name, existing != null ? existing.copy() : new PlayerStats(),
                VanillaStats.player(), null);
    }

    /** Replaces the values being edited (used by presets and paste). */
    public void loadStats(Object stats) {
        if (stats == null || targetId.isEmpty()) return;
        workingStats = stats;
        editorPanel.loadTarget(activeType, targetLabel, stats, vanillaStats);
        markDirty();
    }

    // ---- actions -----------------------------------------------------------------------------

    private Kind kind() {
        return switch (activeType) {
            case MOB    -> mobInstance != null ? Kind.MOB_INSTANCE : Kind.MOB;
            case BLOCK  -> Kind.BLOCK;
            case ITEM   -> Kind.ITEM;
            case PLAYER -> Kind.PLAYER;
        };
    }

    private String key() {
        return mobInstance != null ? mobInstance.toString() : targetId;
    }

    public void onApply() {
        if (workingStats == null) return;
        CraftStatsNetwork.sendApply(kind(), key(), workingStats);
        unsavedChanges = 0;
        footerBar.update(0);
        footerBar.flash("Applied " + targetLabel);
    }

    public void onReset() {
        if (workingStats == null) return;
        CraftStatsNetwork.sendReset(kind(), key());
        workingStats = switch (activeType) {
            case MOB -> new MobStats();
            case BLOCK -> new BlockStats();
            case ITEM -> new ItemStats();
            case PLAYER -> new PlayerStats();
        };
        editorPanel.loadTarget(activeType, targetLabel, workingStats, vanillaStats);
        unsavedChanges = 0;
        footerBar.update(0);
        footerBar.flash("Reset " + targetLabel + " to vanilla");
    }

    public void onResetAll() {
        CraftStatsNetwork.sendResetAll();
        workingStats = null;
        targetId = "";
        targetLabel = "";
        mobInstance = null;
        unsavedChanges = 0;
        editorPanel.loadTarget(null, "", null, null);
        footerBar.update(0);
        footerBar.flash("Removed all CraftStats changes");
    }

    public void onCopyProfile() {
        if (workingStats == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("target_type", activeType.name().toLowerCase());
        o.add("stats", StatSchema.GSON_COMPACT.toJsonTree(workingStats));
        Minecraft.getInstance().keyboardHandler.setClipboard(StatSchema.GSON.toJson(o));
        footerBar.flash("Copied to clipboard");
    }

    /** Accepts either a bare stats object or one wrapped with "target_type" (from Copy). */
    public void onPasteProfile() {
        if (workingStats == null) return;
        try {
            JsonObject o = JsonParser.parseString(Minecraft.getInstance().keyboardHandler.getClipboard()).getAsJsonObject();
            if (o.has("target_type") && o.has("stats")) {
                TargetType pasted = TargetType.valueOf(o.get("target_type").getAsString().toUpperCase());
                if (pasted != activeType) {
                    footerBar.flash("Clipboard holds " + pasted.displayName() + " stats, not " + activeType.displayName());
                    return;
                }
                o = o.getAsJsonObject("stats");
            }
            loadStats(StatSchema.parse(activeType, o));
            footerBar.flash("Pasted - press Apply to use it");
        } catch (Exception e) {
            footerBar.flash("Clipboard doesn't contain CraftStats JSON");
        }
    }

    public void markDirty() {
        unsavedChanges++;
        footerBar.update(unsavedChanges);
    }

    public TargetType getActiveType() { return activeType; }
}
