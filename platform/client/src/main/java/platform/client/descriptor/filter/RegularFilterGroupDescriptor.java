package platform.client.descriptor.filter;

import platform.base.BaseUtils;
import platform.client.descriptor.ContainerMovable;
import platform.client.descriptor.CustomConstructible;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.base.context.ContextIdentityObject;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.client.logics.ClientRegularFilterGroup;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupDescriptor extends ContextIdentityObject implements ClientIdentitySerializable, ContainerMovable<ClientComponent>, CustomConstructible {

    public List<RegularFilterDescriptor> filters = new ArrayList<RegularFilterDescriptor>();
    public int defaultFilter = -1;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        GroupObjectDescriptor result = null;
        for(RegularFilterDescriptor filter : filters)
            result = FilterDescriptor.getDownGroup(result, filter.getGroupObject(groupList), groupList);
        return result;
    }

    public ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects) {
        GroupObjectDescriptor groupObject = getGroupObject(groupObjects);
        if (groupObject != null) {
            return parent.findContainerBySID(groupObject.getSID() + GroupObjectContainerSet.FILTERS_CONTAINER);
        } else
            return null;
    }

    public ClientComponent getClientComponent(ClientContainer parent) {
        return client;
    }

    public RegularFilterGroupDescriptor() {
    }

    public ClientRegularFilterGroup client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
        outStream.writeInt(defaultFilter);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        filters = pool.deserializeList(inStream);
        client = pool.context.getRegularFilterGroup(ID);
        defaultFilter = inStream.readInt();
    }

    public void customConstructor() {
        client = new ClientRegularFilterGroup(getID(), getContext());
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public boolean moveFilter(RegularFilterDescriptor filterFrom, int index) {
        BaseUtils.moveElement(filters, filterFrom, index);
        BaseUtils.moveElement(client.filters, filterFrom.client, index);
        updateDependency(this, "filters");
        return true;
    }

    public List<RegularFilterDescriptor> getFilters() {
        return filters;
    }

    public void addToFilters(RegularFilterDescriptor filter) {
        client.filters.add(filter.client);
        filters.add(filter);
        updateDependency(this, "filters");
    }

    public void removeFromFilters(RegularFilterDescriptor filter) {
        client.filters.remove(filter.client);
        filters.remove(filter);
        updateDependency(this, "filters");
    }

    public String getCodeConstructor() {
        return "new RegularFilterGroupEntity(genID());";
    }
}
