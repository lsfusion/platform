package lsfusion.gwt.client.form.property.cell;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.form.event.GKeyStroke.*;

public class GEditBindingMap implements Serializable {
    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String CHANGE_WYS = "change_wys";

    public interface EditEventFilter {
        boolean accept(NativeEvent e);
    }

    public static final transient EditEventFilter numberEventFilter = e -> isNumberAddKeyEvent(e);

    private HashMap<GKeyStroke, String> keyBindingMap;
    private LinkedHashMap<String, String> contextMenuBindingMap;
    private String mouseBinding;

    public GEditBindingMap() {
    }

    public GEditBindingMap(HashMap<GKeyStroke, String> keyBindingMap, LinkedHashMap<String, String> contextMenuBindingMap, String mouseBinding) {
        this.keyBindingMap = keyBindingMap;
        this.contextMenuBindingMap = contextMenuBindingMap;
        this.mouseBinding = mouseBinding;
    }

    public String getEventSID(Event event) {

        String keyAction;
        if (KEYDOWN.equals(event.getType()) && (keyAction = getKeyAction(getKeyStroke(event))) != null) {
            return keyAction;
        } else if (GMouseStroke.isChangeEvent(event)) {
            return mouseBinding;
        }
        return null;
    }
    public static String getDefaultEventSID(Event event, EditEventFilter editEventFilter) {
        if (isEditObjectEvent(event)) // has to be before isChangeEvent, since also handles MOUSE CHANGE event
            return EDIT_OBJECT;
        if (GMouseStroke.isChangeEvent(event))
            return CHANGE;
        if (isGroupChangeKeyEvent(event))
            return GROUP_CHANGE;
        if (isCharModifyKeyEvent(event, editEventFilter))
            return CHANGE;
        return null;
    }
    public static boolean isDefaultFilterChange(Event event, EditEventFilter editEventFilter) {
        if (GMouseStroke.isChangeEvent(event))
            return true;
        if (isCharModifyKeyEvent(event, editEventFilter))
            return true;
        return false;
    }

    public void setKeyAction(GKeyStroke key, String actionSID) {
        createKeyBindingMap().put(key, actionSID);
    }

    public String getKeyAction(GKeyStroke key) {
        return keyBindingMap != null ? keyBindingMap.get(key) : null;
    }

    private HashMap<GKeyStroke,String> createKeyBindingMap() {
        if (keyBindingMap == null) {
            keyBindingMap = new HashMap<>();
        }
        return keyBindingMap;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return contextMenuBindingMap;
    }

}
