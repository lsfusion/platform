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
import lsfusion.client.form.object.table.grid.ClientGridProperty;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTreeGroup extends ClientGridProperty implements ClientIdentitySerializable, AbstractTreeGroup<ClientComponent> {

    public List<ClientGroupObject> groups = new ArrayList<>();

    public ClientContainer filtersContainer;
    public ClientFilterControls filterControls;
    public List<ClientFilter> filters = new ArrayList<>();
    
    public ClientToolbar toolbar;

    public boolean plainTreeMode;
    
    public boolean expandOnClick;
    public int hierarchicalWidth;
    public String hierarchicalCaption;

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

    public final ClientPropertyReader hierarchicalCaptionClassReader = new ExtraReader(PropertyReadType.TREE_HIERARCHICALCAPTION) {
        @Override
        public int getID() {
            return ClientTreeGroup.this.getID();
        }
    };

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        // Tree

        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filtersContainer = pool.deserializeObject(inStream);
        filterControls = pool.deserializeObject(inStream);
        pool.deserializeCollection(filters, inStream);

        plainTreeMode = inStream.readBoolean();
        
        expandOnClick = inStream.readBoolean();
        hierarchicalWidth = inStream.readInt();
        hierarchicalCaption = pool.readString(inStream);

        List<ClientGroupObject> upGroups = new ArrayList<>();
        for (ClientGroupObject group : groups) {
            group.upTreeGroups.addAll(upGroups);
            upGroups.add(group);
        }
    }

    @Override
    public String getCaption() {
        return getHierarchicalCaption();
    }

    public String getHierarchicalCaption() {
        if (hierarchicalCaption != null)
            return hierarchicalCaption;
        return ClientResourceBundle.getString("form.tree");
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
        if(hierarchicalWidth > 0) {
            return hierarchicalWidth;
        }

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
        return captionHeight;
    }

    @Override
    protected Integer getDefaultWidth() {
        return getExpandWidth() + getLastGroup().getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return getLastGroup().getHeight(lineHeight, captionHeight);
    }
}
