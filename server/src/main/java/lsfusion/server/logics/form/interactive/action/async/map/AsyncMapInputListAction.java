package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncNoWaitExec;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.action.async.QuickAccess;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapInputListAction<T extends PropertyInterface> {

    public String action;
    public AsyncMapEventExec<T> asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public String keyStroke;
    public ImList<QuickAccess> quickAccessList;

    public AsyncMapInputListAction(String action, AsyncMapEventExec<T> asyncExec, String keyStroke, ImList<QuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.quickAccessList = quickAccessList;
    }

    // for input, actually it seems that async events are not used in that case
    public InputListAction map() {
        return new InputListAction(action, Action.getAsyncExec(asyncExec), keyStroke, quickAccessList);
    }

    public InputListAction map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        // here we can switch from sync to async
        AsyncEventExec mappedAsyncExec = this.asyncExec != null ? this.asyncExec.map(mapObjects, form, toDraw) : null;
        if(mappedAsyncExec == null && !PropertyDrawView.defaultSync)
            mappedAsyncExec = AsyncNoWaitExec.instance;
        return new InputListAction(action, mappedAsyncExec, keyStroke, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> map(ImRevMap<T, P> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.map(mapping) : null, keyStroke, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> mapInner(ImRevMap<T, P> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.mapInner(mapping) : null, keyStroke, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.mapJoin(mapping) : null, keyStroke, quickAccessList);
    }

    public AsyncMapInputListAction<T> replace(String replaceAction, AsyncMapEventExec<T> asyncExec) {
        if(action.equals(replaceAction))
            return new AsyncMapInputListAction<>(action, asyncExec, keyStroke, quickAccessList);
        return this;
    }
}
