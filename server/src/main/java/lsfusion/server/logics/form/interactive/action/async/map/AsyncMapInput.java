package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.file.AppImage;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.action.input.InputContextListEntity;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import org.apache.commons.lang3.ArrayUtils;

public class AsyncMapInput<T extends PropertyInterface> extends AsyncMapValue<T> {

    public final InputContextListEntity<?, T> list;

    public final ImList<AsyncMapInputListAction<T>> actions;
    public final boolean strict;

    public final boolean hasDrawOldValue;
    public final PropertyInterfaceImplement<T> oldValue;

    public final String customEditorFunction;

    public AsyncMapInput(DataClass type, InputContextListEntity<?, T> list, ImList<AsyncMapInputListAction<T>> actions, boolean strict, boolean hasDrawOldValue, PropertyInterfaceImplement<T> oldValue, String customEditorFunction) {
        super(type);

        this.list = list;
        this.actions = actions;
        this.strict = strict;

        this.hasDrawOldValue = hasDrawOldValue;
        this.oldValue = oldValue;

        this.customEditorFunction = customEditorFunction;
    }

    public AsyncMapInput<T> override(String action, AsyncMapEventExec<T> asyncExec) {
        return new AsyncMapInput<>(type, list, actions != null ? actions.mapListValues(a -> a.replace(action, asyncExec)) : null, strict, hasDrawOldValue, oldValue, customEditorFunction);
    }

    private <P extends PropertyInterface> AsyncMapInput<P> override(InputContextListEntity<?, P> list, ImList<AsyncMapInputListAction<P>> actions, PropertyInterfaceImplement<P> oldValue) {
        return new AsyncMapInput<>(type, list, actions, strict, hasDrawOldValue, oldValue, customEditorFunction);
    }

    public AsyncMapInput<T> newSession() {
        return override(list != null ? list.newSession() : null, actions, oldValue);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapInput<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null, actions != null ? actions.mapListValues(action -> action.map(mapping)) : null, oldValue != null ? oldValue.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null, actions != null ? actions.mapListValues(action -> action.mapInner(mapping)) : null, oldValue != null ? oldValue.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null, actions != null ? actions.mapListValues(action -> action.mapJoin(mapping)) : null, oldValue instanceof PropertyInterface ? mapping.get((T)oldValue) : null);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?> drawProperty, GroupObjectEntity toDraw) {
        if (hasDrawOldValue && !(
                oldValue instanceof PropertyMapImplement && drawProperty != null && context instanceof FormInstanceContext && drawProperty.isProperty((FormInstanceContext) context) &&
                ((PropertyMapImplement<?, T>) oldValue).mapEntityObjects(mapObjects).equalsMap(drawProperty.getAssertCellProperty((FormInstanceContext) context))))
            return null;
        return new AsyncInput(type, list != null ? new InputList(strict) : null,
                filter(((FormInstanceContext) context).securityPolicy, securityProperty, actions != null ? actions.mapListValues(action -> action.map(mapObjects, (FormInstanceContext) context, securityProperty, drawProperty, toDraw)).toArray(new InputListAction[actions.size()]) : null), customEditorFunction);
    }

    public static InputListAction[] filter(SecurityPolicy policy, ActionOrProperty securityProperty, InputListAction[] actions) {
        if (policy != null && actions != null) {
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

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapInput))
            return null;

        AsyncMapInput<T> dataInput = ((AsyncMapInput<T>)input);
        if(list != null || dataInput.list != null) // later it maybe makes sense to "or" this lists
            return null;

        DataClass compatibleType = ((DataClass<?>)type).getCompatible(dataInput.type, true);
        if(compatibleType != null)
            return new AsyncMapInput<>(compatibleType, null, null, false, hasDrawOldValue || dataInput.hasDrawOldValue,
                    oldValue == null || dataInput.oldValue == null || oldValue.equals(dataInput.oldValue) ? BaseUtils.nvl(oldValue, dataInput.oldValue) : null, customEditorFunction);
        return null;
    }

    @Override
    public <X extends PropertyInterface> Pair<InputContextListEntity<X, T>, AsyncDataConverter<X>> getAsyncValueList(Result<String> value) {
        return new Pair<>((InputContextListEntity<X, T>) list, null);
    }

    public static AsyncMode getAsyncMode(boolean strict) {
        return strict ? AsyncMode.OBJECTVALUES : AsyncMode.VALUES;
    }
}
