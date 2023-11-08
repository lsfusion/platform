package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class AsyncMapExec<T extends PropertyInterface> extends AsyncMapEventExec<T> {

    public AsyncExec map(ConnectionContext context) {
        return (AsyncExec) map(MapFact.EMPTYREV(), context, null, null, null);
    }

    private final static AsyncMapExec RECURSIVE = new AsyncMapExec<PropertyInterface>() {

        @Override
        public AsyncMapEventExec<PropertyInterface> newSession() {
            return this;
        }

        @Override
        public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<PropertyInterface, P> mapping) {
            return (AsyncMapEventExec<P>) this;
        }

        @Override
        public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<PropertyInterface, P> mapping) {
            return (AsyncMapEventExec<P>) this;
        }

        @Override
        public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<PropertyInterface, PropertyInterfaceImplement<P>> mapping) {
            return (AsyncMapEventExec<P>) this;
        }

        @Override
        public AsyncEventExec map(ImRevMap<PropertyInterface, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?> drawProperty, GroupObjectEntity toDraw) {
            return null;
        }

        @Override
        public AsyncMapEventExec<PropertyInterface> merge(AsyncMapEventExec<PropertyInterface> input) {
            throw new UnsupportedOperationException();
        }
    };
    public static <T extends PropertyInterface> AsyncMapExec<T> RECURSIVE() {
        return RECURSIVE;
    }
}
