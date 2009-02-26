package platform.client.interop;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientObjectView extends ClientCellView {

    public ClientObjectImplementView object;

    public ClientObjectView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView iObject) throws IOException, ClassNotFoundException {
        super(inStream, containers);
        object = iObject;
    }

    public int getID() {
        return object.ID;
    }

    public ClientGroupObjectImplementView getGroupObject() {
        return object.groupObject;
    }

    public int getMaximumWidth() {
        return getPreferredWidth();
    }

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) {

        if (externalID) return null;

        if (!object.groupObject.singleViewType) {
            form.switchClassView(object.groupObject);
            return null;
        } else
            return super.getEditorComponent(form, value, isDataChanging, externalID);
    }

}
