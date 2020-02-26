package lsfusion.server.logics.event;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;

import java.sql.SQLException;

// удаление объекта с заданным ClassDataProperty
public class ApplyRemoveClassesEvent extends ApplyCalcEvent {
    
    public final ChangedProperty property;

    public ApplyRemoveClassesEvent(ChangedProperty property) {
        this.property = property;
        
        assert property.isSingleApplyDroppedIsClassProp();
    }

    public boolean hasChanges(StructChanges changes) {
        return property.hasChanges(changes);
    }

    public IsClassProperty getIsClassProperty() {
        return (IsClassProperty) property.property;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ApplyRemoveClassesEvent && property.equals(((ApplyRemoveClassesEvent) o).property);
    }

    @Override
    public int hashCode() {
        return property.hashCode();
    }

    @Override
    public String toString() {
        return property.toString();
    }
}
