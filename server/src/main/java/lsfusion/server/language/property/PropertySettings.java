package lsfusion.server.language.property;

import lsfusion.server.data.table.IndexType;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.debug.BooleanDebug;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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
    public String defaultCompare;
    public Boolean autoset;
    public LocalizedString pattern;
    public LocalizedString regexp;
    public LocalizedString regexpMessage;
    public Boolean echoSymbols;
    public Boolean aggr;
    public String eventId;
    public Property.Lazy lazy;
    public DebugInfo.DebugPoint debugPoint;
}
