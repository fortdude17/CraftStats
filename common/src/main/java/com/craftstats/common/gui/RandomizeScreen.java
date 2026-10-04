package com.craftstats.common.gui;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.randomize.RandomizeManager;
import com.craftstats.common.stats.StatAccess;
import com.craftstats.common.stats.StatCatalog;
import com.craftstats.common.stats.StatDef;
import com.craftstats.common.stats.TargetType;
import com.craftstats.common.gui.editor.StatRow;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Roll random stats around the vanilla values, preview them, then use them in the editor. */
public class RandomizeScreen extends BaseScreen {

    private static final List<String> INTENSITIES = List.of("mild", "wild", "chaos");

    private final Screen parent;
    private final TargetType type;
    private final String targetLabel;
    private final Object vanilla;
    private final Consumer<Object> onResult;

    private EditBox seedBox;
    private Button intensityBtn;
    private Object rolled;
    private final List<String> preview = new ArrayList<>();

    public RandomizeScreen(Screen parent, TargetType type, String targetLabel, Object vanilla, Consumer<Object> onResult) {
        super(Component.literal("Randomize"));
        this.parent = parent;
        this.type = type;
        this.targetLabel = targetLabel;
        this.vanilla = vanilla;
        this.onResult = onResult;
    }

    @Override
    protected void init() {
        int w = Math.min(280, width - 20), x = (width - w) / 2;
        seedBox = addRenderableWidget(new EditBox(font, x, 40, w - 84, 20, Component.literal("Seed")));
        seedBox.setMaxLength(19);
        seedBox.setValue(String.valueOf(RandomizeManager.newSeed()));
        seedBox.setResponder(s -> roll());
        addRenderableWidget(Button.builder(Component.literal("New Seed"), b -> {
            seedBox.setValue(String.valueOf(RandomizeManager.newSeed()));
        }).bounds(x + w - 80, 40, 80, 20).build());
        intensityBtn = addRenderableWidget(Button.builder(intensityLabel(), b -> {
            var cfg = CraftStatsConfig.get();
            int i = INTENSITIES.indexOf(cfg.randomizeIntensity.toLowerCase());
            cfg.randomizeIntensity = INTENSITIES.get((i + 1) % INTENSITIES.size());
            CraftStatsConfig.save();
            b.setMessage(intensityLabel());
            roll();
        }).bounds(x, 64, w, 20).tooltip(Tooltip.create(Component.literal(
                "Mild: within 20% of vanilla\nWild: a quarter to three times vanilla\nChaos: anything goes"))).build());

        int bw = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Use These").withStyle(ChatFormatting.GREEN), b -> {
            if (rolled != null) onResult.accept(rolled);
            Minecraft.getInstance().setScreen(parent);
        }).bounds(x, height - 28, bw, 20).tooltip(Tooltip.create(Component.literal(
                "Puts the values in the editor. Press Apply there to use them."))).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x + bw + 4, height - 28, bw, 20).build());
        roll();
    }

    private Component intensityLabel() {
        return Component.literal("Intensity: " + CraftStatsConfig.get().randomizeIntensity);
    }

    private void roll() {
        preview.clear();
        long seed;
        try {
            seed = Long.parseLong(seedBox.getValue().trim());
        } catch (NumberFormatException e) {
            rolled = null;
            preview.add("The seed must be a whole number");
            return;
        }
        rolled = RandomizeManager.randomize(type, vanilla, seed);
        for (StatDef d : StatCatalog.all(type)) {
            if (!d.has(StatDef.RANDOM)) continue;
            Object before = StatAccess.get(vanilla, d.field()), after = StatAccess.get(rolled, d.field());
            if (after == null || after.equals(before)) continue;
            preview.add(d.label() + ": " + (before == null ? "-" : StatRow.format(before)) + " -> " + StatRow.format(after));
        }
        if (preview.isEmpty()) preview.add("Nothing changed with this seed");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        Ui.centered(g, "Randomize " + targetLabel, width / 2, 12, Ui.WHITE);
        Ui.centered(g, "Same seed + same intensity = same result", width / 2, 24, Ui.GRAY);
        int w = Math.min(280, width - 20), x = (width - w) / 2;
        int top = 92, bottom = height - 36;
        Ui.listPanel(g, x, top, w, bottom - top);
        int y = top + 4;
        for (String line : preview) {
            if (y + 10 > bottom) { Ui.text(g, "...", x + 6, y, Ui.GRAY); break; }
            Ui.textFit(g, line, x + 6, y, w - 12, line.contains("->") ? Ui.YELLOW : Ui.GRAY);
            y += 11;
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
