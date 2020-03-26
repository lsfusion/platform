package lsfusion.server.logics.event;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.DataProperty;

import java.sql.SQLException;

public class ApplyStoredEvent extends ApplyCalcEvent implements ApplySingleEvent {
    
    public final Property property;

    public ApplyStoredEvent(Property property) {
        this.property = property;
        assert property.isStored();
    }

    public boolean hasChanges(StructChanges changes) {
        return property.hasChanges(changes);
    }

    @Override
    public Property getProperty() {
        return property;
    }

    @Override
    public ImSet<OldProperty> getEventOldDepends() {
        if (property instanceof DataProperty && ((DataProperty) property).event != null)
            return ((DataProperty) property).event.getOldDepends();
        return super.getEventOldDepends();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ApplyStoredEvent && property.equals(((ApplyStoredEvent) o).property);
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
