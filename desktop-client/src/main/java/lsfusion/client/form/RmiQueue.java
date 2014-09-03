package lsfusion.client.form;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.client.ClientLoggers;
import lsfusion.client.SwingUtils;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.exceptions.RemoteAbandonedException;
import lsfusion.interop.exceptions.FatalHandledRemoteException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RmiQueue {
    private static final Logger logger = ClientLoggers.invocationLogger;

    private final static Object edtSyncBlocker = new Object();

    private final Queue<RmiFuture> rmiFutures = new ArrayDeque<RmiFuture>();
    private final ExecutorService rmiExecutor;

    private final TableManager tableManager;
    private final Provider<String> serverMessageProvider;
    private final AsyncListener asyncListener;
    private boolean asyncStarted = false;
    private int syncsDepth = 0;

    private long nextRmiRequestIndex = 0;
    private long lastReceivedRequestIndex = -1;

    private AtomicBoolean abandoned = new AtomicBoolean();

    public RmiQueue(TableManager tableManager, Provider<String> serverMessageProvider, AsyncListener asyncListener) {
        this.serverMessageProvider = serverMessageProvider;
        this.tableManager = tableManager;
        this.asyncListener = asyncListener;

        rmiExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory("rmi-queue"));

        ConnectionLostManager.registerRmiQueue(this);
    }

    public static void notifyEdtSyncBlocker() {
        synchronized (edtSyncBlocker) {
            edtSyncBlocker.notify();
        }
    }

    public static void waitOnEdtSyncBlocker() throws InterruptedException {
        synchronized (edtSyncBlocker) {
            edtSyncBlocker.wait();
        }
    }

    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned) {
        return runRetryableRequest(request, abandoned, false);
    }
    
    private static AtomicLong reqIdGen = new AtomicLong();
    
    // вызывает request (предположительно remote) несколько раз, проблемы с целостностью предполагается что решается либо индексом, либо результат не так важен
    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned, boolean registeredFailure) {
        int reqCount = 0;
        boolean abandonedIt = false;
        long reqId = reqIdGen.incrementAndGet();
        try {
            do {
                try {
                    return request.call();
                } catch (Throwable t) {
                    abandonedIt = abandoned.get();
                    if(abandonedIt) // suppress'им все, failedRmiRequest'ы flush'ся отдельно
                        throw new RemoteAbandonedException();
                        
                    if (t instanceof RemoteException) {
                        RemoteException remote = (RemoteException) t;

                        int maxFatal = ExceptionUtils.getFatalRemoteExceptionCount(t);
                        if (reqCount > maxFatal) {
                            ConnectionLostManager.connectionLost();

                            t = new FatalHandledRemoteException(remote, reqId);
                        } else {
                            reqCount++;

                            if (!registeredFailure) {
                                ConnectionLostManager.registerFailedRmiRequest();
                                registeredFailure = true;
                            }

                            ConnectionLostManager.addFailedRmiRequest(remote, reqId);
                            t = null;
                        }
                    }

                    if (t != null) {
                        throw Throwables.propagate(t);
                    }
                }

                SystemUtils.sleep(300);
            } while (true);
        } finally {
            if (registeredFailure) {
                ConnectionLostManager.unregisterFailedRmiRequest(abandonedIt);
            }
        }        
    }

    public static void handleNotRetryableRemoteException(RemoteException remote) {
        ConnectionLostManager.connectionBroke();
    }

    public void abandon() {
        abandoned.set(true);
    }

    public <T> T directRequest(long requestIndex, final RmiRequest<T> request) throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("Direct request: " + request);
        }

        request.setRequestIndex(requestIndex);
        request.setLastReceivedRequestIndex(lastReceivedRequestIndex - 1);

        return blockingRequest(request, true);
    }

    public <T> T syncRequest(final RmiRequest<T> request) {
        if (logger.isDebugEnabled()) {
            logger.debug("Sync request: " + request);
        }

        return blockingRequest(request, false);
    }

    private <T> T blockingRequest(final RmiRequest<T> request, boolean direct) {
        SwingUtils.assertDispatchThread();

        if (!direct && syncsDepth != 0) {
            IllegalStateException ex = new IllegalStateException("Nested sync request shouldn't occure.");
            logger.error("Nested sync request: ", ex);
            throw ex;
        }

        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();

        syncsDepth++;
        try {
            RmiFuture<T> rmiFuture;
            if (direct) {
                rmiFuture = new RmiFuture<T>(request);
                rmiExecutor.execute(rmiFuture);
            } else {
                rmiFuture = execRmiRequestInternal(request);
            }

            while (!rmiFuture.isDone()) {
                waitOnEdtSyncBlocker();

                ConnectionLostManager.blockIfHasFailed();
                if (abandoned.get()) {
                    throw new RuntimeException("RmiQueue is abandoned");
                }
                if (!direct) {
                    flushCompletedRequestsNow(true);
                }
            }

            return rmiFuture.get();
        } catch (Throwable t) {
            if (t instanceof ExecutionException) {
                t = t.getCause();
            }

            throw Throwables.propagate(t);
        } finally {
            syncsDepth--;
            busyDisplayer.stop();
        }
    }

    public <T> void asyncRequest(final RmiRequest<T> request) {
        if (logger.isDebugEnabled()) {
            logger.debug("Async request: " + request);
        }

        execRmiRequestInternal(request);

        request.onAsyncRequest();

        if (!asyncStarted) {
            asyncStarted = true;
            asyncListener.onAsyncStarted();
        }
    }

    private <T> RmiFuture<T> execRmiRequestInternal(RmiRequest<T> request) {
        SwingUtils.assertDispatchThread();

        request.setRequestIndex(nextRmiRequestIndex++);
        request.setLastReceivedRequestIndex(lastReceivedRequestIndex);

        if (logger.isDebugEnabled()) {
            logger.debug("Executing request's thread: " + request);
        }

        RmiFuture<T> rmiFuture = new RmiFuture<T>(request);

        rmiFutures.add(rmiFuture);
        rmiExecutor.execute(rmiFuture);

        return rmiFuture;
    }

    private void flushCompletedRequests() {
        SwingUtils.assertDispatchThread();

        if (abandoned.get()) {
            return;
        }

        //не обрабатываем результат, пока не закончится редактирование и не вызовется this.editingStopped()
        if (!tableManager.isEditing()) {
            flushCompletedRequestsNow(false);
        }
    }

    private void flushCompletedRequestsNow(boolean inSyncRequest) {
        while (!rmiFutures.isEmpty() && rmiFutures.element().isDone()) {
            try {
                execNextFutureCallback();
            } catch (Throwable t) {
                //при синхоронном вызове нужно, чтобы exception выбрасывался из того вызова
                // => обрабатываем асинхронные запросы в очереди как обычно, но для последнего (синхронного) - выбрасываем наверх
                if (rmiFutures.isEmpty() && inSyncRequest) {
                    throw Throwables.propagate(t);
                } else {
                    if (t instanceof ServerException || t instanceof ExecutionException) {
                        t = t.getCause();
                    }
                    ClientExceptionManager.handle(t);
                }
            }
        }
    }

    void editingStopped() {
        flushCompletedRequests();
    }

    boolean isSyncStarted() {
        return syncsDepth != 0;
    }

    private void execNextFutureCallback() throws Exception {
        RmiFuture future = rmiFutures.remove();
        try {
            future.execCallback();
        } finally {
            if (rmiFutures.isEmpty() && asyncStarted) {
                asyncStarted = false;
                asyncListener.onAsyncFinished();
            }
        }
    }

    public class RmiFuture<T> extends FutureTask<T> {
        private final RmiRequest<T> request;

        public RmiFuture(final RmiRequest<T> request) {
            super(new RequestCallable<T>(request));
            this.request = request;
        }

        @Override
        protected void done() {
            notifyEdtSyncBlocker();
            SwingUtils.invokeLater(new ERunnable() {
                @Override
                public void run() throws Exception {
                    flushCompletedRequests();
                }
            });
        }

        public void execCallback() throws Exception {
            SwingUtils.assertDispatchThread();

            lastReceivedRequestIndex = request.getRequestIndex();

            assert isDone();

            if (logger.isDebugEnabled()) {
                logger.debug("Executing RmiFutureCallback: " + request);
            }

            request.onResponse(get());
        }
    }

    private class RequestCallable<T> implements Callable<T> {
        private final RmiRequest<T> request;

        public RequestCallable(RmiRequest<T> request) {
            this.request = request;
        }

        @Override
        public T call() throws RemoteException {
            return runRetryableRequest(new Callable<T>() {
                public T call() throws Exception {
                    return request.doRequest();
                }
            }, abandoned);
        }
    }
}
