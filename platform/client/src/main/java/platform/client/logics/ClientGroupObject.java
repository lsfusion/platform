package platform.client.logics;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.base.OrderedMap;
import platform.client.Main;
import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.form.layout.AbstractGroupObjectView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientGroupObject extends ArrayList<ClientObject>
                                 implements ClientPropertyRead, ClientIdentitySerializable, AbstractGroupObjectView<ClientComponent> {

    private int ID;
    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();

    public ClientGrid grid = new ClientGrid();
    public ClientShowType showType = new ClientShowType();

    public ClientGroupObject() {
        grid.groupObject = this;
        showType.groupObject = this;
    }

    public List<ClientObject> getDeserializeList(Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        List<ClientObject> result = new ArrayList<ClientObject>();
        ClassViewType newType = classViews.get(this);
        if((newType!=null?newType:controllers.get(this).classView) == ClassViewType.GRID)
            result.addAll(this);
        return result;
    }

    public List<ClientObject> getDeserializeList(Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
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

    public String getCaption() {
        return toString();
    }

    public int getID() {
        return ID;
    }

    public ClientComponent getGrid() {
        return grid;
    }

    public ClientComponent getShowType() {
        return showType;
    }

    public void setID(int iID) {
        ID = iID;
        grid.setID(Main.generateNewID());
        showType.setID(Main.generateNewID());
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        banClassView = (List<ClassViewType>)pool.readObject(inStream);

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
