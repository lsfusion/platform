package lsfusion.client.descriptor;

import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.logics.ClientObject;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ObjectDescriptor extends ContextIdentityObject implements PropertyObjectInterfaceDescriptor, ClientIdentitySerializable, CustomConstructible {

    public ClientObject client;
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

    public void customConstructor() {
        client = new ClientObject(getID(), getContext());
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
        updateDependency(this, "caption");
    }

    public String getCaption() {
        return client.caption;
    }

    public void setBaseClass(ClientClass baseClass) {
        client.baseClass = baseClass;

        updateDependency(this, "baseClass");
    }

    public ClientClass getBaseClass() {
        return client.baseClass;
    }
}
