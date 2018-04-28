package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.DataProperty;
import lsfusion.server.logics.property.OldProperty;

public class ApplyStoredEvent extends ApplyCalcEvent implements ApplySingleEvent {
    
    public final CalcProperty property;

    public ApplyStoredEvent(CalcProperty property) {
        this.property = property;
        assert property.isStored();
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
