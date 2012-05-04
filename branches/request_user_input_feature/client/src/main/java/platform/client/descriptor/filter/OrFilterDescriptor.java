package platform.client.descriptor.filter;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.OrFilterNode;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OrFilterDescriptor extends FilterDescriptor {
    public FilterDescriptor op1;
    public FilterDescriptor op2;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (op1 == null || op2 == null) return null;
        return getDownGroup(op1.getGroupObject(groupList), op2.getGroupObject(groupList), groupList);
    }

    @Override
    public FilterNode createNode(Object group) {
        return new OrFilterNode((GroupObjectDescriptor) group, this);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, op1);
        pool.serializeObject(outStream, op2);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        op1 = (FilterDescriptor) pool.deserializeObject(inStream);
        op2 = (FilterDescriptor) pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        String result = ClientResourceBundle.getString("descriptor.filter.or");
        if (op1 != null && op2 != null) {
            result += "( " + op1.toString() + ", " + op2.toString() + " )";
        }
        return result;
    }

    public void setOp1(FilterDescriptor op1) {
        this.op1 = op1;
        updateDependency(this, "op1");
    }

    public FilterDescriptor getOp1() {
        return op1;
    }

    public void setOp2(FilterDescriptor op2) {
        this.op2 = op2;
        updateDependency(this, "op2");
    }

    public FilterDescriptor getOp2() {
        return op2;
    }

    public String getCodeConstructor() {
        return "new OrFilterEntity(" + op1.getCodeConstructor() + ", " + op2.getCodeConstructor() + ")";
    }
}
