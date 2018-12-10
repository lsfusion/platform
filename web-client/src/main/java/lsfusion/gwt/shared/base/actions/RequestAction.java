package lsfusion.gwt.shared.base.actions;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class RequestAction<R extends Result> implements Action<R> {
    public int requestTry;
    
    public boolean logRemoteException() {
        return true;
    }
}
