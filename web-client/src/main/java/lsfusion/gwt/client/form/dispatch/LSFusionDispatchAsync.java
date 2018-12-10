package lsfusion.gwt.client.form.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.shared.actions.LookupLogicsAndCreateNavigator;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.general.StringResult;

// part of this logics is in *RequestHandlers
public class LSFusionDispatchAsync {

    public static final LSFusionDispatchAsync instance = new LSFusionDispatchAsync();

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public void execute(final LookupLogicsAndCreateNavigator action, final AsyncCallback<StringResult> callback) {
        gwtDispatch.execute(action, callback);
    }

}
