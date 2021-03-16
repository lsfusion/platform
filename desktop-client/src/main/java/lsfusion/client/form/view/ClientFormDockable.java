package lsfusion.client.form.view;

import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.IOException;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController clientForm;

    public ClientFormDockable(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, FormsController formsController, final MainFrame.FormCloseListener closeListener, byte[] firstChanges) throws IOException {
        super(canonicalName, formsController);

        clientForm = new ClientFormController(canonicalName, formSID, remoteForm, firstChanges, navigator, false, false) {
            @Override
            public void onFormHidden() {
                if (control() != null) {
                    setVisible(false);
                }
                
                if (closeListener != null) {
                    closeListener.formClosed(false);
                }
                super.onFormHidden();
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
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

            @Override
            public void setSelected(boolean newSelected) {
                super.setSelected(newSelected);
                if (newSelected) {
                    requestFocusInWindow();
                }
            }
        };

        setContent(clientForm.getLayout());
    }

    public void setCaption(String caption, String tooltip) {
        setTitleText(caption);
        setTitleToolTip(tooltip);
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

    @Override
    protected boolean activateFirstComponents() {
        return clientForm.activateFirstComponents();
    }

    @Override
    protected boolean focusDefaultComponent() {
        boolean focusReceived = super.focusDefaultComponent();
        if (!focusReceived && clientForm != null) {
            return clientForm.focusFirstComponent();
        }
        return false;
    }
}
