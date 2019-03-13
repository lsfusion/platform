package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.OldProperty;
import lsfusion.server.logics.event.SessionEnvEvent;
import lsfusion.server.logics.event.SystemEvent;

public class ApplyGlobalActionEvent extends ApplyGlobalEvent implements ApplyActionEvent {
    
    public final ActionProperty<?> action;

    public ApplyGlobalActionEvent(ActionProperty action) {
        this.action = action;

        assert action.getSessionEnv(SystemEvent.APPLY)!=null;
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
