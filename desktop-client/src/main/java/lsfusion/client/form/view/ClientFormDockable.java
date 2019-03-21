package lsfusion.client.form.view;

import lsfusion.client.view.MainFrame;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.form.controller.FormsController;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, FormsController formsController, final MainFrame.FormCloseListener closeListener, byte[] firstChanges) throws IOException {
        super(canonicalName, formsController);

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
                clientForm.getLayout().setBlocked(true);
                ClientFormDockable.this.blockView();
            }

            @Override
            public void unblockView() {
                clientForm.getLayout().setBlocked(false);
                ClientFormDockable.this.unblockView();
            }

            @Override
            public void setBlockingForm(ClientFormDockable blockingForm) {
                ClientFormDockable.this.setBlockingDockable(blockingForm);
            }
        };

        setContent(clientForm.getCaption(), clientForm.getTooltip(), clientForm.getLayout());
    }

    @Override
    public void onClosing() {
        RmiQueue.runAction(new Runnable() {
            @Override
            public void run() {
                clientForm.closePressed();
            }
        });
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

    @Override
    public void onOpened() {
        if (clientForm != null) 
            MainFrame.instance.setCurrentForm(clientForm);
    }

}
