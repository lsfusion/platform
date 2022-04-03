package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncChange;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.Serializable;

public class AsyncMapChange<X extends PropertyInterface, T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final PropertyMapImplement<X, T> property;

    public final Serializable value;

    public AsyncMapChange(PropertyMapImplement<X, T> property, Serializable value) {
        this.property = property;
        this.value = value;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        ImList<PropertyDrawEntity> changedProps = form.findChangedProperties(property.mapEntityObjects(mapObjects), value == null);
        if(changedProps != null)
            return new AsyncChange(changedProps, value);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> newSession() {
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping) {
        return new AsyncMapChange<>(property.map(mapping), value);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        PropertyMapImplement<X, P> mappedProperty = property.mapInner(mapping);
        if(mappedProperty != null)
            return new AsyncMapChange<>(mappedProperty, value);
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        PropertyMapImplement<X, P> mappedProperty = property.mapJoin(mapping);
        if(mappedProperty != null)
            return new AsyncMapChange<>(mappedProperty, value);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> exec) {
        if(!(exec instanceof AsyncMapChange))
            return null;

        AsyncMapChange<?, T> change = (AsyncMapChange<?, T>) exec;
        if(!(property.equalsMap(change.property) && BaseUtils.hashEquals(value, change.value))) // later it maybe makes sense to "or" this lists
            return null;

        return this;
    }
}
