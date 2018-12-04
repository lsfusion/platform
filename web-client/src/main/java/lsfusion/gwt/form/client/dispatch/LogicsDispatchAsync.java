package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.base.client.AsyncCallbackEx;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

// part of this logics is in LogicsDispatchHandler
public class LogicsDispatchAsync {

    public static final LogicsDispatchAsync instance = new LogicsDispatchAsync();

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends Action<R>, R extends Result> R execute(final A action, final AsyncCallback<R> callback) {
        return gwtDispatch.execute(action, new AsyncCallbackEx<R>());
    }
}
