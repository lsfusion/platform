package lsfusion.gwt.client.form.property.cell;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.event.GMouseStroke;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static lsfusion.gwt.client.form.event.GKeyStroke.*;

public class GEditBindingMap implements Serializable {
    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";

    private static final List<String> changeEvents = Arrays.asList(CHANGE, GROUP_CHANGE);

    public static boolean isChangeEvent(String actionSID) {
        return changeEvents.contains(actionSID);
    }

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

    public static final String TOOLBAR_ACTION = "toolbarAction";
    public static String getDefaultEventSID(Event event, Result<Integer> contextAction, EditEventFilter editEventFilter, boolean hasEditObjectAction, boolean hasChangeAction) {
        if (isEditObjectEvent(event, hasEditObjectAction, hasChangeAction)) // has to be before isChangeEvent, since also handles MOUSE CHANGE event
            return EDIT_OBJECT;
        if (GMouseStroke.isChangeEvent(event)) {
            contextAction.set((Integer) getToolbarAction(event));
            return CHANGE;
        }
        if (isGroupChangeKeyEvent(event))
            return GROUP_CHANGE;
        if (isCharModifyKeyEvent(event, editEventFilter) || isDropEvent(event) || isChangeAppendKeyEvent(event))
            return CHANGE;
        return null;
    }

    public static Object getToolbarAction(Event event) {
        return event.getEventTarget().<Element>cast().getPropertyObject(TOOLBAR_ACTION);
    }

    public static boolean isDefaultFilterChange(Event event, Result<Boolean> contextAction, EditEventFilter editEventFilter) {
        if (GMouseStroke.isChangeEvent(event)) {
            contextAction.set((Boolean) getToolbarAction(event));
            return true;
        }
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
