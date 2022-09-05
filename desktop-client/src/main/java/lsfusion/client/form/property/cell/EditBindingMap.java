package lsfusion.client.form.property.cell;

import lsfusion.base.Result;
import lsfusion.client.classes.ClientType;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.table.view.InternalEditEvent;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Supplier;

import static lsfusion.interop.form.event.KeyStrokes.getKeyStrokeForEvent;

public class EditBindingMap {
    private Map<KeyStroke, String> keyBindingMap = new HashMap<>();
    private LinkedHashMap<String, String> contextMenuBindingMap = new LinkedHashMap<>();

    //пока одно значение, возможно в будущем расширится до мэпа (типа клик, дабл клик и т. д.)
    private String mouseBinding;

    public EditBindingMap(boolean changeMouse) {
        if(changeMouse)
            setMouseAction(ServerResponse.CHANGE);
    }

    public String getEventSID(EventObject editEvent, Result<Integer> contextAction, EditEventFilter editEventFilter,
                              boolean hasEditObjectAction, boolean hasChangeAction, boolean disableInputList,
                              Supplier<Integer> dialogActionIndexSupplier) {
        if (KeyStrokes.isEditObjectEvent(editEvent, hasEditObjectAction, hasChangeAction)) // has to be before isChangeEvent, since also handles MOUSE CHANGE event
            return ServerResponse.EDIT_OBJECT;

        if (editEvent instanceof KeyEvent) {
            if (!KeyStrokes.isSuitableEditKeyEvent(editEvent) || KeyStrokes.isEnterEvent(editEvent)) {
                return null;
            }
        } else if (editEvent instanceof MouseEvent) {
            if (disableInputList) {
                contextAction.set(dialogActionIndexSupplier.get());
            }
            return mouseBinding;
        } else if (editEvent instanceof InternalEditEvent) {
            return ((InternalEditEvent) editEvent).action;
        }

        if (editEventFilter != null && !editEventFilter.accept(editEvent)) {
            return null;
        }

        return ServerResponse.CHANGE;
    }

    public String getKeyPressAction(KeyStroke keyStroke) {
        return keyBindingMap.get(keyStroke);
    }

    public String getKeyPressAction(EventObject editEvent) {
        if (editEvent instanceof KeyEvent) {
            return getKeyPressAction(getKeyStrokeForEvent((KeyEvent) editEvent));
        }
        return null;
    }

    public void setKeyAction(KeyStroke keyStroke, String actionSID) {
        keyBindingMap.put(keyStroke, actionSID);
    }

    public Map<KeyStroke, String> getKeyBindingMap() {
        return Collections.unmodifiableMap(keyBindingMap);
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

    public String getMouseAction() {
        return mouseBinding;
    }

    public static boolean isChangeEvent(String actionSID) {
        return ServerResponse.isChangeEvent(actionSID);
    }

    public static String getPropertyEventActionSID(EventObject e, Result<Integer> contextAction, ClientPropertyDraw property, EditBindingMap overrideMap) {
        ClientType changeType = property.getChangeType();
        EditEventFilter eventFilter = changeType == null ? null : changeType.getEditEventFilter();

        String actionSID = null;
        if (property.editBindingMap != null) {
            actionSID = property.editBindingMap.getEventSID(e, contextAction, eventFilter, property.hasEditObjectAction, property.hasChangeAction, property.disableInputList, property::getDialogInputActionIndex);
        }

        if (actionSID == null) {
            actionSID = overrideMap.getKeyPressAction(e);
            if (actionSID == null) {
                actionSID = overrideMap.getEventSID(e, contextAction, eventFilter, property.hasEditObjectAction, property.hasChangeAction, property.disableInputList, property::getDialogInputActionIndex);
            }
        }
        return actionSID;
    }

    public static String getPropertyKeyPressActionSID(EventObject e, ClientPropertyDraw property) {
        if (property.editBindingMap != null) {
            return property.editBindingMap.getKeyPressAction(e);
        }
        return null;
    }

    public static String getPropertyKeyPressActionSID(KeyStroke keyStroke, ClientPropertyDraw property) {
        if (property.editBindingMap != null) {
            return property.editBindingMap.getKeyPressAction(keyStroke);
        }
        return null;
    }

    public interface EditEventFilter {
        boolean accept(EventObject e);
    }
}
