package lsfusion.client.descriptor.filter;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.nodes.filters.FilterNode;
import lsfusion.client.descriptor.nodes.filters.NotFilterNode;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class NotFilterDescriptor extends FilterDescriptor {
    public FilterDescriptor filter;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (filter == null) return null;
        return filter.getGroupObject(groupList);
    }

    @Override
    public FilterNode createNode(Object group) {
        return new NotFilterNode((GroupObjectDescriptor) group, this);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        filter = (FilterDescriptor) pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        String result = ClientResourceBundle.getString("descriptor.filter.not");
        if (filter != null) {
            result += "( " + filter.toString() + " )";
        }
        return result;
    }

    public void setFilter(FilterDescriptor filter) {
        this.filter = filter;
        updateDependency(this, "filter");
    }

    public FilterDescriptor getFilter() {
        return filter;
    }

}
