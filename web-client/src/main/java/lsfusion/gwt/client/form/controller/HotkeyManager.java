package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.shared.view.GKeyStroke;
import lsfusion.gwt.shared.view.GGroupObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.shared.view.GKeyStroke.isPossibleEditKeyEvent;
import static lsfusion.gwt.shared.GwtSharedUtils.getFromDoubleMap;
import static lsfusion.gwt.shared.GwtSharedUtils.putToDoubleMap;

public class HotkeyManager {
    public interface Binding {
        boolean onKeyPress(NativeEvent event, GKeyStroke key);
    }

    private final HashMap<GKeyStroke, HashMap<GGroupObject, List<Binding>>> bindings = new HashMap<>();

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

        List<Binding> groupBindings = getFromDoubleMap(bindings, key, groupObject);
        if (groupBindings == null) {
            groupBindings = new ArrayList<>();
            putToDoubleMap(bindings, key, groupObject, groupBindings);
        }

        groupBindings.add(binding);
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

            HashMap<GGroupObject, List<Binding>> binding = bindings.get(key);
            if (binding != null && !binding.isEmpty()) {

                while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
                    GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject("groupObject");
                    if (targetGO != null) {
                        List<Binding> groupBindings = binding.get(targetGO);
                        if (groupBindings != null) {
                            for (Binding b : groupBindings) {
                                if (b.onKeyPress(nativeEvent, key)) {
                                    stopPropagation(nativeEvent);
                                    return;
                                }
                            }
                        }
                        break;
                    }
                    elementTarget = elementTarget.getParentElement();
                }

                // если подходящий GroupObject не нашли, используём любой биндинг к этим клавишам в рамках формы
                for (List<Binding> groupBindings : binding.values()) {
                    for (Binding b : groupBindings) {
                        if (b.onKeyPress(nativeEvent, key)) {
                            stopPropagation(nativeEvent);
                            return;
                        }
                    }
                }
            }
        }
    }
}
