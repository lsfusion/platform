package lsfusion.gwt.client.controller.remote.action;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class BaseAction<R extends Result> implements Action<R> {
    public transient int requestTry;
    
    public boolean logRemoteException() {
        return true;
    }
}
