package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapInput<T extends PropertyInterface> extends AsyncMapValue<T> {

    public final InputListEntity<?, T> list;

    public final AsyncMapInputList<T> inputList;

    public final boolean hasOldValue;
    public final PropertyInterfaceImplement<T> oldValue;

    public final String customEditorFunction;

    public AsyncMapInput(DataClass type, InputListEntity<?, T> list, AsyncMapInputList<T> inputList, boolean hasOldValue, PropertyInterfaceImplement<T> oldValue, String customEditorFunction) {
        super(type);

        this.list = list;
        this.inputList = inputList;

        this.hasOldValue = hasOldValue;
        this.oldValue = oldValue;

        this.customEditorFunction = customEditorFunction;
    }

    public AsyncMapInput<T> override(String action, AsyncMapEventExec<T> asyncExec) {
        return new AsyncMapInput<>(type, list, inputList != null ? inputList.replace(action, asyncExec) : null, hasOldValue, oldValue, customEditorFunction);
    }

    private <P extends PropertyInterface> AsyncMapInput<P> override(InputListEntity<?, P> list, AsyncMapInputList<P> inputList, boolean hasOldValue, PropertyInterfaceImplement<P> oldValue) {
        return new AsyncMapInput<P>(type, list, inputList, hasOldValue, oldValue, customEditorFunction);
    }

    public AsyncMapInput<T> newSession() {
        return override(list != null ? list.newSession() : null, inputList, hasOldValue, oldValue);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null, inputList != null ? inputList.map(mapping) : null, hasOldValue, oldValue != null ? oldValue.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null, inputList != null ? inputList.mapInner(mapping) : null, hasOldValue, oldValue != null ? oldValue.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null, inputList != null ? inputList.mapJoin(mapping) : null, hasOldValue, oldValue instanceof PropertyInterface ? mapping.get((T)oldValue) : null);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?> drawProperty, GroupObjectEntity toDraw) {
        if (hasOldValue && !(
                oldValue instanceof PropertyMapImplement && drawProperty != null && context instanceof FormInstanceContext && drawProperty.isProperty((FormInstanceContext) context) &&
                ((PropertyMapImplement<?, T>) oldValue).mapEntityObjects(mapObjects).equalsMap(drawProperty.getAssertCellProperty((FormInstanceContext) context))))
            return null;
        return new AsyncInput(type, list != null && inputList != null ? inputList.map() : null,
                list != null && inputList != null ? inputList.map(mapObjects, (FormInstanceContext) context, securityProperty, drawProperty, toDraw) : null, customEditorFunction);
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
            return new AsyncMapInput<>(compatibleType, null, null, hasOldValue || dataInput.hasOldValue,
                    oldValue == null || dataInput.oldValue == null || oldValue.equals(dataInput.oldValue) ? BaseUtils.nvl(oldValue, dataInput.oldValue) : null, customEditorFunction);
        return null;
    }

    @Override
    public <X extends PropertyInterface> Pair<InputListEntity<X, T>, AsyncDataConverter<X>> getAsyncValueList(Result<String> value) {
        return new Pair<>((InputListEntity<X, T>) list, null);
    }
}
