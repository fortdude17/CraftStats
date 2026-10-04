package com.craftstats.common.gui;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.gui.editor.StatEditor;
import com.craftstats.common.gui.editor.TargetEntries;
import com.craftstats.common.gui.editor.TargetList;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.network.CraftStatsNetwork.Kind;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The CraftStats editor: type tabs along the top, a searchable list on the left, the stat
 * categories (as cards) on the right and the actions along the bottom.
 */
public class CraftStatsScreen extends BaseScreen implements StatEditor.Host {

    private static final int PAD = 6;
    private static final int TAB_H = 20;
    private static final int FOOTER_H = 20;
    private static final int HEADER_H = 22;

    /** Remembers where you were between openings. */
    private static TargetType lastType = TargetType.MOB;
    private static final Map<TargetType, String> lastKey = new EnumMap<>(TargetType.class);

    private TargetType type = lastType;
    private String key = "";
    private String label = "";
    private ItemStack icon = ItemStack.EMPTY;
    private UUID mobInstance;     // editing one mob instead of its type
    private String blockPosKey;   // editing one block position instead of its type
    private BlockPos blockPos;

    private Object working;
    private Object vanilla;
    private int unsaved;
    private int seenRevision = -1;
    private String flash = "";
    private int flashTicks;
    private int confirmResetAll;

    private final TargetList list = new TargetList(this::onListSelect);
    private final StatEditor editor = new StatEditor(this);
    private EditBox listSearch;
    private Button modifiedOnly;
    private boolean onlyModified;
    private final List<Button> tabs = new ArrayList<>();
    private Button presetsBtn, randomBtn, copyBtn, pasteBtn, fromBlockBtn, resetAllBtn, resetBtn, applyBtn;

    private Runnable pendingSelection;
    /** Stats handed in before the screen was initialised (e.g. from the wand's randomize). */
    private Object pendingLoad;

    public CraftStatsScreen() {
        super(Component.literal("CraftStats"));
        String k = lastKey.get(type);
        if (k != null) pendingSelection = () -> select(type, k);
    }

    /** Opens on one target, e.g. from a command or the wand. */
    public CraftStatsScreen(TargetType type, String key) {
        super(Component.literal("CraftStats"));
        this.type = type;
        pendingSelection = () -> select(type, key);
    }

    /** Edit one specific mob. */
    public CraftStatsScreen(LivingEntity mob) {
        super(Component.literal("CraftStats"));
        this.type = TargetType.MOB;
        pendingSelection = () -> selectMobInstance(mob);
    }

    /** Edit one block position. */
    public CraftStatsScreen(String posKey, Block block, BlockPos pos) {
        super(Component.literal("CraftStats"));
        this.type = TargetType.BLOCK;
        pendingSelection = () -> selectBlockPos(posKey, block, pos);
    }

    // ---- layout ------------------------------------------------------------------------------

    private int listW() { return Math.max(110, Math.min(180, width / 4)); }
    private int bodyTop() { return PAD + TAB_H + PAD; }
    private int bodyBottom() { return height - PAD - FOOTER_H - 14; }
    private int rightX() { return PAD + listW() + PAD; }

