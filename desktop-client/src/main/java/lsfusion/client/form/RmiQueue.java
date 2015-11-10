package lsfusion.client.form;

import com.google.common.base.Throwables;
import lsfusion.base.ERunnable;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Provider;
import lsfusion.base.SystemUtils;
import lsfusion.client.ClientLoggers;
import lsfusion.client.SwingUtils;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.interop.exceptions.FatalHandledRemoteException;
import lsfusion.interop.exceptions.RemoteAbandonedException;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;
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

    public static void waitOnEdtSyncBlocker(long timeout) throws InterruptedException {
        synchronized (edtSyncBlocker) {
            if(timeout > 0)
                edtSyncBlocker.wait(timeout);
        }
    }

    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned) {
        return runRetryableRequest(request, abandoned, false);
    }
    
    private static AtomicLong reqIdGen = new AtomicLong();
    
    // вызывает request (предположительно remote) несколько раз, проблемы с целостностью предполагается что решается либо индексом, либо результат не так важен
    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned, boolean registeredFailure) {
        int reqCount = 0;
        long reqId = reqIdGen.incrementAndGet();
        try {
            do {
                try {
                    return request.call();
                } catch (Throwable t) {
                    if(abandoned.get()) // suppress'им все, failedRmiRequest'ы flush'ся отдельно
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
                ConnectionLostManager.unregisterFailedRmiRequest(abandoned.get(), reqId);
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

    boolean busyRunning;
    boolean pendingBusyFlush;

    private <T> T blockingRequest(final RmiRequest<T> request, final boolean direct) {
        SwingUtils.assertDispatchThread();

        if (!direct && syncsDepth != 0) {
            IllegalStateException ex = new IllegalStateException("Nested sync request shouldn't occur.");
            logger.error("Nested sync request: ", ex);
            throw ex;
        }

        BusyDialogDisplayer busyDisplayer = null;
        //BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        //busyDisplayer.start();

        syncsDepth++;
        long start = System.currentTimeMillis();
        try {
            final RmiFuture<T> rmiFuture;
            if (direct) {
                rmiFuture = new RmiFuture<>(request);
                rmiExecutor.execute(rmiFuture);
            } else {
                rmiFuture = execRmiRequestInternal(request);
            }

            while (!rmiFuture.isDone()) {
                long timeout = 1000 - (System.currentTimeMillis() - start);

                boolean flush = !direct;

                if (timeout <= 0) { //секунда прошла, а запрос ещё выполняется
                    if(busyDisplayer == null) {
                        busyDisplayer = new BusyDialogDisplayer(serverMessageProvider);
                        busyDisplayer.start();
                    }

                    busyRunning = true;

                    busyDisplayer.show(new Runnable() {
                        @Override
                        public void run() {
                            while (!rmiFuture.isDone() && !(!direct && isRmiFutureDone())) {
                                try {
                                    waitOnEdtSyncBlocker();
                                } catch (InterruptedException e) {
                                    logger.error(e);
                                }
                            }
                        }
                    });//показываем диалог

                    busyRunning = false; //чтобы не выполнять обработку в EDT busyDialog (на всякий случай)
                    if(pendingBusyFlush)
                        flush = true;
                    pendingBusyFlush = false;
                } else
                    waitOnEdtSyncBlocker(timeout); //blocker выполнения запроса, но не более 1 секунды


                //дождались, выполняем остальное
                ConnectionLostManager.blockIfHasFailed();
                if (abandoned.get()) {
                    throw new RuntimeException("RmiQueue is abandoned");
                }
                if (flush) {
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
            if(busyDisplayer != null)
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

        if(busyRunning) {
            pendingBusyFlush = true;
            return;
        }

        if (abandoned.get()) {
            return;
        }

        //не обрабатываем результат, пока не закончится редактирование и не вызовется this.editingStopped()
        if (!tableManager.isEditing()) {
            flushCompletedRequestsNow(false);
        }
    }

    private void flushCompletedRequestsNow(boolean inSyncRequest) {
        assert !busyRunning;
        while (isRmiFutureDone()) {
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

    private boolean isRmiFutureDone() {
        return !rmiFutures.isEmpty() && rmiFutures.element().isDone();
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
