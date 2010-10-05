package platform.server.form.entity.filter;

import platform.base.IdentityObject;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupEntity extends IdentityObject implements ServerIdentitySerializable {

    public RegularFilterGroupEntity(int iID) {
        ID = iID;
    }

    public List<RegularFilterEntity> filters = new ArrayList<RegularFilterEntity>();

    public void addFilter(RegularFilterEntity filter) {
        filters.add(filter);
    }

    public int defaultFilter = -1;

    public void addFilter(RegularFilterEntity filter, boolean setDefault) {
        if (setDefault) {
            defaultFilter = filters.size();
        }
        filters.add(filter);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
    }

    public void customDeserialize(ServerSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        filters = pool.deserializeList(inStream);
    }
}
