package lsfusion.gwt.client.form.event;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;

import java.io.Serializable;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.event.dom.client.KeyCodes.*;

public class GKeyStroke implements Serializable {
    public static final int KEY_F1 = 112;
    public static final int KEY_F2 = KEY_F1 + 1;
    public static final int KEY_F3 = KEY_F1 + 2;
    public static final int KEY_F4 = KEY_F1 + 3;
    public static final int KEY_F5 = KEY_F1 + 4;
    public static final int KEY_F6 = KEY_F1 + 5;
    public static final int KEY_F7 = KEY_F1 + 6;
    public static final int KEY_F8 = KEY_F1 + 7;
    public static final int KEY_F9 = KEY_F1 + 8;
    public static final int KEY_F10 = KEY_F1 + 9;
    public static final int KEY_F11 = KEY_F1 + 10;
    public static final int KEY_F12 = KEY_F1 + 11;

    public static final int KEY_MINUS = 45;
    
    public static final int KEY_0 = 48;
    public static final int KEY_9 = 57;

    public static final int KEY_SPACE = 32;
    public static final int KEY_INSERT = 45;

    public static final int KEY_C = 67;
    public static final int KEY_R = 82;
    public static final int KEY_V = 86;

    public int keyCode;
    public boolean altPressed;
    public boolean ctrlPressed;
    public boolean shiftPressed;

    public GKeyStroke() {}

    public GKeyStroke(int keyCode) {
        this.keyCode = keyCode;
    }

    public GKeyStroke(int keyCode, boolean altPressed, boolean ctrlPressed, boolean shiftPressed) {
        this.keyCode = keyCode;
        this.altPressed = altPressed;
        this.ctrlPressed = ctrlPressed;
        this.shiftPressed = shiftPressed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GKeyStroke)) return false;

        GKeyStroke key = (GKeyStroke) o;

        return altPressed == key.altPressed &&
                ctrlPressed == key.ctrlPressed &&
                keyCode == key.keyCode
                && shiftPressed == key.shiftPressed;
    }

    @Override
    public int hashCode() {
        int result = keyCode;
        result = 31 * result + (altPressed ? 1 : 0);
        result = 31 * result + (ctrlPressed ? 1 : 0);
        result = 31 * result + (shiftPressed ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return (altPressed ? "alt " : "") +
               (ctrlPressed ? "ctrl " : "") +
               (shiftPressed ? "shift " : "") + getKeyText();
    }

    private String getKeyText() {

        String keyString;
        switch (keyCode) {
            case KEY_BACKSPACE: keyString = "BACKSPACE"; break;
            case KEY_DELETE: keyString = "DELETE"; break;
            case KEY_ENTER: keyString = "ENTER"; break;
            case KEY_ESCAPE: keyString = "ESCAPE"; break;
            case KEY_TAB: keyString = "TAB"; break;
            case KEY_INSERT: keyString = "INSERT"; break;
            case KEY_F1: keyString = "F1"; break;
            case KEY_F1+1: keyString = "F2"; break;
            case KEY_F1+2: keyString = "F3"; break;
            case KEY_F1+3: keyString = "F4"; break;
            case KEY_F1+4: keyString = "F5"; break;
            case KEY_F1+5: keyString = "F6"; break;
            case KEY_F1+6: keyString = "F7"; break;
            case KEY_F1+7: keyString = "F8"; break;
            case KEY_F1+8: keyString = "F9"; break;
            case KEY_F1+9: keyString = "F10"; break;
            case KEY_F1+10: keyString = "F11"; break;
            case KEY_F12: keyString = "F12"; break;
            default:
                if (32 <= keyCode && keyCode <= 127) {
                    keyString = String.valueOf((char)(keyCode));
                } else {
                    keyString = String.valueOf(keyCode);
                }
        }
        return keyString;
    }

    public boolean isEvent(Event event) {
        return BrowserEvents.KEYDOWN.equals(event.getType()) && event.getKeyCode() == keyCode && event.getShiftKey() == shiftPressed && event.getAltKey() == altPressed && event.getCtrlKey() == ctrlPressed;
    }

    public static GKeyStroke getKeyStroke(NativeEvent e) {
        assert BrowserEvents.KEYDOWN.equals(e.getType());
        return new GKeyStroke(e.getKeyCode(), e.getAltKey(), e.getCtrlKey(), e.getShiftKey());
    }

    public static boolean isSpaceKeyEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) && event.getKeyCode() == KEY_SPACE;
    }

    public static boolean isEnterKeyEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) && event.getKeyCode() == KEY_ENTER;
    }

    public static boolean isEscapeKeyEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) && event.getKeyCode() == KEY_ESCAPE;
    }

    public static boolean isEditObjectEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) && event.getKeyCode() == KEY_F9;
    }

    public static boolean isGroupChangeKeyEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) && event.getKeyCode() == KEY_F12;
    }

    public static boolean isCharModifyKeyEvent(NativeEvent event, GEditBindingMap.EditEventFilter editEventFilter) {
        return ((isCharAddKeyEvent(event) && (editEventFilter == null || editEventFilter.accept(event))) || isCharDeleteKeyEvent(event));
    }

    // what events should be stealed by TextBasedEditor
    public static boolean isCharNavigateKeyEvent(NativeEvent event) {
        if (KEYDOWN.equals(event.getType())) {
            int keyCode = event.getKeyCode();
            return keyCode == KEY_LEFT || keyCode == KEY_RIGHT || keyCode == KEY_END || keyCode == KEY_HOME;
        }
        return false;
    }

    public static boolean isPlainKeyEvent(NativeEvent event) {
        return !(event.getCtrlKey() || event.getAltKey() || event.getMetaKey());
    }
    public static boolean isCharDeleteKeyEvent(NativeEvent event) {
        if (isPlainKeyEvent(event) && KEYDOWN.equals(event.getType())) {
            int keyCode = event.getKeyCode();
            return keyCode == KEY_DELETE || keyCode == KEY_BACKSPACE;
        }
        return false;
    }

    public static boolean isCharAddKeyEvent(NativeEvent event) {
        if(KEYPRESS.equals(event.getType()) && isPlainKeyEvent(event)) {
            int keyCode = event.getKeyCode();
            return keyCode != KEY_ENTER && keyCode != KEY_ESCAPE && event.getCharCode() != 0;
        }
        return false;
    }

    public static boolean isKeyEvent(Event event) {
        return KEYPRESS.equals(event.getType()) || KEYDOWN.equals(event.getType());
    }

    public static boolean isNumberAddKeyEvent(NativeEvent event) {
        assert isCharAddKeyEvent(event);

        int charCode = event.getCharCode();
        return ((charCode >= KEY_0 && charCode <= KEY_9) || charCode == KEY_MINUS);
    }

    public static boolean isPossibleStartFilteringEvent(NativeEvent event) {
        return isCharAddKeyEvent(event);
    }

    public static boolean isCopyToClipboardEvent(NativeEvent event) {
        return KEYDOWN.equals(event.getType()) &&
                ((event.getKeyCode() == KEY_C && event.getCtrlKey()) ||
                (event.getKeyCode() == KEY_INSERT && event.getCtrlKey()));
    }

    public static boolean isPasteFromClipboardEvent(Event event) {
//        return (KEYDOWN.equals(event.getType()) &&
//                ((event.getKeyCode() == KEY_V && event.getCtrlKey()) ||
//                (event.getKeyCode() == KEY_INSERT && event.getShiftKey())))
//                || event.getTypeInt() == Event.ONPASTE;
        return event.getTypeInt() == Event.ONPASTE;
    }
}
