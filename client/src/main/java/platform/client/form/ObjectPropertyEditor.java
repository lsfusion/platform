package platform.client.form;

import platform.client.logics.ClientCellView;
import platform.client.logics.classes.ClientClass;

import java.awt.*;
import java.rmi.RemoteException;
import java.io.IOException;

public class ObjectPropertyEditor implements PropertyEditorComponent {

    ClientForm clientForm;
    ClientCellView property;

    ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm iclientForm, ClientCellView iproperty, ClientClass cls, Object value) throws IOException, ClassNotFoundException {

        clientForm = iclientForm;
        property = iproperty;

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
