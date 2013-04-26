package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.base.context.ContextObject;
import platform.client.logics.classes.ClientTypeClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class DataObjectDescriptor extends ContextObject implements PropertyObjectInterfaceDescriptor {
    public Object object;
    public ClientTypeClass typeClass;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        BaseUtils.serializeObject(outStream, object);

        ClientTypeSerializer.serializeClientType(outStream, typeClass.getDefaultType());
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        object = BaseUtils.deserializeObject(inStream);

        typeClass = ClientTypeSerializer.deserializeClientType(inStream).getTypeClass();
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DataObjectDescriptor && object.equals(((DataObjectDescriptor) o).object) && typeClass.equals(((DataObjectDescriptor) o).typeClass);
    }

    @Override
    public int hashCode() {
        return object.hashCode() * 31 + typeClass.hashCode();
    }

    @Override
    public String toString() {
        return object.toString() + " - " + typeClass.toString();
    }
}
