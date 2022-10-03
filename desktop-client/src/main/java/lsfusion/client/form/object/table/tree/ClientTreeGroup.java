package lsfusion.client.form.object.table.tree;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientIdentitySerializable;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientFilterControls;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTreeGroup extends ClientComponent implements ClientIdentitySerializable, AbstractTreeGroup<ClientComponent> {

    public List<ClientGroupObject> groups = new ArrayList<>();

    public ClientContainer filtersContainer;
    public ClientFilterControls filterControls;
    public List<ClientFilter> filters = new ArrayList<>();
    
    public ClientToolbar toolbar;

    public boolean autoSize;
    public Boolean boxed;

    public boolean plainTreeMode;
    
    public boolean expandOnClick;

    public Boolean resizeOverflow;

    public int headerHeight;

    public int lineHeight;
    public int lineWidth;

    public ClientTreeGroup() {
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
    public ClientFilterControls getFilterControls() {
        return filterControls;
    }

    public List<ClientFilter> getFilters() {
        return filters;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(autoSize);
        outStream.writeBoolean(boxed != null);
        if(boxed != null)
            outStream.writeBoolean(boxed);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbar);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, filters);
        
        outStream.writeBoolean(expandOnClick);

        outStream.writeInt(headerHeight);

        outStream.writeInt(lineWidth);
        outStream.writeInt(lineHeight);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        autoSize = inStream.readBoolean();
        boxed = inStream.readBoolean() ? inStream.readBoolean() : null;

        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filtersContainer = pool.deserializeObject(inStream);
        filterControls = pool.deserializeObject(inStream);
        pool.deserializeCollection(filters, inStream);

        plainTreeMode = inStream.readBoolean();
        
        expandOnClick = inStream.readBoolean();

        headerHeight = inStream.readInt();

        resizeOverflow = inStream.readBoolean() ? inStream.readBoolean() : null;

        lineWidth = inStream.readInt();
        lineHeight = inStream.readInt();

        List<ClientGroupObject> upGroups = new ArrayList<>();
        for (ClientGroupObject group : groups) {
            group.upTreeGroups.addAll(upGroups);
            upGroups.add(group);
        }
    }

    @Override
    public String getCaption() {
        return  ClientResourceBundle.getString("form.tree");
    }

    @Override
    public String toString() {
        String result = "";
        for (ClientGroupObject group : groups) {
            if (!result.isEmpty()) {
                result += ",";
            }
            result += group.toString();
        }
        return result + "[sid:" + getSID() + "]";
    }

    public int getExpandWidth() {
        int size = 0;
        for (ClientGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 20 * 4 : 20;
        }
        return size;
    }

    private ClientGroupObject getLastGroup() {
        return groups.get(groups.size() - 1);
    }

    public int getHeaderHeight() {
        return headerHeight;
    }

    @Override
    protected Integer getDefaultWidth() {
        return getExpandWidth() + getLastGroup().getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return getLastGroup().getHeight(lineHeight, headerHeight);
    }
}
