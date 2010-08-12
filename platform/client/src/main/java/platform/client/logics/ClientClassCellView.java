package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.editor.ClassPropertyEditor;
import platform.client.logics.classes.ClientObjectClass;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

public class ClientClassCellView extends ClientObjectView {

    public int getShiftID() {
        return 2000;
    }

    ClientClassCellView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView object) throws IOException, ClassNotFoundException {
        super(inStream, containers, object);
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        if (form.remoteForm.canChangeClass(object.getID())) {
            return new ClassPropertyEditor(form.getComponent(), (ClientObjectClass)object.baseClass, (ClientObjectClass)value);
        } else {
            return null;
        }
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return null;
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return null;
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return null;
    }
}
