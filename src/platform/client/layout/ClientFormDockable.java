package platform.client.layout;

import platform.client.navigator.ClientNavigator;
import platform.client.form.ClientForm;
import platform.server.view.form.RemoteForm;

import java.sql.SQLException;
import java.awt.*;

class ClientFormDockable extends FormDockable {

    ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession) throws SQLException {
        super(iformID, inavigator, currentSession);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteForm remoteForm) {
        return new ClientForm(remoteForm, navigator);
    }

    // закрываются пользователем
    void closed() {
        // надо удалить RemoteForm
    }
}
