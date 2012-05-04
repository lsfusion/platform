package platform.client.form.editor;

import platform.client.form.ClientDialog;
import platform.client.form.ClientNavigatorDialog;
import platform.client.form.PropertyEditorComponent;
import platform.interop.KeyStrokes;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

import static platform.client.SwingUtils.translate;

public class ObjectPropertyEditor extends JDialog implements PropertyEditorComponent {

    private final Component owner;
    private RemoteDialogInterface dialog;

    private ClientDialog clientDialog;
    private boolean isDialog;

    public ObjectPropertyEditor(Component owner, RemoteDialogInterface dialog, boolean isDialog) {

        this.owner = owner;
        this.dialog = dialog;
        this.isDialog = isDialog;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject event) throws IOException, ClassNotFoundException {
        if (KeyStrokes.isSpaceEvent(event) || KeyStrokes.isObjectEditorDialogEvent(event)) {
            clientDialog = new ClientNavigatorDialog(owner, dialog, isDialog);
        } else {
            clientDialog = new ClientDialog(owner, dialog, event instanceof KeyEvent, isDialog);
        }

        clientDialog.showDialog(false, translate(tableLocation, (int) cellRectangle.getX(), (int) cellRectangle.getMaxY()));

        dialog = null; // лучше сбрасывать ссылку, чтобы раньше начал отрабатывать сборщик мусора

        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return clientDialog.dialogValue;
    }

    public Object getCellDisplayValue() throws RemoteException {
        return clientDialog.displayValue;
    }

    public boolean valueChanged() {
        return clientDialog.result == ClientDialog.VALUE_CHOSEN;
    }

    @Override
    public String checkValue(Object value){
        return null;
    }
}
