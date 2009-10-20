package platform.client.logics;

import platform.client.form.ClientForm;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientDataClass;
import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;

public class ClientObjectView extends ClientCellView {

    public ClientObjectImplementView object;

    public final boolean show;

    public ClientObjectView(DataInputStream inStream, Collection<ClientContainerView> containers, ClientObjectImplementView iObject) throws IOException, ClassNotFoundException {
        super(inStream, containers);
        object = iObject;

        show = inStream.readBoolean();
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

    public PropertyEditorComponent getEditorComponent(ClientForm form, Object value, boolean isDataChanging, boolean externalID) throws IOException, ClassNotFoundException {

        if (externalID) return null;

        if (!object.groupObject.singleViewType) {
            form.switchClassView(object.groupObject);
            return null;
        } else {
            ClientClass cls;
            if(baseType instanceof ClientDataClass)
                cls = (ClientDataClass)baseType;
            else // идиотизм конечно но пока ладно
                cls = form.models.get(object.groupObject).objects.get(object).classModel.currentClass;

            return cls.getEditorComponent(form, this, value, getFormat());
        }
    }

}
