package platform.client.logics;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.base.OrderedMap;
import platform.client.form.GroupObjectController;
import platform.client.form.ClientFormController;
import platform.interop.ClassViewType;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ClientGroupObject extends ArrayList<ClientObject>
                                 implements Serializable, ClientPropertyRead {

    private Integer ID = 0;

    public Integer getID() {
        return ID;
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
        hash = 23 * hash + (this.ID != null ? this.ID.hashCode() : 0);
        return hash;
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

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private String actionID = null;
    public String getActionID() {
        if(actionID==null)
            actionID = "changeGroupObject" + idGenerator.idShift();
        return actionID;
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
