package platform.client.descriptor;

import platform.client.logics.ClientObject;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.descriptor.increment.IncrementDependency;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ObjectDescriptor extends IdentityDescriptor implements PropertyObjectInterfaceDescriptor, ClientIdentitySerializable {

    public ClientObject client;
    public GroupObjectDescriptor groupTo;

    public void setBaseClass(ClientClass baseClass) {
        client.baseClass = baseClass;

        IncrementDependency.update(this, "baseClass");
    }
    public ClientClass getBaseClass() {
        return client.baseClass;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
        getBaseClass().serialize(outStream);
        outStream.writeUTF(client.caption);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        
        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getObject(iID);
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups) {
        return groupTo;
    }
}
