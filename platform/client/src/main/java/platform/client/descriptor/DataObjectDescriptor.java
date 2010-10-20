package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class DataObjectDescriptor implements PropertyObjectInterfaceDescriptor {
    Object object;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        BaseUtils.serializeObject(object);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        object = BaseUtils.deserializeObject(inStream);

        //todo: think about baseClass serialization
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DataObjectDescriptor && object.equals(((DataObjectDescriptor) o).object);
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
