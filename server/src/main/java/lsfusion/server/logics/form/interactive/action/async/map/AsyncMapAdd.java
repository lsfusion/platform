package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncAddRemove;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapAdd<T extends PropertyInterface> extends AsyncMapFormExec<T> {
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
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        AsyncAddRemove object = map(toDraw);
        if (object != null)
            return object;

        for(GroupObjectEntity group : form.getGroups())
            if(!BaseUtils.hashEquals(group, toDraw)) {
                object = map(group);
                if (object != null)
                    return object;
            }
        return null;
    }

    public AsyncAddRemove map(GroupObjectEntity group) {
        if(group.isSimpleList())
            for(ObjectEntity object : group.getObjects())
                if (object.baseClass instanceof CustomClass && customClass.isChild((CustomClass) object.baseClass))
                    return new AsyncAddRemove(object, true);
        return null;
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapAdd))
            return null;

        AsyncMapAdd<T> asyncInput = (AsyncMapAdd<T>) input;
        return new AsyncMapAdd<>(ClassFormSelector.merge(customClass, asyncInput.customClass));
    }

    @Override
    public boolean needOwnPushResult() {
        return true; // if add we need to send new ID
    }
}
