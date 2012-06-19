package platform.client.form.editor;

import platform.client.form.ClientDialog;
import platform.client.form.ClientNavigatorDialog;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.cell.PropertyTableCellEditor;
import platform.interop.KeyStrokes;
import platform.interop.form.RemoteDialogInterface;

import javax.swing.*;
import java.awt.*;
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

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject event) {
        if (KeyStrokes.isSpaceEvent(event) || KeyStrokes.isObjectEditorDialogEvent(event)) {
            clientDialog = new ClientNavigatorDialog(owner, dialog, isDialog);
        } else {
            clientDialog = new ClientDialog(owner, dialog, event, isDialog);
        }

        clientDialog.showDialog(false, translate(tableLocation, (int) cellRectangle.getX(), (int) cellRectangle.getMaxY()));

        dialog = null; // лучше сбрасывать ссылку, чтобы раньше начал отрабатывать сборщик мусора

        return null;
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        //пока не нужен
    }

    public Object getCellEditorValue() {
        return clientDialog.dialogValue;
    }

    public Object getCellDisplayValue() throws RemoteException {
        return clientDialog.displayValue;
    }

    public boolean valueChanged() {
        return clientDialog.result == ClientDialog.VALUE_CHOSEN;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }
}
