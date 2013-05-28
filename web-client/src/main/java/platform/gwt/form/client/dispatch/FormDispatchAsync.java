package platform.gwt.form.client.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.actions.form.FormBoundAction;
import platform.gwt.form.shared.actions.form.FormRequestIndexCountingAction;
import platform.gwt.form.shared.view.GForm;

import java.util.LinkedList;

public class FormDispatchAsync {
    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    private final GForm form;
    private final GFormController formController;

    private int nextRequestIndex = 0;

    private final LinkedList<QueuedAction> q = new LinkedList<QueuedAction>();
    private QueuedAction currentDispatchingAction;

    //отдельный флаг закрытой формы нужен, чтобы не посылать случайных запросов в закрытую форму (в частности changePageSize)
    private boolean formClosed = false;

    public FormDispatchAsync(GFormController formController) {
        this.formController = formController;
        this.form = formController.getForm();
    }

    public <A extends FormRequestIndexCountingAction<R>, R extends Result> int execute(A action, AsyncCallback<R> callback) {
        execute((FormBoundAction) action, callback);
        return action.requestIndex;
    }

    public <A extends FormBoundAction<R>, R extends Result> void execute(A action, AsyncCallback<R> callback) {
        action.formSessionID = form.sessionID;
        if (action instanceof FormRequestIndexCountingAction) {
            ((FormRequestIndexCountingAction) action).requestIndex = nextRequestIndex++;
        }

        queueAction(action, callback);
    }

    private <A extends Action<R>, R extends Result> void queueAction(final A action, final AsyncCallback<R> callback) {
        Log.debug("Queueing action: " + action.toString());

        final QueuedAction queuedAction = new QueuedAction(action, callback);
        q.add(queuedAction);

        formController.onAsyncStarted();

        executeInternal(action, new AsyncCallbackEx<R>() {
            @Override
            public void failure(Throwable caught) {
                queuedAction.failed(caught);
            }

            @Override
            public void success(R result) {
                queuedAction.succeded(result);
            }

            @Override
            public void postProcess() {
                flushCompletedRequests();
                formController.onAsyncFinished();
            }
        });
    }

    public void flushCompletedRequests() {
        if (!formController.isEditing()) {
            while (!q.isEmpty() && q.peek().finished) {
                execNextAction();
            }
        }
    }

    private void execNextAction() {
        currentDispatchingAction = q.remove();
        currentDispatchingAction.procceed();
        currentDispatchingAction = null;
    }

    public int getCurrentDispatchingRequestIndex() {
        return currentDispatchingAction != null ? currentDispatchingAction.getRequestIndex() : -1;
    }

    public <A extends FormBoundAction<R>, R extends Result> void executePriorityAction(final A action, final AsyncCallback<R> callback) {
        action.formSessionID = form.sessionID;
        Log.debug("Executing priority action: " + action.toString());
        executeInternal(action, callback);
    }

    private <A extends Action<R>, R extends Result> void executeInternal(final A action, final AsyncCallback<R> callback) {
        if (!formClosed) {
            final double startExecTime = Duration.currentTimeMillis();
            gwtDispatch.execute(action, new AsyncCallbackEx<R>() {
                @Override
                public void preProcess() {
                    double execTime = Duration.currentTimeMillis() - startExecTime;
                    Log.debug("Executed action: " + action.toString() + " in " + (int) (execTime / 1000) + " ms.");
                }

                @Override
                public void failure(Throwable caught) {
                    callback.onFailure(caught);
                }

                @Override
                public void success(R result) {
                    callback.onSuccess(result);
                }
            });
        }
    }

    public void close() {
        formClosed = true;
    }
}
