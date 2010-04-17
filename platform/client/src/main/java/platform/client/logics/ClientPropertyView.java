package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.rmi.RemoteException;

public class ClientPropertyView extends ClientCellView {

    public Integer ID = 0;

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

    public int getID() {
        return ID;
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {

        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(this.ID)));
        if(inStream.readBoolean()) return null;

        return ClientTypeSerializer.deserialize(inStream).getEditorComponent(form, this, value, getFormat());
    }

    public PropertyEditorComponent getClassComponent(ClientForm form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteFormInterface createForm(RemoteNavigatorInterface navigator) throws RemoteException {
        return navigator.createChangeForm(ID);
    }

    public RemoteFormInterface createClassForm(RemoteNavigatorInterface navigator, Integer value) throws RemoteException {
        return navigator.createPropertyForm(ID, BaseUtils.objectToInt(value));
    }
}
