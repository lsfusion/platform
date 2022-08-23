package lsfusion.client.form.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.identity.IdentityObject;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.grid.user.toolbar.ClientCalculations;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.form.object.AbstractGroupObject;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;
import static java.lang.Math.round;

public class ClientGroupObject extends IdentityObject implements ClientIdentitySerializable, AbstractGroupObject<ClientComponent, String> {

    public ClientTreeGroup parent;
    public boolean isRecursive;
    public int pageSize = -1;
    public boolean needVerticalScroll;

    public boolean isMap;
    public boolean isCalendarDate;
    public boolean isCalendarDateTime;
    public boolean isCalendarPeriod;

    public boolean hasHeaders;
    public boolean hasFooters;

    public ClassViewType viewType;
    public ListViewType listViewType;
    public PivotOptions pivotOptions;
    public String customRenderFunction;
    public String mapTileProvider;

    public boolean asyncInit;

    public ClientContainer filtersContainer;
    public List<ClientFilter> filters = new ArrayList<>();
    public ClientGrid grid;
    public ClientToolbar toolbar;
    public ClientCalculations calculations;

    public List<ClientObject> objects = new ArrayList<>();

    public RowBackgroundReader rowBackgroundReader = new RowBackgroundReader();
    public RowForegroundReader rowForegroundReader = new RowForegroundReader();
    public CustomOptionsReader customOptionsReader = new CustomOptionsReader();

    // transient
    public int columnSumWidth;
    public int columnCount;
    public int rowMaxHeight;

    public int getWidth(int lines) {
        int columnCount = this.columnCount;
        if(lines == -1)
            lines = Math.min(columnCount <= 3 ? columnCount : (int) round(3 + pow(columnCount - 6, 0.7)), 6);

        return columnCount > 0 ? lines * columnSumWidth / columnCount : 0;
    }

    public final static int BORDER_VERT_SIZE = 1;

    public int getHeight(int lines, int headerHeight) {
        if(lines == -1)
            lines = 5;

        return (lines * (rowMaxHeight + BORDER_VERT_SIZE)) +
                + 3 * BORDER_VERT_SIZE // borders around grid + header border
                + (headerHeight >= 0 ? headerHeight : ClientGrid.DEFAULT_HEADER_HEIGHT);
    }

    public boolean mayHaveChildren() {
        return isRecursive || (parent != null && parent.groups.indexOf(this) != parent.groups.size() - 1);
    }

    public ClientGroupObject() {
    }

    public static List<ClientObject> getObjects(List<ClientGroupObject> groups) {
        List<ClientObject> result = new ArrayList<>();
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

    @Override
    public ClientComponent getToolbarSystem() {
        return toolbar;
    }

    @Override
    public ClientContainer getFiltersContainer() {
        return filtersContainer;
    }

    @Override
    public ClientComponent getCalculations() {
        return calculations;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.serializeCollection(outStream, objects);
        pool.serializeObject(outStream, grid);
        pool.serializeObject(outStream, toolbar);
        pool.serializeObject(outStream,filtersContainer);
        pool.serializeCollection(outStream, filters);
        pool.serializeObject(outStream, calculations);
        outStream.writeBoolean(needVerticalScroll);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        viewType = pool.readObject(inStream);
        listViewType = pool.readObject(inStream);
        pivotOptions = pool.readObject(inStream);
        customRenderFunction = pool.readString(inStream);
        mapTileProvider = pool.readString(inStream);

        asyncInit = pool.readBoolean(inStream);

        pool.deserializeCollection(objects, inStream);

        parent = pool.deserializeObject(inStream);

        grid = pool.deserializeObject(inStream);
        toolbar = pool.deserializeObject(inStream);
        filtersContainer = pool.deserializeObject(inStream);
        pool.deserializeCollection(filters, inStream);
        calculations = pool.deserializeObject(inStream);

        isRecursive = inStream.readBoolean();
        isMap = inStream.readBoolean();
        isCalendarDate = inStream.readBoolean();
        isCalendarDateTime = inStream.readBoolean();
        isCalendarPeriod= inStream.readBoolean();

        hasHeaders = inStream.readBoolean();
        hasFooters = inStream.readBoolean();

        Integer ps = pool.readInt(inStream);
        if (ps != null) {
            pageSize = ps;
        }
        needVerticalScroll = inStream.readBoolean();
        sID = inStream.readUTF();
    }

    public static List<ClientGroupObjectValue> mergeGroupValues(OrderedMap<ClientGroupObject, List<ClientGroupObjectValue>> groupColumnKeys) {
        //находим декартово произведение ключей колонок
        List<ClientGroupObjectValue> propColumnKeys = new ArrayList<>();
        propColumnKeys.add(ClientGroupObjectValue.EMPTY);
        for (Map.Entry<ClientGroupObject, List<ClientGroupObjectValue>> entry : groupColumnKeys.entrySet()) {
            List<ClientGroupObjectValue> groupObjectKeys = entry.getValue();

            List<ClientGroupObjectValue> newPropColumnKeys = new ArrayList<>();
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

    // по аналогии с сервером
    public ClientGroupObject getUpTreeGroup() {
        return BaseUtils.last(upTreeGroups);
    }

    public List<ClientGroupObject> upTreeGroups = new ArrayList<>();

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

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
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

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            controller.updateRowForegroundValues(readKeys);
        }

        public int getID() {
            return ClientGroupObject.this.getID();
        }

        public byte getType() {
            return PropertyReadType.ROW_FOREGROUND;
        }
    }

    public class CustomOptionsReader implements ClientPropertyReader {
        public ClientGroupObject getGroupObject() {
            return ClientGroupObject.this;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            //do nothing in desktop
        }

        public int getID() {
            return ClientGroupObject.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CUSTOM_OPTIONS;
        }
    }
}
