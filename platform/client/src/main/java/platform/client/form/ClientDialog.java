package platform.client.form;

import platform.client.SwingUtils;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ClientDialog extends JDialog {

    private ClientFormController currentForm;

    public ClientDialog(Component owner, RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        addWindowListener(new WindowAdapter() {

            public void windowActivated(WindowEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm.getComponent());
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

    boolean isReadOnlyMode() {
        return true;
    }

    // необходим чтобы в диалоге менять формы (панели)
    void setCurrentForm(RemoteDialogInterface remoteForm) throws IOException, ClassNotFoundException {

        if (currentForm != null) remove(currentForm.getComponent());
        currentForm = new ClientFormController(remoteForm, null) {

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
            public void okPressed() {

                objectChosen = CHOSEN_VALUE;
                ClientDialog.this.setVisible(false);
            }

            @Override
            boolean closePressed() {

                ClientDialog.this.setVisible(false);
                return true;
            }
        };
        add(currentForm.getComponent(), BorderLayout.CENTER);

        validate();
    }

}
