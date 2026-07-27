package lsfusion.server.logics.action.implement;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.RecursiveBody;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ActionValueImplement<T extends PropertyInterface> extends ActionImplement<T, ObjectValue> implements RecursiveBody {

    // кривовато, но иначе там нужно небольшой рефакторинг проводить
    private final ImMap<T, PropertyObjectInterfaceInstance> mapObjects;
    private final FormInstance formInstance;

    public ActionValueImplement(Action<T> action, ImMap<T, ? extends ObjectValue> mapping, ImMap<T, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance) {
        super(action, (ImMap<T, ObjectValue>)mapping);
        this.mapObjects = mapObjects;
        this.formInstance = formInstance;
    }

    public Action<?> getSingleApplyAction() { // RecursiveBody
        return action;
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack) throws SQLException, SQLHandledException {
        execute(session, stack, null);
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack, PushAsyncResult pushedAsyncResult) throws SQLException, SQLHandledException {
        action.execute(mapping, session, stack, FormEnvironment.create(mapObjects, formInstance), pushedAsyncResult);
    }
    
    public ActionValueImplement<T> updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        return new ActionValueImplement<>(action, session.updateCurrentClasses(mapping), FormEnvironment.updateCurrentClasses(session, mapObjects), formInstance);
    }
}
