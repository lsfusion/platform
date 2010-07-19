package platform.fullclient.layout;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import net.sf.jasperreports.engine.JRException;
import platform.client.form.ClientForm;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.io.IOException;

public class ClientFormDockable extends FormDockable {

    public ClientFormDockable(int iformID, ClientNavigator inavigator, boolean currentSession, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException {
        super(iformID, inavigator, currentSession, factory);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, MultipleCDockableFactory<FormDockable,?> factory) throws IOException, ClassNotFoundException, JRException {
        super(navigator, remoteForm, factory);
    }

    @Override
    Component getActiveComponent(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        return new ClientForm(remoteForm, navigator).getComponent();
    }

    // Р·Р°РєСЂС‹РІР°СЋС‚СЃСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»РµРј
    void closed() {
        // РЅР°РґРѕ СѓРґР°Р»РёС‚СЊ RemoteForm
    }
}
