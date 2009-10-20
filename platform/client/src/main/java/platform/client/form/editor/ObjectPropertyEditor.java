package platform.client.form.editor;

import platform.client.logics.ClientCellView;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.ClientDialog;

import java.awt.*;
import java.rmi.RemoteException;
import java.io.IOException;

public class ObjectPropertyEditor implements PropertyEditorComponent {

    private ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm clientForm, ClientCellView cellView, ClientObjectClass cls, Object value) throws IOException, ClassNotFoundException {

        clientDialog = new ClientDialog(clientForm, cellView, cls, value);
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
