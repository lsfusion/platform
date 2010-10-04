package platform.client.descriptor;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.logics.ClientForm;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class FormDescriptor extends IdentityDescriptor implements IdentitySerializable {

    ClientForm client;

    public List<GroupObjectDescriptor> groups;
    public List<PropertyDrawDescriptor> propertyDraws;
    public Set<FilterDescriptor> fixedFilters;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        this.ID = ID;

        groups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        //todo:

        client = (ClientForm) pool.context;
    }
}
