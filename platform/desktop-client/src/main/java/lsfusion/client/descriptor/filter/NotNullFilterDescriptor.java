package lsfusion.client.descriptor.filter;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.nodes.filters.FilterNode;
import lsfusion.client.descriptor.nodes.filters.NotNullFilterNode;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NotNullFilterDescriptor extends PropertyFilterDescriptor {
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
    }

    @Override
    public String toString() {
        String result = ClientResourceBundle.getString("descriptor.filter.defined");
        if (property != null)
            result += "( " + property + " )";
        return result;
    }

    @Override
    public FilterNode createNode(Object group) {
        return new NotNullFilterNode((GroupObjectDescriptor) group, this);
    }

}
