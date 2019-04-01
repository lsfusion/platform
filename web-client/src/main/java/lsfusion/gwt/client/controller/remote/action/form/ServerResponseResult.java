package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.action.GAction;
import net.customware.gwt.dispatch.shared.Result;

public class ServerResponseResult implements Result {
    public GAction[] actions;
    public boolean resumeInvocation;
    public long requestIndex;

    public ServerResponseResult() {}

    public ServerResponseResult(GAction[] actions, long requestIndex, boolean resumeInvocation) {
        this.actions = actions;
        this.requestIndex = requestIndex;
        this.resumeInvocation = resumeInvocation;
    }
}
