package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.interop.ClassViewType;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

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

        if ((object.groupObject.banClassView & (ClassViewType.GRID | ClassViewType.PANEL)) == 0) {
            form.switchClassView(object.groupObject);
            return null;
        } else
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

    @Override
    public boolean checkEquals() {
        return false;
    }
}
