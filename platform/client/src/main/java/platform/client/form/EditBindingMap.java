package platform.client.form;

import platform.interop.KeyStrokes;
import platform.interop.form.ServerResponse;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditBindingMap {
    private Map<KeyStroke, String> keyBindingMap = new HashMap<KeyStroke, String>();
    private LinkedHashMap<String, String> contextMenuBindingMap = new LinkedHashMap<String, String>();

    //пока одно значение, возможно в будущем расширится до мэпа (типа клик, дабл клик и т. д.)
    private String mouseBinding;

    public EditBindingMap() {
        setKeyAction(KeyStrokes.getObjectEditorDialogEvent(), ServerResponse.EDIT_OBJECT);
        setMouseAction(ServerResponse.CHANGE);
    }

    public String getAction(EventObject editEvent) {
        if (editEvent instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) editEvent;

            KeyStroke ks = KeyStrokes.getKeyStrokeForEvent(keyEvent);
            String actionSID = keyBindingMap.get(ks);
            if (actionSID != null) {
                return actionSID;
            }

            if (!KeyStrokes.isSuitableEditKeyEvent(editEvent)) {
                return null;
            }
        } else if (editEvent instanceof MouseEvent) {
            return mouseBinding;
        } else if (editEvent instanceof InternalEditEvent) {
            return ((InternalEditEvent) editEvent).action;
        }

        return ServerResponse.CHANGE;
    }

    public void setKeyAction(KeyStroke keyStroke, String actionSID) {
        keyBindingMap.put(keyStroke, actionSID);
    }

    public void setContextMenuAction(String actionSID, String caption) {
        contextMenuBindingMap.put(actionSID, caption);
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return contextMenuBindingMap;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public static boolean isEditableAwareEditEvent(String actionSID) {
        return ServerResponse.CHANGE.equals(actionSID)
                || ServerResponse.CHANGE_WYS.equals(actionSID)
                || ServerResponse.EDIT_OBJECT.equals(actionSID)
                || ServerResponse.GROUP_CHANGE.equals(actionSID);
    }
}
