package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.rmi.RemoteException;

public class ClientPropertyView extends ClientCellView {

    private int ID = 0;
    public int getID() {
        return ID;
    }

    public int getShiftID() {
        return 0;
    }

    public ClientGroupObjectImplementView groupObject;

    public ClientPropertyView(DataInputStream inStream, Collection<ClientContainerView> containers, Collection<ClientGroupObjectImplementView> groups) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        if(!inStream.readBoolean()) {
            int groupID = inStream.readInt();
            for(ClientGroupObjectImplementView group : groups)
                if(group.getID() == groupID) {
                    groupObject = group;
                    break;
                }
        }
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {

        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(this.ID)));
        if(inStream.readBoolean()) return null;

        return ClientTypeSerializer.deserialize(inStream).getEditorComponent(form, this, value, getFormat(), getFont());
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createEditorPropertyDialog(ID);
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createClassPropertyDialog(ID, BaseUtils.objectToInt(value));
    }
}
