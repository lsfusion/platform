package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;

public class ClientPropertyView extends ClientCellView {

    public Integer ID = 0;

    public ClientGroupObjectImplementView groupObject;

    public ClientPropertyView(DataInputStream inStream, Collection<ClientContainerView> containers, Collection<ClientGroupObjectImplementView> groups) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        if(!inStream.readBoolean()) {
            int groupID = inStream.readInt();
            for(ClientGroupObjectImplementView group : groups)
                if(group.ID==groupID) {
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

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) throws IOException, ClassNotFoundException {

        ClientObjectValue objectValue;
        if (isDataChanging) {
            ClientChangeValue changeValue = ClientChangeValue.deserialize(new DataInputStream(new ByteArrayInputStream(
                    form.remoteForm.getPropertyChangeValueByteArray(this.ID, externalID))));
            if (changeValue == null) return null;

            objectValue = changeValue.getObjectValue(value);
        } else
            objectValue = new ClientObjectValue(ClientClass.deserialize(new DataInputStream(new ByteArrayInputStream(
                    form.remoteForm.getPropertyValueClassByteArray(this.ID)))),value);

        return objectValue.cls.getEditorComponent(form, this, objectValue.object, getFormat());
    }
}
