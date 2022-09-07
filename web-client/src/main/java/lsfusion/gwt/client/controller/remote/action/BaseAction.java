package lsfusion.gwt.client.controller.remote.action;

import lsfusion.gwt.client.GRequestAttemptInfo;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class BaseAction<R extends Result> implements Action<R> {
    public GRequestAttemptInfo requestAttempt;

    public boolean logRemoteException() {
        return true;
    }
}
