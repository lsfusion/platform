package platform.gwt.form.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.form.shared.actions.form.ChangeGroupObject;
import platform.gwt.form.shared.actions.form.FormBoundAction;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.view.GForm;

import java.util.LinkedList;

public class FormDispatchAsync extends StandardDispatchAsync {
    private GForm form;

    public FormDispatchAsync(ExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    public void setForm(GForm form) {
        this.form = form;
    }

    private LinkedList<QueuedAction> q = new LinkedList<QueuedAction>();
    private boolean executing = false;

    @Override
    public <A extends Action<R>, R extends Result> void execute(A action, AsyncCallback<R> callback) {
        if (action instanceof FormBoundAction<?> && form != null) {
            ((FormBoundAction) action).formSessionID = form.sessionID;
        }

        if (q.isEmpty() && !executing) {
            executeAction(action, callback);
        } else {
            queueAction(action, callback);
        }
    }

    private <A extends Action<R>, R extends Result> void queueAction(A action, AsyncCallback<R> callback) {
        Log.debug("Queued action: " + action.getClass());
        q.add(new QueuedAction(action, callback));
    }

    private <A extends Action<R>, R extends Result> void executeAction(final A action, final AsyncCallback<R> callback) {
        Log.debug("Executing action: " + action.getClass());
        final long startExecTime = System.currentTimeMillis();
        executing = true;
        super.execute(action, new AsyncCallbackEx<R> () {
            @Override
            public void preProcess() {
                long execTime = System.currentTimeMillis() - startExecTime;
                Log.debug("Executed: " + action.getClass() + ", in " + execTime/1000 + " ms.");
            }

            @Override
            public void failure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void success(R result) {
                callback.onSuccess(result);
            }

            @Override
            public void postProcess() {
                executeNextQueuedAction();
            }
        });
    }

    private void executeNextQueuedAction() {
        executing = false;

        if (!q.isEmpty()) {
            QueuedAction qa = q.poll();
            executeAction(qa.action, qa.callback);
        }
    }

    public void executeChangeGroupObject(final ChangeGroupObject changeGroupObject, final AsyncCallback<FormChangesResult> callback) {
        if (!q.isEmpty()) {
            QueuedAction qa = q.getLast();
            if (qa.action instanceof ChangeGroupObject && ((ChangeGroupObject) qa.action).groupId == changeGroupObject.groupId) {
                //съедаем последнее изменение текущего объекта, если для этого же GroupObject
                q.removeLast();
            }
        }
        execute(changeGroupObject, callback);
    }
}
