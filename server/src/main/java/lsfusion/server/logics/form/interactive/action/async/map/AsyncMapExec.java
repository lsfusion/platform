package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapExec<T extends PropertyInterface> extends AsyncMapEventExec<T> {
    
    public final AsyncExec asyncExec;

    private final static AsyncMapExec RECURSIVE = new AsyncMapExec(null);
    public static <T extends PropertyInterface> AsyncMapExec<T> RECURSIVE() {
        return RECURSIVE;
    }

    public AsyncMapExec(AsyncExec asyncExec) {
        this.asyncExec = asyncExec;
    }

    @Override
    public AsyncMapEventExec<T> newSession() {
        return this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping) {
        return (AsyncMapEventExec<P>) this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        return (AsyncMapEventExec<P>) this;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return (AsyncMapEventExec<P>) this;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form) {
        return asyncExec;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapExec))
            return null;

        AsyncExec merged = asyncExec.merge(((AsyncMapExec<PropertyInterface>) input).asyncExec);
        if(merged != null)
            return new AsyncMapExec<>(merged);
        return null;
    }
}
