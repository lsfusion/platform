package lsfusion.server.logics.action.flow;

public class ChangeFlowType {
    public static final ChangeFlowType APPLY = new ChangeFlowType();
    public static final ChangeFlowType CANCEL = new ChangeFlowType();
    public static final ChangeFlowType BREAK = new ChangeFlowType();
    public static final ChangeFlowType RETURN = new ChangeFlowType();
    public static final ChangeFlowType SYNC = new ChangeFlowType();
    public static final ChangeFlowType NEWSESSION = new ChangeFlowType();

    public static final ChangeFlowType READONLYCHANGE = new ChangeFlowType(); // has changes in this session (no other opening forms, because in that case security policy will work)
    public static final ChangeFlowType INTERACTIVEFORM = new ChangeFlowType(); // has opening interactive forms inside
    public static final ChangeFlowType HASSESSIONUSAGES = new ChangeFlowType(); // checks if action uses this session (used for formAction WAIT | NOWAIT heuristic)
    public static final ChangeFlowType NEEDMORESESSIONUSAGES = new ChangeFlowType(); // optimization, checks if action needs to fill moreSessionUsages (used for formAction WAIT | NOWAIT heuristic),
    public static final ChangeFlowType INTERNALASYNC = new ChangeFlowType(); // checks if InternalClientAction is async
    ;
    public boolean isChange() {
        return this instanceof FormChangeFlowType || this == READONLYCHANGE;
    }    
}
