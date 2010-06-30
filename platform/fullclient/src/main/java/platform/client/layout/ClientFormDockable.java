package platform.client.layout;

import net.sf.jasperreports.engine.JRException;
import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

public class ClientFormDockable extends FormDockable {

    public ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession) throws IOException, ClassNotFoundException {
        super(iformID, inavigator, currentSession);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        super(navigator, remoteForm);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        return new ClientForm(remoteForm, navigator);
    }

    // закрываются пользователем
    void closed() {
        // надо удалить RemoteForm
    }
}
