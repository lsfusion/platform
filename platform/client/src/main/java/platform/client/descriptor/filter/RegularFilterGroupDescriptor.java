package platform.client.descriptor.filter;

import platform.client.descriptor.IdentityDescriptor;
import platform.client.logics.ClientRegularFilterGroup;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.descriptor.GroupObjectDescriptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class RegularFilterGroupDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    public List<RegularFilterDescriptor> filters;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        GroupObjectDescriptor result = null;
        for(RegularFilterDescriptor filter : filters)
            result = FilterDescriptor.getDownGroup(result, filter.getGroupObject(groupList), groupList);
        return result;
    }

    ClientRegularFilterGroup client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        filters = pool.deserializeList(inStream);
        client = pool.context.getRegularFilterGroup(ID);
    }

    @Override
    public String toString() {
        return client.toString();
    }
}
