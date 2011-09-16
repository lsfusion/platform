package platform.client.form;

import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.remote.SelectedObject;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.rmi.RemoteException;

public class ClientDialog extends ClientModalForm {
    public final static int NOT_CHOSEN = 0;
    public final static int CHOSEN_VALUE = 1;

    public int objectChosen = NOT_CHOSEN;
    public Object dialogValue;
    public Object displayValue;

    public boolean showQuickFilterOnStartup = true;

    private RemoteDialogInterface remoteDialog;

    public ClientDialog(Component owner, final RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(owner, dialog, false); // обозначаем parent'а и модальность

        Boolean undecorated = dialog.isUndecorated();
        if (undecorated == null || undecorated) {
            setResizable(false);
            // делаем, чтобы не выглядел как диалог
            setUndecorated(true);
        }
    }

    @Override
    protected void createListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                int initialFilterPropertyDrawID = -1;
                try {
                    Integer filterPropertyDraw = remoteDialog.getInitFilterPropertyDraw();
                    if (filterPropertyDraw != null) {
                        initialFilterPropertyDrawID = filterPropertyDraw;
                    }
                } catch (RemoteException ignored) {
                }

                if (initialFilterPropertyDrawID > 0) {
                    currentForm.selectProperty(initialFilterPropertyDrawID);
                }

                if (showQuickFilterOnStartup && initialFilterPropertyDrawID > 0) {
                    currentForm.quickEditFilter(initialFilterPropertyDrawID);
                } else {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm.getComponent());
                }

                //чтобы только 1й активации...
                ClientDialog.this.removeWindowListener(this);
            }
        });
    }


    protected Boolean readOnly;
    boolean isReadOnlyMode() {
        return (readOnly != null) ? readOnly : true;
    }

    // необходим чтобы в диалоге менять формы (панели)
    protected ClientFormController createFormController() throws IOException, ClassNotFoundException {
        remoteDialog = (RemoteDialogInterface) remoteForm;
        readOnly = remoteDialog.isReadOnly();

        return new ClientFormController(remoteDialog, null) {

            @Override
            public boolean isModal() {
                return true;
            }

            @Override
            public boolean isDialog() {
                return true;
            }

            @Override
            public boolean isNewSession() {
                return false;
            }

            @Override
            public boolean isReadOnlyMode() {
                return super.isReadOnlyMode() || ClientDialog.this.isReadOnlyMode();
            }

            @Override
            boolean nullPressed() {

                objectChosen = CHOSEN_VALUE;
                dialogValue = null;
                displayValue = null;
                ClientDialog.this.setVisible(false);
                return true;
            }

            @Override
            public boolean okPressed() {

                objectChosen = CHOSEN_VALUE;
                try {
                    SelectedObject selectedObject = remoteDialog.getSelectedObject();
                    dialogValue = selectedObject.value;
                    displayValue = selectedObject.displayValue;
                } catch (RemoteException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.value.of.dialogue"), e);
                }
                ClientDialog.this.setVisible(false);

                return true;
            }

            @Override
            boolean closePressed() {

                ClientDialog.this.setVisible(false);
                return true;
            }
        };
    }

    public void setDefaultSize() {
        setSize(SwingUtils.clipDimension(calculatePreferredSize(isUndecorated()),
                                         new Dimension(200, 100),
                                         new Dimension(1000, 700)));
    }
}
