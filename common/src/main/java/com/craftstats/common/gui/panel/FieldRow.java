package com.craftstats.common.gui.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

public class FieldRow {

    public final int x, y, w;
    static final int H       = 18;
    private static final int LABEL_W = 140;
    private static final int BTN_W   = 12;

    private final FieldDef  def;
    private final Object    target;
    private final Runnable  onChange;
    private final Field     field;

    private EditBox numberBox;
    private EditBox textBox;
    private int     pillIndex    = 0;

    private FieldRow(FieldDef def, Object target, int x, int y, int w, Runnable onChange) {
        this.def      = def;
        this.target   = target;
        this.x        = x;
        this.y        = y;
        this.w        = w;
        this.onChange = onChange;

        Field f = null;
        try {
            f = resolveField(target.getClass(), def.fieldName());
            if (f != null) f.setAccessible(true);
        } catch (Exception ignored) {}
        this.field = f;

        initWidget();
    }

    private static Field resolveField(Class<?> clazz, String name) {
        try { return clazz.getField(name); } catch (NoSuchFieldException e1) {
            try { return clazz.getDeclaredField(name); } catch (NoSuchFieldException e2) {
                String camel = snakeToCamel(name);
                try { return clazz.getField(camel); } catch (NoSuchFieldException e3) {
                    try { return clazz.getDeclaredField(camel); } catch (NoSuchFieldException e4) {
                        return null;
                    }
                }
            }
        }
    }

