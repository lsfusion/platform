package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.form.event.BindingMode;
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
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

import java.util.Map;

public class AsyncMapInputListAction<T extends PropertyInterface> {

    public String action;
    public AsyncMapEventExec<T> asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public String keyStroke;
    public Map<String, BindingMode> bindingModesMap;
    public Integer priority;
    public ImList<QuickAccess> quickAccessList;

    public AsyncMapInputListAction(String action, AsyncMapEventExec<T> asyncExec, String keyStroke, Map<String, BindingMode> bindingModesMap, Integer priority, ImList<QuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.keyStroke = keyStroke;
        this.bindingModesMap = bindingModesMap;
        this.priority = priority;
        this.quickAccessList = quickAccessList;
    }

    // for input, actually it seems that async events are not used in that case
    public InputListAction map() {
        return new InputListAction(action, Action.getAsyncExec(asyncExec), keyStroke, bindingModesMap, priority, quickAccessList);
    }

    public InputListAction map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, SecurityPolicy policy, ActionOrProperty securityProperty, GroupObjectEntity toDraw) {
        // here we can switch from sync to async
        AsyncEventExec mappedAsyncExec = this.asyncExec != null ? this.asyncExec.map(mapObjects, form, policy, securityProperty, toDraw) : null;
        if(mappedAsyncExec == null && !PropertyDrawView.defaultSync)
            mappedAsyncExec = AsyncNoWaitExec.instance;
        return new InputListAction(action, mappedAsyncExec, keyStroke, bindingModesMap, priority, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> map(ImRevMap<T, P> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.map(mapping) : null, keyStroke, bindingModesMap, priority, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> mapInner(ImRevMap<T, P> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.mapInner(mapping) : null, keyStroke, bindingModesMap, priority, quickAccessList);
    }

    public <P extends PropertyInterface> AsyncMapInputListAction<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return new AsyncMapInputListAction<>(action, asyncExec != null ? asyncExec.mapJoin(mapping) : null, keyStroke, bindingModesMap, priority, quickAccessList);
    }

    public AsyncMapInputListAction<T> replace(String replaceAction, AsyncMapEventExec<T> asyncExec) {
        if(action.equals(replaceAction))
            return new AsyncMapInputListAction<>(action, asyncExec, keyStroke, bindingModesMap, priority, quickAccessList);
        return this;
    }
}
