package lsfusion.server.logics.property.actions.flow;

public enum ChangeFlowType {
    APPLY, CANCEL, BREAK, RETURN, SYNC, NEWSESSION, 
    FORMCHANGE, // в той же сессии или в открываемой форме  
    READONLYCHANGE // в любой сессии (открываемые формы не учитываются, так как там политика безопасности сработает)
    ; 

    public boolean isChange() {
        return this == FORMCHANGE || this == READONLYCHANGE;
    }    
}
