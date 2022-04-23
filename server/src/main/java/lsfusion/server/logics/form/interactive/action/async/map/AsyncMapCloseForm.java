package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncCloseForm;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapCloseForm<T extends PropertyInterface> extends AsyncMapExec<T> {

    public AsyncMapCloseForm() {
    }
    
    @Override
    public AsyncMapCloseForm<T> newSession() {
        return this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapCloseForm<P> map(ImRevMap<T, P> mapping) {
        return (AsyncMapCloseForm<P>) this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        return (AsyncMapCloseForm<P>) this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return (AsyncMapCloseForm<P>) this;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        return new AsyncCloseForm();
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapCloseForm))
            return null;
        return new AsyncMapCloseForm<>();
    }

    @Override
    public int getMergeOptimisticPriority() {
        return 1;
    }
}
