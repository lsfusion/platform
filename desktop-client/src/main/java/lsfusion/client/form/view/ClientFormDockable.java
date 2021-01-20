package lsfusion.client.form.view;

import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.CloseAllAction;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.*;
import java.util.List;

public class ClientFormDockable extends ClientDockable {

    private ClientFormController form;

    public ClientFormDockable(String caption, FormsController formsController, List<ClientDockable> openedForms, boolean async) {
        super(null, formsController);
        setCaption(caption, null);
        addAction(new CloseAllAction(openedForms));
        this.async = async;
    }

    public void asyncInit() {
        setContent(new JLabel(ClientImages.get("loading.gif")));
    }

    public void init(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, ClientForm clientForm, final MainFrame.FormCloseListener closeListener, byte[] firstChanges) {
        this.form = new ClientFormController(canonicalName, formSID, remoteForm, clientForm, firstChanges, navigator, false, false) {
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
                ClientFormDockable.this.form.getLayout().setBlocked(true);
                ClientFormDockable.this.blockView();
            }

            @Override
            public void unblockView() {
                ClientFormDockable.this.form.getLayout().setBlocked(false);
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

        setContent(this.form.getLayout());
        async = false;
    }

    public void setCaption(String caption, String tooltip) {
        setTitleText(caption);
        setTitleToolTip(tooltip);
    }

    @Override
    public void onClosing() {
        if(async) {
            ((DockableMainFrame) MainFrame.instance).removeOpenForm(getTitleText());
            super.onClosing();
        } else {
            RmiQueue.runAction(new Runnable() {
                @Override
                public void run() {
                    form.closePressed();
                }
            });
        }
    }

    @Override
    public void onClosed() {
        super.onClosed();

        // удаляем ссылку на clientForm, поскольку ClientFormDockable совершенно не собирается быть собранным сборщиком мусора,
        // поскольку на него хранят ссылку внутренние объекты DockingFrames
       if(!async) {
           form.closed();
           form = null;
       }

        // на всякий случай
        System.gc();
    }

    @Override
    public void onShowingChanged(boolean oldShowing, boolean newShowing) {
        if (form != null) {
            form.setSelected(newShowing);
        }
    }

    @Override
    public void onOpened() {
        if (form != null)
            MainFrame.instance.setCurrentForm(form);
    }

    @Override
    protected boolean focusDefaultComponent() {
        boolean focusReceived = super.focusDefaultComponent();
        if (!focusReceived && form != null) {
            return form.focusFirstComponent();
        }
        return false;
    }
}
