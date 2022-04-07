package lsfusion.server.logics.form.interactive.action.async.map;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.function.Function;

public class AsyncMapInputList<T extends PropertyInterface> {

    public final ImList<AsyncMapInputListAction<T>> actions;
    public final boolean strict;

    public AsyncMapInputList(ImList<AsyncMapInputListAction<T>> actions, boolean strict) {
        this.actions = actions;
        this.strict = strict;
    }

    public InputList map() {
        return new InputList(actions.mapListValues((Function<AsyncMapInputListAction<T>, InputListAction>) AsyncMapInputListAction::map).toArray(new InputListAction[actions.size()]), strict);
    }

    public InputList map(ImRevMap<T, ObjectEntity> mapObjects, FormEntity form, GroupObjectEntity toDraw) {
        return new InputList(actions.mapListValues(action -> action.map(mapObjects, form, toDraw)).toArray(new InputListAction[actions.size()]), strict);
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
