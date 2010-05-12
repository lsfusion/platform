package platform.client.logics;

import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.editor.ClassPropertyEditor;
import platform.client.logics.classes.ClientConcreteClass;

import java.rmi.RemoteException;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.Collection;

public class ClientClassCellView extends ClientObjectView {

    public final boolean show;

    public int getShiftID() {
        return 2000;
    }

    ClientClassCellView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView object) throws IOException, ClassNotFoundException {
        super(inStream, containers, object);

        show = inStream.readBoolean();
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return new ClassPropertyEditor(form, object, (ClientConcreteClass)value);
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return null;
    }

    public RemoteFormInterface createEditorForm(RemoteNavigatorInterface navigator, int callerID) throws RemoteException {
        return null;
    }

    public RemoteFormInterface createClassForm(RemoteNavigatorInterface navigator, int callerID, Integer value) throws RemoteException {
        return null;
    }
}
