package lsfusion.server.logics.event;

import lsfusion.server.logics.property.ChangedProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;

// удаление объекта с заданным ClassDataProperty
public class ApplyRemoveClassesEvent extends ApplyCalcEvent {
    
    public final ChangedProperty property;

    public ApplyRemoveClassesEvent(ChangedProperty property) {
        this.property = property;
        
        assert property.isSingleApplyDroppedIsClassProp();
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
