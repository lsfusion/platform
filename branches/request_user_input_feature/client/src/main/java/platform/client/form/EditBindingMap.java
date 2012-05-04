package platform.client.form;

import platform.interop.KeyStrokes;
import platform.interop.form.EditActionResult;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class EditBindingMap {
    private Map<KeyStroke, String> keyBindingMap = new HashMap<KeyStroke, String>();

    //пока одно значение, возможно в будущем расширится до мэпа (типа клик, дабл клик и т. д.)
    private String mouseBinding;

    public EditBindingMap() {
        setKeyAction(KeyStrokes.getObjectEditorDialogEvent(), EditActionResult.EDIT_OBJECT);
        setMouseAction(EditActionResult.CHANGE);
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
        }

        return EditActionResult.CHANGE;
    }

    public void setKeyAction(KeyStroke keyStroke, String actionSID) {
        keyBindingMap.put(keyStroke, actionSID);
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }
}
