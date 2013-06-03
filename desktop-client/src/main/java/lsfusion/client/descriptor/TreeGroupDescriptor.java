package lsfusion.client.descriptor;

import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.logics.ClientContainer;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientTreeGroup;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.GroupObjectContainerSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupDescriptor extends ContextIdentityObject implements ClientIdentitySerializable, CustomConstructible, ContainerMovable<ClientContainer> {
    public ClientTreeGroup client;
    public List<GroupObjectDescriptor> groups = new ArrayList<GroupObjectDescriptor>();

    public void customConstructor() {
        client = new ClientTreeGroup(getID(), getContext());
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, groups, serializationType);
        outStream.writeBoolean(client.plainTreeMode);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groups = pool.deserializeList(inStream);

        client = pool.context.getTreeGroup(ID);
    }

    public void setGroups(List<GroupObjectDescriptor> groups) {
        List<ClientGroupObject> clientGroups = new ArrayList<ClientGroupObject>();
        for (GroupObjectDescriptor group : groups) {
            clientGroups.add(group.client);

            group.setParent(this);
            group.client.parent = client;
        }

        this.groups = groups;
        client.groups = clientGroups;

        getContext().updateDependency(this, "groups");
    }

    public void setPlainTreeMode(boolean plainTreeMode) {
        client.plainTreeMode = plainTreeMode;
        getContext().updateDependency(this, "plainTreeMode");
    }

    public boolean getPlainTreeMode() {
        return client.plainTreeMode;
    }

    public List<GroupObjectDescriptor> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public ClientContainer getDestinationContainer(ClientContainer parent, List<GroupObjectDescriptor> groupObjects) {
        return parent;
    }

    public ClientContainer getClientComponent(ClientContainer parent) {
        return parent.findContainerBySID(getSID() + GroupObjectContainerSet.TREE_GROUP_CONTAINER);
    }
}