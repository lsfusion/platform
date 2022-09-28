package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncAddRemove;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapRemove<T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final T propertyInterface;

    public AsyncMapRemove(T propertyInterface) {
        this.propertyInterface = propertyInterface;
    }

    @Override
    public AsyncMapEventExec<T> newSession() {
        return null;
    }


    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping) {
        return new AsyncMapRemove<>(mapping.get(propertyInterface));
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        P mappedInterface = mapping.get(propertyInterface);
        if(mappedInterface != null)
            return new AsyncMapRemove<>(mappedInterface);
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        PropertyInterfaceImplement<P> mapRemove;
        if ((mapRemove = mapping.get(propertyInterface)) instanceof PropertyInterface)
            return new AsyncMapRemove<>((P) mapRemove);
        return null;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        ObjectEntity object;
        if((object = mapObjects.get(propertyInterface)) != null && object.groupTo.isSimpleList())
            return new AsyncAddRemove(object, false);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapRemove))
            return null;

        AsyncMapRemove<T> asyncInput = (AsyncMapRemove<T>) input;
        if (BaseUtils.hashEquals(propertyInterface, asyncInput.propertyInterface))
            return this;
        return null;
    }
}
