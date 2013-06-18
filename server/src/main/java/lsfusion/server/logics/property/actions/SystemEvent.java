package lsfusion.server.logics.property.actions;

import lsfusion.base.ImmutableObject;
import lsfusion.server.logics.property.PrevScope;

public class SystemEvent extends ImmutableObject implements BaseEvent {
    
    private SystemEvent() {        
    }

    public PrevScope getScope() {
        if(this == APPLY)
            return PrevScope.DB;

        return PrevScope.EVENT;
    }

    public final static SystemEvent APPLY = new SystemEvent();
    public final static SystemEvent SESSION = new SystemEvent();
}
