package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import net.sf.jasperreports.engine.JRException;
import platform.client.form.ClientFormController;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

public class ClientFormDockable extends FormDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        super(iformID, inavigator, currentSession, factory);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException, JRException {
        super(navigator, remoteForm, factory);
    }

    public ClientFormDockable(int formID, MultipleCDockableFactory<FormDockable,?> factory, ClientNavigator navigator) throws IOException, ClassNotFoundException, IOException {
        super(formID, factory, navigator);
    }

    private ClientFormController getClientForm(ClientNavigator navigator, RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        if (clientForm == null) {
            clientForm = new ClientFormController(remoteForm, navigator);
        }
        return clientForm;
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        return getClientForm(navigator, remoteForm).getComponent();
    }

    protected String getCaption() {
        return clientForm.getCaption();
    }

    @Override
    void closed() {
        super.closed();

        // сбрасываем ссылку на RemoteFormInterface, чтобы его как можно быстрее собрал сборщик мусора
        clientForm.remoteForm = null;

        // удаляем ссылку на clientForm, поскольку ClientFormDockable совершенно не собирается быть собранным сборщиком мусора,
        // поскольку на него хранят ссылку внутренние объекты DockingFrames
        clientForm = null;
    }
}
