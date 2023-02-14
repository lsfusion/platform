package lsfusion.server.logics.form.struct.object;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.physics.dev.debug.DebugInfo;

public class TreeGroupEntity extends IdentityObject {
    public boolean plainTreeMode = false;

    public TreeGroupEntity() {
        
    }

    public TreeGroupEntity(int ID) {
        this.ID = ID;
    }

    private DebugInfo.DebugPoint debugPoint;

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }

    public DebugInfo.DebugPoint getDebugPoint() {
        return debugPoint;
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
