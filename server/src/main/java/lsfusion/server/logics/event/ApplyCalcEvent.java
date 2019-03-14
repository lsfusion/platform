package lsfusion.server.logics.event;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.changed.OldProperty;

public abstract class ApplyCalcEvent extends ApplyGlobalEvent {

    public SessionEnvEvent getSessionEnv() {
        return SessionEnvEvent.ALWAYS;
    }

    @Override
    public ImSet<OldProperty> getEventOldDepends() {
        return SetFact.EMPTY();
    }
}
