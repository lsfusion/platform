package lsfusion.gwt.client.form.controller.dispatch;

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
        execute((FormAction<R>) action, callback);
        return action.requestIndex;
    }

    public <A extends FormAction<R>, R extends Result> long execute(A action, AsyncCallback<R> callback, boolean direct, boolean flushAnyway) {
        executeQueue(action, callback, direct, flushAnyway);
        if(action instanceof FormRequestAction) {
            return ((FormRequestAction<?>) action).requestIndex;
        } else {
            return -1;
        }
    }
    public <A extends FormAction<R>, R extends Result> long execute(A action, AsyncCallback<R> callback) {
        return execute(action, callback, false);
    }
    public <A extends FormAction<R>, R extends Result> long execute(A action, AsyncCallback<R> callback, boolean flushAnyway) {
        return execute(action, callback, false, flushAnyway);
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> void fillAction(A action) {
        ((FormAction) action).formSessionID = form.sessionID;
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> void fillQueuedAction(A action) {
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

    @Override
    protected boolean isClosed() {
        return formClosed;
    }

    public void close() {
        formClosed = true;
    }
}
