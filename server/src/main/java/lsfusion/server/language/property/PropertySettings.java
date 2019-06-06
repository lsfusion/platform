package lsfusion.server.language.property;

import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.logics.event.Event;
import lsfusion.server.physics.dev.debug.BooleanDebug;

public class PropertySettings extends ActionOrPropertySettings {
    public String table = null;
    public boolean isPersistent = false;
    public Boolean isComplex = null;
    public boolean isPreread = false;
    public boolean noHint = false;
    public boolean isLoggable = false;
    public BooleanDebug notNull = null;
    public BooleanDebug notNullResolve = null;
    public Event notNullEvent = null;
}
