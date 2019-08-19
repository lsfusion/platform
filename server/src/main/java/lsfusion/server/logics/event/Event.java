package lsfusion.server.logics.event;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;

public class Event {
    public final BaseEvent base;
    public final SessionEnvEvent session;
    public final ImSet<Property> after; 

    public PrevScope getScope() {
        return base.getScope();
    }

    public static final Event APPLY = new Event(SystemEvent.APPLY, SessionEnvEvent.ALWAYS, SetFact.EMPTY());
    public static final Event SESSION = new Event(SystemEvent.SESSION, SessionEnvEvent.ALWAYS, SetFact.EMPTY());

    public Event(BaseEvent base, SessionEnvEvent session, ImSet<Property> after) {
        this.base = base;
        this.session = session;
        this.after = after;
    }

    @Override
    public String toString() {
        return base + "," + session;
    }
}
