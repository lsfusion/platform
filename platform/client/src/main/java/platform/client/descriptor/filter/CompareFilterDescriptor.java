package platform.client.descriptor.filter;

import platform.client.descriptor.OrderDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.interop.Compare;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CompareFilterDescriptor extends PropertyFilterDescriptor {
    private Compare compare;
    private OrderDescriptor value;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, ID, inStream);

        compare = Compare.deserialize(inStream);
        value = (OrderDescriptor) pool.deserializeObject(inStream);
    }
}
