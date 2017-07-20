package lsfusion.interop.form;

import lsfusion.interop.action.ClientAction;

import java.io.Serializable;

public class ServerResponse implements Serializable {
    public static final ServerResponse EMPTY = new ServerResponse(-1, null, false);

    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String PASTE = "paste";
    public static final String CHANGE_WYS = "change_wys";

    public final ClientAction[] actions;
    public final boolean resumeInvocation;
    public final long requestIndex;

    public ServerResponse(ClientAction[] actions) {
        this(-1, actions);
    }

    public ServerResponse(long requestIndex, ClientAction[] actions) {
        this(requestIndex, actions, true);
    }

    public ServerResponse(ClientAction[] actions, boolean resumeInvocation) {
        this(-1, actions, resumeInvocation);
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
