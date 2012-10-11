package platform.fullclient.layout;

import net.sf.jasperreports.engine.JRException;
import platform.client.MainFrame;
import platform.client.form.ClientFormController;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(ClientNavigator navigator, String formSID, DockableManager dockableManager) throws IOException, ClassNotFoundException, JRException {
        this(navigator, formSID, navigator.remoteNavigator.createForm(formSID, null, false, true), dockableManager, null);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, DockableManager dockableManager, MainFrame.FormCloseListener closeListener) throws IOException, ClassNotFoundException, JRException {
        this(navigator, remoteForm.getSID(), remoteForm, dockableManager, closeListener);
    }

    private ClientFormDockable(ClientNavigator navigator, String formSID, RemoteFormInterface remoteForm, DockableManager dockableManager, final MainFrame.FormCloseListener closeListener) throws IOException, ClassNotFoundException, JRException {
        super(formSID, dockableManager);

        clientForm = new ClientFormController(remoteForm, navigator) {
            @Override
            public void hideForm() {
                if (closeListener != null) {
                    closeListener.formClosed();
                }
                setVisible(false);
                super.hideForm();
            }

            @Override
            public void block() {
                ClientFormDockable.this.blockView();
            }

            @Override
            public void unblock() {
                ClientFormDockable.this.unblockView();
            }
        };

        setContent(clientForm.getCaption(), clientForm.getComponent());
    }

    @Override
    public void onClosing() {
        clientForm.closePressed();
    }

    @Override
    public void onClosed() {
        super.onClosed();

        // удаляем ссылку на clientForm, поскольку ClientFormDockable совершенно не собирается быть собранным сборщиком мусора,
        // поскольку на него хранят ссылку внутренние объекты DockingFrames
        clientForm.closed();
        clientForm = null;

        // на всякий случай
        System.gc();
    }
}
