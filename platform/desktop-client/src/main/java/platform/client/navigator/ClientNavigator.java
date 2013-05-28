package platform.client.navigator;

import platform.interop.navigator.RemoteNavigatorInterface;

import java.io.IOException;
import java.util.Map;

public abstract class ClientNavigator {
    public final RemoteNavigatorInterface remoteNavigator;

    public final ClientNavigatorElement rootElement;
    public final Map<String, ClientNavigatorWindow> windows;

    public final RelevantFormNavigatorPanel relevantFormNavigator;
    public final RelevantClassNavigatorPanel relevantClassNavigator;

    public ClientNavigator(RemoteNavigatorInterface remoteNavigator, ClientNavigatorElement rootElement, Map<String, ClientNavigatorWindow> windows) {
        this.remoteNavigator = remoteNavigator;
        this.rootElement = rootElement;
        this.windows = windows;

        relevantFormNavigator = new RelevantFormNavigatorPanel(this);
        relevantClassNavigator = new RelevantClassNavigatorPanel(this);
    }

    public void openModalForm(ClientNavigatorForm form) throws ClassNotFoundException, IOException {
        openForm(form);
    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;
    public abstract void openAction(ClientNavigatorAction action);
}