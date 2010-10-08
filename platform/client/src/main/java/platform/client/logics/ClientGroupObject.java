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
import java.io.Serializable;
import java.util.*;

public class ClientGroupObject extends ArrayList<ClientObject>
                                 implements Serializable, ClientPropertyRead, ClientIdentitySerializable {

    private int ID;

    public ClientGroupObject() {

    }

    public ClientGroupObject(DataInputStream inStream, Collection<ClientContainer> containers) throws IOException, ClassNotFoundException {
        ID = inStream.readInt();
        banClassView = inStream.readByte();

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            add(new ClientObject(inStream,containers,this));

        grid = new ClientGrid(inStream, containers);
        showType = new ClientShowType(inStream, containers);
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

    public byte banClassView = 0;

    public ClientGrid grid;
    public ClientShowType showType;

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

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        outStream.writeByte(banClassView);
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
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
}
