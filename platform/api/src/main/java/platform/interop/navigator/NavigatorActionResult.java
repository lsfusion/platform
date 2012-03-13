package platform.interop.navigator;

import platform.interop.action.ClientAction;

import java.io.Serializable;
import java.util.List;

public class NavigatorActionResult implements Serializable {
    public final List<ClientAction> actions;
    public final boolean resumeInvocation;

    public NavigatorActionResult(List<ClientAction> actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}
