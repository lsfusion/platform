package lsfusion.server.logics.action.flow;

public enum ChangeFlowType {
    APPLY, CANCEL, BREAK, RETURN, SYNC, NEWSESSION,
    PRIMARY, // primary design button classes, usually NEW, which means the main cases
    FORMCHANGE, // has changes in this session or other opening form  
    READONLYCHANGE, // has changes in this session (no other opening forms, because in that case security policy will work)
    INTERACTIVEFORM, // has opening interactive forms inside
    HASSESSIONUSAGES, // checks if action uses this session (used for formAction WAIT | NOWAIT heuristic)
    NEEDMORESESSIONUSAGES, // optimization, checks if action needs to fill moreSessionUsages (used for formAction WAIT | NOWAIT heuristic),
    INTERNALASYNC // checks if InternalClientAction is async
    ; 

    public boolean isChange() {
        return this == FORMCHANGE || this == READONLYCHANGE;
    }    
}
