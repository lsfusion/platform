package platform.gwt.form2.client.dispatch;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.form2.shared.actions.form.FormBoundAction;
import platform.gwt.form2.shared.actions.form.FormRequestIndexCountingAction;
import platform.gwt.form2.shared.view.GForm;

import java.util.LinkedList;

public class FormDispatchAsync extends StandardDispatchAsync {
    private GForm form;

    public FormDispatchAsync(ExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    public void setForm(GForm form) {
        this.form = form;
    }

    long nextRequestIndex = 0;

    private LinkedList<QueuedAction> q = new LinkedList<QueuedAction>();

    @Override
    public <A extends Action<R>, R extends Result> void execute(A action, AsyncCallback<R> callback) {
        if (action instanceof FormBoundAction<?> && form != null) {
            ((FormBoundAction) action).formSessionID = form.sessionID;
            if (action instanceof FormRequestIndexCountingAction) {
                ((FormRequestIndexCountingAction) action).requestIndex = nextRequestIndex++;
            }
        }

        queueAction(action, callback);
    }

    private <A extends Action<R>, R extends Result> void queueAction(final A action, final AsyncCallback<R> callback) {
        Log.debug("Executing action: " + action.toString());

        final QueuedAction queuedAction = new QueuedAction(action, callback);
        q.add(queuedAction);

        final long startExecTime = System.currentTimeMillis();
        super.execute(action, new AsyncCallbackEx<R> () {
            @Override
            public void preProcess() {
                long execTime = System.currentTimeMillis() - startExecTime;

                Log.debug("Executed action: " + action.toString() + " in " + execTime/1000 + " ms.");
            }

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
            }
        });
    }

    private void flushCompletedRequests() {
        while (!q.isEmpty() && q.peek().finished) {
            q.remove().procceed();
        }
    }
}
