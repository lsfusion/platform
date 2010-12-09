package platform.client.form;

import platform.client.SwingUtils;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.rmi.RemoteException;

public class ClientDialog extends JDialog {

    private ClientFormController currentForm;
    public boolean showQuickFilterOnStartup = true;

    public ClientDialog(Component owner, final RemoteDialogInterface dialog) throws IOException, ClassNotFoundException {
        super(SwingUtils.getWindow(owner), Dialog.ModalityType.DOCUMENT_MODAL); // обозначаем parent'а и модальность

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // делаем, чтобы не выглядел как диалог
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray, 1));

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                int initialFilterPropertyDrawID = -1;
                if (showQuickFilterOnStartup) {
                    showQuickFilterOnStartup = false;
                    try {
                         initialFilterPropertyDrawID = dialog.getInitFilterPropertyDraw();
                    } catch (RemoteException ignored) {
                    }
                }

                if (initialFilterPropertyDrawID > 0) {
                    currentForm.quickEditFilter(initialFilterPropertyDrawID);
                } else {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(currentForm.getComponent());
                }
            }
        });

        setCurrentForm(dialog);
    }

    public static int NOT_CHOSEN = 0;
    public static int CHOSEN_VALUE = 1;

    public int objectChosen = NOT_CHOSEN;
    public Object dialogValue;

    boolean isReadOnlyMode() {
        return true;
    }

    // необходим чтобы в диалоге менять формы (панели)
    void setCurrentForm(final RemoteDialogInterface remoteDialog) throws IOException, ClassNotFoundException {

        if (currentForm != null) remove(currentForm.getComponent());
        currentForm = new ClientFormController(remoteDialog, null) {

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

                objectChosen = CHOSEN_VALUE;
                dialogValue = null;
                ClientDialog.this.setVisible(false);
                return true;
            }

            @Override
            public void okPressed() {

                objectChosen = CHOSEN_VALUE;
                try {
                    dialogValue = remoteDialog.getDialogValue();
                } catch (RemoteException e) {
                    throw new RuntimeException("Ошибка при получении значения диалога", e);
                }
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

    public void closed() {
        if (currentForm != null)
            currentForm.closed();
    }

    public Dimension calculatePreferredSize() {
        Dimension preferredSize = currentForm.calculatePreferredSize();

        // так как у нас есть только preferredSize самого contentPane, а нам нужен у JDialog
        // сколько будет занимать все "рюшечки" вокруг contentPane мы посчитать не можем, поскольку
        preferredSize.width += 20;
        preferredSize.height += 60;

        return preferredSize;
    }
}
