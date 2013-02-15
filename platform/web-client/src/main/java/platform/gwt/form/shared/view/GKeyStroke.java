package platform.gwt.form.shared.view;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

import java.io.Serializable;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static com.google.gwt.dom.client.BrowserEvents.KEYPRESS;
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

    public static final int KEY_INSERT = 45;

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

    public static GKeyStroke getKeyStroke(NativeEvent e) {
        assert BrowserEvents.KEYDOWN.equals(e.getType()) ||
                BrowserEvents.KEYPRESS.equals(e.getType()) ||
                BrowserEvents.KEYUP.equals(e.getType());
        return new GKeyStroke(e.getKeyCode(), e.getAltKey(), e.getCtrlKey(), e.getShiftKey());
    }

    public static boolean isCommonEditKeyEvent(NativeEvent event) {
        if (event.getCtrlKey() || event.getAltKey() || event.getMetaKey()) {
            return false;
        }

        String eventType = event.getType();
        int keyCode = event.getKeyCode();
        if (KEYPRESS.equals(eventType)) {
            return isChangeKeyCode(keyCode);
        } else if (KEYDOWN.equals(eventType)) {
            return keyCode == KeyCodes.KEY_DELETE;
        }
        return false;
    }

    public static boolean isPossibleEditKeyEvent(NativeEvent event) {
        String eventType = event.getType();
        int keyCode = event.getKeyCode();
        if (KEYDOWN.equals(eventType)) {
            return isChangeKeyCode(keyCode) ||
                    keyCode == KEY_DELETE ||
                    keyCode == KEY_BACKSPACE ||
                    keyCode == KEY_INSERT ||
                    keyCode == KEY_ESCAPE ||
                    keyCode == KEY_ENTER ||
                    (KEY_F1 <= keyCode && keyCode <= KEY_F12);
        } else if (KEYPRESS.equals(eventType)) {
            return !event.getCtrlKey() && !event.getAltKey() && !event.getMetaKey() && isChangeKeyCode(keyCode);
        }
        return false;
    }

    public static boolean isChangeKeyCode(int keyCode) {
        return keyCode != KEY_ENTER
                && keyCode != KEY_ESCAPE
                && keyCode != KEY_TAB
                && keyCode != KEY_HOME
                && keyCode != KEY_END
                && keyCode != KEY_PAGEUP
                && keyCode != KEY_PAGEDOWN
                && keyCode != KEY_LEFT
                && keyCode != KEY_RIGHT
                && keyCode != KEY_UP
                && keyCode != KEY_DOWN;
    }
}
