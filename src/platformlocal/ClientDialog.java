package platformlocal;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class ClientDialog extends JDialog {

    ClientNavigator navigator;
    ClientForm currentForm;

    ClientDialog(ClientForm owner) {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL);

        setLayout(new BorderLayout());

        setBounds(owner.getBounds());

        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).ID);
        navigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) {
                setCurrentForm(element.ID);
            }
        };

        add(navigator, BorderLayout.LINE_START);

        setCurrentForm(navigator.remoteNavigator.getDefaultForm());
    }

    private boolean objectChosen = false;

    public boolean showObjectDialog() {

        setVisible(true);
        return objectChosen;
    }

    public Object objectChosen() {

        return 22;
    }

    public void setCurrentForm(int formID) {

        if (currentForm != null) remove(currentForm);
        try {
            currentForm = new ClientForm(navigator.remoteNavigator.CreateForm(formID)) {

                boolean okPressed() {
                    if (super.okPressed()) {
                        objectChosen = true;
                        ClientDialog.this.setVisible(false);
                        return true;
                    } else
                        return false;
                }

                boolean closePressed() {
                    if (super.closePressed()) {
                        ClientDialog.this.setVisible(false);
                        return true;
                    } else
                        return false;
                }
            };
        } catch (SQLException e) {
            e.printStackTrace();
        }
        add(currentForm, BorderLayout.CENTER);

        validate();
    }
}
