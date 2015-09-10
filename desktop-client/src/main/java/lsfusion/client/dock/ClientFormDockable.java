package lsfusion.client.dock;

import lsfusion.client.MainFrame;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.form.RemoteFormInterface;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(ClientNavigator navigator, String canonicalName, String formSID, DockableManager dockableManager) throws IOException, JRException {
        this(navigator, canonicalName, formSID, navigator.remoteNavigator.createForm(formSID, null, false, true), dockableManager, null, null);
    }

    public ClientFormDockable(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, DockableManager dockableManager, final MainFrame.FormCloseListener closeListener, byte[] firstChanges) throws IOException {
        super(canonicalName, dockableManager);

        clientForm = new ClientFormController(canonicalName, formSID, remoteForm, firstChanges, navigator) {
            @Override
            public void hideForm() {
                if (control() != null) {
                    setVisible(false);
                }
                
                if (closeListener != null) {
                    closeListener.formClosed();
                }
                super.hideForm();
            }

            @Override
            public void blockView() {
                ClientFormDockable.this.blockView();
            }

            @Override
            public void unblockView() {
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
            clientForm.setSelected(newShowing);
        }
    }
}
