package platform.gwt.form.client;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.shared.view.GKeyStroke;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static platform.gwt.base.client.GwtClientUtils.stopPropagation;
import static platform.gwt.base.shared.GwtSharedUtils.putToDoubleMap;
import static platform.gwt.form.shared.view.GKeyStroke.isPossibleEditKeyEvent;

public class HotkeyManager {
    private Element defaultRoot;

    public interface Binding {
        public boolean onKeyPress(NativeEvent event, GKeyStroke key);
    }

    private static HotkeyManager instance = new HotkeyManager();

    public static HotkeyManager get() {
        return instance;
    }

    private final HashMap<GKeyStroke, HashMap<Element, Binding>> bindings = new HashMap<GKeyStroke, HashMap<Element, Binding>>();

    private HotkeyManager() {
    }

    public void install(Widget rootWidget) {
        rootWidget.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                handleKeyEvent(event.getNativeEvent());
            }
        }, KeyDownEvent.getType());
    }

    public void addHotkeyBinding(Element rootElement, GKeyStroke key, Binding binding) {
        assert rootElement != null && key != null && binding != null;

        putToDoubleMap(bindings, key, rootElement, binding);
    }

    public void removeHotkeyBinding(Element rootElement) {
        //удаление биндингов на элементах-потомках rootElement'а

        if (defaultRoot != null && rootElement.isOrHasChild(defaultRoot)) {
            defaultRoot = null;
        }

        for (Iterator<Map.Entry<GKeyStroke, HashMap<Element, Binding>>> it = bindings.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<GKeyStroke, HashMap<Element, Binding>> b = it.next();
            for (Iterator<Map.Entry<Element, Binding>> bindIter = b.getValue().entrySet().iterator(); bindIter.hasNext(); ) {
                Map.Entry<Element, Binding> binding = bindIter.next();
                if (rootElement.isOrHasChild(binding.getKey())) {
                    bindIter.remove();
                }
            }
            if (b.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    //для будущего использования...
    @SuppressWarnings("UnusedDeclaration")
    public void setDefaultRoot(Element defaultRoot) {
        this.defaultRoot = defaultRoot;
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

            HashMap<Element, Binding> binding = bindings.get(key);
            if (binding != null) {
                Binding bindingInDefaultRoot = null;
                Binding bindingToUse = null;
                //ищем биндинг, рут которго является родителем target'а ивента
                //если такого биндинга нет, то используем любой биндинг внутри дефолт элемента
                for (Map.Entry<Element, Binding> e : binding.entrySet()) {
                    Element root = e.getKey();
                    if (root.isOrHasChild(elementTarget)) {
                        bindingToUse = e.getValue();
                        break;
                    }
                    if (defaultRoot != null && bindingInDefaultRoot == null && defaultRoot.isOrHasChild(root)) {
                        bindingInDefaultRoot = e.getValue();
                    }
                }

                if (bindingToUse == null) {
                    bindingToUse = bindingInDefaultRoot;
                }

                if (bindingToUse != null) {
                    if (bindingToUse.onKeyPress(nativeEvent, key)) {
                        stopPropagation(nativeEvent);
                    }
                }
            }
        }
    }
}
