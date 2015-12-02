package lsfusion.client.navigator;

import bibliothek.gui.dock.common.SingleCDockable;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class NavigatorController implements INavigatorController {
    private final ClientNavigator mainNavigator;
    private final LinkedHashMap<ClientNavigatorWindow, NavigatorView> views = new LinkedHashMap<ClientNavigatorWindow, NavigatorView>();
    private final Map<JComponent, SingleCDockable> docks = new HashMap<JComponent, SingleCDockable>();

    public NavigatorController(ClientNavigator mainNavigator) {
        this.mainNavigator = mainNavigator;
    }

    public void initWindowViews() {
        // нет никакой гарантии, что в ходе работы mainNavigator.windows не появятся новые элементы (например, с классовыми или объектными формами)
        for (ClientNavigatorWindow window : mainNavigator.windows.values()) {
            NavigatorView navigatorView = window.createView(this);
            views.put(window, navigatorView);
        }
    }

    public void update() {
        Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result = new HashMap<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>>();

        for (ClientNavigatorWindow wind : mainNavigator.windows.values()) {
            result.put(wind, new LinkedHashSet<ClientNavigatorElement>());
        }

        dfsAddElements(mainNavigator.rootElement, null, result);

        for (Map.Entry<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> entry : result.entrySet()) {
            NavigatorView view = views.get(entry.getKey());
            if (view != null) {
                // может быть ситуация, когда в mainNavigator.windows есть окно, но во views его нет - при сериализации элементов в классовых и связанных формах
                view.refresh(entry.getValue());
                SingleCDockable dockable = docks.get(view.getView());
                if (dockable != null) {
                    dockable.setVisible(entry.getValue().size() != 0);
                }
            }
        }
    }

    public LinkedHashMap<ClientNavigatorWindow, JComponent> getWindowsViews() {
        LinkedHashMap<ClientNavigatorWindow, JComponent> av = new LinkedHashMap<ClientNavigatorWindow, JComponent>();
        for (Map.Entry<ClientNavigatorWindow, NavigatorView> entry : views.entrySet()) {
            av.put(entry.getKey(), entry.getValue().getView());
        }

        return av;
    }

    public void openElement(ClientNavigatorElement element, int modifiers) {
        try {
            if (element instanceof ClientNavigatorForm) {
                ClientNavigatorForm form = (ClientNavigatorForm) element;
                if (form.modalityType.isModal()) {
                    mainNavigator.openModalForm(form, modifiers);
                } else {
                    mainNavigator.openForm(form, modifiers);
                }
            } else if (element instanceof ClientNavigatorAction) {
                mainNavigator.openAction((ClientNavigatorAction) element);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void dfsAddElements(ClientNavigatorElement currentElement, ClientNavigatorWindow currentWindow, Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result) {
        if(currentElement != null) {
            if ((currentElement.window != null) && (currentElement.window.drawRoot)) {
                result.get(currentElement.window).add(currentElement);
            } else {
                if (currentWindow != null) {
                    result.get(currentWindow).add(currentElement);
                }
            }
            ClientNavigatorWindow nextWindow = currentElement.window == null ? currentWindow : currentElement.window;

            // считаем, что если currentWindow == null, то это baseElement и он всегда выделен, но не рисуется никуда
            if (currentElement.window == null
                    || currentWindow == null
                    || currentElement == views.get(currentWindow).getSelectedElement()
                    || currentElement.window == currentWindow
                    || currentElement.window.drawRoot) {
                for (ClientNavigatorElement element : currentElement.children) {
                    if (!result.get(nextWindow).contains(element)) {
                        dfsAddElements(element, nextWindow, result);
                    }
                }
            }
        }
    }

    public void recordDockable(JComponent view, SingleCDockable dockable) {
        docks.put(view, dockable);
    }
}
