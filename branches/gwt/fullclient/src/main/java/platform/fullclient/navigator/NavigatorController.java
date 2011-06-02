package platform.fullclient.navigator;

import bibliothek.gui.dock.common.SingleCDockable;
import platform.client.logics.DeSerializer;
import platform.client.navigator.*;
import platform.interop.NavigatorWindowType;
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

    public void update(ClientNavigatorWindow window, ClientNavigatorElement element) {
        ClientNavigatorElement baseElement = ClientNavigatorElement.get(AbstractNavigator.BASE_ELEMENT_SID);
        ClientNavigatorWindow baseWindow = baseElement.window;
        Map<ClientNavigatorWindow, HashSet<ClientNavigatorElement>> result = new HashMap<ClientNavigatorWindow, HashSet<ClientNavigatorElement>>();

        for (ClientNavigatorWindow wind : ClientNavigatorWindow.sidToWindow.values()) {
            result.put(wind, new HashSet<ClientNavigatorElement>());
        }

        dfsAddElements(baseElement, baseWindow, result);

        for (Map.Entry<ClientNavigatorWindow, HashSet<ClientNavigatorElement>> entry : result.entrySet()) {
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

    private void dfsAddElements(ClientNavigatorElement currentElement, ClientNavigatorWindow currentWindow, Map<ClientNavigatorWindow, HashSet<ClientNavigatorElement>> result) {
        result.get(currentWindow).add(currentElement);
        ClientNavigatorWindow nextWindow = currentElement.window == null ? currentWindow : currentElement.window;

        if ((currentElement.window == null) || currentElement == getView(currentWindow).getSelectedElement() || (currentElement.window == currentWindow)) {
            for (String sid : currentElement.childrenSid) {
                ClientNavigatorElement element = ClientNavigatorElement.get(sid);
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
