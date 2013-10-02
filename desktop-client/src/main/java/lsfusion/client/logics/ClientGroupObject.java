package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.GroupObjectLogicsSupplier;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.interop.form.layout.AbstractGroupObject;
import lsfusion.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientGroupObject extends IdentityObject implements ClientIdentitySerializable, AbstractGroupObject<ClientComponent> {

    public ClientTreeGroup parent;
    public boolean isRecursive;
    public int pageSize = -1;
    public boolean needVerticalScroll;

    public List<ClassViewType> banClassView = new ArrayList<ClassViewType>();

    public ClientGrid grid;
    public ClientShowType showType;
    public ClientToolbar toolbar;
    public ClientFilter filter;

    public List<ClientObject> objects = new ArrayList<ClientObject>();

    public RowBackgroundReader rowBackgroundReader = new RowBackgroundReader();
    public RowForegroundReader rowForegroundReader = new RowForegroundReader();

    public boolean hasUserPreferences = false;
    
    public FontInfo fontInfo;

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
        toolbar = new ClientToolbar(context);
        filter = new ClientFilter(context);

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

    @Override
    public ClientComponent getToolbar() {
        return toolbar;
    }

    @Override
    public ClientComponent getFilter() {
        return filter;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, showType);
        pool.serializeObject(outStream, toolbar);
        pool.serializeObject(outStream, filter);
        outStream.writeBoolean(needVerticalScroll);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        banClassView = (List<ClassViewType>) pool.readObject(inStream);

        pool.deserializeCollection(objects, inStream);

        parent = pool.deserializeObject(inStream);

        grid = pool.deserializeObject(inStream);
        showType = pool.deserializeObject(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        isRecursive = inStream.readBoolean();
        Integer ps = pool.readInt(inStream);
        if (ps != null) {
            pageSize = ps;
        }
        needVerticalScroll = inStream.readBoolean();
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
