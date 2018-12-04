package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.form.shared.actions.LookupLogics;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

// part of this logics is in *RequestHandlers
public class LSFusionDispatchAsync {

    public static final LSFusionDispatchAsync instance = new LSFusionDispatchAsync();

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public void execute(final LookupLogics action, final AsyncCallback<StringResult> callback) {
        gwtDispatch.execute(action, callback);
    }

}
