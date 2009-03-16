package platform.client.form.editor;

import platform.client.logics.ClientCellView;
import platform.client.logics.classes.ClientClass;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.ClientDialog;

import java.awt.*;
import java.rmi.RemoteException;
import java.io.IOException;

public class ObjectPropertyEditor implements PropertyEditorComponent {

    private final ClientForm clientForm;

    private ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm iclientForm, ClientCellView iproperty, ClientClass cls, Object value) throws IOException, ClassNotFoundException {

        clientForm = iclientForm;

        clientDialog = new ClientDialog(clientForm, cls, value);
    }

    private boolean objectChosen;
    public Component getComponent() {

        objectChosen = clientDialog.showObjectDialog();

        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return clientDialog.objectChosen();
    }

    public boolean valueChanged() {
        return objectChosen;
    }
}
