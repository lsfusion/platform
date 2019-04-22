package lsfusion.gwt.client.form.controller.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestCountingAction;
import lsfusion.gwt.client.form.controller.GFormController;
import net.customware.gwt.dispatch.shared.Result;

public class FormDispatchAsync extends RemoteDispatchAsync {
    private final GForm form;
    private final GFormController formController;

    //отдельный флаг закрытой формы нужен, чтобы не посылать случайных запросов в закрытую форму (в частности changePageSize)
    private boolean formClosed = false;

    public FormDispatchAsync(GFormController formController) {
        this.formController = formController;
        this.form = formController.getForm();
    }

    public <A extends FormRequestCountingAction<R>, R extends Result> long execute(A action, AsyncCallback<R> callback) {
        execute((FormAction) action, callback);
        return action.requestIndex;
    }

    public <A extends FormAction<R>, R extends Result> void execute(A action, AsyncCallback<R> callback) {
        execute(action, callback, false);
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> void fillAction(A action) {
        ((FormAction) action).formSessionID = form.sessionID;
        if (action instanceof FormRequestAction) {
            if (action instanceof FormRequestCountingAction)
                ((FormRequestCountingAction) action).requestIndex = nextRequestIndex++;
            ((FormRequestAction) action).lastReceivedRequestIndex = lastReceivedRequestIndex;
        }
    }

    @Override
    protected void onAsyncStarted() {
        formController.onAsyncStarted();
    }

    @Override
    protected void onAsyncFinished() {
        formController.onAsyncFinished();
    }

    @Override
    protected boolean isEditing() {
        return formController.isEditing();
    }

    public <A extends FormAction<R>, R extends Result> void executePriorityAction(final A action, final AsyncCallback<R> callback) {
        action.formSessionID = form.sessionID;
        Log.debug("Executing priority action: " + action.toString());
        executeInternal(action, callback);
    }

    @Override
    protected boolean isClosed() {
        return formClosed;
    }

    public void close() {
        formClosed = true;
    }
}
