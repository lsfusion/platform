package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.form.interactive.action.async.AsyncChange;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.Serializable;

public class AsyncMapChange<X extends PropertyInterface, T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final ObjectEntity object;
    public final PropertyMapImplement<X, T> property;

    public final T valueInterface;
    public final Object value;

    public AsyncMapChange(PropertyMapImplement<X, T> property, ObjectEntity object, Object value, T valueInterface) {
        this.property = property;
        this.object = object;
        assert object == null || property == null;

        this.value = value;
        this.valueInterface = valueInterface;
        assert valueInterface == null || value == null;
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, SecurityPolicy policy, ActionOrProperty securityProperty, GroupObjectEntity toDraw) {
        if(valueInterface != null)
            return null;

        ImList<PropertyDrawEntity> changedProps = form.findChangedProperties(object != null ? object : property.mapEntityObjects(mapObjects), value == null);
        if(changedProps != null)
            return new AsyncChange(changedProps, value instanceof LocalizedString ? ThreadLocalContext.localize((LocalizedString) value) : (Serializable) value);

        return null;
    }

    @Override
    public AsyncMapEventExec<T> newSession() {
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<T, P> mapping) {
        PropertyMapImplement<X, P> mappedProperty = null;
        if(object == null) {
            mappedProperty = property.map(mapping);
            if(mappedProperty == null)
                return null;
        }

        if (valueInterface != null)
            return new AsyncMapChange<>(mappedProperty, object, null, mapping.get(valueInterface));
        else
            return new AsyncMapChange<>(mappedProperty, object, value, null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<T, P> mapping) {
        PropertyMapImplement<X, P> mappedProperty = null;
        if(object == null) {
            mappedProperty = property.mapInner(mapping);
            if(mappedProperty == null)
                return null;
        }

        if(valueInterface != null) {
            P mappedValueInterface = mapping.get(valueInterface);
            if(mappedValueInterface != null)
                return new AsyncMapChange<>(mappedProperty, object, null, mappedValueInterface);
        } else
            return new AsyncMapChange<>(mappedProperty, object, value, null);
        return null;
    }

    @Override
    public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        PropertyMapImplement<X, P> mappedProperty = null;
        if(object == null) {
            mappedProperty = property.mapJoin(mapping);
            if(mappedProperty == null)
                return null;
        }

        if(valueInterface != null) {
            PropertyInterfaceImplement<P> mappedValueInterface = mapping.get(valueInterface);
            if(mappedValueInterface != null)
                return mappedValueInterface.mapAsyncChange(mappedProperty, object);
        } else
            return new AsyncMapChange<>(mappedProperty, object, value, null);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> exec) {
        if(!(exec instanceof AsyncMapChange))
            return null;

        AsyncMapChange<?, T> change = (AsyncMapChange<?, T>) exec;
        if(!(BaseUtils.nullEquals(object, change.object) &&
                (property == null ? change.property == null : change.property != null && property.equalsMap(change.property)) && // nullEquals
                BaseUtils.nullEquals(value, change.value))) // later it maybe makes sense to "or" this lists
            return null;

        return this;
    }
}
