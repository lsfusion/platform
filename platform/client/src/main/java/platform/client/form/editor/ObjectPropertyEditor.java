package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.ClientDialog;
import platform.client.form.ClientNavigatorDialog;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.io.IOException;
import java.util.EventObject;

public class ObjectPropertyEditor extends JDialog implements PropertyEditorComponent {

    private final ClientForm owner;
    private final RemoteDialogInterface dialog;

    private ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm owner, RemoteDialogInterface dialog) {

        this.owner = owner;
        this.dialog = dialog;
    }

    Object objectChosen() throws RemoteException {
        return dialog.getDialogValue();
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {

        if (editEvent instanceof KeyEvent && ((KeyEvent)editEvent).getKeyCode() == KeyEvent.VK_SPACE) {
            clientDialog = new ClientNavigatorDialog(owner,dialog);
            clientDialog.setBounds(owner.getBounds());
        } else
        {
            clientDialog = new ClientDialog(owner,dialog);
            clientDialog.setLocation(new Point((int)(tableLocation.getX() + cellRectangle.getX()), (int)(tableLocation.getY() + cellRectangle.getMaxY())));
            clientDialog.setSize(500, 200);
        }

        clientDialog.setVisible(true);
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return objectChosen();
    }

    public boolean valueChanged() {
        return clientDialog.objectChosen;
    }
}
