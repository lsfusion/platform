package platform.client.descriptor.filter;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.IdentityDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientRegularFilter;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class RegularFilterDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    private FilterDescriptor filter;

    @Override
    public void setID(int ID) {
        super.setID(ID);
        client.setID(ID);
    }

    public FilterDescriptor getFilter() {
        return filter;
    }

    public void setFilter(FilterDescriptor filter) {
        this.filter = filter;
        IncrementDependency.update(this, "filter");
    }

    public RegularFilterDescriptor() {
        client = new ClientRegularFilter();
    }

    ClientRegularFilter client;

    public void setCaption(String caption) { // usage через reflection
        client.caption = caption;
        IncrementDependency.update(this, "caption");
    }

    public String getCaption() {
        return client.caption;
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

    @Override
    public String toString() {
        return client.toString();
    }
}
