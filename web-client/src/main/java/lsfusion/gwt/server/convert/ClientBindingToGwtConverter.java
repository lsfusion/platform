package lsfusion.gwt.server.convert;

import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientBindingToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientBindingToGwtConverter instance = new ClientBindingToGwtConverter();
    }

    public static ClientBindingToGwtConverter getInstance() {
        return ClientBindingToGwtConverter.InstanceHolder.instance;
    }

    @Converter(from = KeyInputEvent.class)
    public GKeyInputEvent convertKeyInputEvent(KeyInputEvent keyInputEvent) {
        return new GKeyInputEvent(convertOrCast(keyInputEvent.keyStroke));
    }

    @Converter(from = MouseInputEvent.class)
    public GMouseInputEvent convertMouseInputEvent(MouseInputEvent mouseInputEvent) {
        return new GMouseInputEvent(convertOrCast(mouseInputEvent.mouseEvent));
    }

    public GInputBindingEvent convertBinding(lsfusion.interop.form.event.InputEvent event, Integer priority) {
        Map<String, BindingMode> bindingModes = event != null ? event.bindingModes : null;
        return new GInputBindingEvent(convertOrCast(event),
                new GBindingEnv(priority != null && priority.equals(0) ? null : priority,
                        convertOrCast(bindingModes != null ? bindingModes.get("preview") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("dialog") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("window") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("group") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("editing") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("showing") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("panel") : null),
                        convertOrCast(bindingModes != null ? bindingModes.get("cell") : null)
                ));
    }

    @Converter(from = EditBindingMap.class)
    public GEditBindingMap convertBindingMap(EditBindingMap editBindingMap) {
        HashMap<GKeyStroke, String> keyBindingMap = null;
        if (editBindingMap.getKeyBindingMap() != null) {
            keyBindingMap = new HashMap<>();
            for (Map.Entry<KeyStroke, String> e : editBindingMap.getKeyBindingMap().entrySet()) {
                GKeyStroke key = convertOrCast(e.getKey());
                keyBindingMap.put(key, e.getValue());
            }
        }

        LinkedHashMap<String, String> contextMenuBindingMap = editBindingMap.getContextMenuItems() != null
                ? new LinkedHashMap<>(editBindingMap.getContextMenuItems())
                : null;
        String mouseBinding = editBindingMap.getMouseAction();

        return new GEditBindingMap(keyBindingMap, contextMenuBindingMap, mouseBinding);
    }

    @Converter(from = BindingMode.class)
    public GBindingMode convertBindingMode(BindingMode bindingMode) {
        return  GBindingMode.valueOf(bindingMode.name());
    }

    @Converter(from = KeyStroke.class)
    public GKeyStroke convertKeyStroke(KeyStroke keyStroke) {
        int modifiers = keyStroke.getModifiers();
        boolean isAltPressed = (modifiers & InputEvent.ALT_MASK) != 0;
        boolean isCtrlPressed = (modifiers & InputEvent.CTRL_MASK) != 0;
        boolean isShiftPressed = (modifiers & InputEvent.SHIFT_MASK) != 0;
        int keyCode = convertKeyCode(keyStroke.getKeyCode());

        return new GKeyStroke(keyCode, isAltPressed, isCtrlPressed, isShiftPressed);
    }

    private int convertKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_DELETE:
                return KeyCodes.KEY_DELETE;
            case KeyEvent.VK_ESCAPE:
                return KeyCodes.KEY_ESCAPE;
            case KeyEvent.VK_ENTER:
                return KeyCodes.KEY_ENTER;
            case KeyEvent.VK_INSERT:
                return GKeyStroke.KEY_INSERT;
            default:
                return keyCode;
        }
    }
}
