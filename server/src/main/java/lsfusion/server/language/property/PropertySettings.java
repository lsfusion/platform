package lsfusion.server.language.property;

import lsfusion.server.data.table.IndexType;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.logics.event.Event;
import lsfusion.server.physics.dev.debug.BooleanDebug;

public class PropertySettings extends ActionOrPropertySettings {
    public String table = null;
    public String field = null;
    public boolean isMaterialized = false;
    public IndexType indexType = null;
    public String indexName = null;
    public Boolean isComplex = null;
    public boolean isPreread = false;
    public Boolean isHint = null;
    public BooleanDebug notNull = null;
    public BooleanDebug notNullResolve = null;
    public Event notNullEvent = null;
    public Event notNullResolveEvent = null;
}
