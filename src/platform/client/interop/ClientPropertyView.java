package platform.client.interop;

import platform.client.form.ClientForm;

import java.io.DataInputStream;
import java.io.IOException;
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

    protected ClientObjectValue getEditorObjectValue(ClientForm form, Object value, boolean externalID) {

        ClientChangeValue changeValue = ByteDeSerializer.deserializeClientChangeValue(form.remoteForm.getPropertyEditorObjectValueByteArray(this.ID, externalID));
        if (changeValue == null) return null;

        return changeValue.getObjectValue(value);
    }
}
