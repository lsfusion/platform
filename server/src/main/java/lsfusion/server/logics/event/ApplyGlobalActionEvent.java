package lsfusion.server.logics.event;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;

public class ApplyGlobalActionEvent extends ApplyGlobalEvent implements ApplyEvent {
    
    public final Action<?> action;

    public ApplyGlobalActionEvent(Action action) {
        this.action = action;

        assert action.getSessionEnv(SystemEvent.APPLY)!=null;
    }

    public boolean hasChanges(StructChanges changes) {
        for (SessionProperty property : action.getGlobalEventSessionCalcDepends())
            if (property.hasChanges(changes))
                return true;
        return false;
    }

    @Override
    public SessionEnvEvent getSessionEnv() {
        return action.getSessionEnv(SystemEvent.APPLY);
    }

    @Override
    public ImSet<OldProperty> getEventOldDepends() {
        return action.getOldDepends();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ApplyGlobalActionEvent && action.equals(((ApplyGlobalActionEvent) o).action);
    }

    @Override
    public int hashCode() {
        return action.hashCode();
    }

    @Override
    public String toString() {
        return action.toString();
    }
}
