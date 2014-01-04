package lsfusion.interop;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EventObject;

@SuppressWarnings("MagicConstant")
public class KeyStrokes {
    public static KeyStroke getEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    }

    public static KeyStroke getEnter(int modifiers) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers);
    }

    public static KeyStroke getAltEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
    }

    public static KeyStroke getEscape() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    }

    public static KeyStroke getBackspace() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
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

    public static KeyStroke getF6() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
    }

    public static KeyStroke getF8() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);
    }

    //---- form buttons keystrokes
    public static KeyStroke getApplyKeyStroke() {
        return getEnter(InputEvent.ALT_DOWN_MASK);
    }

    public static KeyStroke getCancelKeyStroke() {
        return getEscape(InputEvent.SHIFT_DOWN_MASK);
    }

    public static KeyStroke getCloseKeyStroke() {
        return getEscape(0);
    }

    public static KeyStroke getEditKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getNullKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
    }

    public static KeyStroke getOkKeyStroke() {
        return getEnter(InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getPrintKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getRefreshKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getXlsKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
    }
    //----

    public static KeyStroke getEscape(int modifier) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, modifier);
    }

    public static KeyStroke getFilterKeyStroke(int modifier) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F2, modifier);
    }

    public static KeyStroke getFindKeyStroke(int modifier) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F3, modifier);
    }

    public static KeyStroke getSwitchClassViewKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getRemoveFiltersKeyStroke() {
        return getEscape();
    }

    public static KeyStroke getSelectionFilterKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getSelectionPropertyKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getAddActionPropertyKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
    }

    public static KeyStroke getEditActionPropertyKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
    }

    public static KeyStroke getDeleteActionPropertyKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getImportActionPropertyKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getGroupCorrectionDialogKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_MASK);
    }

    public static KeyStroke getGroupCorrectionKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);
    }

    public static boolean isEditObjectEvent(EventObject event) {
        return isBackSpaceEvent(event);
    }

    public static boolean isKeyEvent(EventObject event, int keyCode) {
        return (event instanceof KeyEvent && ((KeyEvent) event).getKeyCode() == keyCode);
    }
    
    public static boolean isEnterEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_ENTER);
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

    public static boolean isDigitKeyEvent(EventObject event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent)event;
            int keyCode = keyEvent.getKeyCode();
            return keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 || keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9;
        }
        return false;
    }

    public static boolean isSuitableStartFilteringEvent(EventObject event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            return isSuitableEditKeyEvent(event) &&
                    !isBackSpaceEvent(keyEvent) &&
                    !isDeleteEvent(keyEvent);
        }
        return false;
    }

    public static boolean isSuitableDialogFilteringEvent(EventObject event) {
        return isSuitableStartFilteringEvent(event) && !isSpaceEvent(event);
    }

    public static boolean isSuitableEditKeyEvent(EventObject event) {
        if ((event instanceof KeyEvent)) {
            KeyEvent keyEvent = (KeyEvent) event;
            //будем считать, что если нажата кнопка ALT или CTRL, то явно пользователь не хочет вводить текст
            return !keyEvent.isActionKey() &&
                   !keyEvent.isAltDown() &&
                   !keyEvent.isControlDown() &&
                   !KeyStrokes.isCharUndefinedEvent(keyEvent) &&
                   !KeyStrokes.isEscapeEvent(keyEvent);
        }
        return false;
    }

    public static boolean isSuitableNumberEditEvent(EventObject event) {
        return isSuitableEditKeyEvent(event) && (
                isDigitKeyEvent(event) ||
                isDeleteEvent(event) ||
                isBackSpaceEvent(event)
        );
    }

    public static KeyStroke getKeyStrokeForEvent(KeyEvent e) {
        return e.getID() == KeyEvent.KEY_TYPED
               ? KeyStroke.getKeyStroke(e.getKeyChar())
               : KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), e.getID() == KeyEvent.KEY_RELEASED);
    }
}
