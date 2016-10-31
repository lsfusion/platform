package lsfusion.server.session;

public enum LocalNestedType {
    ALL, MANAGESESSION, NOMANAGESESSION;
    
    public boolean is(boolean manageSession) {
        if(this == ALL)
            return true;
        
        return (this == MANAGESESSION) == manageSession;        
    }
}
