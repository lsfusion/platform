package lsfusion.gwt.form.client;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.shared.view.GGroupObject;
import lsfusion.gwt.form.shared.view.GKeyStroke;

import java.util.HashMap;

import static lsfusion.gwt.base.client.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.base.shared.GwtSharedUtils.putToDoubleMap;
import static lsfusion.gwt.form.shared.view.GKeyStroke.isPossibleEditKeyEvent;

public class HotkeyManager {
    public interface Binding {
        public boolean onKeyPress(NativeEvent event, GKeyStroke key);
    }

    private final HashMap<GKeyStroke, HashMap<GGroupObject, Binding>> bindings = new HashMap<GKeyStroke, HashMap<GGroupObject, Binding>>();

    public void install(Widget rootWidget) {
        rootWidget.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                handleKeyEvent(event.getNativeEvent());
            }
        }, KeyDownEvent.getType());
    }

    public void addHotkeyBinding(GGroupObject groupObject, GKeyStroke key, Binding binding) {
        assert key != null && binding != null;

        putToDoubleMap(bindings, key, groupObject, binding);
    }

    private void handleKeyEvent(NativeEvent nativeEvent) {
        assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

        EventTarget target = nativeEvent.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        if (isPossibleEditKeyEvent(nativeEvent)) {
            Element elementTarget = Element.as(target);

            GKeyStroke key = GKeyStroke.getKeyStroke(nativeEvent);

            HashMap<GGroupObject, Binding> binding = bindings.get(key);
            if (binding != null) {
                Binding bindingToUse = null;

                while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
                    GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject("groupObject");
                    if (targetGO != null) {
                        if (binding.containsKey(targetGO)) {
                            bindingToUse = binding.get(targetGO);
                        }
                        break;
                    }
                    elementTarget = elementTarget.getParentElement();
                }

                if (bindingToUse != null) {
                    if (bindingToUse.onKeyPress(nativeEvent, key)) {
                        stopPropagation(nativeEvent);
                    }
                } else if (!binding.isEmpty()) {  // если подходящий GroupObject не нашли, используём любой биндинг к этим клавишам в рамках формы
                    if (binding.values().iterator().next().onKeyPress(nativeEvent, key)) {
                        stopPropagation(nativeEvent);
                    }
                }
            }
        }
    }
}
