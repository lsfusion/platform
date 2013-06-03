package lsfusion.client.descriptor.filter;

import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.descriptor.CustomConstructible;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.logics.ClientRegularFilter;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class RegularFilterDescriptor extends ContextIdentityObject implements ClientIdentitySerializable, CustomConstructible {

    private FilterDescriptor filter;

    public FilterDescriptor getFilter() {
        return filter;
    }

    public void setFilter(FilterDescriptor filter) {
        this.filter = filter;
        updateDependency(this, "filter");
    }

    public RegularFilterDescriptor() {
    }

    ClientRegularFilter client;

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        updateDependency(this, "caption");
    }

    public String getCaption() {
        return client.caption;
    }

    public ClientRegularFilter getClient() {
        return client;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (filter == null) return null;
        return filter.getGroupObject(groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);

        outStream.writeUTF(client.caption);
        new ObjectOutputStream(outStream).writeObject(client.key);
        outStream.writeBoolean(client.showKey);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        filter = (FilterDescriptor) pool.deserializeObject(inStream);
        client = pool.context.getRegularFilter(ID);
    }

    public void customConstructor() {
        client = new ClientRegularFilter(getID());
    }

    @Override
    public String toString() {
        return client.toString();
    }

}
