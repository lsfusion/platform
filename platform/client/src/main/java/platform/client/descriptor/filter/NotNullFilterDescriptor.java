package platform.client.descriptor.filter;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.NotNullFilterNode;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NotNullFilterDescriptor extends PropertyFilterDescriptor {
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);
    }

    @Override
    public String toString() {
        String result = "ОПРЕДЕЛЕНО";
        if (property != null)
            result += "( " + property + " )";
        return result;
    }

    @Override
    public FilterNode createNode(Object group) {
        return new NotNullFilterNode((GroupObjectDescriptor) group, this);
    }
}
