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

    public final T valueInterface;
    public final Serializable value;

    public AsyncMapChange(PropertyMapImplement<X, T> property, Serializable value, T valueInterface) {
        this.property = property;
        this.value = value;
        this.valueInterface = valueInterface;
        assert valueInterface == null || value == null;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        if(valueInterface != null)
            return null;

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
        PropertyMapImplement<X, P> mappedProperty = property.map(mapping);
        if (valueInterface != null)
            return new AsyncMapChange<>(mappedProperty, null, mapping.get(valueInterface));
        else
            return new AsyncMapChange<>(mappedProperty, value, null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        PropertyMapImplement<X, P> mappedProperty = property.mapInner(mapping);
        if(mappedProperty != null) {
            if(valueInterface != null) {
                P mappedValueInterface = mapping.get(valueInterface);
                if(mappedValueInterface != null)
                    return new AsyncMapChange<>(mappedProperty, null, mappedValueInterface);
            } else
                return new AsyncMapChange<>(mappedProperty, value, null);
        }
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        PropertyMapImplement<X, P> mappedProperty = property.mapJoin(mapping);
        if(mappedProperty != null) {
            if(valueInterface != null) {
                PropertyInterfaceImplement<P> mappedValueInterface = mapping.get(valueInterface);
                if(mappedValueInterface != null)
                    return mappedValueInterface.mapAsyncChange(mappedProperty);
            } else
                return new AsyncMapChange<>(mappedProperty, value, null);
        }
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
