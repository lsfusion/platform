package lsfusion.server.logics.property;

public class PropertySettings extends ActionOrPropertySettings {
    public String table = null;
    public boolean isPersistent = false;
    public boolean isComplex = false;
    public boolean noHint = false;
    public boolean isLoggable = false;
    public BooleanDebug notNull = null;
    public BooleanDebug notNullResolve = null;
    public Event notNullEvent = null;
}
