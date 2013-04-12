package platform.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form.shared.view.actions.GAction;

public class ServerResponseResult implements Result {
    public GAction[] actions;
    public boolean resumeInvocation;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, boolean resumeInvocation) {
        this.actions = actions;
        this.resumeInvocation = resumeInvocation;
    }
}
