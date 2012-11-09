package platform.gwt.form.shared.view;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.google.gwt.event.dom.client.KeyCodes.*;

public class GEditBindingMap implements Serializable {
    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String CHANGE_WYS = "change_wys";

    public static final int KEY_F1 = 112;
    public static final int KEY_F12 = KEY_F1 + 11;
    public static final int KEY_INSERT = 45;

    private HashMap<Key, String> keyBindingMap;
    private LinkedHashMap<String, String> contextMenuBindingMap;
    private String mouseBinding;

    public GEditBindingMap() {
    }

    public GEditBindingMap(HashMap<Key, String> keyBindingMap, LinkedHashMap<String, String> contextMenuBindingMap, String mouseBinding) {
        this.keyBindingMap = keyBindingMap;
        this.contextMenuBindingMap = contextMenuBindingMap;
        this.mouseBinding = mouseBinding;
    }

    public String getAction(NativeEvent event) {
        String eventType = event.getType();
        if ("dblclick".equals(eventType)) {
            return mouseBinding;
        } else if (isPossibleEditKeyEvent(event)) {
            String actionSID = getKeyAction(new Key(event.getKeyCode(), event.getAltKey(), event.getCtrlKey(), event.getShiftKey()));

            if (actionSID != null) {
                return actionSID;
            }

            if (isCommonEditKeyEvent(event)) {
                return CHANGE;
            }
        }

        return null;
    }

    private boolean isCommonEditKeyEvent(NativeEvent event) {
        if (event.getCtrlKey() || event.getAltKey() || event.getMetaKey()) {
            return false;
        }

        String eventType = event.getType();
        int keyCode = event.getKeyCode();
        if ("keypress".equals(eventType)) {
            return isCommonKeyPress(keyCode);
        } else if ("keydown".equals(eventType)) {
            return keyCode == KeyCodes.KEY_DELETE;
        }
        return false;
    }

    private boolean isPossibleEditKeyEvent(NativeEvent event) {
        String eventType = event.getType();
        int keyCode = event.getKeyCode();
        if ("keypress".equals(eventType)) {
            return isCommonKeyPress(keyCode);
        } else if ("keydown".equals(eventType)) {
            return keyCode == KEY_DELETE ||
                    keyCode == KEY_BACKSPACE ||
                    keyCode == KEY_INSERT ||
                    (KEY_F1 <= keyCode && keyCode <= KEY_F12);
        }
        return false;
    }

    private boolean isCommonKeyPress(int keyCode) {
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

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public String getMouseAction() {
        return mouseBinding;
    }

    public void setKeyAction(Key key, String actionSID) {
        getKeyBindingMap().put(key, actionSID);
    }

    public String getKeyAction(Key key) {
        return keyBindingMap != null ? keyBindingMap.get(key) : null;
    }

    public void setContextMenuAction(String actionSID, String caption) {
        getContextMenuItems().put(actionSID, caption);
    }

    private HashMap<Key,String> getKeyBindingMap() {
        if (keyBindingMap == null) {
            keyBindingMap = new HashMap<Key, String>();
        }
        return keyBindingMap;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        if (contextMenuBindingMap == null) {
            contextMenuBindingMap = new LinkedHashMap<String, String>();
        }
        return contextMenuBindingMap;
    }

    public static class Key implements Serializable {
        public int keyCode;
        public boolean altPressed;
        public boolean ctrlPressed;
        public boolean shiftPressed;

        public Key() {}

        public Key(int keyCode) {
            this.keyCode = keyCode;
        }

        public Key(int keyCode, boolean altPressed, boolean ctrlPressed, boolean shiftPressed) {
            this.keyCode = keyCode;
            this.altPressed = altPressed;
            this.ctrlPressed = ctrlPressed;
            this.shiftPressed = shiftPressed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            Key key = (Key) o;

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
    }
}
