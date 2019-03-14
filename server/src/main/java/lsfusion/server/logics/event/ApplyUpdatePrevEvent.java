package lsfusion.server.logics.event;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.action.session.changed.OldProperty;

public class ApplyUpdatePrevEvent implements ApplySingleEvent {
    public final OldProperty property;

    @Override
    public Property getProperty() {
        return property;
    }

    public ApplyUpdatePrevEvent(OldProperty property) {
        this.property = property;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ApplyUpdatePrevEvent && property.equals(((ApplyUpdatePrevEvent) o).property);
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
