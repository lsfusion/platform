package lsfusion.server.logics.property.actions;

import lsfusion.base.ImmutableObject;

public class SystemEvent extends ImmutableObject implements BaseEvent {
    
    private SystemEvent() {        
    }
    
    public final static SystemEvent APPLY = new SystemEvent();
    public final static SystemEvent SESSION = new SystemEvent();
}
