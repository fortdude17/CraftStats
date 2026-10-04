package com.craftstats.common.gui.editor;

import com.craftstats.common.gui.Ui;
import com.craftstats.common.stats.StatAccess;
import com.craftstats.common.stats.StatDef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One editable stat: a label plus vanilla widgets bound to a field of the stats object.
 * Optional numbers can be left empty, which means "vanilla"; the vanilla value is the hint.
 */
public class StatRow {

    public static final int H = 22;
    private static final int WIDGET_H = 20;
    private static final int RESET_W = 20;

    public final StatDef def;
    private final Object target;
    private final Object vanilla;
    private final Runnable onChange;
    private final boolean supported;
    private final Class<?> fieldType;

    private EditBox box;
    private Button button;
    private Button reset;
    private boolean invalid;
    private boolean refreshing;

    private int x, y, w;

    public StatRow(StatDef def, Object target, Object vanilla, boolean supported, Runnable onChange) {
        this.def = def;
        this.target = target;
        this.vanilla = vanilla;
        this.supported = supported;
        this.onChange = onChange;
        this.fieldType = StatAccess.typeOf(target, def.field());
    }

    /** Creates the widgets; they are added to the screen by the caller. */
    public List<AbstractWidget> createWidgets() {
        List<AbstractWidget> out = new ArrayList<>();
        Tooltip tip = tooltip();
        switch (def.kind()) {
            case NUMBER, TEXT -> {
                box = new EditBox(Ui.font(), 0, 0, 60, WIDGET_H, Component.literal(def.label()));
                box.setMaxLength(def.kind() == StatDef.Kind.TEXT ? 256 : 24);
                Object current = StatAccess.get(target, def.field());
                box.setValue(current == null ? "" : format(current));
                String hint = hint();
                if (!hint.isEmpty()) box.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
                box.setResponder(this::onTyped);
                box.setTooltip(tip);
                out.add(box);
                validateText(box.getValue());
            }
            case TOGGLE, CHOICE -> {
                button = Button.builder(buttonText(), b -> onPress(false)).bounds(0, 0, 60, WIDGET_H).tooltip(tip).build();
                out.add(button);
            }
        }
        reset = Button.builder(Component.literal("x"), b -> resetToDefault()).bounds(0, 0, RESET_W, WIDGET_H)
                .tooltip(Tooltip.create(Component.literal("Back to default"))).build();
        out.add(reset);
        if (!supported) for (AbstractWidget wd : out) wd.active = false;
        updateResetButton();
        return out;
    }

    private Tooltip tooltip() {
        MutableComponent text = Component.literal(def.label()).withStyle(ChatFormatting.YELLOW);
        if (!def.help().isEmpty()) text.append(Component.literal("\n" + def.help()).withStyle(ChatFormatting.GRAY));
        if (def.kind() == StatDef.Kind.NUMBER)
            text.append(Component.literal("\nRange: " + format(def.min()) + " to " + format(def.max())).withStyle(ChatFormatting.DARK_GRAY));
        if (!supported) text.append(Component.literal("\nThis one doesn't have this stat.").withStyle(ChatFormatting.RED));
        return Tooltip.create(text);
    }

    // ---- layout ----------------------------------------------------------------------------

    public void layout(int x, int y, int w, boolean visible) {
        this.x = x; this.y = y; this.w = w;
        int inputW = Math.max(50, Math.min(140, w * 2 / 5));
        if (def.kind() == StatDef.Kind.TEXT) inputW = Math.max(inputW, Math.min(170, w / 2));
        int inputX = x + w - RESET_W - 2 - inputW;
        AbstractWidget input = box != null ? box : button;
        if (input != null) {
            input.setX(inputX);
            input.setY(y + 1);
            input.setWidth(inputW);
            input.visible = visible;
        }
        reset.setX(x + w - RESET_W);
        reset.setY(y + 1);
        reset.visible = visible;
    }

    public void renderLabel(GuiGraphics g) {
        int inputX = (box != null ? box : button).getX();
        int color = !supported ? Ui.DARK : changed() ? Ui.YELLOW : Ui.WHITE;
        Ui.textFit(g, def.label(), x + 4, y + 7, inputX - x - 8, color);
    }

    // ---- values ----------------------------------------------------------------------------

    private boolean changed() {
        Object v = StatAccess.get(target, def.field());
        Object fresh = StatAccess.get(defaultsHolder(), def.field());
        return v == null ? fresh != null : !v.equals(fresh);
    }

    private Object defaultsHolder() {
        return StatAccess.defaults(target.getClass());
    }

    private String hint() {
        Object v = vanilla == null ? null : StatAccess.get(vanilla, def.field());
        if (v instanceof Number n) return "default " + format(n);
        if (def.kind() == StatDef.Kind.NUMBER && !Double.isNaN(def.fallback())) return "default " + format(def.fallback());
        if (def.kind() == StatDef.Kind.TEXT) {
            if (def.has(StatDef.ID_EFFECT)) return "e.g. minecraft:speed";
            if (def.has(StatDef.ID_ENTITY)) return "e.g. minecraft:zombie";
            if (def.has(StatDef.ID_ITEM)) return "e.g. minecraft:diamond";
        }
        return "";
    }

