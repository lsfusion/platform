package platform.client.form;

import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;
import platform.base.BaseUtils;

import javax.swing.*;
import java.io.IOException;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;

public class ClientDialog extends JDialog {

    final ClientNavigator navigator;
    private ClientForm currentForm;

    public ClientDialog(ClientForm owner, RemoteFormInterface dialog) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        //        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).sID);
        navigator = new ClientNavigator(owner.clientNavigator.remoteNavigator) {

           public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
               setCurrentForm(navigator.remoteNavigator.createForm(element.ID,true));
           }
        };

        addWindowListener(new WindowAdapter() {

           public void windowActivated(WindowEvent e) {
               KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm);
           }

        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("Mouse Pressed");
            }
        });

        setCurrentForm(dialog);
    }

    public Object objectChosen(int callerID) throws RemoteException {
       return BaseUtils.intToObject(navigator.remoteNavigator.getDialogObject(callerID));
    }

    public boolean objectChosen;

    boolean isReadOnly() { return true; }

    // необходим чтобы в диалоге менять формы (панели)
    void setCurrentForm(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

       if (currentForm != null) remove(currentForm);
       currentForm = new ClientForm(remoteForm, navigator, isReadOnly()) {

           public boolean okPressed() {
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
