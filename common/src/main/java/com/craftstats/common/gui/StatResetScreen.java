package com.craftstats.common.gui;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatResetScreen extends Screen {

    private static final int ROW_H   = 22;
    private static final int PAD     = 6;
    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 30;

    private final Screen parent;

    record WorldEntry(String worldName, Path statsFile, int entryCount) {}
    private final List<WorldEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private int msgTimer      = 0;
    private String lastMsg    = "";

    private Button deleteSelectedBtn, deleteAllBtn, closeBtn;

    public StatResetScreen(Screen parent) {
        super(Component.literal("CraftStats — Emergency Reset"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scanWorlds();

        int bh = 18;
        int bw1 = 200, bw2 = 180, bw3 = 60;
        int totalW = bw1 + bw2 + bw3 + PAD * 2;
        int bx = (this.width - totalW) / 2;
        int by = this.height - FOOTER_H - PAD + (FOOTER_H - bh) / 2;

        deleteSelectedBtn = Button.builder(Component.literal("Delete Selected World's Stats"),
                b -> deleteSelected())
                .bounds(bx, by, bw1, bh).build();
        deleteAllBtn = Button.builder(Component.literal("Delete ALL Worlds' Stats"),
                b -> deleteAll())
                .bounds(bx + bw1 + PAD, by, bw2, bh).build();
        closeBtn = Button.builder(Component.literal("Close"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(bx + bw1 + bw2 + PAD * 2, by, bw3, bh).build();

        addRenderableWidget(deleteSelectedBtn);
        addRenderableWidget(deleteAllBtn);
        addRenderableWidget(closeBtn);
        refreshButtonState();
    }

    private void scanWorlds() {
        entries.clear();
        try {
            Path savesDir = Minecraft.getInstance().gameDirectory.toPath().resolve("saves");
            if (!Files.exists(savesDir)) return;
            try (var stream = Files.list(savesDir)) {
                stream.filter(Files::isDirectory).forEach(worldDir -> {
                    Path statsFile = worldDir.resolve("craftstats").resolve("stats.json");
                    if (Files.exists(statsFile))
                        entries.add(new WorldEntry(
                                worldDir.getFileName().toString(), statsFile, countEntries(statsFile)));
                });
            }
            entries.sort(Comparator.comparing(WorldEntry::worldName));
        } catch (Exception ignored) {}
        if (selectedIndex >= entries.size()) selectedIndex = entries.size() - 1;
        refreshButtonState();
    }

    private int countEntries(Path statsFile) {
        try {
            var root = JsonParser.parseString(Files.readString(statsFile)).getAsJsonObject();
            int n = 0;
            for (String key : new String[]{"mobs","blocks","items","players","block_positions"})
                if (root.has(key)) n += root.getAsJsonObject(key).size();
            return n;
        } catch (Exception e) { return 0; }
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;
        WorldEntry e = entries.get(selectedIndex);
        try {
            Files.deleteIfExists(e.statsFile());
            flash("§aDeleted stats for '" + e.worldName() + "'.");
        } catch (IOException ex) {
            flash("§cError: " + ex.getMessage());
        }
        scanWorlds();
    }

    private void deleteAll() {
        int count = 0;
        for (WorldEntry e : new ArrayList<>(entries)) {
            try { Files.deleteIfExists(e.statsFile()); count++; }
            catch (IOException ignored) {}
        }
        flash("§aCleared stats for " + count + " world(s).");
        selectedIndex = -1;
        scanWorlds();
    }

    private void flash(String msg) { lastMsg = msg; msgTimer = 100; }

    private void refreshButtonState() {
        if (deleteSelectedBtn != null)
            deleteSelectedBtn.active = selectedIndex >= 0 && selectedIndex < entries.size();
        if (deleteAllBtn != null)
            deleteAllBtn.active = !entries.isEmpty();
    }

    @Override public void tick() { if (msgTimer > 0) msgTimer--; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int listX   = PAD * 2;
        int listY   = HEADER_H + PAD;
        int footerY = this.height - FOOTER_H - PAD;
        int listH   = footerY - listY - PAD;
        int listW   = this.width - PAD * 4;

        g.fill(0, 0, this.width, this.height, 0xFF0D0D1C);

        g.fill(0, 0, this.width, HEADER_H, 0xFF0F3460);
        drawBorder(g, 0, 0, this.width, HEADER_H, 0xFF3355AA);
        g.drawCenteredString(font, "§b§lCraftStats — Emergency Reset", this.width / 2, 10, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7Delete mod data before loading a world to fix crashes / unplayable states",
                this.width / 2, 24, 0xFFAAAAAA);
        g.drawCenteredString(font, "§8Click a row to select, then use the buttons below",
                this.width / 2, 36, 0xFF555555);

        g.fill(listX, listY, listX + listW, listY + listH, 0xFF16213E);
        drawBorder(g, listX, listY, listX + listW, listY + listH, 0xFF3355AA);

        if (entries.isEmpty()) {
            g.drawCenteredString(font, "§7No CraftStats data found in any world save.",
                    this.width / 2, listY + listH / 2 - 4, 0xFF666666);
            g.drawCenteredString(font, "§8(All saves are clean — no mod data to remove)",
                    this.width / 2, listY + listH / 2 + 10, 0xFF444444);
        } else {
            int rowY = listY + PAD;
            for (int i = 0; i < entries.size(); i++) {
                WorldEntry e = entries.get(i);
                boolean sel = i == selectedIndex;
                boolean hov = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_H;
                if (sel)      g.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_H, 0xFF1E3A8A);
                else if (hov) g.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_H, 0xFF1A2A50);
                String name = e.worldName().length() > 40 ? e.worldName().substring(0,39) + "…" : e.worldName();
                String info = e.entryCount() + " custom entr" + (e.entryCount() == 1 ? "y" : "ies");
                g.drawString(font, (sel ? "§e▶ §f" : "§7  §f") + name, listX + 6, rowY + (ROW_H - 8) / 2, 0xFFFFFFFF, false);
                g.drawString(font, "§7" + info, listX + listW - font.width(info) - 8, rowY + (ROW_H - 8) / 2, 0xFFAAAAAA, false);
                if (i < entries.size() - 1)
                    g.fill(listX + 4, rowY + ROW_H - 1, listX + listW - 4, rowY + ROW_H, 0xFF222244);
                rowY += ROW_H;
            }
        }

        g.fill(0, footerY, this.width, this.height, 0xFF0F3460);
        drawBorder(g, 0, footerY, this.width, this.height, 0xFF3355AA);
        if (msgTimer > 0)
            g.drawCenteredString(font, lastMsg, this.width / 2, footerY - 14, 0xFFFFFFFF);

        super.render(g, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        int listX   = PAD * 2;
        int listY   = HEADER_H + PAD;
        int footerY = this.height - FOOTER_H - PAD;
        int listW   = this.width - PAD * 4;
        int listH   = footerY - listY - PAD;
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            int idx = ((int)(my - listY) - PAD) / ROW_H;
            if (idx >= 0 && idx < entries.size()) {
                selectedIndex = idx;
                refreshButtonState();
                return true;
            }
        }
        return false;
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1, y1, x2, y1 + 1, c); g.fill(x1, y2 - 1, x2, y2, c);
        g.fill(x1, y1, x1 + 1, y2, c); g.fill(x2 - 1, y1, x2, y2, c);
    }

    @Override public boolean isPauseScreen() { return false; }
}
