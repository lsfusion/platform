package platform.client.form.editor;

import platform.client.logics.ClientCellView;
import platform.client.logics.ClientPropertyView;
import platform.client.logics.ClientObjectView;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.ClientDialog;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.SwingUtils;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.io.IOException;

public class ObjectPropertyEditor extends JDialog implements PropertyEditorComponent {

    public final ClientDialog clientDialog;

    public ObjectPropertyEditor(ClientForm owner, RemoteFormInterface dialog) throws IOException, ClassNotFoundException {

        clientDialog = new ClientDialog(owner,dialog);
    }

    public Object objectChosen() throws RemoteException {
        return clientDialog.objectChosen();
    }

    public Component getComponent() {

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
