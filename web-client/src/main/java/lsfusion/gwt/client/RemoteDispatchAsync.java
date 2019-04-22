package lsfusion.gwt.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.AsyncCallbackEx;
import lsfusion.gwt.client.controller.dispatch.DispatchAsyncWrapper;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.form.controller.dispatch.QueuedAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

import java.util.LinkedList;

public abstract class RemoteDispatchAsync {
    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    protected long nextRequestIndex = 0;
    protected long lastReceivedRequestIndex = -1;

    private final LinkedList<QueuedAction> q = new LinkedList<>();

    protected abstract <A extends RequestAction<R>, R extends Result> void fillAction(A action);

    public <A extends RequestAction<R>, R extends Result> void execute(A action, AsyncCallback<R> callback, boolean direct) {
        fillAction(action);
        queueAction(action, callback, direct);
    }


    protected void onAsyncStarted() {
    }

    protected void onAsyncFinished() {
    }

    protected <A extends Action<R>, R extends Result> void queueAction(final A action, final AsyncCallback<R> callback, boolean direct) {
        Log.debug("Queueing action: " + action.toString());

        final QueuedAction queuedAction = new QueuedAction(action, callback);
        // в десктопе реализован механизм direct запросов, которые работают не через очередь, а напрямую блокируют EDT.
        // в вебе нет возможности реализовать подобный механизм. поэтому ставим direct запросы в начало очереди.
        // иначе мог произойти deadlock, когда, к примеру, между ExecuteEditAction и continueServerInvocation вклинивался changePageSize
        if (direct) {
            q.add(0, queuedAction);
        } else {
            q.add(queuedAction);
        }

        onAsyncStarted();

        executeInternal(action, new AsyncCallbackEx<R>() {
            @Override
            public void failure(Throwable caught) {
                queuedAction.failed(caught);
            }

            @Override
            public void success(R result) {
                queuedAction.succeeded(result);
            }

            @Override
            public void postProcess() {
                flushCompletedRequests();
                onAsyncFinished();
            }
        });
    }

    protected boolean isEditing() {
        return false;
    }

    public void flushCompletedRequests() {
        if (!isEditing()) {
            while (!q.isEmpty() && q.peek().finished) {
                QueuedAction remove = q.remove();

                long requestIndex = remove.getRequestIndex();
                if(requestIndex >= 0)
                    lastReceivedRequestIndex = requestIndex;

                remove.proceed();
            }
        }
    }

    protected boolean isClosed() {
        return false;
    }

    protected <A extends Action<R>, R extends Result> void executeInternal(final A action, final AsyncCallback<R> callback) {
        if (!isClosed()) {
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
}