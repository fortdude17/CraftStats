package com.craftstats.common.gui;

import com.craftstats.common.propagate.PropagateManager;
import com.craftstats.common.stats.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class PropagateScreen extends Screen {

    private final ResourceLocation targetType;
    private final MobStats         newStats;
    private final MobStats         prevStats;
    private final PropagateManager.PropagatePreview preview;

    private EditBox confirmBox;
    private Button  executeBtn, cancelBtn;
    private boolean perInstance = false;

    private static final int PAD = 8;

    public PropagateScreen(ResourceLocation targetType, MobStats newStats, MobStats prevStats) {
        super(Component.literal("Propagate Changes"));
        this.targetType = targetType;
        this.newStats   = newStats;
        this.prevStats  = prevStats;
        this.preview    = PropagateManager.previewMob(targetType, newStats, prevStats);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int bottomY = this.height - 40;

        confirmBox = new EditBox(this.font, cx - 60, bottomY - 20, 120, 16,
                Component.literal("Type CONFIRM"));
        confirmBox.setMaxLength(10);
        addRenderableWidget(confirmBox);

        executeBtn = Button.builder(Component.literal("Execute"),
                b -> { if (confirmBox.getValue().equals("CONFIRM")) execute(); })
                .bounds(cx + PAD, bottomY, 60, 16).build();
        cancelBtn  = Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx - 64, bottomY, 60, 16).build();
        addRenderableWidget(executeBtn);
        addRenderableWidget(cancelBtn);

        Button instanceBtn = Button.builder(
                Component.literal(perInstance ? "Mode: Per-Instance" : "Mode: Global"),
                b -> { perInstance = !perInstance;
                    b.setMessage(Component.literal(perInstance ? "Mode: Per-Instance" : "Mode: Global")); })
                .bounds(PAD, PAD + 50, 110, 16).build();
        addRenderableWidget(instanceBtn);

        if (PropagateManager.canUndo()) {
            Button undoBtn = Button.builder(Component.literal("Undo Last"),
                    b -> { PropagateManager.undoLastPropagate(); onClose(); })
                    .bounds(PAD, PAD + 70, 90, 16).build();
            addRenderableWidget(undoBtn);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        g.fill(0, 0, this.width, this.height, 0xCC000000);

        g.drawCenteredString(this.font, "Propagate Changes", this.width / 2, PAD, 0xFFFFFFFF);
        g.drawString(this.font, "Target: " + targetType, PAD, PAD + 14, 0xFFCCCCCC, false);
        g.drawString(this.font, "Affected entities: " + preview.affectedCount(), PAD, PAD + 26, 0xFFFFAA44, false);
        g.drawString(this.font, "Type CONFIRM to proceed:", this.width / 2 - 60, this.height - 60, 0xFFFF4444, false);

        List<PropagateManager.DiffEntry> diffs = preview.diff();
        int rowY = PAD + 44;
        g.drawString(this.font, "Changes:", PAD + 120, rowY, 0xFFAAAAAA, false);
        rowY += 12;
        for (PropagateManager.DiffEntry d : diffs) {
            g.drawString(this.font, d.field() + ": " + d.oldValue() + " → " + d.newValue(),
                    PAD + 120, rowY, 0xFFFFFF44, false);
            rowY += 10;
            if (rowY > this.height - 80) break;
        }

        super.render(g, mx, my, delta);
    }

    private void execute() {
        PropagateManager.propagateMob(targetType, newStats, perInstance, prevStats);
        onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
