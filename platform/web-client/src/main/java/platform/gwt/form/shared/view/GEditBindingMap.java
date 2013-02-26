package platform.gwt.form.shared.view;

import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.InternalEditEvent;
import platform.gwt.form.shared.view.grid.NativeEditEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.google.gwt.dom.client.BrowserEvents.DBLCLICK;
import static platform.gwt.form.shared.view.GKeyStroke.*;

public class GEditBindingMap implements Serializable {
    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String CHANGE_WYS = "change_wys";
    public static final String QUICK_FILTER = "qfilter";

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

    public String getAction(EditEvent event) {
        if (event instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) event).getNativeEvent();
            String eventType = nativeEvent.getType();
            if (DBLCLICK.equals(eventType)) {
                return mouseBinding;
            } else if (isPossibleEditKeyEvent(nativeEvent)) {
                String actionSID = getKeyAction(getKeyStroke(nativeEvent));

                if (actionSID != null) {
                    return actionSID;
                }

                if (isCommonEditKeyEvent(nativeEvent)) {
                    return CHANGE;
                }
            }
        } else if (event instanceof InternalEditEvent) {
            return ((InternalEditEvent) event).getAction();
        }

        return null;
    }

    public String getStartFilteringEvent(EditEvent event) {
        if (event instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) event).getNativeEvent();
            if (isPossibleStartFilteringEvent(nativeEvent)) {
                return QUICK_FILTER;
            }
        }
        return null;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public String getMouseAction() {
        return mouseBinding;
    }

    public void setKeyAction(GKeyStroke key, String actionSID) {
        createKeyBindingMap().put(key, actionSID);
    }

    public String getKeyAction(GKeyStroke key) {
        return keyBindingMap != null ? keyBindingMap.get(key) : null;
    }

    public void setContextMenuAction(String actionSID, String caption) {
        createContextMenuItems().put(actionSID, caption);
    }

    private HashMap<GKeyStroke,String> createKeyBindingMap() {
        if (keyBindingMap == null) {
            keyBindingMap = new HashMap<GKeyStroke, String>();
        }
        return keyBindingMap;
    }

    public LinkedHashMap<String, String> createContextMenuItems() {
        if (contextMenuBindingMap == null) {
            contextMenuBindingMap = new LinkedHashMap<String, String>();
        }
        return contextMenuBindingMap;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return contextMenuBindingMap;
    }

    public static boolean isEditableAwareEditEvent(String actionSID) {
        return CHANGE.equals(actionSID)
                || CHANGE_WYS.equals(actionSID)
                || EDIT_OBJECT.equals(actionSID)
                || GROUP_CHANGE.equals(actionSID);
    }

    public static boolean isQuickEditFilterEvent(String actionSID) {
        return QUICK_FILTER.equals(actionSID);
    }
}
