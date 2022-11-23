package lsfusion.gwt.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.AsyncCallbackEx;
import lsfusion.gwt.client.base.busy.GBusyDialogDisplayer;
import lsfusion.gwt.client.base.busy.LoadingManager;
import lsfusion.gwt.client.controller.dispatch.GWTDispatch;
import lsfusion.gwt.client.controller.remote.action.*;
import lsfusion.gwt.client.controller.remote.action.form.GetAsyncValues;
import lsfusion.gwt.client.form.controller.dispatch.QueuedAction;
import lsfusion.gwt.client.view.ServerMessageProvider;
import net.customware.gwt.dispatch.shared.Result;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public abstract class RemoteDispatchAsync implements ServerMessageProvider {
    private final GWTDispatch gwtDispatch = GWTDispatch.instance;

    protected long nextRequestIndex = 0;
    protected long lastReceivedRequestIndex = -1;

    private final LinkedList<QueuedAction> q = new LinkedList<>();

    private LoadingManager loadingManager;

    public LoadingManager getLoadingManager() {
        if (loadingManager == null)
            loadingManager = new GBusyDialogDisplayer(this);

        return loadingManager;
    }

    protected abstract <A extends BaseAction<R>, R extends Result> void fillAction(A action);
    protected abstract <A extends RequestAction<R>, R extends Result> long fillQueuedAction(A action);

    public <A extends RequestCountingAction<R>, R extends Result> long asyncExecute(A action, RequestCountingAsyncCallback<R> callback) {
        return executeQueue(action, callback, false, false);
    }

    public <A extends RequestAction<R>, R extends Result> long syncExecute(A action, RequestAsyncCallback<R> callback, boolean continueInvocation) {
        return executeQueue(action, callback, true, continueInvocation);
    }

    public <A extends PriorityAction<R>, R extends Result> void executePriority(final A action, final PriorityAsyncCallback<R> callback) {
        gwtExecute((BaseAction<R>) action, () -> priorityExec, new Object(), callback);
    }

    public int syncCount;
    public int flushCount;
    public int asyncCount;

    private static final int ASYNC_TIME_OUT = 20;

    private Timer asyncTimer = new Timer() {
        @Override
        public void run() {
            showAsync(true);
        }
    };

    protected abstract void showAsync(boolean set);

    public void onAsyncStarted() {
        if(asyncCount == 0)
            asyncTimer.schedule(ASYNC_TIME_OUT);
        asyncCount++;
    }

    public void onAsyncFinished() {
        asyncCount--;
        if (asyncCount == 0) {
            asyncTimer.cancel();
            showAsync(false);
        }
    }

    public <A extends RequestAction<R>, R extends Result> long executeQueue(A action, RequestAsyncCallback<R> callback, boolean sync, boolean continueInvocation) {
        // in desktop there is direct query mechanism (for continuing single invocating), which blocks EDT, and guarantee synchronization
        // in web there is no such mechanism, so we'll put the queued action to the very beginning of the queue
        // otherwise there might be deadlock, when, for example, between ExecuteEventAction and continueServerInvocation there was changePageSize
        long requestIndex = fillQueuedAction(action);
        final QueuedAction queuedAction = new QueuedAction(requestIndex, callback, action instanceof GetAsyncValues);
        if (continueInvocation) {
            q.add(0, queuedAction);
            waitingForContinueInvocation = false;
        } else {
            q.add(queuedAction);
        }

        if(sync) {
            // actually we want the rule :
            //      started = syncCount > 0 && flushCount == 0
            // so all the checks is an incremental when set(started) do start; when dropped(start) do stop
            if (syncCount == 0 && flushCount == 0)
                getLoadingManager().start();
            syncCount++;
        } else
            onAsyncStarted();

        // we will not prioritize queued calls if we're waiting for the continueInvocation to come (not to cancel this calls when the continue call comes)
        // should be "synchronized" with the params (q, waitingForContinueInvocation) changes, see usages / gwtOnPriorirtyIncreased calls
        gwtExecute((BaseAction<R>) action,
            () -> - ((waitingForContinueInvocation ? 1 : 0) + q.indexOf(queuedAction)) * queueIndexStep,
            queuedAction, new AsyncCallbackEx<R>() {
            @Override
            public void preProcess() {
                if(sync) {
                    syncCount--;
                    if (syncCount == 0 && flushCount == 0)
                        getLoadingManager().stop(true);
                } else
                    onAsyncFinished();
            }

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
            }
        });

        return requestIndex;
    }

    public void onEditingFinished() {
        if(pendingFlushCompletedRequests) {
            flushCompletedRequests();
            pendingFlushCompletedRequests = false;
        }
    }

    private void flushCompletedRequests() {
        flushCompletedRequests(() -> {
            if(syncCount > 0 && flushCount == 0)
                getLoadingManager().stop(false);
            flushCount++;
        }, () -> {
            flushCount--;
            if(syncCount > 0 && flushCount == 0)
                getLoadingManager().start();
        });
    }

    protected boolean isEditing() {
        return false;
    }
    protected long getEditingRequestIndex() {
        return -1;
    }

    // at first we want "zero" requests, than modal windows / lower indices requests
    public static int queueIndexStep = 1000000;
    public static int windowDeepStep = 10000;
    public static int requestIndexDeepStep = 1;

    // to be less important than zero requests, but more than the others
    public static int priorityExec = - queueIndexStep / 2;

    private boolean waitingForContinueInvocation;

    private boolean pendingFlushCompletedRequests;
    public void flushCompletedRequests(Runnable preProceed, Runnable postProceed) {
        q.forEach(queuedAction -> {
            if (queuedAction.preProceeded != null && !queuedAction.preProceeded && queuedAction.finished) {
                preProceed.run();
                queuedAction.proceed(postProceed);
                queuedAction.preProceeded = true;
            }
        });

        QueuedAction action;
        while (!q.isEmpty() && (action = q.peek()).finished) {
            long requestIndex = action.requestIndex;

            // when editing suspending all requests (except some actions that are marked for editing)
            if (isEditing() && requestIndex > getEditingRequestIndex()) {
                pendingFlushCompletedRequests = true;
                break;
            }

            q.remove();
            waitingForContinueInvocation = action.isContinueInvocation();
            if(!waitingForContinueInvocation) // see priority function (should be synchronized with it)
                gwtOnPriorityExecuted(q);

            if (requestIndex >= 0)
                lastReceivedRequestIndex = requestIndex;

            if (action.preProceeded == null || !action.preProceeded) {
                preProceed.run();
                action.proceed(postProceed);
            }
        }
    }

    protected boolean isClosed() {
        return false;
    }

    protected int getDispatchPriority() {
        return 0;
    }

    protected <A extends BaseAction<R>, R extends Result> void gwtOnPriorityExecuted(List<?> ids) {
        if(!isClosed())
            gwtDispatch.onPriorityIncreased(ids);
    }

    protected <A extends BaseAction<R>, R extends Result> void gwtExecute(final A action, Supplier<Integer> priority, Object id, final AsyncCallback<R> callback) {
        if (!isClosed()) {
            fillAction(action);
            Log.debug("Executing action: " + action.toString());

            final double startExecTime = Duration.currentTimeMillis();
            gwtDispatch.execute(action, () -> priority.get() + getDispatchPriority(), id, new AsyncCallbackEx<R>() {
                @Override
                public void preProcess() {
                    double execTime = Duration.currentTimeMillis() - startExecTime;
                    Log.debug("Executed action: " + action.toString() + " in " + (int) (execTime / 1000) + " ms.");
                }

                @Override
                public void success(R result) {
                    callback.onSuccess(result);
                }

                @Override
                public void failure(Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        }
    }
}