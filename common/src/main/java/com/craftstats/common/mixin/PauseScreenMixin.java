package com.craftstats.common.mixin;

import com.craftstats.common.gui.CraftStatsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void craftstats$addButton(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        boolean singleplayer = mc.getSingleplayerServer() != null;
        boolean canUse = singleplayer
                || (mc.player != null && (mc.player.hasPermissions(2) || mc.player.isCreative()));
        if (!canUse) return;

        int maxBottom = this.height / 4 + 8;
        for (var child : this.children()) {
            if (child instanceof AbstractWidget w)
                maxBottom = Math.max(maxBottom, w.getY() + w.getHeight());
        }
        addRenderableWidget(Button.builder(
                Component.literal("CraftStats"),
                b -> Minecraft.getInstance().setScreen(new CraftStatsScreen()))
                .bounds(this.width / 2 - 100, maxBottom + 4, 200, 20)
                .build());
    }
}
