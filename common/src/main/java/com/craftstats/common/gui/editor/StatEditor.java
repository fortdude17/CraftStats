package com.craftstats.common.gui.editor;

import com.craftstats.common.gui.Ui;
import com.craftstats.common.stats.StatAccess;
import com.craftstats.common.stats.StatCatalog;
import com.craftstats.common.stats.StatDef;
import com.craftstats.common.stats.TargetType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The right side of the editor. Shows the stat categories as cards; clicking a card opens
 * its stats as rows of vanilla widgets. Typing in the search box lists matching stats from
 * every category.
 */
public class StatEditor {

    /** The screen that owns the widgets. */
    public interface Host {
        <T extends AbstractWidget> T addWidget(T widget);
        void removeWidget(AbstractWidget widget);
        void onEdited();
    }

    private static final int HEADER_H = 24;
    private static final int CARD_H = 36;
    private static final int GAP = 4;

    private final Host host;
    private int x, y, w, h;

    private TargetType type;
    private Object stats;
    private Object vanilla;
    private List<StatCatalog.Category> categories = List.of();
    private Predicate<StatDef> supported = d -> true;

    private int openCategory = -1;      // -1 = cards
    private String query = "";
    private int scroll;

    private final List<AbstractWidget> owned = new ArrayList<>();
    private final List<Button> cards = new ArrayList<>();
    private final List<ItemStack> cardIcons = new ArrayList<>();
    private final List<StatRow> rows = new ArrayList<>();
    private final List<List<AbstractWidget>> rowWidgets = new ArrayList<>();
    private Button back;
    private EditBox search;

