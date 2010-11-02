package platform.client.descriptor;

import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientObject;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ObjectDescriptor extends IdentityDescriptor implements PropertyObjectInterfaceDescriptor, ClientIdentitySerializable {

    public ClientObject client = new ClientObject();
    public GroupObjectDescriptor groupTo;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
        getBaseClass().serialize(outStream);
        pool.writeString(outStream, client.caption);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getObject(ID);
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups) {
        return groupTo;
    }

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        IncrementDependency.update(this, "caption");
    }

    public String getCaption() {
        return client.caption;
    }

    public void setBaseClass(ClientClass baseClass) {
        client.baseClass = baseClass;

        IncrementDependency.update(this, "baseClass");
    }

    public ClientClass getBaseClass() {
        return client.baseClass;
    }

    public void setAddOnTransaction(boolean addOnTransaction) {
        client.addOnTransaction = addOnTransaction;
        IncrementDependency.update(this, "addOnTransaction");
    }

    public boolean getAddOnTransaction() {
        return client.addOnTransaction;
    }
}
