package com.craftstats.common.mixin;

import com.craftstats.common.gui.StatResetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void craftstats$addButton(CallbackInfo ci) {
        int bw = 120, bh = 20;

        addRenderableWidget(Button.builder(
                Component.literal("CraftStats"),
                b -> Minecraft.getInstance().setScreen(new StatResetScreen(this)))
                .bounds(4, this.height - bh - 30, bw, bh)
                .build());
    }
}