    public StatEditor(Host host) {
        this.host = host;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public boolean hasTarget() { return stats != null; }

    /**
     * Shows a target. Keeps the open category when the type stays the same, so switching
     * between two zombies keeps you on the same page.
     */
    public void load(TargetType type, Object stats, Object vanilla, List<StatCatalog.Category> categories,
                     Predicate<StatDef> supported) {
        boolean sameType = type == this.type && categories.equals(this.categories);
        this.type = type;
        this.stats = stats;
        this.vanilla = vanilla;
        this.categories = categories;
        this.supported = supported;
        if (!sameType) { openCategory = -1; query = ""; }
        scroll = 0;
        rebuild();
    }

    public void clear() {
        stats = null;
        type = null;
        categories = List.of();
        rebuild();
    }

    /** Rebuild all widgets (after load, resize or a view change). */
    public void rebuild() {
        for (AbstractWidget wd : owned) host.removeWidget(wd);
        owned.clear();
        cards.clear();
        cardIcons.clear();
        rows.clear();
        rowWidgets.clear();
        back = null;
        search = null;
        if (stats == null) return;

        int searchW = Math.min(150, w / 2);
        search = own(new EditBox(Ui.font(), x + w - searchW, y + 2, searchW, 20, Component.literal("Search stats")));
        search.setHint(Component.literal("Search stats...").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(50);
        search.setValue(query);
        search.setResponder(text -> {
            String q = text.toLowerCase(Locale.ROOT).trim();
            if (q.equals(query)) return;
            query = q;
            scroll = 0;
            rebuildBody();
        });

        if (openCategory >= 0 || !query.isEmpty()) {
            back = own(Button.builder(Component.literal("<"), b -> {
                openCategory = -1;
                query = "";
                scroll = 0;
                rebuild();
            }).bounds(x, y + 2, 20, 20).tooltip(Tooltip.create(Component.literal("Back to categories"))).build());
        }
        rebuildBody();
    }

    private <T extends AbstractWidget> T own(T widget) {
        owned.add(widget);
        return host.addWidget(widget);
    }

    private void rebuildBody() {
        // Remove the body widgets but keep the header (search box keeps focus while typing).
        List<AbstractWidget> keep = new ArrayList<>();
        if (search != null) keep.add(search);
        if (back != null) keep.add(back);
        for (AbstractWidget wd : owned) if (!keep.contains(wd)) host.removeWidget(wd);
        owned.retainAll(keep);
        cards.clear();
        cardIcons.clear();
        rows.clear();
        rowWidgets.clear();

        boolean searching = !query.isEmpty();
        if (searching && back == null) {
            back = own(Button.builder(Component.literal("<"), b -> {
                openCategory = -1;
                query = "";
                scroll = 0;
                rebuild();
            }).bounds(x, y + 2, 20, 20).tooltip(Tooltip.create(Component.literal("Back to categories"))).build());
        }

        if (searching) {
            for (StatCatalog.Category c : categories)
                for (StatDef d : c.stats()) addRow(d, query);
        } else if (openCategory >= 0 && openCategory < categories.size()) {
            for (StatDef d : categories.get(openCategory).stats()) addRow(d, null);
        } else {
            for (int i = 0; i < categories.size(); i++) {
                final int idx = i;
                StatCatalog.Category c = categories.get(i);
                Button card = own(Button.builder(Component.empty(), b -> {
                    openCategory = idx;
                    scroll = 0;
                    rebuild();
                }).bounds(0, 0, 100, CARD_H).tooltip(Tooltip.create(Component.literal(cardTooltip(c)))).build());
                cards.add(card);
                cardIcons.add(Ui.icon(c.icon()));
            }
        }
        layout();
    }

    private String cardTooltip(StatCatalog.Category c) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (StatDef d : c.stats()) {
            if (shown++ == 8) { sb.append("\n..."); break; }
            sb.append(sb.isEmpty() ? "" : "\n").append(d.label());
        }
        return sb.toString();
    }

    private void addRow(StatDef d, String filter) {
        StatRow row = new StatRow(d, stats, vanilla, supported.test(d), host::onEdited);
        if (filter != null && !row.matches(filter)) return;
        rows.add(row);
        List<AbstractWidget> ws = row.createWidgets();
        for (AbstractWidget wd : ws) own(wd);
        rowWidgets.add(ws);
    }

    private int bodyY() { return y + HEADER_H; }
    private int bodyH() { return h - HEADER_H; }

    private int columns() { return Math.max(1, Math.min(3, (w + GAP) / (118 + GAP))); }

    private int contentHeight() {
        if (!cards.isEmpty()) {
            int rowsOfCards = (cards.size() + columns() - 1) / columns();
            return rowsOfCards * (CARD_H + GAP);
        }
        return rows.size() * StatRow.H;
    }

    private void layout() {
        int maxScroll = Math.max(0, contentHeight() - bodyH());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int top = bodyY(), bottom = bodyY() + bodyH();
        if (!cards.isEmpty()) {
            int cols = columns();
            int cardW = (w - 8 - (cols - 1) * GAP) / cols;
            for (int i = 0; i < cards.size(); i++) {
                Button b = cards.get(i);
                int cx = x + (i % cols) * (cardW + GAP);
                int cy = top + (i / cols) * (CARD_H + GAP) - scroll;
                b.setX(cx);
                b.setY(cy);
                b.setWidth(cardW);
                b.visible = cy >= top && cy + CARD_H <= bottom;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            int ry = top + i * StatRow.H - scroll;
            rows.get(i).layout(x, ry, w - 10, ry >= top && ry + StatRow.H <= bottom);
        }
    }

    public void render(GuiGraphics g, int mx, int my) {
        if (stats == null) {
            Ui.centered(g, "Pick something on the left to edit it", x + w / 2, y + h / 2 - 4, Ui.GRAY);
            return;
        }
        // Header title
        int titleX = back != null ? x + 26 : x;
        String title = !query.isEmpty() ? rows.size() + " matching stats"
                : openCategory >= 0 ? categories.get(openCategory).name()
                : categories.stream().mapToInt(c -> c.stats().size()).sum() + " stats";
        int titleMax = (search != null ? search.getX() : x + w) - titleX - 6;
        Ui.textFit(g, title, titleX, y + 8, titleMax, Ui.WHITE);

        int top = bodyY(), bottom = bodyY() + bodyH();
        if (!cards.isEmpty()) {
            for (int i = 0; i < cards.size(); i++) {
                Button b = cards.get(i);
                if (!b.visible) continue;
                StatCatalog.Category c = categories.get(i);
                Ui.item(g, cardIcons.get(i), b.getX() + 6, b.getY() + (CARD_H - 16) / 2);
                int tx = b.getX() + 28, tw = b.getWidth() - 32;
                Ui.textFit(g, c.name(), tx, b.getY() + 7, tw, Ui.WHITE);
                int changed = StatAccess.countChanged(type, stats, c.stats());
                String sub = c.stats().size() + " stats";
                Ui.textFit(g, sub, tx, b.getY() + 20, tw, Ui.GRAY);
                if (changed > 0) {
                    String ch = changed + " changed";
                    int cw = Ui.font().width(ch);
                    if (Ui.font().width(sub) + cw + 8 <= tw) Ui.text(g, ch, b.getX() + b.getWidth() - cw - 5, b.getY() + 20, Ui.YELLOW);
                    else Ui.outline(g, b.getX(), b.getY(), b.getWidth(), CARD_H, 0xFFFFFF55);
                }
            }
        }
        for (StatRow row : rows) {
            int ry = rowY(row);
            if (ry < top || ry + StatRow.H > bottom) continue;
            row.renderLabel(g);
        }
        if (cards.isEmpty() && rows.isEmpty())
            Ui.centered(g, "No stats match \"" + query + "\"", x + w / 2, top + 10, Ui.DARK);
        Ui.scrollbar(g, x + w - 6, top, bodyH(), contentHeight(), scroll);
    }

    private int rowY(StatRow row) {
        return bodyY() + rows.indexOf(row) * StatRow.H - scroll;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (mx < x || mx >= x + w || my < bodyY() || my >= bodyY() + bodyH()) return false;
        int step = cards.isEmpty() ? StatRow.H : CARD_H + GAP;
        scroll -= (int) Math.signum(amount) * step;
        layout();
        return true;
    }

    /** Right-click on a toggle or choice button cycles backwards. */
    public boolean mouseClickedRight(double mx, double my) {
        for (int i = 0; i < rows.size(); i++) {
            for (AbstractWidget wd : rowWidgets.get(i)) {
                if (wd instanceof Button b && b.visible && b.active && b.isMouseOver(mx, my)
                        && rows.get(i).def.kind() == StatDef.Kind.CHOICE) {
                    rows.get(i).onPress(true);
                    return true;
                }
            }
        }
        return false;
    }

    /** Called when the stats object was changed from outside (preset, paste, randomize). */
    public void replaceStats(Object newStats) {
        this.stats = newStats;
        rebuildBody();
    }

    /** Opens a category card (-1 = back to the cards). Used by the client smoke test. */
    public void openCategory(int index) {
        openCategory = index;
        query = "";
        scroll = 0;
        rebuild();
    }

    public int categoryCount() { return categories.size(); }

    public int rowCount() { return rows.size(); }

    public boolean isSearchFocused() {
        return search != null && search.isFocused();
    }
}
