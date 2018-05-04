package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

public class ActionPropertyValueImplement<T extends PropertyInterface> extends ActionPropertyImplement<T, ObjectValue> {

    // кривовато, но иначе там нужно небольшой рефакторинг проводить
    private final ImMap<T, PropertyObjectInterfaceInstance> mapObjects;
    private final FormInstance formInstance;

    public ActionPropertyValueImplement(ActionProperty<T> action, ImMap<T, ? extends ObjectValue> mapping, ImMap<T, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance) {
        super(action, (ImMap<T, ObjectValue>)mapping);
        this.mapObjects = mapObjects;
        this.formInstance = formInstance;
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack) throws SQLException, SQLHandledException {
        property.execute(mapping, session, stack, mapObjects == null ? null : new FormEnvironment<>(mapObjects, null, formInstance));
    }
    
    public ActionPropertyValueImplement<T> updateCurrentClasses(final DataSession session) throws SQLException, SQLHandledException {
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

        return new ActionPropertyValueImplement<>(property, session.updateCurrentClasses(mapping), updatedMapObjects, formInstance);
    }
}
