package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.OldProperty;
import lsfusion.server.logics.property.actions.SessionEnvEvent;

public abstract class ApplyGlobalEvent implements ApplyEvent {
    
    public abstract SessionEnvEvent getSessionEnv();
    
    public abstract ImSet<OldProperty> getEventOldDepends();
}
