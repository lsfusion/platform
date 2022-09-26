package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

// domain logic (action) level (with mapping and no objects)
public abstract class AsyncMapEventExec<T extends PropertyInterface> {

    // hack - in theory push results should be mixed on the client
    public boolean needOwnPushResult() {
        return false;
    }

    public abstract AsyncMapEventExec<T> newSession();

    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping);

    public abstract AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input);
    public int getMergeOptimisticPriority() { // interactive should have higher prioirty
        return 0;
    }
    
    public abstract AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, SecurityPolicy policy, ActionOrProperty securityProperty, GroupObjectEntity toDraw);
}