    @Override
    protected void init() {
        tabs.clear();
        TargetType[] types = visibleTypes();
        int tabW = (width - PAD * 2 - (types.length - 1) * 2) / types.length;
        for (int i = 0; i < types.length; i++) {
            TargetType t = types[i];
            Button b = addRenderableWidget(Button.builder(Component.literal(t.plural()), btn -> switchType(t))
                    .bounds(PAD + i * (tabW + 2), PAD, tabW, TAB_H)
                    .tooltip(Tooltip.create(Component.literal(StatCatalog.count(t) + " stats"))).build());
            b.active = t != type;
            tabs.add(b);
        }

        int lw = listW(), top = bodyTop();
        listSearch = addRenderableWidget(new EditBox(font, PAD, top, lw, 18, Component.literal("Search")));
        listSearch.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
        listSearch.setResponder(list::setQuery);
        modifiedOnly = addRenderableWidget(Button.builder(modifiedLabel(), b -> toggleModifiedOnly())
                .bounds(PAD, top + 21, lw, 18).build());
        list.setBounds(PAD, top + 44, lw, bodyBottom() - top - 44);

        int rx = rightX(), rw = width - rx - PAD;
        editor.setBounds(rx, top + HEADER_H, rw, bodyBottom() - top - HEADER_H);
        fromBlockBtn = addRenderableWidget(Button.builder(Component.literal("Copy From Block..."), b -> openBlockPicker())
                .bounds(rx + rw - 110, top, 110, 18).tooltip(Tooltip.create(Component.literal(
                        "Copy the real values of another block"))).build());

        // Footer
        int fy = height - PAD - FOOTER_H;
        int n = 7, gap = 3;
        int bw = Math.min(80, (width - PAD * 2 - gap * (n - 1)) / n);
        int lx = PAD;
        presetsBtn = addRenderableWidget(Button.builder(Component.literal("Presets"), b -> openPresets()).bounds(lx, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Load or save a set of stats"))).build());
        randomBtn = addRenderableWidget(Button.builder(Component.literal("Randomize"), b -> openRandomize()).bounds(lx += bw + gap, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Roll random stats around the vanilla values"))).build());
        copyBtn = addRenderableWidget(Button.builder(Component.literal("Copy"), b -> copyToClipboard()).bounds(lx += bw + gap, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Copy these stats to the clipboard as JSON"))).build());
        pasteBtn = addRenderableWidget(Button.builder(Component.literal("Paste"), b -> pasteFromClipboard()).bounds(lx + bw + gap, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Paste stats copied earlier"))).build());
        int rxF = width - PAD - bw;
        applyBtn = addRenderableWidget(Button.builder(Component.literal("Apply").withStyle(ChatFormatting.GREEN), b -> apply())
                .bounds(rxF, fy, bw, FOOTER_H).tooltip(Tooltip.create(Component.literal("Save the changes and use them in the world"))).build());
        resetBtn = addRenderableWidget(Button.builder(Component.literal("Reset"), b -> reset()).bounds(rxF -= bw + gap, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Remove all changes from this one"))).build());
        resetAllBtn = addRenderableWidget(Button.builder(Component.literal("Reset All"), b -> resetAll()).bounds(rxF - bw - gap, fy, bw, FOOTER_H)
                .tooltip(Tooltip.create(Component.literal("Remove every CraftStats change in this world"))).build());

        list.setEntries(TargetEntries.get(type));
        list.setModified(e -> StatTargets.get(type, e.key()) != null);
        list.setFilter(onlyModified ? e -> StatTargets.get(type, e.key()) != null : e -> true);
        seenRevision = StatRegistry.revision();

        if (pendingSelection != null) {
            Runnable r = pendingSelection;
            pendingSelection = null;
            r.run();
            if (pendingLoad != null) { Object p = pendingLoad; pendingLoad = null; loadStats(p); }
        } else if (working != null) {
            // Window resized: keep the current edit.
            loadEditor();
            list.select(key, true);
        } else if (type == TargetType.WORLD) {
            select(TargetType.WORLD, TargetType.WORLD_KEY);
        }
        updateButtons();
        if (!ClientHooks.mayEdit()) flash("View only: you need operator permissions to apply changes");
    }

    private TargetType[] visibleTypes() {
        List<TargetType> out = new ArrayList<>();
        for (TargetType t : TargetType.values()) if (CraftStatsConfig.get().isEnabled(t)) out.add(t);
        if (out.isEmpty()) out.add(TargetType.MOB);
        return out.toArray(TargetType[]::new);
    }

    private Component modifiedLabel() {
        int n = StatTargets.count(type);
        return Component.literal((onlyModified ? "Changed only: " : "Changed: ") + n)
                .withStyle(onlyModified ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
    }

    private void toggleModifiedOnly() {
        onlyModified = !onlyModified;
        list.setFilter(onlyModified ? e -> StatTargets.get(type, e.key()) != null : e -> true);
        modifiedOnly.setMessage(modifiedLabel());
    }

    private void updateButtons() {
        boolean has = working != null;
        boolean canEdit = ClientHooks.mayEdit();
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).active = visibleTypes()[i] != type;
        presetsBtn.active = has && CraftStatsConfig.get().enablePresets;
        randomBtn.active = has && canEdit && CraftStatsConfig.get().enableRandomize && RandomizeManager.supports(type)
                && type != TargetType.PLAYER;
        copyBtn.active = has;
        pasteBtn.active = has;
        applyBtn.active = has && canEdit;
        resetBtn.active = has && canEdit;
        resetAllBtn.active = canEdit;
        fromBlockBtn.visible = type == TargetType.BLOCK && has;
        modifiedOnly.setMessage(modifiedLabel());
    }

    // ---- selection ---------------------------------------------------------------------------

    private void switchType(TargetType t) {
        if (t == type) return;
        type = t;
        lastType = t;
        working = null;
        key = "";
        mobInstance = null;
        blockPosKey = null;
        editor.clear();
        listSearch.setValue("");
        list.setEntries(TargetEntries.get(t));
        list.setModified(e -> StatTargets.get(t, e.key()) != null);
        list.setFilter(onlyModified ? e -> StatTargets.get(t, e.key()) != null : e -> true);
        list.select("", false);
        String remembered = lastKey.get(t);
        if (t == TargetType.WORLD) select(t, TargetType.WORLD_KEY);
        else if (remembered != null) select(t, remembered);
        updateButtons();
    }

    private void onListSelect(TargetList.Entry e) {
        if (unsaved > 0 && working != null) flash("Discarded " + unsaved + " unapplied change(s) to " + label);
        select(type, e.key());
    }

    /** Selects a whole type (mob type, block type, item, player...) by key. */
    public void select(TargetType t, String k) {
        TargetList.Entry entry = null;
        for (TargetList.Entry e : TargetEntries.get(t)) if (e.key().equals(k)) { entry = e; break; }
        if (entry == null) return;
        if (t != type) { type = t; list.setEntries(TargetEntries.get(t)); }
        mobInstance = null;
        blockPosKey = null;
        key = k;
        label = entry.label();
        icon = entry.icon();
        Object stored = StatTargets.get(t, k);
        working = stored != null ? StatSchema.copyAny(stored) : StatSchema.empty(t);
        if (t == TargetType.PLAYER && stored == null) working = new PlayerStats();
        vanilla = StatTargets.vanilla(registryAccess(), t, k);
        lastKey.put(t, k);
        lastType = t;
        unsaved = 0;
        loadEditor();
        list.select(k, true);
        updateButtons();
    }

    public void selectMobInstance(LivingEntity entity) {
        EntityType<?> et = entity.getType();
        String typeKey = EntityType.getKey(et).toString();
        select(TargetType.MOB, typeKey);
        mobInstance = entity.getUUID();
        label = entity.getName().getString() + " (just this one)";
        MobStats existing = StatRegistry.getMobInstance(entity.getUUID());
        if (existing == null) existing = StatRegistry.getMob(EntityType.getKey(et));
        working = existing != null ? existing.copy() : new MobStats();
        loadEditor();
    }

    public void selectBlockPos(String posKey, Block block, BlockPos pos) {
        select(TargetType.BLOCK, BuiltInRegistries.BLOCK.getKey(block).toString());
        blockPosKey = posKey;
        blockPos = pos;
        label = block.getName().getString() + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        BlockStats existing = StatRegistry.getBlockAt(posKey);
        working = existing != null ? existing.copy() : new BlockStats();
        loadEditor();
    }

    private void loadEditor() {
        List<StatCatalog.Category> cats = blockPosKey != null ? StatCatalog.blockPositionCategories() : StatCatalog.categories(type);
        editor.load(type, working, vanilla, cats, supportPredicate());
    }

    private Predicate<StatDef> supportPredicate() {
        if (type != TargetType.MOB) return d -> true;
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return d -> true;
        EntityType<?> et = Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id);
        return d -> VanillaStats.mobSupports(et, d.field());
    }

    private static net.minecraft.core.RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : null;
    }

    // ---- StatEditor.Host ---------------------------------------------------------------------

    @Override
    public <T extends AbstractWidget> T addWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public void removeWidget(AbstractWidget widget) {
        super.removeWidget(widget);
    }

    @Override
    public void onEdited() {
        unsaved++;
    }

    // ---- actions -----------------------------------------------------------------------------

    private Kind kind() {
        if (mobInstance != null) return Kind.MOB_INSTANCE;
        if (blockPosKey != null) return Kind.BLOCK_POS;
        return Kind.of(type);
    }

    private String requestKey() {
        if (mobInstance != null) return mobInstance.toString();
        if (blockPosKey != null) return blockPosKey;
        return key;
    }

    private void apply() {
        if (working == null) return;
        StatAccess.sanitize(type, working);
        CraftStatsNetwork.sendApply(kind(), requestKey(), working);
        unsaved = 0;
        flash("Applied " + label);
    }

    private void reset() {
        if (working == null) return;
        CraftStatsNetwork.sendReset(kind(), requestKey());
        working = type == TargetType.PLAYER ? new PlayerStats() : StatSchema.empty(type);
        editor.replaceStats(working);
        unsaved = 0;
        flash("Reset " + label + " to vanilla");
    }

    private void resetAll() {
        if (confirmResetAll == 0) {
            confirmResetAll = 80;
            resetAllBtn.setMessage(Component.literal("Sure?").withStyle(ChatFormatting.RED));
            flash("Click again to remove ALL CraftStats changes in this world");
            return;
        }
        confirmResetAll = 0;
        resetAllBtn.setMessage(Component.literal("Reset All"));
        CraftStatsNetwork.sendResetAll();
        if (working != null) {
            working = type == TargetType.PLAYER ? new PlayerStats() : StatSchema.empty(type);
            editor.replaceStats(working);
        }
        unsaved = 0;
        flash("Removed all CraftStats changes");
    }

    private void copyToClipboard() {
        if (working == null) return;
        JsonObject o = new JsonObject();
        o.addProperty("target_type", type.id());
        o.add("stats", StatSchema.GSON_COMPACT.toJsonTree(working));
        Minecraft.getInstance().keyboardHandler.setClipboard(StatSchema.GSON.toJson(o));
        flash("Copied to the clipboard");
    }

    /** Accepts a bare stats object or one wrapped with "target_type" (from Copy). */
    private void pasteFromClipboard() {
        if (working == null) return;
        try {
            JsonObject o = JsonParser.parseString(Minecraft.getInstance().keyboardHandler.getClipboard()).getAsJsonObject();
            if (o.has("target_type") && o.has("stats")) {
                TargetType pasted = TargetType.valueOf(o.get("target_type").getAsString().toUpperCase());
                if (pasted != type) {
                    flash("The clipboard holds " + pasted.displayName() + " stats, not " + type.displayName());
                    return;
                }
                o = o.getAsJsonObject("stats");
            }
            loadStats(StatSchema.parse(type, o));
            flash("Pasted. Press Apply to use it");
        } catch (Exception e) {
            flash("The clipboard doesn't contain CraftStats stats");
        }
    }

    /** Replaces the values being edited (presets, paste, randomize). */
    public void loadStats(Object stats) {
        if (stats == null) return;
        if (working == null) { pendingLoad = stats; return; }
        working = stats;
        editor.replaceStats(stats);
        unsaved++;
    }

    private void openPresets() {
        if (working == null) return;
        Minecraft.getInstance().setScreen(new PresetScreen(this, type, working, this::loadStats));
    }

    private void openRandomize() {
        if (working == null) return;
        Minecraft.getInstance().setScreen(new RandomizeScreen(this, type, label, vanilla, this::loadStats));
    }

    private void openBlockPicker() {
        if (!(working instanceof BlockStats target)) return;
        Minecraft.getInstance().setScreen(new BlockPickerScreen(this, picked -> {
            BlockStats from = VanillaStats.block(picked);
            target.hardness = from.hardness;
            target.blastResistance = from.blastResistance;
            target.slipperiness = from.slipperiness;
            target.jumpFactor = from.jumpFactor;
            target.speedFactor = from.speedFactor;
            target.lightEmission = from.lightEmission;
            target.noCollision = from.noCollision;
            target.climbable = from.climbable;
            loadStats(target);
            flash("Copied the values of " + picked.getName().getString());
        }));
    }

    private void flash(String message) {
        flash = message;
        flashTicks = 80;
    }

    // ---- input & rendering -------------------------------------------------------------------

    @Override
    protected boolean onMouseClicked(double x, double y, int button) {
        if (button == 1 && editor.mouseClickedRight(x, y)) return true;
        return button == 0 && list.mouseClicked(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hScroll, double vScroll) {
        return list.mouseScrolled(x, y, vScroll) || editor.mouseScrolled(x, y, vScroll)
                || super.mouseScrolled(x, y, hScroll, vScroll);
    }

    @Override
    public void tick() {
        if (flashTicks > 0) flashTicks--;
        if (confirmResetAll > 0 && --confirmResetAll == 0) resetAllBtn.setMessage(Component.literal("Reset All"));
        // New data from the server (or another editor applied changes): refresh markers and lists.
        if (StatRegistry.revision() != seenRevision) {
            seenRevision = StatRegistry.revision();
            if (type == TargetType.PLAYER) list.setEntries(TargetEntries.get(type));
            list.refilter();
            modifiedOnly.setMessage(modifiedLabel());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        list.render(g, mx, my);

        int rx = rightX(), rw = width - rx - PAD, top = bodyTop();
        if (working != null) {
            Ui.item(g, icon.isEmpty() ? new ItemStack(Items.BOOK) : icon, rx, top + 1);
            String sub = unsaved > 0 ? "  " + unsaved + " unapplied" : "";
            int maxW = (fromBlockBtn.visible ? fromBlockBtn.getX() : rx + rw) - rx - 24;
            String name = Ui.fit(label, Math.max(20, maxW - font.width(sub)));
            Ui.text(g, name, rx + 20, top + 5, Ui.WHITE);
            if (!sub.isEmpty()) Ui.text(g, sub, rx + 20 + font.width(name), top + 5, Ui.GOLD);
        }
        Ui.separator(g, rx, top + HEADER_H - 3, rw);
        editor.render(g, mx, my);

        // Status line above the footer
        String status = flashTicks > 0 ? flash : "";
        if (!status.isEmpty()) Ui.centered(g, status, width / 2, height - PAD - FOOTER_H - 10, Ui.AQUA);
    }

    @Override
    public void removed() {
        if (working != null && unsaved == 0) lastKey.put(type, key);
        super.removed();
    }

    public TargetType getActiveType() { return type; }

    /** For the client smoke test. */
    public StatEditor editor() { return editor; }

    /** For the client smoke test: switch type and select the first entry. */
    public boolean showFirst(TargetType t) {
        switchType(t);
        if (working == null && !TargetEntries.get(t).isEmpty()) select(t, TargetEntries.get(t).get(0).key());
        return working != null;
    }
}
