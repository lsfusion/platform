package lsfusion.client.navigator.controller;

import bibliothek.gui.dock.common.SingleCDockable;
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

import static lsfusion.base.BaseUtils.nvl;

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
                    assert entry.getKey().visible; 
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
            boolean sync = !element.isDesktopAsync();
            long requestIndex = mainNavigator.openAction((ClientNavigatorAction) element, modifiers, sync);
            if(!sync) {
                element.asyncExec.exec(requestIndex);
            }
        }
    }

    private void dfsAddElements(ClientNavigatorElement currentElement, ClientNavigatorWindow currentWindow, Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result) {
        ClientNavigatorWindow parentWindow = nvl(currentElement.window, currentWindow);
        ClientNavigatorWindow window = currentElement.parentWindow ? parentWindow : currentWindow;
        if (window != null) {
            result.get(window).add(currentElement);
        }

        //consider that if currentWindow == null, then it is a baseElement and it is always selected, but not drawn anywhere
        if (currentElement.window == null
                || currentWindow == null
                || currentWindow.isSystem()
                || currentElement == views.get(window).getSelectedElement()
                || currentElement.window == currentWindow) {
            for (ClientNavigatorElement element : currentElement.children) {
                if (!result.get(parentWindow).contains(element)) {
                    dfsAddElements(element, parentWindow, result);
                }
            }
        }
    }

    public void recordDockable(JComponent view, SingleCDockable dockable) {
        docks.put(view, dockable);
    }

    public void resetSelectedElements(ClientNavigatorElement newSelectedElement) {
        for (NavigatorView value : views.values()) {
            value.resetSelectedElement(newSelectedElement);
        }
    }
}
