package lsfusion.server.form.entity;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
