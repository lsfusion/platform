package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

public class AsyncMapInput<T extends PropertyInterface> extends AsyncMapFormExec<T> {

    public final DataClass type;

    public final InputListEntity<?, T> list;

    public final AsyncMapInputList<T> inputList;

    public final String customEditorFunction;

    public AsyncMapInput(DataClass type, InputListEntity<?, T> list, AsyncMapInputList<T> inputList, String customEditorFunction) {
        this.type = type;
        this.list = list;
        this.inputList = inputList;
        this.customEditorFunction = customEditorFunction;
    }

    public AsyncMapInput<T> override(String action, AsyncMapEventExec<T> asyncExec) {
        return new AsyncMapInput<>(type, list, inputList != null ? inputList.replace(action, asyncExec) : null, customEditorFunction);
    }

    private <P extends PropertyInterface> AsyncMapInput<P> override(InputListEntity<?, P> list, AsyncMapInputList<P> inputList) {
        return new AsyncMapInput<P>(type, list, inputList, customEditorFunction);
    }

    public AsyncMapInput<T> newSession() {
        return override(list != null ? list.newSession() : null, inputList);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null, inputList != null ? inputList.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null, inputList != null ? inputList.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapFormExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null, inputList != null ? inputList.mapJoin(mapping) : null);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, SecurityPolicy policy, ActionOrProperty securityProperty, GroupObjectEntity toDraw) {
        return new AsyncInput(type, list != null && inputList != null ? inputList.map(mapObjects, form, policy, securityProperty, toDraw) : null, customEditorFunction);
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
            return new AsyncMapInput<>(compatibleType, null, null, customEditorFunction);
        return null;
    }

    @Override
    public int getMergeOptimisticPriority() {
        return 1;
    }

    @Override
    public boolean needOwnPushResult() {
        return true; // we have to send input value
    }
}
