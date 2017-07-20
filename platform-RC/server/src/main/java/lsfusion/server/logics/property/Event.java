package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.actions.BaseEvent;
import lsfusion.server.logics.property.actions.SessionEnvEvent;
import lsfusion.server.logics.property.actions.SystemEvent;

public class Event {
    public final BaseEvent base;
    public final SessionEnvEvent session;
    public final ImSet<CalcProperty> after; 

    public PrevScope getScope() {
        return base.getScope();
    }

    public static final Event APPLY = new Event(SystemEvent.APPLY, SessionEnvEvent.ALWAYS, SetFact.<CalcProperty>EMPTY());
    public static final Event SESSION = new Event(SystemEvent.SESSION, SessionEnvEvent.ALWAYS, SetFact.<CalcProperty>EMPTY());

    public Event(BaseEvent base, SessionEnvEvent session, ImSet<CalcProperty> after) {
        this.base = base;
        this.session = session;
        this.after = after;
    }

    @Override
    public String toString() {
        return base + "," + session;
    }
}
