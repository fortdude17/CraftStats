package com.craftstats.common.gui.panel;

import com.craftstats.common.gui.BlockSourceBrowserScreen;
import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.widget.*;
import com.craftstats.common.stats.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class StatEditorPanel {

    private static final int HEADER_H  = 22;
    private static final int TAB_H     = 18;
    private static final int ROW_H     = FieldRow.H;
    private static final int PAD       = 4;

    private final CraftStatsScreen parent;
    private final int x, y, w, h;

    private TargetType  activeType;
    private String      targetId = "";
    private Object      stats;

    private int         activeTab    = 0;
    private int         scrollOffset = 0;
    private boolean     instanceMode = false;

    private final List<FieldRow> rows    = new ArrayList<>();
    private final List<String>   tabNames = new ArrayList<>();

    private Button copyBtn, pasteBtn, presetBtn, fromBlockBtn;

    public StatEditorPanel(CraftStatsScreen parent, int x, int y, int w, int h) {
        this.parent = parent;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void init() {
        int bw = 40, bh = 14;

        int bx4 = x + w - (bw + 10) - PAD;
        int bx5 = bx4 - 66 - PAD;
        int bx2 = bx4 - bw - PAD;
        int bx1 = bx2 - bw - PAD;

        bx1 = x + w - bw * 2 - (bw + 10) - 66 - PAD * 3;
        bx2 = bx1 + bw + PAD;
        int bx3 = bx2 + bw + PAD;
        bx4 = bx3 + 66 + PAD;
        int by = y + (HEADER_H - bh) / 2;
        copyBtn     = Button.builder(Component.literal("Copy"),      b -> parent.onCopyProfile())
                .bounds(bx1, by, bw, bh).build();
        pasteBtn    = Button.builder(Component.literal("Paste"),     b -> parent.onPasteProfile())
                .bounds(bx2, by, bw, bh).build();
        fromBlockBtn= Button.builder(Component.literal("From Block"),b -> openFromBlock())
                .bounds(bx3, by, 66, bh).build();
        presetBtn   = Button.builder(Component.literal("Presets"),   b -> openPresets())
                .bounds(bx4, by, bw + 10, bh).build();
        fromBlockBtn.active = false;
    }

    public void setInstanceMode(boolean instance) { this.instanceMode = instance; }

    public void loadTarget(TargetType type, String id, Object statsObj) {
        this.activeType = type;
        this.targetId   = id;
        this.stats      = statsObj;
        this.activeTab  = 0;
        this.scrollOffset = 0;
        if (fromBlockBtn != null) fromBlockBtn.active = (type == TargetType.BLOCK);
        rebuildTabs();
        rebuildRows();
    }

    private void rebuildTabs() {
        tabNames.clear();
        if (activeType == null) return;
        switch (activeType) {
            case MOB    -> { tabNames.add("Combat"); tabNames.add("Movement"); tabNames.add("Behavior"); }
            case BLOCK  -> { tabNames.add("Physical"); tabNames.add("Step FX"); }
            case ITEM   -> { tabNames.add("Combat"); tabNames.add("Properties"); tabNames.add("Food"); tabNames.add("Flags"); }
            case PLAYER -> { tabNames.add("Combat"); tabNames.add("Movement"); tabNames.add("Defense");
                             tabNames.add("Effects"); tabNames.add("Flags"); }
        }
    }

    private void rebuildRows() {
        rows.clear();
        if (stats == null || activeType == null) return;
        List<FieldDef> fields = getFieldsForTab();
        int bodyY = y + HEADER_H + TAB_H + PAD;
        int curY  = bodyY;
        for (FieldDef fd : fields) {
            FieldRow row = FieldRow.create(fd, stats, x + PAD, curY, w - PAD * 2,
                    () -> parent.markDirty());
            rows.add(row);
            curY += ROW_H + 2;
        }
    }

    private List<FieldDef> getFieldsForTab() {
        if (activeType == null) return List.of();
        return switch (activeType) {
            case MOB    -> getMobFields(activeTab);
            case BLOCK  -> getBlockFields(activeTab);
            case ITEM   -> getItemFields(activeTab);
            case PLAYER -> getPlayerFields(activeTab);
        };
    }

    private List<FieldDef> getMobFields(int tab) {
        return switch (tab) {
            case 0 -> List.of(
                    FieldDef.num("max_health",         "Max Health"),
                    FieldDef.num("attackDamage",        "Attack Damage"),
                    FieldDef.num("armor",               "Armor"),
                    FieldDef.num("knockbackResist",     "Knockback Resist"),
                    FieldDef.toggle("invincible",       "Invincible"),
                    FieldDef.toggle("immuneFire",       "Immune: Fire"),
                    FieldDef.toggle("immuneFall",       "Immune: Fall"),
                    FieldDef.toggle("immuneDrown",      "Immune: Drown"),
                    FieldDef.toggle("immuneExplosion",  "Immune: Explosion"),
                    FieldDef.toggle("immunePoison",     "Immune: Poison"),
                    FieldDef.toggle("immuneMagic",      "Immune: Magic"),
                    FieldDef.toggle("silent",           "Silent"),
                    FieldDef.toggle("glowing",          "Glowing")
            );
            case 1 -> List.of(
                    FieldDef.num("moveSpeed",   "Move Speed"),
                    FieldDef.num("followRange", "Follow Range"),
                    FieldDef.num("sizeScale",   "Size Scale")
            );
            case 2 -> List.of(
                    FieldDef.num("xpReward",         "XP Reward  (0 = vanilla)"),
                    FieldDef.toggle("burnsDaylight", "Burns in Daylight"),
                    FieldDef.toggle("canDespawn",    "Can Despawn")
            );
            default -> List.of();
        };
    }

    private List<FieldDef> getBlockFields(int tab) {
        return getBlockFieldsForTab(tab);
    }

    public static List<FieldDef> getBlockFieldsForTab(int tab) {
        return switch (tab) {

            case 0 -> List.of(
                    FieldDef.num("hardness",       "Hardness  (0=instant, -1=unbreakable)"),
                    FieldDef.num("blastResistance","Blast Resistance"),
                    FieldDef.num("slipperiness",   "Slipperiness  (0.6=normal, 0.98=ice)"),
                    FieldDef.num("lightEmission",  "Light Emission  (0-15)"),
                    FieldDef.num("dropXp",         "XP Dropped  (-1=vanilla)"),
                    FieldDef.toggle("noCollision",          "No Collision  (walk through)"),
                    FieldDef.toggle("canFall",              "Can Fall  (like sand/gravel)"),
                    FieldDef.toggle("climbable",            "Climbable  (like ladder)"),
                    FieldDef.toggle("requiresCorrectTool",  "Requires Correct Tool"),
                    FieldDef.pill("pushReaction",  "Piston Reaction",
                            new String[]{"normal","destroy","block","ignore","push_only"})
            );

            case 1 -> List.of(
                    FieldDef.num("stepDamage",           "Step Damage / 10 ticks"),
                    FieldDef.num("speedModifier",        "Speed Multiplier  (1.0=normal, 2.0=2x)"),
                    FieldDef.num("onStepPotionLevel",    "Potion Level  (1=I, 2=II)"),
                    FieldDef.num("onStepPotionDuration", "Potion Duration  (ticks, 20=1s)"),
                    FieldDef.toggle("levitate",    "Levitate on Step"),
                    FieldDef.toggle("glowOnStep",  "Glow on Step"),
                    FieldDef.toggle("freezeOnStep","Freeze on Step  (powder snow)"),
                    FieldDef.text("onStepPotion",  "Potion ID  (e.g. minecraft:speed)")
            );
            default -> List.of();
        };
    }

    private List<FieldDef> getItemFields(int tab) {
        return switch (tab) {
            case 0 -> List.of(
                    FieldDef.num("attackDamage",    "Attack Damage"),
                    FieldDef.num("attackSpeed",     "Attack Speed"),
                    FieldDef.num("enchantability",  "Enchantability"),
                    FieldDef.num("sweepMultiplier", "Sweep Multiplier")
            );
            case 1 -> List.of(
                    FieldDef.num("maxDurability",   "Max Durability"),
                    FieldDef.num("stackSize",       "Stack Size"),
                    FieldDef.num("miningSpeed",     "Mining Speed"),
                    FieldDef.text("repairMaterial", "Repair Material"),
                    FieldDef.toggle("fireproof",    "Fireproof"),
                    FieldDef.toggle("unbreakable",  "Unbreakable"),
                    FieldDef.toggle("consumedOnUse","Consumed on Use"),
                    FieldDef.toggle("itemGlow",     "Item Glow")
            );
            case 2 -> List.of(
                    FieldDef.num("nutrition",       "Nutrition"),
                    FieldDef.num("saturation",      "Saturation"),
                    FieldDef.num("eatDuration",     "Eat Duration"),
                    FieldDef.text("onEatEffect",    "On Eat Effect"),
                    FieldDef.toggle("isFood",       "Is Food"),
                    FieldDef.toggle("alwaysEdible", "Always Edible"),
                    FieldDef.toggle("fastEat",      "Fast Eat"),
                    FieldDef.toggle("isMeat",       "Is Meat")
            );
            case 3 -> List.of(
                    FieldDef.toggle("isWeapon",   "Weapon"),
                    FieldDef.toggle("isTool",     "Tool"),
                    FieldDef.toggle("isArmor",    "Armor"),
                    FieldDef.toggle("throwable",  "Throwable"),
                    FieldDef.toggle("boomerang",  "Boomerang"),
                    FieldDef.toggle("projectile", "Projectile")
            );
            default -> List.of();
        };
    }

    private List<FieldDef> getPlayerFields(int tab) {
        return switch (tab) {

            case 0 -> List.of(
                    FieldDef.num("maxHealth",          "Max Health"),
                    FieldDef.num("baseDamage",         "Base Damage"),
                    FieldDef.num("attackSpeed",        "Attack Speed"),
                    FieldDef.num("reachDistance",      "Reach Distance"),
                    FieldDef.num("critMultiplier",     "Crit Multiplier"),
                    FieldDef.num("attackKnockback",    "Attack Knockback"),
                    FieldDef.num("sweepingDamageRatio","Sweep Damage Ratio"),
                    FieldDef.toggle("oneHitKill",      "One Hit Kill")
            );

            case 1 -> List.of(
                    FieldDef.num("walkSpeed",               "Walk Speed  (also affects sprint)"),
                    FieldDef.num("flySpeed",                "Fly Speed"),
                    FieldDef.num("jumpForce",               "Jump Force"),
                    FieldDef.num("stepHeight",              "Step Height"),
                    FieldDef.num("gravity",                 "Gravity  (0.08=normal)"),
                    FieldDef.num("sneakingSpeed",           "Sneaking Speed"),
                    FieldDef.num("miningEfficiency",        "Mining Efficiency"),
                    FieldDef.num("movementEfficiency",      "Movement Efficiency"),
                    FieldDef.num("submergedMiningSpeed",    "Underwater Mining Speed"),
                    FieldDef.num("waterMovementEfficiency", "Water Movement Eff."),
                    FieldDef.toggle("noFallDamage",         "No Fall Damage"),
                    FieldDef.toggle("noClip",               "No Clip"),
                    FieldDef.toggle("infiniteSprint",       "Infinite Sprint")
            );

            case 2 -> List.of(
                    FieldDef.num("armor",                "Armor Points"),
                    FieldDef.num("armorToughness",       "Armor Toughness"),
                    FieldDef.num("knockbackResistance",  "Knockback Resistance  (0-1)"),
                    FieldDef.num("maxAbsorption",        "Max Absorption Hearts"),
                    FieldDef.num("luck",                 "Luck"),
                    FieldDef.num("burningTime",          "Burning Time  (8=normal)"),
                    FieldDef.num("fallDamageMultiplier", "Fall Damage Multiplier"),
                    FieldDef.num("safeFallDistance",     "Safe Fall Distance  (3=normal)"),
                    FieldDef.num("explosionKbResistance","Explosion KB Resistance")
            );

            case 3 -> List.of(
                    FieldDef.toggle("fxNightVision",  "Night Vision"),
                    FieldDef.toggle("fxWaterBreath",  "Water Breathing"),
                    FieldDef.toggle("fxFireResist",   "Fire Resistance"),
                    FieldDef.toggle("fxRegen",        "Regeneration"),
                    FieldDef.toggle("fxGlowing",      "Glowing"),
                    FieldDef.toggle("fxInvisibility", "Invisibility"),
                    FieldDef.num("fxHaste",           "Haste Level  (0=off, 1=I, 2=II)"),
                    FieldDef.num("fxStrength",        "Strength Level  (0=off, 1=I, 2=II)"),
                    FieldDef.num("fxSpeed",           "Speed Level  (0=off, 1=I, 2=II)")
            );

            case 4 -> List.of(
                    FieldDef.toggle("godMode",        "God Mode"),
                    FieldDef.toggle("keepInventory",  "Keep Inventory"),
                    FieldDef.toggle("fireImmune",     "Fire Immune"),
                    FieldDef.toggle("drownImmune",    "Drown Immune"),
                    FieldDef.toggle("noPoison",       "No Poison"),
                    FieldDef.toggle("noMagic",        "No Magic Damage"),
                    FieldDef.num("maxFoodLevel",      "Max Food Level"),
                    FieldDef.num("hungerDrainRate",   "Hunger Drain Rate"),
                    FieldDef.num("exhaustionCap",     "Exhaustion Cap"),
                    FieldDef.num("xpMultiplier",      "XP Multiplier  (1.0=normal)")
            );
            default -> List.of();
        };
    }

    public void render(GuiGraphics g, int mx, int my, float delta) {
        Minecraft mc = Minecraft.getInstance();

        g.fill(x, y, x + w, y + HEADER_H, 0xFF0F3460);
        String typeLabel = activeType != null ? activeType.name() : "?";
        if (instanceMode && activeType == TargetType.MOB) typeLabel = "MOB INSTANCE";
        String header = targetId.isEmpty() ? "No target selected" : "[" + typeLabel + "] " + targetId;
        g.drawString(mc.font, header, x + PAD, y + 7, 0xFFFFFFFF, false);
        copyBtn.render(g, mx, my, delta);
        pasteBtn.render(g, mx, my, delta);
        fromBlockBtn.render(g, mx, my, delta);
        presetBtn.render(g, mx, my, delta);

        if (!tabNames.isEmpty()) {
            int tabW = (w - PAD) / tabNames.size();
            for (int i = 0; i < tabNames.size(); i++) {
                int tx = x + i * tabW;
                int ty = y + HEADER_H;
                boolean active = i == activeTab;
                g.fill(tx, ty, tx + tabW, ty + TAB_H,
                        active ? 0xFF533483 : 0xFF2D2D4E);
                g.drawCenteredString(mc.font, tabNames.get(i),
                        tx + tabW / 2, ty + 5, active ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }

        int bodyY  = y + HEADER_H + TAB_H + PAD;
        int bodyH  = h - HEADER_H - TAB_H - PAD;
        g.enableScissor(x, bodyY, x + w, bodyY + bodyH);
        for (int i = 0; i < rows.size(); i++) {
            FieldRow row = rows.get(i);
            int ry = row.y - scrollOffset;
            if (ry + ROW_H < bodyY) continue;
            if (ry > bodyY + bodyH) break;

            if (i % 2 == 0) {
                g.fill(x + 1, ry, x + w - 1, ry + ROW_H, 0x12FFFFFF);
            }
            row.render(g, mx, my, delta, scrollOffset);
        }
        g.disableScissor();
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (copyBtn.mouseClicked(mx, my, btn))      return true;
        if (pasteBtn.mouseClicked(mx, my, btn))     return true;
        if (fromBlockBtn.mouseClicked(mx, my, btn)) return true;
        if (presetBtn.mouseClicked(mx, my, btn))    return true;

        if (!tabNames.isEmpty() && my >= y + HEADER_H && my < y + HEADER_H + TAB_H) {
            int tabW = (w - PAD) / tabNames.size();
            int idx = (int) ((mx - x) / tabW);
            if (idx >= 0 && idx < tabNames.size() && idx != activeTab) {
                activeTab = idx;
                scrollOffset = 0;
                rebuildRows();
                return true;
            }
        }

        int bodyY = y + HEADER_H + TAB_H + PAD;
        int bodyH = h - HEADER_H - TAB_H - PAD;
        if (mx < x || mx > x + w || my < bodyY || my > bodyY + bodyH) return false;

        rows.forEach(FieldRow::clearFocus);

        int adjustedY = (int) my + scrollOffset;
        for (FieldRow row : rows) {
            if (row.mouseClicked(mx, adjustedY, btn)) return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int bodyY = y + HEADER_H + TAB_H + PAD;
        int bodyH = h - HEADER_H - TAB_H - PAD;
        if (mx >= x && mx < x + w && my >= bodyY && my < bodyY + bodyH) {
            int maxScroll = Math.max(0, rows.size() * (ROW_H + 2) - bodyH);
            scrollOffset  = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll * ROW_H));
            return true;
        }
        return false;
    }

    public void clearAllFocus() {
        rows.forEach(FieldRow::clearFocus);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        for (FieldRow row : rows) {
            if (row.keyPressed(key, scan, mods)) return true;
        }
        return false;
    }

    public boolean charTyped(char c, int mods) {
        for (FieldRow row : rows) {
            if (row.charTyped(c, mods)) return true;
        }
        return false;
    }

    private void openFromBlock() {
        if (!(stats instanceof BlockStats target)) return;
        Screen current = Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(
            new BlockSourceBrowserScreen(current, extracted -> {

                applyExtractedBlockStats(extracted, target);
                parent.markDirty();

                rebuildRows();
            })
        );
    }

    private void applyExtractedBlockStats(BlockStats from, BlockStats to) {
        to.hardness       = from.hardness;
        to.blastResistance= from.blastResistance;
        to.slipperiness   = from.slipperiness;
        to.bounceFactor   = from.bounceFactor;
        to.noCollision    = from.noCollision;
        to.lightEmission  = from.lightEmission;
        to.climbable      = from.climbable;
        to.stepDamage     = from.stepDamage;
        to.speedModifier  = from.speedModifier;
        to.freezeOnStep   = from.freezeOnStep;
    }

    private void openPresets() {
        Minecraft.getInstance().setScreen(
                new com.craftstats.common.gui.PresetBrowserScreen(parent,
                        activeType, stats, targetId));
    }
}
