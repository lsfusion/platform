package platform.client.descriptor;

import platform.base.context.ContextIdentityObject;
import platform.client.logics.ClientObject;
import platform.client.logics.classes.ClientClass;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.FormEventType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
