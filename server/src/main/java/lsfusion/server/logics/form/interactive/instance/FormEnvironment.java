package lsfusion.server.logics.form.interactive.instance;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class FormEnvironment<P extends PropertyInterface> {
    private final ImMap<P, PropertyObjectInterfaceInstance> mapObjects;
    private final PropertyDrawInstance changingDrawInstance;
    private final FormInstance formInstance;

    public FormEnvironment(ImMap<P, PropertyObjectInterfaceInstance> mapObjects, PropertyDrawInstance changingDrawInstance, FormInstance formInstance) {
        this.mapObjects = mapObjects;
        this.changingDrawInstance = changingDrawInstance;
        this.formInstance = formInstance;
    }

    public static <P extends PropertyInterface> FormEnvironment<P> create(ImMap<P, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance) {
        return mapObjects == null ? null : new FormEnvironment<>(mapObjects, null, formInstance);
    }

    // the map objects have to be class-updated when their execution is postponed (see the apply recursion bodies - ActionValueImplement / NewSessionBody)
    public static <P extends PropertyInterface> ImMap<P, PropertyObjectInterfaceInstance> updateCurrentClasses(UpdateCurrentClassesSession session, ImMap<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException, SQLHandledException {
        if(mapObjects == null)
            return null;

        ImValueMap<P, PropertyObjectInterfaceInstance> mUpdateMapObjects = mapObjects.mapItValues(); // exception кидается
        for(int i=0,size=mapObjects.size();i<size;i++) {
            PropertyObjectInterfaceInstance mapObject = mapObjects.getValue(i);
            if(mapObject instanceof ObjectValue)
                mapObject = (PropertyObjectInterfaceInstance) session.updateCurrentClass((ObjectValue) mapObject);
            mUpdateMapObjects.mapValue(i, mapObject);
        }
        return mUpdateMapObjects.immutableValue();
    }

    public ImMap<P, PropertyObjectInterfaceInstance> getMapObjects() {
        return mapObjects;
    }
    
    public <T extends PropertyInterface> FormEnvironment<T> mapJoin(ImMap<T, ? extends PropertyInterfaceImplement<P>> map) {
        return new FormEnvironment<>(MapFact.nullInnerJoin(map, mapObjects), changingDrawInstance, formInstance);
    }

    public <T extends PropertyInterface> FormEnvironment<T> map(ImRevMap<T, P> map) {
        return new FormEnvironment<>(MapFact.nullInnerJoin(map, mapObjects), changingDrawInstance, formInstance);
    }

    public PropertyDrawInstance getChangingDrawInstance() {
        return changingDrawInstance;
    }
    
    public FormInstance getInstance() {
        return formInstance;
    }
}
