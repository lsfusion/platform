package platform.server.form.entity;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.MOrderSet;
import platform.base.identity.IdentityObject;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class TreeGroupEntity extends IdentityObject implements ServerIdentitySerializable {
    public boolean plainTreeMode = false;

    public TreeGroupEntity() {
        
    }

    public TreeGroupEntity(int ID) {
        this.ID = ID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, getGroups().toJavaList(), serializationType);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        setGroups(SetFact.fromJavaOrderSet(pool.<GroupObjectEntity>deserializeList(inStream)));
        plainTreeMode = inStream.readBoolean();
    }

    private Object groups = SetFact.mOrderExclSet();
    private boolean finalizedGroups;

    @LongMutable
    public ImOrderSet<GroupObjectEntity> getGroups() {
        if(!finalizedGroups) {
            finalizedGroups = true;
            groups = ((MOrderExclSet<GroupObjectEntity>)groups).immutableOrder();
        }
        return (ImOrderSet<GroupObjectEntity>) groups;
    }
    public void add(GroupObjectEntity group) {
        assert !finalizedGroups;
        group.treeGroup = this;
        ((MOrderExclSet<GroupObjectEntity>)groups).exclAdd(group);
    }
    public void setGroups(ImOrderSet<GroupObjectEntity> groups) {
        assert !finalizedGroups;
        finalizedGroups = true;
        this.groups = groups;
    }
}
