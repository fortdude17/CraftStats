package com.craftstats.common.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;
//#if MC >= 1.21.9
//$ import net.minecraft.client.input.CharacterEvent;
//$ import net.minecraft.client.input.KeyEvent;
//$ import net.minecraft.client.input.MouseButtonEvent;
//$ import net.minecraft.client.input.MouseButtonInfo;
//#endif

/**
 * Forwards plain mouse/keyboard input to vanilla widgets. Minecraft 1.21.9 replaced the
 * (x, y, button) style methods with event objects; this hides the difference.
 */
public final class UiInput {

    private UiInput() {}

    public static boolean click(GuiEventListener widget, double x, double y, int button) {
        //#if MC >= 1.21.9
        //$ return widget.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0)), false);
        //#else
        return widget.mouseClicked(x, y, button);
        //#endif
    }

    public static boolean key(GuiEventListener widget, int key, int scanCode, int modifiers) {
        //#if MC >= 1.21.9
        //$ return widget.keyPressed(new KeyEvent(key, scanCode, modifiers));
        //#else
        return widget.keyPressed(key, scanCode, modifiers);
        //#endif
    }

    public static boolean chr(GuiEventListener widget, char c, int modifiers) {
        //#if MC >= 1.21.9
        //$ return widget.charTyped(new CharacterEvent(c, modifiers));
        //#else
        return widget.charTyped(c, modifiers);
        //#endif
    }
}
