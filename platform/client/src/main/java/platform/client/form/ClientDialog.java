package platform.client.form;

import platform.client.SwingUtils;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.ClientCellView;
import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientObjectView;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.io.IOException;

public class ClientDialog extends JDialog {

    private final ClientNavigator navigator;
    private ClientForm currentForm;

    private final ClientObjectClass cls;

    public ClientDialog(ClientForm owner, ClientCellView cellView, ClientObjectClass icls, Object value) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setLayout(new BorderLayout());

        setBounds(owner.getBounds());

        cls = icls;
//        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).sID);
        navigator = new ClientNavigator(owner.clientNavigator.remoteNavigator) {

            public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
                setCurrentForm(navigator.remoteNavigator.createForm(element.ID,true));
            }
        };

        // помечаем кэш чтобы был поиск
        if (value instanceof Integer)
            navigator.remoteNavigator.addCacheObject(cls.ID, (Integer)value);

        // создаем слева навигаторы
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

        if(cellView instanceof ClientPropertyView)
            setCurrentForm(navigator.remoteNavigator.createChangeForm(((ClientPropertyView)cellView).ID));
        else
            setCurrentForm(navigator.remoteNavigator.createClassForm(((ClientObjectView)cellView).object.ID));
    }

    private boolean objectChosen = false;

    public boolean showObjectDialog() {

        setVisible(true);
        return objectChosen;
    }

    public Object objectChosen() throws RemoteException {

        int objectID = navigator.remoteNavigator.getCacheObject(cls.ID); //navigator.remoteNavigator.getLeadObject();
        if (objectID == -1) return null;

        return objectID;
    }

    // необходим чтобы в диалоге менять формы (панели)
    void setCurrentForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        if (currentForm != null) remove(currentForm);
        currentForm = new ClientForm(remoteForm, navigator) {

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
        add(currentForm, BorderLayout.CENTER);

        validate();
    }
}