package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.rmi.RemoteException;

public class ClientObjectCellView extends ClientObjectView {

    public final boolean show;

    public int getShiftID() {
        return 1000;
    }

    public ClientObjectCellView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView object) throws IOException, ClassNotFoundException {
        super(inStream, containers, object);

        show = inStream.readBoolean();
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {

        if (form.switchClassView(object.groupObject))
            return null;
        else
            return baseType.getEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createObjectDialog(object.getID());
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createObjectDialog(object.getID(), BaseUtils.objectToInt(value));
    }
}
