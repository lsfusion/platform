package lsfusion.server.logics.event;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.DataProperty;

public class ApplyStoredEvent extends ApplyCalcEvent implements ApplySingleEvent {
    
    public final Property property;

    public ApplyStoredEvent(Property property) {
        this.property = property;
        assert property.isStored();
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
