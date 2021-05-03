package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncChange;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class AsyncMapChange<T extends PropertyInterface> extends AsyncMapInputExec<T> {

    public final DataClass type;

    public final InputListEntity<?, T> list;

    public final InputList inputList;

    public final LP targetProp;

    public AsyncMapChange(DataClass type, InputListEntity<?, T> list, InputList inputList, LP targetProp) {
        this.type = type;
        this.list = list;
        this.inputList = inputList;
        this.targetProp = targetProp;
    }

    private <P extends PropertyInterface> AsyncMapChange<P> override(InputListEntity<?, P> list) {
        return new AsyncMapChange<P>(type, list, inputList, targetProp);
    }

    public AsyncMapChange<T> newSession() {
        return override(list != null ? list.newSession() : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapInputExec<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapInputExec<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> AsyncMapInputExec<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null);
    }

    @Override
    public AsyncEventExec map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form) {
        return new AsyncChange(type, targetProp, list != null ? inputList : null);
    }

    @Override
    public AsyncMapEventExec<T> merge(AsyncMapEventExec<T> input) {
        if(!(input instanceof AsyncMapChange))
            return null;

        AsyncMapChange<T> dataInput = ((AsyncMapChange<T>)input);
        if(list != null || dataInput.list != null) // later it maybe makes sense to "or" this lists
            return null;

        DataClass compatibleType = ((DataClass<?>)type).getCompatible(dataInput.type, true);
        if(compatibleType != null && targetProp.property.equals(dataInput.targetProp.property))
            return new AsyncMapChange<>(compatibleType, null, null, targetProp);
        return null;
    }
}
