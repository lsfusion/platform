package platform.server.form.entity.filter;

import platform.base.IdentityObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupEntity extends IdentityObject implements IdentitySerializable {

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

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:

    }
}
