package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

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

    // the branches are always merged all at once (see merge below); an exec implements only the pairwise step of that merge, and only for the execs of its own class
    protected abstract AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input);

    // merging all the branches at once, with their conditions (they are null when not known, see Action.getBranchAsyncEventExec)
    public static <T extends PropertyInterface> AsyncMapEventExec<T> merge(ImList<AsyncMapEventExec<T>> execs, ImList<PropertyInterfaceImplement<T>> wheres) {
        assert wheres == null || wheres.size() == execs.size();

        if(execs.isEmpty())
            return null;

        // an exec merges only with the execs of its own class - checked here once for all the branches, asserted in the merges themselves
        AsyncMapEventExec<T> first = execs.get(0);
        for(int i = 1, size = execs.size(); i < size; i++)
            if(execs.get(i).getClass() != first.getClass())
                return null;

        // the input needs all the branches at once - it combines their value lists into one conditional list - the rest are folded pairwise
        if(first instanceof AsyncMapInput)
            return AsyncMapInput.mergeInputs(BaseUtils.immutableCast(execs), wheres);

        AsyncMapEventExec<T> result = first;
        for(int i = 1, size = execs.size(); i < size && result != null; i++)
            result = result.merge(execs.get(i));
        return result;
    }
    public int getOptimisticPriority() { // interactive should have higher prioirty
        return 0;
    }
    
    public abstract AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?, ?> drawProperty, GroupObjectEntity toDraw);
}
