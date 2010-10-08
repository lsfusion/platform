package platform.client.descriptor.filter;

import platform.client.descriptor.OrderDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.Compare;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class CompareFilterDescriptor extends PropertyFilterDescriptor {
    private Compare compare;
    private OrderDescriptor value;

    @Override
    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        return getDownGroup(super.getGroupObject(groupList), value.getGroupObject(groupList), groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        compare.serialize(outStream);
        pool.serializeObject(outStream, value);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, ID, inStream);

        compare = Compare.deserialize(inStream);
        value = (OrderDescriptor) pool.deserializeObject(inStream);
    }
}
