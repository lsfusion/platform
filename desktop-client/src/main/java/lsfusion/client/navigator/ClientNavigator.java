package lsfusion.client.navigator;

import lsfusion.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.util.Map;

public abstract class ClientNavigator {
    public final RemoteNavigatorInterface remoteNavigator;

    public final ClientNavigatorElement rootElement;
    public final Map<String, ClientNavigatorWindow> windows;

    public ClientNavigator(RemoteNavigatorInterface remoteNavigator, ClientNavigatorElement rootElement, Map<String, ClientNavigatorWindow> windows) {
        this.remoteNavigator = remoteNavigator;
        this.rootElement = rootElement;
        this.windows = windows;
    }

    public void openModalForm(ClientNavigatorForm form) throws ClassNotFoundException, IOException {
        openForm(form);
    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;
    public abstract void openAction(ClientNavigatorAction action);
}