package platform.client.form;

import platform.client.interop.ClientCellView;
import platform.client.interop.classes.ClientClass;

import java.awt.*;

public class ObjectPropertyEditor implements PropertyEditorComponent {

    ClientForm clientForm;
    ClientCellView property;

    ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm iclientForm, ClientCellView iproperty, ClientClass cls, Object value) {

        clientForm = iclientForm;
        property = iproperty;

        clientDialog = new ClientDialog(clientForm, cls, value);
    }

    private boolean objectChosen;
    public Component getComponent() {

        objectChosen = clientDialog.showObjectDialog();

        return null;
    }

    public Object getCellEditorValue() {
        return clientDialog.objectChosen();
    }

    public boolean valueChanged() {
        return objectChosen;
    }
}
