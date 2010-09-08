package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientType;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Collection;

public class ClientPropertyDraw extends ClientCell {

    private int ID = 0;
    private String sID;

    public ClientGroupObject groupObject;
    public ClientGroupObject[] columnGroupObjects;
    public ClientPropertyDraw[] columnDisplayProperties;
    public int[] columnDisplayPropertiesIds;

    public boolean autoHide = false;

    public ClientPropertyDraw(DataInputStream inStream, Collection<ClientContainer> containers, Collection<ClientGroupObject> groups) throws IOException, ClassNotFoundException {
        super(inStream, containers);

        ID = inStream.readInt();
        sID = inStream.readUTF();
        if (inStream.readBoolean()) {
            int groupID = inStream.readInt();
            groupObject = getClientGroupObject(groups, groupID);
        }

        if (inStream.readBoolean()) {
            int length = inStream.readInt();
            columnGroupObjects = new ClientGroupObject[length];
            columnDisplayPropertiesIds = new int[length];
            for (int i = 0; i < length; ++i) {
                int groupID = inStream.readInt();
                columnGroupObjects[i] = getClientGroupObject(groups, groupID);
            }
            for (int i = 0; i < length; ++i) {
                columnDisplayPropertiesIds[i] = inStream.readInt();
            }
        } else {
            //чтобы не проверять везде на null
            columnGroupObjects = new ClientGroupObject[0];
            columnDisplayPropertiesIds = new int[0];
        }

        autoHide = inStream.readBoolean();
    }

    private ClientGroupObject getClientGroupObject(Collection<ClientGroupObject> groups, int groupID) {
        for (ClientGroupObject group : groups) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    public ClientGroupObject getGroupObject() {
        return groupObject;
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        ClientType changeType = getPropertyChangeType(form);
        if (changeType == null) return null;
        return changeType.getEditorComponent(form, this, value, getFormat(), design);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, Object value) throws IOException, ClassNotFoundException {
        return baseType.getClassComponent(form, this, value, getFormat());
    }

    public RemoteDialogInterface createEditorForm(RemoteFormInterface form) throws RemoteException {
        return form.createEditorPropertyDialog(ID);
    }

    public RemoteDialogInterface createClassForm(RemoteFormInterface form, Integer value) throws RemoteException {
        return form.createClassPropertyDialog(ID, BaseUtils.objectToInt(value));
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        return sID;
    }

    public int getShiftID() {
        return 0;
    }

    public ClientType getPropertyChangeType(ClientFormController form) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(form.remoteForm.getPropertyChangeType(this.ID)));
        if (inStream.readBoolean()) return null;

        return ClientTypeSerializer.deserialize(inStream);
    }

    public Object parseString(ClientFormController form, String s) throws ParseException {
        ClientType changeType = null;
        try {
            changeType = getPropertyChangeType(form);
            if (changeType == null) throw new ParseException("PropertyView не может быть изменено.", 0);

            return changeType.parseString(s);
        } catch (IOException e) {
            throw new ParseException("Ошибка получения данных о propertyChangeType.", 0);
        }
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return baseType.shouldBeDrawn(form);
    }
}
