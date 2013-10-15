package lsfusion.client.form;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.client.SwingUtils;
import lsfusion.interop.DaemonThreadFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

public class RmiQueue {
    public static final long FOREVER = 3L*24L*60L*60L*1000L;

    private RmiFuture currentDispatchingFuture;

    private final Queue<RmiFuture> rmiFutures = new ArrayDeque<RmiFuture>();
    private final ExecutorService rmiExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory("-client-dispatch-"));

    private final TableManager tableManager;
    private final Provider<String> serverMessageProvider;
    private final AsyncListener asyncListener;
    private boolean asyncStarted = false;
    private boolean syncStarted = false;

    private long nextRmiRequestIndex = 0;

    public RmiQueue(TableManager tableManager, Provider<String> serverMessageProvider, AsyncListener asyncListener) {
        this.serverMessageProvider = serverMessageProvider;
        this.tableManager = tableManager;
        this.asyncListener = asyncListener;
    }

    public <T> void asyncRequest(RmiRequest<T> request) {
        syncRequestWithTimeOut(0, request);
    }

    /**
     * @param timeOut time to wait in milliseconds
     * @param request rmi request
     * @return <code>false</code> if timed out
     */
    public <T> boolean syncRequestWithTimeOut(long timeOut, final RmiRequest<T> request) {
        if (timeOut >= FOREVER) {
            syncRequest(request);
            return true;
        }

        execRmiRequestInternal(request, request);

        boolean timedOut = true;
        //если timeout <=0 то даже не ждём, сразу выходим, т.к. чисто асинхронный вызов
        if (timeOut > 0) {
            long startTime = System.currentTimeMillis();
            timedOut = false;
            for (RmiFuture rmiFuture : rmiFutures) {
                long currentExecutionTime = System.currentTimeMillis() - startTime;
                try {
                    rmiFuture.get(timeOut - currentExecutionTime, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    timedOut = true;
                    break;
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        }

        if (timedOut) {
            request.onAsyncRequest();

            if(!asyncStarted) {
                asyncStarted = true;
                asyncListener.onAsyncStarted();
            }
        } else {
            forceProcessAllRequests();
        }

        return timedOut;
    }

    public <T> T syncRequest(final RmiRequest<T> request) {
//        System.out.println("----Sync request # " + nextRmiRequestIndex);

        try {
            final Result<T> result = new Result<T>();

            execRmiRequestInternal(request, new Callback<T>() {
                @Override
                public void done(T r) throws Exception {
                    result.set(r);
                    request.done(r);
                }
            });

            forceProcessAllRequests();

            return result.result;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void forceProcessAllRequests() {
        syncStarted = true;
        try {
            while (!rmiFutures.isEmpty()) {
                execNextFutureCallback();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            syncStarted = false;
        }
    }

    private <T> void execRmiRequestInternal(RmiRequest<T> request, Callback<T> callback) {
//        System.out.println("----Async request # " + nextRmiRequestIndex);
        SwingUtils.assertDispatchThread();

        request.setRequestIndex(nextRmiRequestIndex++);

        RmiFuture<T> rmiFuture = new RmiFuture<T>(request, callback);

        rmiFutures.add(rmiFuture);
        rmiExecutor.execute(rmiFuture);
    }

    private void flushCompletedRequests() {
        SwingUtils.assertDispatchThread();

        if (tableManager.isEditing()) {
            //не обрабатываем результат, пока не закончится редактирование и не вызовется this.editingStopped()
        } else {
            while (!rmiFutures.isEmpty() && rmiFutures.element().isDone()) {
                try {
                    execNextFutureCallback();
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        }
    }

    void editingStopped() {
        flushCompletedRequests();
    }

    boolean isSyncStarted() {
        return syncStarted;
    }

    private void execNextFutureCallback() throws Exception {
        currentDispatchingFuture = rmiFutures.remove();
        currentDispatchingFuture.execCallback();

        if(rmiFutures.isEmpty() && asyncStarted) {
            asyncStarted = false;
            asyncListener.onAsyncFinished();
        }
    }

    public long getCurrentDispatchingRequestIndex() {
        return currentDispatchingFuture.getRequestIndex();
    }

    public class RmiFuture<T> extends FutureTask<T> {
        private final RmiRequest<T> request;
        private final Callback<T> callback;

        public RmiFuture(final RmiRequest<T> request, Callback<T> callback) {
            super(request);
            this.request = request;
            this.callback = callback;
        }

        public long getRequestIndex() {
            return request.getRequestIndex();
        }

        @Override
        protected void done() {
            SwingUtils.invokeLater(new ERunnable() {
                @Override
                public void run() throws Exception {
                    flushCompletedRequests();
                }
            });
        }

        public void execCallback() throws Exception {
            SwingUtils.assertDispatchThread();

            T result;

            if (isDone()) {
                result = get();
            } else {
                BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
                busyDisplayer.start();

                try {

                    result = get();
                } finally {
                    busyDisplayer.stop();
                }
            }

            callback.done(result);
        }
    }
}
