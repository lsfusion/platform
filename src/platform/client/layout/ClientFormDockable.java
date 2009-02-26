package platform.client.layout;

import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.sql.SQLException;

class ClientFormDockable extends FormDockable {

    ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession) throws SQLException {
        super(iformID, inavigator, currentSession);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) {
        return new ClientForm(remoteForm, navigator);
    }

    // закрываются пользователем
    void closed() {
        // надо удалить RemoteForm
    }
}
