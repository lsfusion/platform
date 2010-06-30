package platform.client.form;

import platform.client.SwingUtils;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ClientDialog extends JDialog {

    final ClientNavigator navigator;
    private ClientForm currentForm;

    public ClientDialog(ClientForm owner, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        //        RemoteNavigator remoteNavigator = owner.remoteForm.getNavigator(((ClientPropertyView)owner.editingCell).sID);
        navigator = new ClientNavigator(owner.clientNavigator.remoteNavigator) {

           public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
//               setCurrentForm(((RemoteDialogInterface)currentForm.remoteForm).createFormDialog(remoteNavigator));
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

    public static int NOT_CHOSEN = 0;
    public static int CHOSEN_VALUE = 1;
    public static int CHOSEN_NULL = 2;

    public int objectChosen = NOT_CHOSEN;

    boolean isReadOnlyMode() { return true; }

    // необходим чтобы в диалоге менять формы (панели)
    void setCurrentForm(RemoteDialogInterface remoteForm) throws IOException, ClassNotFoundException {

       if (currentForm != null) remove(currentForm);
       currentForm = new ClientForm(remoteForm, navigator) {

           @Override
           public boolean isDialogMode() {
               return true;
           }

           @Override
           public boolean isReadOnlyMode() {
               return super.isReadOnlyMode() || ClientDialog.this.isReadOnlyMode();
           }

           @Override
           boolean nullPressed() {

               objectChosen = CHOSEN_NULL;
               ClientDialog.this.setVisible(false);
               return true;
           }

           @Override
           public boolean okPressed() {

               objectChosen = CHOSEN_VALUE;
               ClientDialog.this.setVisible(false);
               return true;
           }

           @Override
           boolean closePressed() {

               ClientDialog.this.setVisible(false);
               return true;
           }
       };
       add(currentForm, BorderLayout.CENTER);

       validate();
    }

}
