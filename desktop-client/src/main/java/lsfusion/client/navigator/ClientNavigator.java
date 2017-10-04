package lsfusion.client.navigator;

import lsfusion.client.dock.ClientFormDockable;
import lsfusion.client.dock.DockableManager;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.sf.jasperreports.engine.JRException;

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
    
    public ClientFormDockable createFormDockableByCanonicalName(String canonicalName, DockableManager dockableManager) throws IOException, JRException {
        // todo [dale]: Здесь непонятно для чего этот код может быть вызван (только для форм в навигаторе или для всех форм) (и вызывается ли он вообще?)
        ClientNavigatorElement element = rootElement.findElementByCanonicalName(canonicalName);
        if ((element instanceof ClientNavigatorForm)) {
            ClientNavigatorForm ne = (ClientNavigatorForm) element;
            return new ClientFormDockable(this, ne.formCanonicalName, ne.formSID, dockableManager);
        }
        return null;
    }

    public void openModalForm(ClientNavigatorForm form, int modifiers) throws ClassNotFoundException, IOException {
        openForm(form, modifiers);
    }

    public abstract void openForm(ClientNavigatorForm element, int modifiers) throws IOException, ClassNotFoundException;
    public abstract void openAction(ClientNavigatorAction action);
}