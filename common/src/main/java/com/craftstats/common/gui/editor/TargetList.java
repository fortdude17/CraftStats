package com.craftstats.common.gui.editor;

import com.craftstats.common.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** A scrollable, searchable list of targets with item icons, drawn like a vanilla selection list. */
public class TargetList {

    public record Entry(String key, String label, ItemStack icon) {}

    public static final int ROW_H = 18;

    private int x, y, w, h;
    private List<Entry> all = List.of();
    private final List<Entry> shown = new ArrayList<>();
    private String query = "";
    private Predicate<Entry> filter = e -> true;
    private String selected = "";
    private int scroll;
    private final Consumer<Entry> onSelect;
    private Predicate<Entry> modified = e -> false;

    public TargetList(Consumer<Entry> onSelect) {
        this.onSelect = onSelect;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        clampScroll();
    }

    public void setEntries(List<Entry> entries) {
        this.all = entries;
        refilter();
    }

    public void setQuery(String q) {
        query = q.toLowerCase(Locale.ROOT).trim();
        refilter();
        scroll = 0;
    }

    public void setFilter(Predicate<Entry> f) {
        filter = f;
        refilter();
        scroll = 0;
    }

    public void setModified(Predicate<Entry> m) { modified = m; }

    public void refilter() {
        shown.clear();
        for (Entry e : all) {
            if (!filter.test(e)) continue;
            if (!query.isEmpty() && !e.label().toLowerCase(Locale.ROOT).contains(query)
                    && !e.key().toLowerCase(Locale.ROOT).contains(query)) continue;
            shown.add(e);
        }
        clampScroll();
    }

    public int size() { return shown.size(); }
    public List<Entry> shown() { return shown; }

    public void select(String key, boolean scrollTo) {
        selected = key == null ? "" : key;
        if (!scrollTo) return;
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).key().equals(selected)) {
                int rowTop = i * ROW_H;
                if (rowTop < scroll || rowTop + ROW_H > scroll + h) scroll = rowTop - h / 3;
                clampScroll();
                return;
            }
        }
    }

    public String selected() { return selected; }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() * ROW_H - h)));
    }

    public void render(GuiGraphics g, int mx, int my) {
        Ui.listPanel(g, x, y, w, h);
        g.enableScissor(x, y, x + w, y + h);
        int first = scroll / ROW_H;
        for (int i = first; i < shown.size(); i++) {
            int ry = y + i * ROW_H - scroll;
            if (ry > y + h) break;
            Entry e = shown.get(i);
            boolean sel = e.key().equals(selected);
            boolean hover = mx >= x && mx < x + w - 6 && my >= ry && my < ry + ROW_H && my >= y && my < y + h;
            if (sel) Ui.selection(g, x + 2, ry, w - 10, ROW_H, true);
            else if (hover) g.fill(x + 2, ry, x + w - 8, ry + ROW_H, 0x30FFFFFF);
            Ui.item(g, e.icon(), x + 4, ry + 1);
            boolean mod = modified.test(e);
            int textX = x + (e.icon().isEmpty() ? 6 : 23);
            Ui.textFit(g, e.label(), textX, ry + 5, x + w - 10 - textX - (mod ? 8 : 0), sel ? Ui.WHITE : Ui.GRAY);
            if (mod) Ui.text(g, "*", x + w - 16, ry + 5, Ui.GOLD);
        }
        g.disableScissor();
        Ui.scrollbar(g, x + w - 6, y, h, shown.size() * ROW_H, scroll);
        if (shown.isEmpty()) Ui.centered(g, all.isEmpty() ? "Nothing here" : "No matches", x + w / 2, y + 8, Ui.DARK);
    }

    public boolean isOver(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean mouseClicked(double mx, double my) {
        if (!isOver(mx, my) || mx >= x + w - 6) return false;
        int i = (int) ((my - y + scroll) / ROW_H);
        if (i < 0 || i >= shown.size()) return true;
        Entry e = shown.get(i);
        selected = e.key();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        onSelect.accept(e);
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isOver(mx, my)) return false;
        scroll -= (int) (amount * ROW_H * 2);
        clampScroll();
        return true;
    }
}
