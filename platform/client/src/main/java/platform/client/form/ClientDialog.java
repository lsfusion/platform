package platform.client.form;

import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

public class ClientDialog extends JDialog {

    private final ClientNavigator navigator;
    private ClientForm currentForm;

    public ClientDialog(ClientForm owner, RemoteFormInterface dialog) throws IOException, ClassNotFoundException {
       super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

       setLayout(new BorderLayout());

       setBounds(owner.getBounds());

    //        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).sID);
       navigator = new ClientNavigator(owner.clientNavigator.remoteNavigator) {

           public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
               setCurrentForm(navigator.remoteNavigator.createForm(element.ID,true));
           }
       };

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

       setCurrentForm(dialog);
    }

    public Object objectChosen() throws RemoteException {
       return BaseUtils.intToObject(navigator.remoteNavigator.getDialogObject());
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

    public boolean objectChosen;
    public Component getComponent() {

       setVisible(true);

       return null;
    }
}
