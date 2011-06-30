package platform.client.descriptor.filter;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.NotFilterNode;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    public String getCodeConstructor() {
        return "new NotFilterEntity(" + filter.getCodeConstructor() + ")";
    }
}
