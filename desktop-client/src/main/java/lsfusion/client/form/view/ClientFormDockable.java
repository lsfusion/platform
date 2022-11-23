package lsfusion.client.form.view;

import lsfusion.client.base.view.ClientDockable;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.CloseAllAction;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.navigator.controller.AsyncFormController;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class ClientFormDockable extends ClientDockable {

    private FormsController formsController;
    private ClientFormController form;

    public ClientFormDockable(String canonicalName, String caption, FormsController formsController, List<ClientDockable> openedForms, AsyncFormController asyncFormController, boolean async) {
        super(canonicalName, formsController);
        setCaption(caption, null);
        addAction(new CloseAllAction(openedForms));
        this.formsController = formsController;
        this.async = async;
        this.asyncFormController = asyncFormController;
    }

    public void asyncInit() {
        setContent(new JLabel(new ImageIcon(ClientImages.get("loading_async.gif").getImage().getScaledInstance(32, 32, Image.SCALE_DEFAULT))));
    }

    public void init(ClientNavigator navigator, String canonicalName, String formSID, RemoteFormInterface remoteForm, ClientForm clientForm, final MainFrame.FormCloseListener closeListener, byte[] firstChanges, String formId) {
        this.form = new ClientFormController(canonicalName, formSID, remoteForm, formsController, clientForm, firstChanges, navigator, false, false) {
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
                if (ClientFormDockable.this.form != null)
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
        onContendAdded();
        async = false;

        this.formId = formId;
    }

    public void setCaption(String caption, String tooltip) {
        setTitleText(caption);
        setTitleToolTip(tooltip);
    }

    @Override
    public void onClosing() {
        if(async) {
            asyncFormController.removeAsyncForm();
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
    protected boolean activateFirstComponents() {
        if (form != null) {
            return form.activateFirstComponents();
        }
        return false;
    }

    @Override
    protected boolean focusDefaultComponent() {
        boolean focusReceived = super.focusDefaultComponent();
        if (!focusReceived && form != null) {
            return form.focusFirstComponent();
        }
        return false;
    }

    public void directProcessKeyEvent(KeyEvent e) {
        if (form != null && !form.isEditing()) {
            form.getLayout().directProcessKeyEvent(e);
        }
    }

    public void focusGained() {
        if(form != null) {
            form.getLayout().focusGained();
        }
    }
}
