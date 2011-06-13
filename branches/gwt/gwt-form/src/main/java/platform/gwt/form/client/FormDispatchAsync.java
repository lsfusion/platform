package platform.gwt.form.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form.shared.actions.form.FormBoundAction;
import platform.gwt.view.GForm;

public class FormDispatchAsync extends StandardDispatchAsync {
    private GForm form;

    public FormDispatchAsync(ExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    public void setForm(GForm form) {
        this.form = form;
    }

    @Override
    public <A extends Action<R>, R extends Result> void execute(A action, AsyncCallback<R> callback) {
        if (action instanceof FormBoundAction<?> && form != null) {
            ((FormBoundAction) action).formSessionID = form.sessionID;
        }
        super.execute(action, callback);
    }
}
