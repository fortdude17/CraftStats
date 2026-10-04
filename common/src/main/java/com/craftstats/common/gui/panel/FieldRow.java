package com.craftstats.common.gui.panel;

import com.craftstats.common.gui.UiInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * One editable row bound to a field of a stats object via reflection. Optional numbers
 * (boxed types) can be left empty, which means "vanilla"; the vanilla value is shown as a
 * grey hint.
 */
public class FieldRow {

    public static final int H = 18;
    private static final int LABEL_W = 150;
    private static final int BTN_W   = 12;
    private static final int TOGGLE_W = 36;

    public final int x, y, w;

    private final FieldDef def;
    private final Object   target;
    private final Object   vanilla;
    private final Runnable onChange;
    private final Field    field;

    private EditBox box;
    private boolean invalid;

    private FieldRow(FieldDef def, Object target, Object vanilla, int x, int y, int w, Runnable onChange) {
        this.def = def;
        this.target = target;
        this.vanilla = vanilla;
        this.x = x; this.y = y; this.w = w;
        this.onChange = onChange;
        this.field = resolve(target.getClass(), def.fieldName());
        initWidget();
    }

    public static FieldRow create(FieldDef def, Object target, Object vanilla, int x, int y, int w, Runnable onChange) {
        return new FieldRow(def, target, vanilla, x, y, w, onChange);
    }

    private static Field resolve(Class<?> type, String name) {
        try {
            Field f = type.getField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No field '" + name + "' on " + type.getSimpleName(), e);
        }
    }

    private int inputX() { return x + LABEL_W; }
    private int inputW() { return def.type() == FieldDef.FieldType.NUMBER ? w - LABEL_W - BTN_W * 2 - 6 : w - LABEL_W - 2; }

    private void initWidget() {
        if (def.type() != FieldDef.FieldType.NUMBER && def.type() != FieldDef.FieldType.TEXT) return;
        box = new EditBox(Minecraft.getInstance().font, inputX(), y, inputW(), H, Component.literal(def.label()));
        box.setMaxLength(def.type() == FieldDef.FieldType.TEXT ? 256 : 32);
        Object current = get(target);
        box.setValue(current == null ? "" : format(current));
        String hint = vanilla != null ? format(get(vanilla)) : "";
        if (!hint.isEmpty()) box.setHint(Component.literal("default: " + hint).withStyle(s -> s.withColor(0x707080)));
        box.setResponder(this::onTyped);
    }

    // ---- reflection helpers -------------------------------------------------------------

    private Object get(Object obj) {
        if (obj == null) return null;
        try { return field.get(obj); } catch (IllegalAccessException e) { return null; }
    }

    private void set(Object value) {
        try { field.set(target, value); } catch (IllegalAccessException | IllegalArgumentException ignored) {}
    }

    private boolean isOptionalNumber() {
        return !field.getType().isPrimitive();
    }

