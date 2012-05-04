package platform.client.form;

import com.google.common.base.Throwables;
import platform.client.ClientResourceBundle;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.remote.SelectedObject;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientDialog extends ClientModalForm {
    public final static int NOT_CHOSEN = 0;
    public final static int VALUE_CHOSEN = 1;

    public int result = NOT_CHOSEN;
    public Object dialogValue;
    public Object displayValue;

    public KeyEvent initFilterKeyEvent = null;

    private RemoteDialogInterface remoteDialog;

    public ClientDialog(Component owner, final RemoteDialogInterface dialog, EventObject initFilterEvent, boolean isDailog) {
        super(owner, dialog, false, isDailog); // обозначаем parent'а и модальность

        this.initFilterKeyEvent = initFilterEvent instanceof KeyEvent ? (KeyEvent) initFilterEvent : null;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        try {
            Boolean undecorated = dialog.isUndecorated();
            if (undecorated == null || undecorated) {
                setResizable(false);
                // делаем, чтобы не выглядел как диалог
                setUndecorated(true);
            }
        } catch (RemoteException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void windowActivatedFirstTime() {
        int initialFilterPropertyDrawID = -1;
        try {
            Integer filterPropertyDraw = remoteDialog.getInitFilterPropertyDraw();
            if (filterPropertyDraw != null) {
                initialFilterPropertyDrawID = filterPropertyDraw;
            }
        } catch (RemoteException ignored) {
        }

        if (initialFilterPropertyDrawID > 0) {
            form.selectProperty(initialFilterPropertyDrawID);
        }

        if (initFilterKeyEvent != null && initialFilterPropertyDrawID > 0) {
            form.quickEditFilter(initFilterKeyEvent, initialFilterPropertyDrawID);
        } else {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(form.getComponent());
        }
    }

    // необходим чтобы в диалоге менять формы (панели)
    protected ClientFormController createFormController(boolean isDialog) throws IOException, ClassNotFoundException {
        remoteDialog = (RemoteDialogInterface) remoteForm;

        return new ClientFormController(remoteDialog, null, isDialog, true, false) {
            @Override
            void nullPressed() {
                result = VALUE_CHOSEN;
                dialogValue = null;
                displayValue = null;

                super.nullPressed();
                hideDialog();
            }

            @Override
            public boolean okPressed() {
                result = VALUE_CHOSEN;
                try {
                    SelectedObject selectedObject = remoteDialog.getSelectedObject();
                    dialogValue = selectedObject.value;
                    displayValue = selectedObject.displayValue;
                } catch (RemoteException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.value.of.dialogue"), e);
                }

                if (super.okPressed()) {
                    hideDialog();
                    return false;
                }
                return true;
            }

            @Override
            void closePressed() {
                super.closePressed();
                hideDialog();
            }
        };
    }
}
