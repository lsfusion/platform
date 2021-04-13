package lsfusion.interop.action;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;

import java.io.Serializable;

public class ServerResponse implements Serializable {
    public static final ServerResponse EMPTY = new ServerResponse(-1, null, false);

    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String CHANGE_WYS = "change_wys";

    public static final ImList<String> changeEvents = ListFact.toList(CHANGE, CHANGE_WYS, GROUP_CHANGE);
    public static final ImList<String> events = ListFact.toList(CHANGE, CHANGE_WYS, GROUP_CHANGE, EDIT_OBJECT);

    public static final String INPUT = "input";
    public static final String FILTER = "filter";

    public static final String RECHECK = "recheck";
    public static final String CANCELED = "canceled";

    public final ClientAction[] actions;
    public final boolean resumeInvocation;
    public final long requestIndex;
    
    public long timeSpent = -1;

    public ServerResponse(ClientAction[] actions) {
        this(-1, actions);
    }

    public ServerResponse(long requestIndex, ClientAction[] actions) {
        this(requestIndex, actions, true);
    }

    public ServerResponse(long requestIndex, ClientAction[] actions, boolean resumeInvocation) {
        this.requestIndex = requestIndex;
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }

    public static ServerResponse EMPTY(int requestIndex) {
        return new ServerResponse(requestIndex, null, false);
    }
}
