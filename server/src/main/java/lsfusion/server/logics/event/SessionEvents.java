package lsfusion.server.logics.event;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.Property;

public interface SessionEvents {
    ImMap<OldProperty, SessionEnvEvent> getSessionEventOldDepends(FunctionSet<? extends Property> properties);
}
