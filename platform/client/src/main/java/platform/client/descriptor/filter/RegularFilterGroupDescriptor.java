package platform.client.descriptor.filter;

import platform.base.BaseUtils;
import platform.client.descriptor.IdentityDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientRegularFilterGroup;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.descriptor.GroupObjectDescriptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    public List<RegularFilterDescriptor> filters;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        GroupObjectDescriptor result = null;
        for(RegularFilterDescriptor filter : filters)
            result = FilterDescriptor.getDownGroup(result, filter.getGroupObject(groupList), groupList);
        return result;
    }

    public RegularFilterGroupDescriptor() {
        filters = new ArrayList<RegularFilterDescriptor>();
        client = new ClientRegularFilterGroup();
    }

    public ClientRegularFilterGroup client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        filters = pool.deserializeList(inStream);
        client = pool.context.getRegularFilterGroup(ID);
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public boolean moveFilter(RegularFilterDescriptor filterFrom, int index) {
        BaseUtils.moveElement(filters, filterFrom, index);
        BaseUtils.moveElement(client.filters, filterFrom.client, index);
        IncrementDependency.update(this, "filters");
        return true;
    }

    public List<RegularFilterDescriptor> getFilters() {
        return filters;
    }

    public void addToFilters(RegularFilterDescriptor filter) {
        client.filters.add(filter.client);
        filters.add(filter);
        IncrementDependency.update(this, "filters");
    }

    public void removeFromFilters(RegularFilterDescriptor filter) {
        client.filters.remove(filter.client);
        filters.remove(filter);
        IncrementDependency.update(this, "filters");
    }
}
