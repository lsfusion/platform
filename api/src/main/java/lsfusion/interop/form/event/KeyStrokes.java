package lsfusion.interop.form.event;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public class KeyStrokes {
    public static KeyStroke getEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    }

    public static KeyStroke getEnter(int modifiers) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers);
    }

    public static KeyStroke getShiftEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
    }

    public static KeyStroke getCtrlEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getEscape() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    }

    public static KeyStroke getTab() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    }

    public static KeyStroke getShiftTab() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
    }

    public static KeyStroke getCtrlTab() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getCtrlShiftTab() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    }

    public static KeyStroke getCtrlHome() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getCtrlEnd() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getF3() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
    }

    public static KeyStroke getF6() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
    }

    public static KeyStroke getF8() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);
    }

    public static KeyEvent createAddUserFilterKeyEvent(Component component) {
        return new KeyEvent(component, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED);
    }
    
    public static KeyStroke getFilterKeyStroke(int modifier) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F3, modifier);
    }

    public static KeyStroke getRemoveFiltersKeyStroke() {
        return getEscape();
    }

    public static KeyStroke getAddActionKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
    }

    public static KeyStroke getEditActionKeyStroke() {
        return getEnter();
    }

    public static KeyStroke getDeleteActionKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getGroupCorrectionKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);
    }

    public static boolean isEditObjectEvent(EventObject event, boolean hasEditObjectAction, boolean hasChangeAction) {
        return hasEditObjectAction && (
                isKeyEvent(event, KeyEvent.VK_F9) ||
                (!hasChangeAction && MouseStrokes.isDblClickEvent(event)) ||
                (event instanceof InputEvent && ((InputEvent) event).isControlDown() && MouseStrokes.isDownEvent(event))); // ctrl doesn't work for now since it is used for a cell selection
    }

    public static boolean isKeyEvent(EventObject event, int keyCode) {
        return (event instanceof KeyEvent && ((KeyEvent) event).getKeyCode() == keyCode);
    }
    
    public static boolean isEnterEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_ENTER);
    }

    public static boolean isShiftEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_SHIFT);
    }

    public static boolean isSpaceEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_SPACE);
    }

    public static boolean isBackSpaceEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_BACK_SPACE);
    }

    public static boolean isDeleteEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_DELETE);
    }

    public static boolean isCharUndefinedEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.CHAR_UNDEFINED);
    }

    public static boolean isEscapeEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_ESCAPE);
    }
    
    public static boolean isTabEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_TAB);
    }

    public static boolean isDigitKeyEvent(EventObject event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent)event;
            int keyCode = keyEvent.getKeyCode();
            return (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 && !keyEvent.isShiftDown()) 
                    || (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9);
        }
        return false;
    }
    
    public static boolean isMinusKeyEvent(EventObject event) {
        return event instanceof KeyEvent && (isKeyEvent(event, KeyEvent.VK_MINUS) || isKeyEvent(event, 109));
    }

    public static boolean isSuitableStartFilteringEvent(EventObject event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            return isSuitableEditKeyEvent(event) &&
                    !isBackSpaceEvent(keyEvent) &&
                    !isDeleteEvent(keyEvent) &&
                    !isEnterEvent(keyEvent);
        }
        return false;
    }

    public static boolean isSuitableEditKeyEventForRegularFilter(EventObject event) {
        return isSuitableEditKeyEvent(event) && !isEnterEvent(event);
    }

    public static boolean isSuitableDialogFilteringEvent(EventObject event) {
        return isSuitableStartFilteringEvent(event) && !isSpaceEvent(event);
    }

    public static boolean isSuitableEditKeyEvent(EventObject event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            //будем считать, что если нажата кнопка ALT или CTRL, то явно пользователь не хочет вводить текст
            return !keyEvent.isActionKey() &&
                   !keyEvent.isAltDown() &&
                   !keyEvent.isControlDown() &&
                   !KeyStrokes.isShiftEvent(keyEvent) &&
                   !KeyStrokes.isCharUndefinedEvent(keyEvent) &&
                   !KeyStrokes.isEscapeEvent(keyEvent) &&
                   !KeyStrokes.isTabEvent(keyEvent);
        }
        return false;
    }

    public static boolean isSuitableNumberEditEvent(EventObject event) {
        return isSuitableEditKeyEvent(event) && (
                isDigitKeyEvent(event) ||
                isMinusKeyEvent(event) ||
                isDeleteEvent(event) ||
                isBackSpaceEvent(event)
        );
    }

    public static KeyStroke getKeyStrokeForEvent(KeyEvent e) {
        return e.getID() == KeyEvent.KEY_TYPED
               ? KeyStroke.getKeyStroke(e.getKeyChar())
               : KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), e.getID() == KeyEvent.KEY_RELEASED);
    }

    public static boolean isChangeAppendKeyEvent(EventObject event) {
        return event instanceof ActionEvent;
    }
}
