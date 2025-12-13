package lsfusion.server.logics.form.struct.object;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.physics.dev.debug.DebugInfo;

public class TreeGroupEntity extends IdentityEntity<TreeGroupEntity, GroupObjectEntity> {
    public boolean plainTreeMode = false;

    public TreeGroupEntity(IDGenerator ID, String sID, ImOrderSet<GroupObjectEntity> groups) {
        super(ID, sID, "tree");

        this.groups = groups;
    }

    private DebugInfo.DebugPoint debugPoint;

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }

    public DebugInfo.DebugPoint getDebugPoint() {
        return debugPoint;
    }

    private final ImOrderSet<GroupObjectEntity> groups;

    public ImOrderSet<GroupObjectEntity> getGroups() {
        return groups;
    }

    public TreeGroupView view;

    // copy-constructor
    protected TreeGroupEntity(TreeGroupEntity src, ObjectMapping mapping) {
        super(src, mapping);

        plainTreeMode = src.plainTreeMode;
        debugPoint = src.debugPoint;

        groups = mapping.get(src.groups);
        view = mapping.get(src.view);
    }

    @Override
    public GroupObjectEntity getAddParent(ObjectMapping mapping) {
        return getGroups().get(0);
    }
    @Override
    public TreeGroupEntity getAddChild(GroupObjectEntity groupObjectEntity, ObjectMapping mapping) {
        return groupObjectEntity.treeGroup;
    }
    @Override
    public TreeGroupEntity copy(ObjectMapping mapping) {
        return new TreeGroupEntity(this, mapping);
    }
}
