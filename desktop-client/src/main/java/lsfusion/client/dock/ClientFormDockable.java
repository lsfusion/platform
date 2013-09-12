package lsfusion.client.dock;

import lsfusion.client.MainFrame;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.form.RemoteFormInterface;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(ClientNavigator navigator, String formSID, DockableManager dockableManager) throws IOException, JRException {
        this(navigator, formSID, navigator.remoteNavigator.createForm(formSID, null, false, true), dockableManager, null);
    }

    public ClientFormDockable(ClientNavigator navigator, RemoteFormInterface remoteForm, DockableManager dockableManager, MainFrame.FormCloseListener closeListener) throws IOException, JRException {
        this(navigator, remoteForm.getSID(), remoteForm, dockableManager, closeListener);
    }

    private ClientFormDockable(ClientNavigator navigator, String formSID, RemoteFormInterface remoteForm, DockableManager dockableManager, final MainFrame.FormCloseListener closeListener) throws IOException {
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
                super.block();
                ClientFormDockable.this.blockView();
            }

            @Override
            public void unblock() {
                super.unblock();
                ClientFormDockable.this.unblockView();
            }
        };

        setContent(clientForm.getCaption(), clientForm.getLayout());
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

    @Override
    public void onShowingChanged(boolean oldShowing, boolean newShowing) {
        if (clientForm != null) {
            clientForm.changeShowing(newShowing);
        }
    }
}
