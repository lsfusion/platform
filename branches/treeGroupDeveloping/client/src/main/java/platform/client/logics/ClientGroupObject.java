package platform.client.logics;

import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.base.OrderedMap;
import platform.base.BaseUtils;
import platform.client.Main;
import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectController;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.base.context.ApplicationContext;
import platform.interop.form.layout.AbstractGroupObject;
import platform.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClientGroupObject implements ClientPropertyRead, ClientIdentitySerializable, AbstractGroupObject<ClientComponent> {

    public ClientTreeGroup parent;
    public boolean isRecursive;

    private int ID;
    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();

    public ClientGrid grid;
    public ClientShowType showType;

    public List<ClientObject> objects = new ArrayList<ClientObject>();

    public boolean mayHaveChildren() {
        return isRecursive || (parent!= null && parent.groups.indexOf(this) != parent.groups.size() - 1);
    }

    public ClientGroupObject() {
    }

    // конструктор при создании нового объекта
    public ClientGroupObject(int ID, ApplicationContext context) {
        this.ID = ID;

        grid = new ClientGrid(context);
        showType = new ClientShowType(context);

        grid.groupObject = this;
        showType.groupObject = this;
    }

    public static List<ClientObject> getObjects(List<ClientGroupObject> groups) {
        List<ClientObject> result = new ArrayList<ClientObject>();
        for(ClientGroupObject group : groups)
            result.addAll(group);
        return result;        
    }

    public List<ClientGroupObject> getDeserializeList(Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        List<ClientGroupObject> result = new ArrayList<ClientGroupObject>();
        ClassViewType newType = classViews.get(this);
        if((newType!=null?newType:controllers.get(this).classView) == ClassViewType.GRID)
            result.add(this);
        return result;
    }

    public List<ClientObject> getDeserializeList(Set<ClientPropertyDraw> panelProperties, Map<ClientGroupObject, ClassViewType> classViews, Map<ClientGroupObject, GroupObjectController> controllers) {
        return ClientGroupObject.getObjects(getDeserializeList(classViews, controllers));
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
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        banClassView = (List<ClassViewType>)pool.readObject(inStream);

        pool.deserializeCollection(objects, inStream);

        parent = pool.deserializeObject(inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);

        isRecursive = inStream.readBoolean();
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
        if (objects.isEmpty()) {
            return "Пустая группа";
        }
        
        String result = "";
        for (ClientObject object : objects) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.toString();
        }
        return result;
    }

    public ClientContainer getClientComponent(ClientContainer parent) {
        return parent.findContainerBySID(GroupObjectContainerSet.GROUP_CONTAINER + getID());
    }

    // по аналогии с сервером
    public ClientGroupObject getUpTreeGroup() {
        return BaseUtils.last(upTreeGroups);
    }
    public List<ClientGroupObject> upTreeGroups = new ArrayList<ClientGroupObject>();
    public List<ClientGroupObject> getUpTreeGroups() {
        return BaseUtils.add(upTreeGroups,this);
    }
}
