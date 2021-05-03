package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

// domain logic (action) level (with mapping and no objects)
public abstract class AsyncMapEventExec<T extends PropertyInterface> {

    public abstract AsyncMapEventExec<T> newSession();

    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping);

    public abstract AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input);
    
    public abstract AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form);
}
