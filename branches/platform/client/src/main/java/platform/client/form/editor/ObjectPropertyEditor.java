package platform.client.form.editor;

import platform.client.SwingUtils;
import platform.client.form.ClientDialog;
import platform.client.form.ClientNavigatorDialog;
import platform.client.form.PropertyEditorComponent;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ObjectPropertyEditor extends JDialog implements PropertyEditorComponent {

    private final Component owner;
    private RemoteDialogInterface dialog;

    private ClientDialog clientDialog;

    public ObjectPropertyEditor(Component owner, RemoteDialogInterface dialog) {

        this.owner = owner;
        this.dialog = dialog;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {

        if (editEvent instanceof KeyEvent && ((KeyEvent)editEvent).getKeyCode() == KeyEvent.VK_SPACE) {
            clientDialog = new ClientNavigatorDialog(owner, dialog);
            clientDialog.setBounds(owner.getBounds());
        } else {
            clientDialog = new ClientDialog(owner, dialog);
            clientDialog.setSize(500, 300);
            SwingUtils.requestLocation(clientDialog, new Point((int)(tableLocation.getX() + cellRectangle.getX()), (int)(tableLocation.getY() + cellRectangle.getMaxY())));
        }
        dialog = null; // лучше сбрасывать ссылку, чтобы раньше начал отрабатывать сборщик мусора

        clientDialog.setVisible(true);
        clientDialog.dispose(); // приходится в явную делать dispose, поскольку dialog может закрываться через setVisible
        clientDialog.closed();
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return clientDialog.dialogValue;
    }

    public boolean valueChanged() {
        return clientDialog.objectChosen != ClientDialog.NOT_CHOSEN;
    }
}
