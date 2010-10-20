package platform.client.descriptor.filter;

import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.OrFilterNode;
import platform.interop.serialization.SerializationPool;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.List;

public class OrFilterDescriptor extends FilterDescriptor {
    public FilterDescriptor op1;
    public FilterDescriptor op2;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        return getDownGroup(op1.getGroupObject(groupList), op2.getGroupObject(groupList), groupList);
    }

    @Override
    public FilterNode getNode(GroupObjectDescriptor group) {
        return new OrFilterNode(group, this);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, op1);
        pool.serializeObject(outStream, op2);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        op1 = (FilterDescriptor) pool.deserializeObject(inStream);
        op2 = (FilterDescriptor) pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "ИЛИ( " + op1.toString() + ", " + op2.toString() + " )";
    }
}
