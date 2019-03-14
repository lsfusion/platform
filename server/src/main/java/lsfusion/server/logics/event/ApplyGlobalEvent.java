package lsfusion.server.logics.event;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.changed.OldProperty;

public abstract class ApplyGlobalEvent implements ApplyEvent {
    
    public abstract SessionEnvEvent getSessionEnv();
    
    public abstract ImSet<OldProperty> getEventOldDepends();
}
