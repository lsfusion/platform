package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.form.interactive.action.async.AsyncAddRemove;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapAdd<T extends PropertyInterface> extends AsyncMapInputExec<T> {
    public final CustomClass customClass;

    public AsyncMapAdd(CustomClass customClass) {
        this.customClass = customClass;
    }

    @Override
    public AsyncMapEventExec<T> newSession() {
        return null;
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
        for(ObjectEntity object : form.getObjects())
            if (object.baseClass instanceof CustomClass && customClass.isChild((CustomClass) object.baseClass) && object.isSimpleList())
                return new AsyncAddRemove(object, true);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapAdd))
            return null;

        AsyncMapAdd<T> asyncInput = (AsyncMapAdd<T>) input;
        return new AsyncMapAdd<>(customClass.getUpSet().getOr().or(asyncInput.customClass.getUpSet().getOr()).getCommonClass());
    }
}
