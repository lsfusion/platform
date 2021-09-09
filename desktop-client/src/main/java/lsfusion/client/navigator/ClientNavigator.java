package lsfusion.client.navigator;

import lsfusion.client.navigator.window.ClientNavigatorWindow;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

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

    public abstract long openAction(ClientNavigatorAction action, int modifiers, boolean sync);
}