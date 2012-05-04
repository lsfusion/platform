package platform.interop.navigator;

import platform.interop.action.ClientAction;

import java.io.Serializable;

public class NavigatorActionResult implements Serializable {
    public final ClientAction[] actions;
    public final boolean resumeInvocation;

    public NavigatorActionResult(ClientAction[] actions) {
        this(actions, true);
    }

    public NavigatorActionResult(ClientAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}
