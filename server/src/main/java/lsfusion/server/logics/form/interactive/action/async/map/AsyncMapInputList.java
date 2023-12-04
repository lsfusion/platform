package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.AppImage;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import org.apache.commons.lang3.ArrayUtils;

public class AsyncMapInputList<T extends PropertyInterface> {

    public final ImList<AsyncMapInputListAction<T>> actions;
    public final boolean strict;

    public AsyncMapInputList(ImList<AsyncMapInputListAction<T>> actions, boolean strict) {
        this.actions = actions;
        this.strict = strict;
    }

    public InputList map() {
        return new InputList(strict);
    }

    public InputListAction[] map(ImRevMap<T, ObjectEntity> mapObjects, FormInstanceContext context, ActionOrProperty securityProperty, PropertyDrawEntity drawProperty, GroupObjectEntity toDraw) {
        return filter(context.securityPolicy, securityProperty, actions.mapListValues(action -> action.map(mapObjects, context, securityProperty, drawProperty, toDraw)).toArray(new InputListAction[actions.size()]));
    }

    public static InputListAction[] filter(SecurityPolicy policy, ActionOrProperty securityProperty, InputListAction[] actions) {
        if (policy != null) {
            for (int i = 0; i < actions.length; i++) {
                if (actions[i].id.equals(AppImage.INPUT_NEW)) {
                    if (!policy.checkPropertyEditObjectsPermission(securityProperty)) {
                        return ArrayUtils.remove(actions, i);
                    }
                    break;
                }
            }
        }
        return actions;
    }

    public <P extends PropertyInterface> AsyncMapInputList<P> map(ImRevMap<T, P> mapping) {
        return new AsyncMapInputList<>(actions.mapListValues(action -> action.map(mapping)), strict);
    }

    public <P extends PropertyInterface> AsyncMapInputList<P> mapInner(ImRevMap<T, P> mapping) {
        return new AsyncMapInputList<>(actions.mapListValues(action -> action.mapInner(mapping)), strict);
    }

    public <P extends PropertyInterface> AsyncMapInputList<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return new AsyncMapInputList<>(actions.mapListValues(action -> action.mapJoin(mapping)), strict);
    }

    public AsyncMapInputList<T> replace(String replaceAction, AsyncMapEventExec<T> asyncExec) {
        return new AsyncMapInputList<>(actions.mapListValues(action -> action.replace(replaceAction, asyncExec)), strict);
    }
}
