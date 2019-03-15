package lsfusion.server.logics.action.implement;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.event.ApplyActionEvent;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ActionValueImplement<T extends PropertyInterface> extends ActionImplement<T, ObjectValue> implements ApplyActionEvent {

    // кривовато, но иначе там нужно небольшой рефакторинг проводить
    private final ImMap<T, PropertyObjectInterfaceInstance> mapObjects;
    private final FormInstance formInstance;

    public ActionValueImplement(Action<T> action, ImMap<T, ? extends ObjectValue> mapping, ImMap<T, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance) {
        super(action, (ImMap<T, ObjectValue>)mapping);
        this.mapObjects = mapObjects;
        this.formInstance = formInstance;
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack) throws SQLException, SQLHandledException {
        property.execute(mapping, session, stack, mapObjects == null ? null : new FormEnvironment<>(mapObjects, null, formInstance));
    }
    
    public ActionValueImplement<T> updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        ImMap<T, PropertyObjectInterfaceInstance> updatedMapObjects = null;
        if(mapObjects != null) {
            ImValueMap<T, PropertyObjectInterfaceInstance> mUpdateMapObjects = mapObjects.mapItValues(); // exception кидается
            for(int i=0,size=mapObjects.size();i<size;i++) {
                PropertyObjectInterfaceInstance mapObject = mapObjects.getValue(i);
                if(mapObject instanceof ObjectValue)
                    mapObject = (PropertyObjectInterfaceInstance) session.updateCurrentClass((ObjectValue) mapObject);
                mUpdateMapObjects.mapValue(i, mapObject);
            }
            updatedMapObjects = mUpdateMapObjects.immutableValue();
        }

        return new ActionValueImplement<>(property, session.updateCurrentClasses(mapping), updatedMapObjects, formInstance);
    }
}
