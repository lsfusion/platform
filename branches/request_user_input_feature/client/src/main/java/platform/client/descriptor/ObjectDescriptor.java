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

    public Set<FormEventType> addOnEvent = new HashSet<FormEventType>();

    public ClientObject client;
    public GroupObjectDescriptor groupTo;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
        pool.writeObject(outStream, addOnEvent);
        getBaseClass().serialize(outStream);
        pool.writeString(outStream, client.caption);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        addOnEvent = pool.readObject(inStream);

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

    public void setAddOnEvent(List<FormEventType> addOnEvent) {
        this.addOnEvent.clear();
        this.addOnEvent.addAll(addOnEvent);
        updateDependency(this, "addOnEvent");
    }

    public List<FormEventType> getAddOnEvent() {
        return new ArrayList<FormEventType>(addOnEvent);
    }

    public String getInstanceCode(){
        return this.getVariableName();
    }

    public String getVariableName() {
        String name = getBaseClass().getSID();
        if (name == null) name = "";
        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return "obj" + name;
    }
}
