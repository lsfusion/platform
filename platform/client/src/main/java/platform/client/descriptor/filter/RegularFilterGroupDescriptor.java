package platform.client.descriptor.filter;

import platform.client.logics.ClientForm;
import platform.client.logics.ClientRegularFilterGroup;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterGroupDescriptor implements CustomSerializable {

    ClientRegularFilterGroup client;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:

        client = ((ClientForm)pool.context).getRegularFilterGroup(ID);
    }
}
