package platform.client.descriptor.property;

import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class AbstractGroupDescriptor extends AbstractNodeDescriptor implements ClientIdentitySerializable {
    public List<AbstractGroupDescriptor> children;
    public String caption;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        parent = pool.deserializeObject(inStream);
        children = pool.deserializeList(inStream);
        caption = pool.readString(inStream);
    }

    @Override
    public String toString(){
        return caption;    
    }
}