    /** Parses text into the field's numeric type; returns null if it doesn't parse. */
    private Object parse(String text) {
        try {
            Class<?> t = field.getType();
            String s = text.trim();
            if (t == double.class || t == Double.class)  return Double.parseDouble(s);
            if (t == float.class  || t == Float.class)   return Float.parseFloat(s);
            if (t == int.class    || t == Integer.class) return (int) Math.round(Double.parseDouble(s));
            if (t == long.class   || t == Long.class)    return Long.parseLong(s);
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e9) return String.valueOf((long) d);
            return String.format(Locale.ROOT, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.valueOf(v);
    }

    private void onTyped(String text) {
        if (def.type() == FieldDef.FieldType.TEXT) {
            set(text);
            onChange.run();
            return;
        }
        if (text.isBlank()) {
            invalid = false;
            if (isOptionalNumber()) { set(null); onChange.run(); }
            return;
        }
        Object value = parse(text);
        invalid = value == null || (value instanceof Double d && (d.isNaN() || d.isInfinite()))
                || (value instanceof Float f && (f.isNaN() || f.isInfinite()));
        if (!invalid) { set(value); onChange.run(); }
    }

    private void step(int dir) {
        Object current = get(target);
        if (current == null) current = get(vanilla);
        double v = current instanceof Number n ? n.doubleValue() : 0;
        double abs = Math.abs(v);
        boolean integer = field.getType() == int.class || field.getType() == Integer.class;
        double size = integer ? 1 : abs < 0.1 ? 0.01 : abs < 1 ? 0.05 : abs < 10 ? 0.5 : abs < 100 ? 1 : 10;
        Object next = parse(format(v + dir * size));
        if (next == null) return;
        set(next);
        if (box != null) box.setValue(format(next));
        onChange.run();
    }

    private int valueColor() {
        if (invalid) return 0xFFFF5555;
        Object cur = get(target), van = get(vanilla);
        if (!(cur instanceof Number c) || !(van instanceof Number v)) return 0xFFFFFFFF;
        int cmp = Double.compare(c.doubleValue(), v.doubleValue());
        return cmp > 0 ? 0xFF88EE88 : cmp < 0 ? 0xFFEEAA44 : 0xFFFFFFFF;
    }

    // ---- rendering & input ---------------------------------------------------------------

    public void render(GuiGraphics g, int mx, int my, float delta, int scrollOffset) {
        int ry = y - scrollOffset;
        Minecraft mc = Minecraft.getInstance();
        g.drawString(mc.font, def.label(), x + 3, ry + (H - 8) / 2 + 1, 0xFFCCCCCC, false);
        int ix = inputX(), iw = inputW();

        switch (def.type()) {
            case NUMBER, TEXT -> {
                box.setX(ix);
                box.setY(ry);
                box.setTextColor(def.type() == FieldDef.FieldType.NUMBER ? valueColor() : 0xFFFFFFFF);
                boolean focused = box.isFocused();
                box.setBordered(focused);
                if (!focused) g.fill(ix, ry, ix + iw, ry + H, 0xFF1A1A33);
                box.render(g, mx, my, delta);
                if (def.type() == FieldDef.FieldType.NUMBER) {
                    int bx1 = ix + iw + 2, bx2 = bx1 + BTN_W + 1;
                    boolean hoverMinus = mx >= bx1 && mx < bx1 + BTN_W && my >= ry && my < ry + H;
                    boolean hoverPlus  = mx >= bx2 && mx < bx2 + BTN_W && my >= ry && my < ry + H;
                    g.fill(bx1, ry, bx1 + BTN_W, ry + H, hoverMinus ? 0xFF6677BB : 0xFF333355);
                    g.fill(bx2, ry, bx2 + BTN_W, ry + H, hoverPlus  ? 0xFF6677BB : 0xFF333355);
                    g.drawCenteredString(mc.font, "-", bx1 + BTN_W / 2, ry + (H - 8) / 2, 0xFFDDDDDD);
                    g.drawCenteredString(mc.font, "+", bx2 + BTN_W / 2, ry + (H - 8) / 2, 0xFFDDDDDD);
                }
            }
            case TOGGLE -> {
                boolean on = Boolean.TRUE.equals(get(target));
                g.fill(ix, ry + 1, ix + TOGGLE_W, ry + H - 1, on ? 0xFF2A7A2A : 0xFF5A2A2A);
                int thumbX = on ? ix + TOGGLE_W - 15 : ix + 3;
                g.fill(thumbX, ry + 3, thumbX + 12, ry + H - 3, on ? 0xFF55EE55 : 0xFFEE5555);
                String label = on ? "ON" : "OFF";
                int textX = on ? ix + 4 : ix + TOGGLE_W - 4 - mc.font.width(label);
                g.drawString(mc.font, label, textX, ry + (H - 8) / 2 + 1, 0xFFFFFFFF, false);
            }
            case PILL -> {
                String[] options = def.options();
                String current = String.valueOf(get(target));
                int pillW = (w - LABEL_W - 4) / options.length;
                for (int i = 0; i < options.length; i++) {
                    int px = ix + i * pillW;
                    boolean sel = options[i].equals(current);
                    g.fill(px, ry + 1, px + pillW - 1, ry + H - 1, sel ? 0xFF533483 : 0xFF2D2D4E);
                    if (sel) g.fill(px, ry + H - 3, px + pillW - 1, ry + H - 1, 0xFF9966FF);
                    String label = mc.font.plainSubstrByWidth(options[i].replace('_', ' '), pillW - 4);
                    g.drawCenteredString(mc.font, label, px + pillW / 2, ry + (H - 8) / 2, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
                }
            }
        }
    }

    /** {@code my} must already include the scroll offset. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (my < y || my >= y + H || mx < x || mx >= x + w) return false;
        int ix = inputX(), iw = inputW();
        switch (def.type()) {
            case NUMBER -> {
                int bx1 = ix + iw + 2, bx2 = bx1 + BTN_W + 1;
                if (mx >= bx1 && mx < bx1 + BTN_W) { step(-1); return true; }
                if (mx >= bx2 && mx < bx2 + BTN_W) { step(1);  return true; }
                focusBox();
                return true;
            }
            case TEXT -> { focusBox(); return true; }
            case TOGGLE -> {
                if (mx >= ix + TOGGLE_W && mx >= x + LABEL_W) return false;
                set(!Boolean.TRUE.equals(get(target)));
                onChange.run();
                return true;
            }
            case PILL -> {
                String[] options = def.options();
                int pillW = (w - LABEL_W - 4) / options.length;
                int idx = (int) ((mx - ix) / pillW);
                if (mx < ix || idx < 0 || idx >= options.length) return false;
                set(options[idx]);
                onChange.run();
                return true;
            }
        }
        return false;
    }

    private void focusBox() {
        box.setFocused(true);
        box.setCursorPosition(box.getValue().length());
        box.setHighlightPos(0);
    }

    public void clearFocus() {
        if (box != null) box.setFocused(false);
    }

    public boolean isFocused() {
        return box != null && box.isFocused();
    }

    public boolean keyPressed(int key, int scan, int mods) {
        return isFocused() && UiInput.key(box, key, scan, mods);
    }

    public boolean charTyped(char c, int mods) {
        return isFocused() && UiInput.chr(box, c, mods);
    }
}
