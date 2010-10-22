package platform.client.logics;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.base.OrderedMap;
import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClientGroupObject extends ArrayList<ClientObject>
                                 implements ClientPropertyRead, ClientIdentitySerializable {

    private int ID;
    public byte banClassView = 0;

    public ClientGrid grid = new ClientGrid();
    public ClientShowType showType = new ClientShowType();

    public ClientGroupObject() {
    }

    public List<ClientObject> getDeserializeList(Map<ClientGroupObject, Byte> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        List<ClientObject> result = new ArrayList<ClientObject>();
        Byte newType = classViews.get(this);
        if((newType!=null?newType:controllers.get(this).classView) == ClassViewType.GRID)
            result.addAll(this);
        return result;
    }

    public List<ClientObject> getDeserializeList(Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, Byte> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        return getDeserializeList(classViews, controllers);
    }

    public void update(Map<ClientGroupObjectValue, Object> readKeys, GroupObjectController controller) {
        controller.updateDrawHighlightValues(readKeys);
    }

    public ClientGroupObject getGroupObject() {
        return this;
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + new Integer(ID).hashCode();
        return hash;
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private String actionID = null;
    public String getActionID() {
        if(actionID==null)
            actionID = "changeGroupObject" + idGenerator.idShift();
        return actionID;
    }

    public int getID() {
        return ID;
    }

    public void setID(int iID) {
        ID = iID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        banClassView = inStream.readByte();

        pool.deserializeCollection(this, inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);
    }

    public static List<ClientGroupObjectValue> mergeGroupValues(OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<ClientGroupObjectValue>();
        propColumnKeys.add(new ClientGroupObjectValue());
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : groupColumnKeys.entrySet()) {
            List<ClientGroupObjectValue> groupObjectKeys = entry.getValue();

            List<ClientGroupObjectValue> newPropColumnKeys = new ArrayList<ClientGroupObjectValue>();
            for (ClientGroupObjectValue propColumnKey : propColumnKeys) {
                for (ClientGroupObjectValue groupObjectKey : groupObjectKeys) {
                    newPropColumnKeys.add(new ClientGroupObjectValue(propColumnKey, groupObjectKey));
                }
            }
            propColumnKeys = newPropColumnKeys;
        }
        return propColumnKeys;
    }

    @Override
    public String toString() {
        if (size() == 0) {
            return "Пустая группа";
        }
        
        String result = "";
        for (ClientObject object : this) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.toString();
        }
        return result;
    }
}
