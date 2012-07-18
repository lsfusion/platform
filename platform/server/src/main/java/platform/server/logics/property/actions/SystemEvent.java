package platform.server.logics.property.actions;

public class SystemEvent implements BaseEvent {
    
    private SystemEvent() {        
    }
    
    public final static SystemEvent APPLY = new SystemEvent();
    public final static SystemEvent SESSION = new SystemEvent();
}
