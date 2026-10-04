package com.craftstats.common.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.9
//$ import net.minecraft.client.input.CharacterEvent;
//$ import net.minecraft.client.input.KeyEvent;
//$ import net.minecraft.client.input.MouseButtonEvent;
//#endif

/**
 * Base for CraftStats screens. Subclasses override the version-independent
 * {@code onMouseClicked/onKeyPressed/onCharTyped}; returning false passes the event on to
 * vanilla widget handling.
 */
public abstract class BaseScreen extends Screen {

    protected BaseScreen(Component title) {
        super(title);
    }

    protected boolean onMouseClicked(double x, double y, int button) { return false; }
    protected boolean onKeyPressed(int key, int scanCode, int modifiers) { return false; }
    protected boolean onCharTyped(char c, int modifiers) { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    //#if MC >= 1.21.9
    //$ @Override
    //$ public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //$     return onMouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    //$ }
    //$
    //$ @Override
    //$ public boolean keyPressed(KeyEvent event) {
    //$     return onKeyPressed(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event);
    //$ }
    //$
    //$ @Override
    //$ public boolean charTyped(CharacterEvent event) {
    //$     return onCharTyped((char) event.codepoint(), event.modifiers()) || super.charTyped(event);
    //$ }
    //#else
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return onMouseClicked(x, y, button) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return onKeyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return onCharTyped(c, modifiers) || super.charTyped(c, modifiers);
    }
    //#endif
}
