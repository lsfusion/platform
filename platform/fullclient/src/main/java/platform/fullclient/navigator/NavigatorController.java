package platform.fullclient.navigator;

import bibliothek.gui.dock.common.SingleCDockable;
import platform.client.logics.DeSerializer;
import platform.client.navigator.*;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class NavigatorController implements INavigatorController {
    RemoteNavigatorInterface remoteNavigator;
    List<ClientNavigatorElement> elements;
    public LinkedHashMap<ClientNavigatorWindow, NavigatorView> views = new LinkedHashMap<ClientNavigatorWindow, NavigatorView>();
    public ClientNavigator mainNavigator;
    private Map<JComponent, SingleCDockable> docks = new HashMap<JComponent, SingleCDockable>();

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
                    element.children.add(child);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initViews() {
        for (ClientNavigatorWindow window : ClientNavigatorWindow.sidToWindow.values()) {
            NavigatorView navigatorView = window.getView(this);
            views.put(window, navigatorView);
        }
    }

    public void update() {
        Map<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> result = new HashMap<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>>();

        for (ClientNavigatorWindow wind : ClientNavigatorWindow.sidToWindow.values()) {
            result.put(wind, new LinkedHashSet<ClientNavigatorElement>());
        }

        dfsAddElements(ClientNavigatorElement.root, null, result);

        for (Map.Entry<ClientNavigatorWindow, LinkedHashSet<ClientNavigatorElement>> entry : result.entrySet()) {
            NavigatorView view = views.get(entry.getKey());
            view.refresh(entry.getValue());
            SingleCDockable dockable = docks.get(view.getView());
            if (dockable != null) {
                dockable.setVisible(entry.getValue().size() != 0);
            }
        }
    }

    public void openForm(ClientNavigatorElement element) {
        if (element instanceof ClientNavigatorForm) {
            try {
                ClientNavigatorForm form = (ClientNavigatorForm) element;
                if (form.showModal) {
                    mainNavigator.openModalForm(form);
                } else {
                    mainNavigator.openForm(form);
                }
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
        if ((currentElement.window == null) || (currentWindow == null ? true : currentElement == views.get(currentWindow).getSelectedElement()) || (currentElement.window == currentWindow) || (currentElement.window.drawRoot)) {
            for (ClientNavigatorElement element : currentElement.children) {
                if (!result.get(nextWindow).contains(element)) {
                    dfsAddElements(element, nextWindow, result);
                }
            }
        }
    }

    public void recordDockable(JComponent view, SingleCDockable dockable) {
        docks.put(view, dockable);
    }
}
