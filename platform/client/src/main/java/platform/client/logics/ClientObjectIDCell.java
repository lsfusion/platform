package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.interop.ClassViewType;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

public class ClientObjectIDCell extends ClientObjectCell {

    public int getShiftID() {
        return 1000;
    }

    public ClientObjectIDCell(DataInputStream inStream, Collection<ClientContainer> containers, ClientObject object) throws IOException, ClassNotFoundException {
        super(inStream, containers, object);
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {

        if ((object.groupObject.banClassView & (ClassViewType.GRID | ClassViewType.PANEL)) == 0) {
            form.switchClassView(object.groupObject);
            return null;
        } else
            return baseType.getEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createObjectDialog(object.getID());
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createObjectDialogWithValue(object.getID(), BaseUtils.objectToInt(value));
    }

    @Override
    public boolean checkEquals() {
        return false;
    }
}
