package platformlocal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

public class ClientDialog extends JDialog {

    ClientNavigator navigator;
    ClientForm currentForm;

    ClientClass cls;

    ClientDialog(ClientForm owner, ClientClass icls, Object value) {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL);

        setLayout(new BorderLayout());

        setBounds(owner.getBounds());

        cls = icls;
//        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).sID);
        navigator = new ClientNavigator(owner.clientNavigator.remoteNavigator) {

            public void openForm(ClientNavigatorForm element) {
                setCurrentForm(element.ID);
            }
        };

        if (value instanceof Integer)
            navigator.remoteNavigator.addCacheObject(cls.ID, (Integer)value);

        JPanel navigatorPanel = new JPanel();
        navigatorPanel.setLayout(new BoxLayout(navigatorPanel, BoxLayout.Y_AXIS));

        navigatorPanel.add(navigator);
        navigatorPanel.add(navigator.relevantFormNavigator);
        navigatorPanel.add(navigator.relevantClassNavigator);

        add(navigatorPanel, BorderLayout.LINE_START);

        addWindowListener(new WindowAdapter() {

            public void windowActivated(WindowEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm);
            }

        });

        setCurrentForm(navigator.remoteNavigator.getDefaultForm(cls.ID));
    }

    private boolean objectChosen = false;

    public boolean showObjectDialog() {

        setVisible(true);
        return objectChosen;
    }

    public Object objectChosen() {

        int objectID = navigator.remoteNavigator.getCacheObject(cls.ID); //navigator.remoteNavigator.getLeadObject();
        if (objectID == -1) return null;

        return objectID;
    }

    public void setCurrentForm(int formID) {

        if (formID == -1) return;

        if (currentForm != null) remove(currentForm);
        try {
            currentForm = new ClientForm(navigator.remoteNavigator.CreateForm(formID, true), navigator) {

                boolean okPressed() {
//                    if (super.okPressed()) {
                        objectChosen = true;
                        ClientDialog.this.setVisible(false);
                        return true;
//                    } else
//                        return false;
                }

                boolean closePressed() {
//                    if (super.closePressed()) {
                        ClientDialog.this.setVisible(false);
                        return true;
//                    } else
//                        return false;
                }
            };
        } catch (SQLException e) {
            e.printStackTrace();
        }
        add(currentForm, BorderLayout.CENTER);

        validate();
    }
}