package lsfusion.server.logics;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.OldProperty;
import lsfusion.server.logics.property.actions.SessionEnvEvent;

public abstract class ApplyCalcEvent extends ApplyGlobalEvent {

    public SessionEnvEvent getSessionEnv() {
        return SessionEnvEvent.ALWAYS;
    }

    @Override
    public ImSet<OldProperty> getEventOldDepends() {
        return SetFact.EMPTY();
    }
}
