package platform.server.form.entity;

import platform.base.identity.IdentityObject;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupEntity extends IdentityObject implements ServerIdentitySerializable {
    public List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();

    public TreeGroupEntity() {
        
    }

    public TreeGroupEntity(int iID) {
        ID = iID;
    }

    public void add(GroupObjectEntity group) {
        groups.add(group);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, groups, serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        groups = pool.deserializeList(inStream);
    }
}
