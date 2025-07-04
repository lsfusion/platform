package lsfusion.server.logics.action.flow;

public class ChangeFlowType {
    public static final ChangeFlowType APPLY = new ChangeFlowType();
    public static final ChangeFlowType CANCEL = new ChangeFlowType();
    public static final ChangeFlowType BREAK = new ChangeFlowType();
    public static final ChangeFlowType CONTINUE = new ChangeFlowType();
    public static final ChangeFlowType RETURN = new ChangeFlowType();
    public static final ChangeFlowType SYNC = new ChangeFlowType();

    public static final ChangeFlowType PRIMARY = new ChangeFlowType(); // primary design button classes, usually NEW, which means the main cases
    public static final ChangeFlowType INPUT = new ChangeFlowType(); // has input inside
    public static final ChangeFlowType ANYEFFECT = new ChangeFlowType(); // check if this is not a "shallow" action

    public static final ChangeFlowType READONLYCHANGE = new ChangeFlowType(); // has changes in this session (no other opening forms, because in that case security policy will work)
    public static final ChangeFlowType INTERACTIVEFORM = new ChangeFlowType(); // has opening interactive forms inside
    public static final ChangeFlowType INTERACTIVEWAIT = new ChangeFlowType(); // has interaction that waits for the user action
    public static final ChangeFlowType INTERACTIVEAPI = new ChangeFlowType(); // has interaction that needs ui in the api (not handled with processClientAction)
    public static final ChangeFlowType HASSESSIONUSAGES = new ChangeFlowType(); // checks if action uses this session (used for formAction WAIT | NOWAIT heuristic)
    public static final ChangeFlowType NEEDMORESESSIONUSAGES = new ChangeFlowType(); // optimization, checks if action needs to fill moreSessionUsages (used for formAction WAIT | NOWAIT heuristic),
    public static final ChangeFlowType INTERNALASYNC = new ChangeFlowType(); // checks if InternalClientAction is async
    public static final ChangeFlowType GROUPCHANGE = new ChangeFlowType(); // has interactive form, but not inside request

    public boolean isChange() {
        return this == READONLYCHANGE || this instanceof FormChangeFlowType || this == ANYEFFECT;
    }

    public boolean isManageSession() {
        return this == READONLYCHANGE || this == HASSESSIONUSAGES || this == ANYEFFECT;
    }

    public boolean isSession() {
        return isChange() || isManageSession();
    }
}
