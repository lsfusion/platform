package platform.interop;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public class KeyStrokes {
    public static KeyStroke getEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    }

    public static KeyStroke getAltEnter() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
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

    public static KeyStroke getCtrlHome() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getCtrlEnd() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getF8() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);
    }

    public static KeyStroke getPrintKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getEditKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getXlsKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getNullKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
    }

    public static KeyStroke getRefreshKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
    }

    public static KeyStroke getApplyKeyStroke(int modifiers) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers);
    }

    public static KeyStroke getCancelKeyStroke(boolean single) {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, single ? 0 : InputEvent.ALT_DOWN_MASK);
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

    public static KeyStroke getForwardTraversalKeyStroke() {
        return getTab();
    }

    public static KeyStroke getBackwardTraversalKeyStroke() {
        return getShiftTab();
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

    public static KeyStroke getGroupCorrectionKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_MASK);
    }

    public static boolean isKeyEvent(EventObject event, int keyCode) {
        return (event instanceof KeyEvent && ((KeyEvent) event).getKeyCode() == keyCode);
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

    public static boolean isEnterEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_ENTER);
    }

    public static boolean isEscapeEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_ESCAPE);
    }

    public static boolean isGroupCorrectionEvent(EventObject event) {
        return isKeyEvent(event, KeyEvent.VK_F12);
    }

    public static boolean isObjectEditorDialogEvent(EventObject event) {
        return isBackSpaceEvent(event);
    }
}
