package platform.client.descriptor.filter;

import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.NotFilterNode;
import platform.interop.serialization.SerializationPool;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class NotFilterDescriptor extends FilterDescriptor {
    public FilterDescriptor filter;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        return filter.getGroupObject(groupList);
    }

    @Override
    public FilterNode getNode(GroupObjectDescriptor group) {
        return new NotFilterNode(group, this);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        filter = (FilterDescriptor) pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "НЕ( " + filter.toString() + " )";
    }
}
