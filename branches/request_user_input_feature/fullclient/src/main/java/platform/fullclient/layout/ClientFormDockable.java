package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import net.sf.jasperreports.engine.JRException;
import platform.client.form.ClientFormController;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

public class ClientFormDockable extends FormDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(String formSID, ClientNavigator inavigator, boolean currentSession, MultipleCDockableFactory<FormDockable, ?> factory) throws IOException, ClassNotFoundException {
        super(formSID, inavigator, currentSession, factory, true, null);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable, ?> factory) throws IOException, ClassNotFoundException, JRException {
        super(navigator, remoteForm, factory, null);
    }

    public ClientFormDockable(String formSID, MultipleCDockableFactory<FormDockable, ?> factory, ClientNavigator navigator) throws IOException, ClassNotFoundException, IOException {
        super(formSID, factory, navigator,null);
    }

    private ClientFormController getClientForm(ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        if (clientForm == null) {
            clientForm = new ClientFormController(remoteForm, navigator);
        }
        return clientForm;
    }

    public ClientFormController getClientForm() {
        return clientForm;
    }

    @Override
    public boolean pageChanged() {
        return clientForm.dataChanged;
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm, FormUserPreferences userPreferences) throws IOException, ClassNotFoundException {
        return getClientForm(navigator, remoteForm).getComponent();
    }

    protected String getCaption() {
        return clientForm.getCaption();
    }

    @Override
    void closed() {
        super.closed();

        // удаляем ссылку на clientForm, поскольку ClientFormDockable совершенно не собирается быть собранным сборщиком мусора,
        // поскольку на него хранят ссылку внутренние объекты DockingFrames
        clientForm.closed();
        clientForm = null;

        // на всякий случай
        System.gc();
    }
}
