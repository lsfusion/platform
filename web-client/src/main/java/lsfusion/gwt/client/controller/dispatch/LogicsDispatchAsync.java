package lsfusion.gwt.client.controller.dispatch;

import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.controller.remote.action.PriorityAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.logics.LogicsAction;
import net.customware.gwt.dispatch.shared.Result;

// part of this logics is in *RequestHandlers
public class LogicsDispatchAsync {

    private final String host;
    private Integer port;
    private String exportName;

    public LogicsDispatchAsync(String host, Integer port, String exportName) {
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }

    private final GWTDispatch gwtDispatch = GWTDispatch.instance;

    public <A extends LogicsAction<R>, R extends Result> void execute(final A action, final PriorityAsyncCallback<R> callback) {
        action.host = host;
        action.port = port;
        action.exportName = exportName;
        
        gwtDispatch.execute(action, () -> RemoteDispatchAsync.priorityExec, new Object(), callback);
    }

}
