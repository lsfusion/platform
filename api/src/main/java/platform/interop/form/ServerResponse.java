package platform.interop.form;

import platform.interop.action.ClientAction;

import java.io.Serializable;

public class ServerResponse implements Serializable {
    public static final ServerResponse empty = new ServerResponse(null, false);

    public static final String CHANGE = "change";
    public static final String GROUP_CHANGE = "groupChange";
    public static final String EDIT_OBJECT = "editObject";
    public static final String PASTE = "paste";
    public static final String CHANGE_WYS = "change_wys";

    public final ClientAction[] actions;
    public final boolean resumeInvocation;

    public ServerResponse(ClientAction[] actions) {
        this(actions, true);
    }

    public ServerResponse(ClientAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}
