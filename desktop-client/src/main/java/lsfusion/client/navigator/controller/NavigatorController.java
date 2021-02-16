package lsfusion.client.navigator.controller;

import bibliothek.gui.dock.common.SingleCDockable;
import lsfusion.base.lambda.ERunnable;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.ClientNavigatorAction;
import lsfusion.client.navigator.ClientNavigatorElement;
import lsfusion.client.navigator.view.NavigatorView;
import lsfusion.client.navigator.window.ClientNavigatorWindow;

import javax.swing.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class NavigatorController implements INavigatorController {
    private final ClientNavigator mainNavigator;
    private final LinkedHashMap<ClientNavigatorWindow, NavigatorView> views = new LinkedHashMap<>();
    private final Map<JComponent, SingleCDockable> docks = new HashMap<>();

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
        Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result = new HashMap<>();

        for (ClientNavigatorWindow wind : mainNavigator.windows.values()) {
            result.put(wind, new LinkedHashSet<>());
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
        LinkedHashMap<ClientNavigatorWindow, JComponent> av = new LinkedHashMap<>();
        for (Map.Entry<ClientNavigatorWindow, NavigatorView> entry : views.entrySet()) {
            av.put(entry.getKey(), entry.getValue().getView());
        }

        return av;
    }

    public void openElement(ClientNavigatorElement element, int modifiers) {
        if (element instanceof ClientNavigatorAction) {
            if(element.asyncExec != null) {
                element.asyncExec.exec();
            }
            SwingUtils.invokeLater(() -> mainNavigator.openAction((ClientNavigatorAction) element, modifiers));
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
