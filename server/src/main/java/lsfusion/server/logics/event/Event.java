package lsfusion.server.logics.event;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class Event {
    public final String name;

    public final BaseEvent base;
    public final SessionEnvEvent session;
    public final ImOrderSet<ActionOrProperty> after;

    public PrevScope getScope() {
        return base.getScope();
    }

    public static final Event APPLY = new Event(null, SystemEvent.APPLY, SessionEnvEvent.ALWAYS, SetFact.EMPTYORDER());
    public static final Event SESSION = new Event(null, SystemEvent.SESSION, SessionEnvEvent.ALWAYS, SetFact.EMPTYORDER());

    public Event(String name, BaseEvent base, SessionEnvEvent session, ImOrderSet<ActionOrProperty> after) {
        this.name = name;

        this.base = base;
        this.session = session;
        this.after = after;
    }

    @Override
    public String toString() {
        return base + "," + session;
    }

    public Event onlyScope() {
        return new Event(null, base, session, null);
    }
}
