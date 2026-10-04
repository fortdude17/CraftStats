package com.craftstats.common.mixin;

import com.craftstats.common.client.ClientHooks;
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
        if (!ClientHooks.mayEdit() || this.children().isEmpty()) return;
        int bottom = this.height / 4 + 8;
        for (var child : this.children())
            if (child instanceof AbstractWidget w) bottom = Math.max(bottom, w.getY() + w.getHeight());
        if (bottom + 24 > this.height) return;
        addRenderableWidget(Button.builder(Component.literal("CraftStats"),
                        b -> Minecraft.getInstance().setScreen(new CraftStatsScreen()))
                .bounds(this.width / 2 - 102, bottom + 4, 204, 20)
                .build());
    }
}