    private static String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_')     { up = true; }
            else if (up)      { sb.append(Character.toUpperCase(c)); up = false; }
            else              { sb.append(c); }
        }
        return sb.toString();
    }

    private void initWidget() {
        int inputX = x + LABEL_W;
        int inputW = w - LABEL_W - BTN_W * 2 - 6;
        switch (def.type()) {
            case NUMBER -> {
                numberBox = new EditBox(Minecraft.getInstance().font,
                        inputX, y, inputW, H, Component.empty());
                numberBox.setMaxLength(30);
                numberBox.setValue(getFieldStringValue());
                numberBox.setResponder(v -> writeField(v));
            }
            case TEXT -> {
                textBox = new EditBox(Minecraft.getInstance().font,
                        inputX, y, w - LABEL_W - 2, H, Component.empty());
                textBox.setMaxLength(256);
                textBox.setValue(getFieldStringValue());
                textBox.setResponder(v -> writeStringField(v));
            }
            case PILL -> {
                String cur = getFieldStringValue();
                pillIndex = 0;
                if (def.options() != null) {
                    for (int i = 0; i < def.options().length; i++) {
                        if (def.options()[i].equals(cur)) { pillIndex = i; break; }
                    }
                }
            }
            case TOGGLE -> {}
        }
    }

    public static FieldRow create(FieldDef def, Object target, int x, int y, int w, Runnable onChange) {
        return new FieldRow(def, target, x, y, w, onChange);
    }

    public void render(GuiGraphics g, int mx, int my, float delta, int scrollOffset) {
        int ry = y - scrollOffset;
        Minecraft mc = Minecraft.getInstance();

        g.drawString(mc.font, def.label(), x + 3, ry + (H - 8) / 2 + 1, 0xFFCCCCCC, false);

        int inputX = x + LABEL_W;
        int inputW = w - LABEL_W - BTN_W * 2 - 6;

        switch (def.type()) {
            case NUMBER -> {
                if (numberBox != null) {
                    numberBox.setX(inputX);
                    numberBox.setY(ry);
                    numberBox.setHeight(H);
                    numberBox.setTextColor(valueColor());
                    boolean focused = numberBox.isFocused();

                    numberBox.setBordered(focused);
                    if (!focused) g.fill(inputX, ry, inputX + inputW, ry + H, 0xFF1A1A33);
                    numberBox.render(g, mx, my, delta);

                    int bx1 = inputX + inputW + 2;
                    int bx2 = bx1 + BTN_W + 1;
                    boolean hoverMinus = mx >= bx1 && mx < bx1 + BTN_W && my >= ry && my < ry + H;
                    boolean hoverPlus  = mx >= bx2 && mx < bx2 + BTN_W && my >= ry && my < ry + H;
                    g.fill(bx1, ry, bx1 + BTN_W, ry + H, hoverMinus ? 0xFF6677BB : 0xFF333355);
                    g.fill(bx2, ry, bx2 + BTN_W, ry + H, hoverPlus  ? 0xFF6677BB : 0xFF333355);
                    g.drawCenteredString(mc.font, "−", bx1 + BTN_W / 2, ry + (H - 8) / 2, 0xFFDDDDDD);
                    g.drawCenteredString(mc.font, "+", bx2 + BTN_W / 2, ry + (H - 8) / 2, 0xFFDDDDDD);
                }
            }
            case TEXT -> {
                if (textBox != null) {
                    textBox.setX(inputX);
                    textBox.setY(ry);
                    textBox.setHeight(H);
                    boolean focused = textBox.isFocused();
                    textBox.setBordered(focused);
                    if (!focused) g.fill(inputX, ry, inputX + (w - LABEL_W - 2), ry + H, 0xFF1A1A33);
                    textBox.render(g, mx, my, delta);
                }
            }
            case TOGGLE -> {
                boolean val = getBooleanValue();
                int tw = 36;
                g.fill(inputX, ry + 1, inputX + tw, ry + H - 1, val ? 0xFF2A7A2A : 0xFF5A2A2A);
                int thumbX = val ? inputX + tw - 15 : inputX + 3;
                g.fill(thumbX, ry + 3, thumbX + 12, ry + H - 3, val ? 0xFF55EE55 : 0xFFEE5555);
                g.drawCenteredString(mc.font, val ? "ON" : "OFF",
                        val ? inputX + tw - 8 - mc.font.width("ON")  / 2
                             : inputX + 14 + mc.font.width("OFF") / 2,
                        ry + (H - 8) / 2, 0xFFFFFFFF);
            }
            case PILL -> {
                if (def.options() != null) {
                    int pillW = (w - LABEL_W - 4) / def.options().length;
                    for (int i = 0; i < def.options().length; i++) {
                        int px   = inputX + i * pillW;
                        boolean sel = i == pillIndex;
                        g.fill(px, ry + 1, px + pillW - 1, ry + H - 1, sel ? 0xFF533483 : 0xFF2D2D4E);
                        if (sel) g.fill(px, ry + H - 3, px + pillW - 1, ry + H - 1, 0xFF9966FF);
                        String lbl = def.options()[i];
                        if (lbl.length() > 7) lbl = lbl.substring(0, 6) + "…";
                        g.drawCenteredString(mc.font, lbl, px + pillW / 2,
                                ry + (H - 8) / 2, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
                    }
                }
            }
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        int inputX = x + LABEL_W;
        int inputW = w - LABEL_W - BTN_W * 2 - 6;

        switch (def.type()) {
            case NUMBER -> {
                if (numberBox != null) {
                    int bx1 = inputX + inputW + 2;
                    int bx2 = bx1 + BTN_W + 1;
                    if (mx >= bx1 && mx < bx1 + BTN_W && my >= y && my < y + H) { step(-1); return true; }
                    if (mx >= bx2 && mx < bx2 + BTN_W && my >= y && my < y + H) { step(1);  return true; }
                    if (my >= y && my < y + H && mx >= x && mx < x + w) {
                        numberBox.setFocused(true);
                        numberBox.setCursorPosition(numberBox.getValue().length());
                        numberBox.setHighlightPos(0);
                        return true;
                    }
                }
            }
            case TEXT -> {
                if (textBox != null) {

                    if (my >= y && my < y + H && mx >= x && mx < x + w) {
                        textBox.setFocused(true);
                        textBox.setCursorPosition(textBox.getValue().length());
                        textBox.setHighlightPos(0);
                        return true;
                    }
                }
            }
            case TOGGLE -> {
                if (mx >= x && mx < x + LABEL_W + 36 && my >= y && my < y + H) {
                    writeBooleanField(!getBooleanValue());
                    onChange.run();
                    return true;
                }
            }
            case PILL -> {
                if (def.options() != null) {
                    int pillW = (w - LABEL_W - 4) / def.options().length;
                    int idx   = (int) ((mx - inputX) / pillW);
                    if (my >= y && my < y + H && idx >= 0 && idx < def.options().length) {
                        pillIndex = idx;
                        writeStringField(def.options()[idx]);
                        onChange.run();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int valueColor() {
        if (field == null) return 0xFFFFFFFF;
        try {
            Object defInst = target.getClass().getDeclaredConstructor().newInstance();
            Field df = resolveField(defInst.getClass(), def.fieldName());
            if (df == null) return 0xFFFFFFFF;
            df.setAccessible(true);
            double cur = toDouble(field.get(target));
            double def = toDouble(df.get(defInst));
            if (cur > def) return 0xFF88EE88;
            if (cur < def) return 0xFFEEAA44;
        } catch (Exception ignored) {}
        return 0xFFFFFFFF;
    }

    private static double toDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return 0;
    }

    private void step(int dir) {
        if (field == null) return;
        try {
            if (field.getType() == double.class || field.getType() == Double.class) {
                double v = field.getDouble(target);
                double next = v + dir * stepSize(v);
                field.setDouble(target, next);
                if (numberBox != null) numberBox.setValue(fmt(field.getDouble(target)));
            } else if (field.getType() == float.class || field.getType() == Float.class) {
                float v = field.getFloat(target);
                float next = v + dir * (float) stepSize(v);
                field.setFloat(target, next);
                if (numberBox != null) numberBox.setValue(fmt(field.getFloat(target)));
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                int v = field.getInt(target);
                field.setInt(target, v + dir);
                if (numberBox != null) numberBox.setValue(String.valueOf(v + dir));
            }
            onChange.run();
        } catch (Exception ignored) {}
    }

    private double stepSize(double v) {
        double abs = Math.abs(v);
        if (abs < 0.1)   return 0.001;
        if (abs < 1)     return 0.01;
        if (abs < 10)    return 0.1;
        if (abs < 100)   return 1.0;
        return 10.0;
    }

    private String fmt(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e9) return String.valueOf((long) v);
        String s = String.format("%.4f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String getFieldStringValue() {
        if (field == null) return "";
        try { return String.valueOf(field.get(target)); }
        catch (Exception e) { return ""; }
    }

    private boolean getBooleanValue() {
        if (field == null) return false;
        try { return field.getBoolean(target); }
        catch (Exception e) { return false; }
    }

    private void writeField(String text) {
        if (field == null) return;
        try {
            Class<?> t = field.getType();
            if      (t == double.class  || t == Double.class)  field.setDouble(target, Double.parseDouble(text));
            else if (t == float.class   || t == Float.class)   field.setFloat(target, Float.parseFloat(text));
            else if (t == int.class     || t == Integer.class) field.setInt(target, Integer.parseInt(text));
            else if (t == long.class    || t == Long.class)    field.setLong(target, Long.parseLong(text));
            onChange.run();
        } catch (Exception ignored) {}
    }

    private void writeStringField(String text) {
        if (field == null) return;
        try { field.set(target, text); onChange.run(); }
        catch (Exception ignored) {}
    }

    private void writeBooleanField(boolean v) {
        if (field == null) return;
        try { field.setBoolean(target, v); }
        catch (Exception ignored) {}
    }

    public void clearFocus() {
        if (numberBox != null) numberBox.setFocused(false);
        if (textBox   != null) textBox.setFocused(false);
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (numberBox != null && numberBox.isFocused()) return numberBox.keyPressed(key, scan, mods);
        if (textBox   != null && textBox.isFocused())   return textBox.keyPressed(key, scan, mods);
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (numberBox != null && numberBox.isFocused()) return numberBox.charTyped(c, mods);
        if (textBox   != null && textBox.isFocused())   return textBox.charTyped(c, mods);
        return false;
    }
}
