package platform.fullclient.navigator;

import bibliothek.gui.dock.common.SingleCDockable;
import platform.client.logics.DeSerializer;
import platform.client.navigator.*;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.util.*;

public class NavigatorController implements INavigatorController {
    RemoteNavigatorInterface remoteNavigator;
    List<ClientNavigatorElement> elements;
    Map<ClientNavigatorWindow, NavigatorView> views = new HashMap<ClientNavigatorWindow, NavigatorView>();
    public ClientNavigator mainNavigator;
    private Map<NavigatorView, SingleCDockable> docks = new HashMap<NavigatorView, SingleCDockable>();

    public NavigatorController(RemoteNavigatorInterface iremoteNavigator) {
        remoteNavigator = iremoteNavigator;

        try {
            elements = DeSerializer.deserializeListClientNavigatorElement(remoteNavigator.getNavigatorTree());
            Map<String, ClientNavigatorElement> elementsMap = new HashMap<String, ClientNavigatorElement>();
            for (ClientNavigatorElement element : elements) {
                elementsMap.put(element.getSID(), element);
            }

            for (ClientNavigatorElement element : elements) {
                elementsMap.put(element.getSID(), element);
            }

            for (ClientNavigatorElement element : elements) {
                for (String s : element.childrenSid) {
                    ClientNavigatorElement child = elementsMap.get(s);
                    element.childrens.add(child);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NavigatorView getView(ClientNavigatorWindow window) {
        if (views.containsKey(window)) {
            return views.get(window);
        } else {
            NavigatorView navigatorView = window.getView(this);
            views.put(window, navigatorView);
            return navigatorView;
        }
    }

    public List<NavigatorView> getAllViews() {
        List<NavigatorView> result = new ArrayList<NavigatorView>();
        for (ClientNavigatorWindow window : ClientNavigatorWindow.sidToWindow.values()) {
            result.add(getView(window));
        }
        return result;
    }

    public void update() {
        Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result = new HashMap<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>>();

        for (ClientNavigatorWindow wind : ClientNavigatorWindow.sidToWindow.values()) {
            result.put(wind, new LinkedHashSet<ClientNavigatorElement>());
        }

        dfsAddElements(ClientNavigatorElement.root, null, result);

        for (Map.Entry<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> entry : result.entrySet()) {
            NavigatorView view = getView(entry.getKey());
            view.refresh(entry.getValue());
            SingleCDockable dockable = docks.get(view);
            if (dockable != null) {
                dockable.setVisible(entry.getValue().size() != 0);
            }
        }
    }

    public void openForm(ClientNavigatorElement element) {
        if (element instanceof ClientNavigatorForm) {
            try {
                mainNavigator.openForm((ClientNavigatorForm) element);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void dfsAddElements(ClientNavigatorElement currentElement, ClientNavigatorWindow currentWindow, Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result) {
        if ((currentElement.window != null) && (currentElement.window.drawRoot)) {
            result.get(currentElement.window).add(currentElement);
        } else {
            if (currentWindow != null)
                result.get(currentWindow).add(currentElement);
        }
        ClientNavigatorWindow nextWindow = currentElement.window == null ? currentWindow : currentElement.window;

        // считаем, что если currentWindow == null, то это baseElement и он всегда выделен, но не рисуется никуда
        if ((currentElement.window == null) || (currentWindow == null ? true : currentElement == getView(currentWindow).getSelectedElement()) || (currentElement.window == currentWindow) || (currentElement.window.drawRoot)) {
            for (ClientNavigatorElement element : currentElement.childrens) {
                if (!result.get(nextWindow).contains(element)) {
                    dfsAddElements(element, nextWindow, result);
                }
            }
        }
    }

    public void recordDockable(NavigatorView view, SingleCDockable dockable) {
        docks.put(view, dockable);
    }
}
