package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.context.ApplicationContext;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.base.identity.IdentityObject;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.form.PropertyReadType;
import platform.interop.form.layout.AbstractGroupObject;
import platform.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientGroupObject extends IdentityObject implements ClientIdentitySerializable, AbstractGroupObject<ClientComponent> {

    public ClientPropertyDraw filterProperty;

    public ClientTreeGroup parent;
    public boolean isRecursive;
    public int pageSize = -1;
    public boolean needVerticalScroll;
    public int tableRowsCount = -1;

    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();

    public ClientGrid grid;
    public ClientShowType showType;

    public List<ClientObject> objects = new ArrayList<ClientObject>();

    public RowBackgroundReader rowBackgroundReader = new RowBackgroundReader();
    public RowForegroundReader rowForegroundReader = new RowForegroundReader();

    public boolean hasUserPreferences = false;

    public boolean mayHaveChildren() {
        return isRecursive || (parent != null && parent.groups.indexOf(this) != parent.groups.size() - 1);
    }

    public ClientGroupObject() {
    }

    // конструктор при создании нового объекта
    public ClientGroupObject(int ID, ApplicationContext context) {
        super(ID);

        grid = new ClientGrid(context);
        showType = new ClientShowType(context);

        grid.groupObject = this;
        showType.groupObject = this;
    }

    public static List<ClientObject> getObjects(List<ClientGroupObject> groups) {
        List<ClientObject> result = new ArrayList<ClientObject>();
        for (ClientGroupObject group : groups)
            result.addAll(group.objects);
        return result;
    }

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private String actionID = null;

    public String getActionID() {
        if (actionID == null)
            actionID = "changeGroupObject" + idGenerator.idShift();
        return actionID;
    }

    public String getCaption() {
        if (objects.isEmpty()) {
            return ClientResourceBundle.getString("logics.empty.group");
        }

        String result = "";
        for (ClientObject object : objects) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.getCaption();
        }
        return result;
    }

    public ClientComponent getGrid() {
        return grid;
    }

    public ClientComponent getShowType() {
        return showType;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
        outStream.writeBoolean(needVerticalScroll);
        outStream.writeInt(tableRowsCount);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        banClassView = (List<ClassViewType>) pool.readObject(inStream);

        pool.deserializeCollection(objects, inStream);

        parent = pool.deserializeObject(inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);

        filterProperty = pool.deserializeObject(inStream);

        isRecursive = inStream.readBoolean();
        Integer ps = pool.readInt(inStream);
        if (ps != null) {
            pageSize = ps;
        }
        needVerticalScroll = inStream.readBoolean();
        tableRowsCount = inStream.readInt();
        sID = inStream.readUTF();
    }

    public static List<ClientGroupObjectValue> mergeGroupValues(OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<ClientGroupObjectValue>();
        propColumnKeys.add(ClientGroupObjectValue.EMPTY);
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
        return getCaption() + " (" + getID() + ")";
    }

    public ClientContainer getClientComponent(ClientContainer parent) {
        return parent.findContainerBySID(getSID() + GroupObjectContainerSet.GROUP_CONTAINER);
    }

    // по аналогии с сервером
    public ClientGroupObject getUpTreeGroup() {
        return BaseUtils.last(upTreeGroups);
    }

    public List<ClientGroupObject> upTreeGroups = new ArrayList<ClientGroupObject>();

    public List<ClientGroupObject> getUpTreeGroups() {
        return BaseUtils.add(upTreeGroups, this);
    }

    public boolean isLastGroupInTree() {
        return parent != null && BaseUtils.last(parent.groups) == this;
    }

    public ClientGroupObject getDownGroup() {
        int ind = parent.groups.indexOf(this);
        return ind == parent.groups.size() - 1
               ? null
               : parent.groups.get(ind + 1);
    }

    public class RowBackgroundReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientGroupObject.this;
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return true;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateRowBackgroundValues(readKeys);
        }

        public int getID() {
            return ClientGroupObject.this.getID();
        }

        public byte getType() {
            return PropertyReadType.ROW_BACKGROUND;
        }
    }

    public class RowForegroundReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientGroupObject.this;
        }

        public boolean shouldBeDrawn(ClientFormController form) {
            return true;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, GroupObjectLogicsSupplier controller) {
            controller.updateRowForegroundValues(readKeys);
        }

        public int getID() {
            return ClientGroupObject.this.getID();
        }

        public byte getType() {
            return PropertyReadType.ROW_FOREGROUND;
        }
    }
}