    private void onTyped(String text) {
        if (refreshing) return;
        if (def.kind() == StatDef.Kind.TEXT) {
            StatAccess.set(target, def.field(), text.trim());
            validateText(text);
            changedValue();
            return;
        }
        if (text.isBlank()) {
            invalid = false;
            box.setTextColor(0xFFE0E0E0);
            if (!fieldType.isPrimitive()) StatAccess.set(target, def.field(), null);
            else resetPrimitive();
            changedValue();
            return;
        }
        Double parsed = parse(text);
        invalid = parsed == null;
        if (!invalid) {
            double clamped = Math.max(def.min(), Math.min(def.max(), parsed));
            StatAccess.set(target, def.field(), StatAccess.coerce(fieldType, clamped));
            invalid = clamped != parsed;
        }
        box.setTextColor(invalid ? 0xFFFF5555 : numberColor());
        changedValue();
    }

    private void resetPrimitive() {
        Object fresh = StatAccess.get(defaultsHolder(), def.field());
        StatAccess.set(target, def.field(), fresh);
    }

    private int numberColor() {
        Object cur = StatAccess.get(target, def.field());
        Object van = vanilla == null ? null : StatAccess.get(vanilla, def.field());
        double base = van instanceof Number n ? n.doubleValue() : def.fallback();
        if (!(cur instanceof Number c) || Double.isNaN(base)) return 0xFFE0E0E0;
        int cmp = Double.compare(c.doubleValue(), base);
        return cmp > 0 ? 0xFF7CFC7C : cmp < 0 ? 0xFFFFB347 : 0xFFE0E0E0;
    }

    private void validateText(String text) {
        if (box == null) return;
        if (def.kind() == StatDef.Kind.NUMBER) {
            box.setTextColor(numberColor());
            return;
        }
        String t = text.trim();
        boolean ok = t.isEmpty() || validId(t);
        box.setTextColor(ok ? 0xFFE0E0E0 : 0xFFFF5555);
    }

    private boolean validId(String t) {
        ResourceLocation id = ResourceLocation.tryParse(t);
        if (def.has(StatDef.ID_EFFECT)) return id != null && BuiltInRegistries.MOB_EFFECT.containsKey(id);
        if (def.has(StatDef.ID_ENTITY)) return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
        if (def.has(StatDef.ID_ITEM)) return id != null && BuiltInRegistries.ITEM.containsKey(id);
        return true;
    }

    private static Double parse(String text) {
        try {
            double d = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Left click cycles forward; {@code back} cycles backwards. */
    public void onPress(boolean back) {
        if (def.kind() == StatDef.Kind.TOGGLE) {
            StatAccess.set(target, def.field(), !Boolean.TRUE.equals(StatAccess.get(target, def.field())));
        } else if (def.kind() == StatDef.Kind.CHOICE) {
            String[] opts = def.options();
            String cur = String.valueOf(StatAccess.get(target, def.field()));
            int i = 0;
            for (int k = 0; k < opts.length; k++) if (opts[k].equals(cur)) i = k;
            StatAccess.set(target, def.field(), opts[Math.floorMod(i + (back ? -1 : 1), opts.length)]);
        }
        button.setMessage(buttonText());
        changedValue();
    }

    private Component buttonText() {
        Object v = StatAccess.get(target, def.field());
        if (def.kind() == StatDef.Kind.TOGGLE) {
            boolean on = Boolean.TRUE.equals(v);
            return Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
        }
        String s = String.valueOf(v);
        String pretty = s.isEmpty() ? "-" : Character.toUpperCase(s.charAt(0)) + s.substring(1).replace('_', ' ');
        boolean isDefault = s.equals(def.options()[0]);
        return Component.literal(pretty).withStyle(isDefault ? ChatFormatting.WHITE : ChatFormatting.YELLOW);
    }

    private void resetToDefault() {
        Object fresh = StatAccess.get(defaultsHolder(), def.field());
        StatAccess.set(target, def.field(), fresh);
        if (box != null) {
            box.setValue(fresh == null ? "" : format(fresh));
            validateText(box.getValue());
        }
        if (button != null) button.setMessage(buttonText());
        changedValue();
    }

    private void changedValue() {
        updateResetButton();
        onChange.run();
    }

    private void updateResetButton() {
        if (reset != null) reset.active = supported && changed();
    }

    /** Re-reads the value after the whole stats object was replaced (paste, preset). */
    public void refresh() {
        if (box != null) {
            Object current = StatAccess.get(target, def.field());
            refreshing = true;
            box.setValue(current == null ? "" : format(current));
            refreshing = false;
            validateText(box.getValue());
        }
        if (button != null) button.setMessage(buttonText());
        updateResetButton();
    }

    public boolean matches(String query) {
        return def.label().toLowerCase(Locale.ROOT).contains(query) || def.help().toLowerCase(Locale.ROOT).contains(query);
    }

    public static String format(Object v) {
        if (v == null) return "";
        if (v instanceof Number n && !(v instanceof Integer) && !(v instanceof Long)) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e12) return String.valueOf((long) d);
            String s = String.format(Locale.ROOT, "%.4f", d);
            return s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.valueOf(v);
    }
}
