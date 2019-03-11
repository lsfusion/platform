package lsfusion.gwt.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.shared.actions.logics.LogicsAction;
import lsfusion.gwt.shared.actions.navigator.NavigatorAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

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

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends LogicsAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        action.host = host;
        action.port = port;
        action.exportName = exportName;
        
        gwtDispatch.execute(action, callback);
    }

}
