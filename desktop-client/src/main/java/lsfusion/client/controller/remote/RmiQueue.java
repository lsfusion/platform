package lsfusion.client.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.SystemUtils;
import lsfusion.base.lambda.ERunnable;
import lsfusion.base.lambda.InterruptibleProvider;
import lsfusion.base.lambda.Provider;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.TableManager;
import lsfusion.client.base.busy.BusyDialogDisplayer;
import lsfusion.client.base.busy.BusyDisplayer;
import lsfusion.client.base.exception.ClientExceptionManager;
import lsfusion.client.base.log.ClientLoggers;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.dispatch.DispatcherInterface;
import lsfusion.client.controller.dispatch.DispatcherListener;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.navigator.controller.AsyncFormController;
import lsfusion.interop.base.exception.FatalRemoteClientException;
import lsfusion.interop.base.exception.RemoteAbandonedException;
import lsfusion.interop.base.exception.RemoteHandledException;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RmiQueue implements DispatcherListener {
    private static final Logger logger = ClientLoggers.invocationLogger;
    private static final Logger remoteLogger = ClientLoggers.remoteLogger;

    private final static Object edtSyncBlocker = new Object();

    private final Queue<RmiFuture> rmiFutures = new ArrayDeque<>();
    private final ExecutorService rmiExecutor;

    private final TableManager tableManager;
    private final Provider<String> serverMessageProvider;
    private final InterruptibleProvider<List<Object>> serverMessageListProvider;
    private final AsyncListener asyncListener;
    private boolean asyncStarted = false;
    private int syncsDepth = 0;

    private long nextRmiRequestIndex = 0;
    private long lastReceivedRequestIndex = -1;
    
    private DispatcherInterface dispatcher;
    
    private boolean dispatchingInProgress;
    private boolean dispatchingPostponed;

    private AtomicBoolean abandoned = new AtomicBoolean();

    public RmiQueue(TableManager tableManager, Provider<String> serverMessageProvider, InterruptibleProvider<List<Object>> serverMessageListProvider, AsyncListener asyncListener) {
        this.serverMessageProvider = serverMessageProvider;
        this.serverMessageListProvider = serverMessageListProvider;
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

    public static void waitOnEdtSyncBlocker(boolean busyDialog) throws InterruptedException {
        waitOnEdtSyncBlocker(busyDialog ? MainController.busyDialogTimeout : 1000); // проблема в том что сейчас пара wait / notify не синхронизирована, поэтому на всякий случай вставим timeout
    }

    public static void waitOnEdtSyncBlocker(long timeout) throws InterruptedException {
        synchronized (edtSyncBlocker) {
            if(timeout > 0)
                edtSyncBlocker.wait(timeout);
        }
    }

    public <T> T runRetryableRequest(Callable<T> request) throws Exception {
        return runRetryableRequest(request, abandoned);
    }

    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned) throws Exception {
        return runRetryableRequest(request, abandoned, null, null);
    }

    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned, Pair<Integer, Integer> timeoutParams, RmiFutureInterface futureInterface) throws Exception {
        return runRetryableRequest(request, abandoned, false, timeoutParams, futureInterface);    
    }

    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned, boolean registeredFailure) throws Exception {
        return runRetryableRequest(request, abandoned, registeredFailure, null, null);
    }

    private static AtomicLong reqIdGen = new AtomicLong();

    private static ExecutorService executorService = Executors.newCachedThreadPool();
    
    private static double getTimeout(Pair<Integer, Integer> timeoutParams, int exponent) {
        return timeoutParams.second * Math.pow(timeoutParams.first, exponent);    
    }
    
    // вызывает request (предположительно remote) несколько раз, проблемы с целостностью предполагается что решается либо индексом, либо результат не так важен
    public static <T> T runRetryableRequest(Callable<T> request, AtomicBoolean abandoned, boolean registeredFailure, Pair<Integer, Integer> timeoutParams, RmiFutureInterface futureInterface) throws Exception {
        int reqCount = 0;
        int exponent = 0;
        long reqId = reqIdGen.incrementAndGet();
        try {
            do {
                if(abandoned.get()) // не вызываем call если уже клиент перестартовывает
                    throw new RemoteAbandonedException();
                try {
                    if (MainController.useRequestTimeout && timeoutParams != null && futureInterface != null) {
                        Future<T> future = executorService.submit(request);
                        while (true) {
                            double timeout = getTimeout(timeoutParams, exponent);
                            try {
                                return future.get((long) timeout, TimeUnit.SECONDS);
                            } catch (ExecutionException e) {
                                Throwable cause = e.getCause();
                                throw cause instanceof Exception ? (Exception) cause : Throwables.propagate(cause);
                            } catch (TimeoutException e) {
                                boolean isFirstFuture = futureInterface.isFirst();
                                remoteLogger.info("TimeoutException: timeout - " + timeout + "s, next timeout - " + getTimeout(timeoutParams, exponent) + "s" + (isFirstFuture ? ", first" : ""));
                                if (isFirstFuture) {
                                    future.cancel(true);
                                    exponent++;
                                    throw e;
                                }
                            }
                        }
                    } else {
                        if(futureInterface != null)
                            remoteLogger.info("Remote call without a timeout");
                        return request.call();
                    }
                } catch (Exception t) {
                    if(abandoned.get()) // suppress'им все, failedRmiRequest'ы flush'ся отдельно
                        throw new RemoteAbandonedException();

                    if (t instanceof RemoteException) {
                        RemoteException remote = (RemoteException) t;

                        int maxFatal = ExceptionUtils.getFatalRemoteExceptionCount(t);

                        if(logger.isDebugEnabled()) {
                            logger.debug("Failed rmi request, req count : " + reqCount + ", max fatal : ", t);
                        }
                        
                        if (reqCount > maxFatal) {
                            ConnectionLostManager.connectionLost();

                            t = new FatalRemoteClientException(ExceptionUtils.copyMessage(remote), reqId);
                            ExceptionUtils.copyStackTraces(remote, t);
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

                    if (t != null && !(t instanceof TimeoutException)) {
                        throw t;
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

    public void abandon() {
        abandoned.set(true);
    }

    // возможно получится и было бы лучше сделать так же, как в вебе - без механизма direct запросов, а с добавленеим этих запросов в начало очереди
    public <T> T directRequest(long requestIndex, final RmiRequest<T> request) throws RemoteException {
        request.setRequestIndex(requestIndex);
        request.setLastReceivedRequestIndex(lastReceivedRequestIndex);

        request.setTimeoutParams(new Pair<>(3, 20));

        if (logger.isDebugEnabled()) {
            logger.debug("Direct request: " + request);
        }

        return blockingRequest(request, true);
    }

    public <T> T syncRequest(final RmiRequest<T> request) {
        if (logger.isDebugEnabled()) {
            logger.debug("Sync request: " + request);
        }
        
        request.setTimeoutParams(new Pair<>(3, 20));

        return blockingRequest(request, false);
    }

    public static boolean busyRunning;
    public static List<Runnable> busyRunningRunnableList = new ArrayList<>();
    public boolean pendingBusyFlush;

    private <T> T blockingRequest(final RmiRequest<T> request, final boolean direct) {

        if(!MainController.busyDialog) {
            SwingUtils.assertDispatchThread();

            if (!direct && syncsDepth != 0) {
                IllegalStateException ex = new IllegalStateException("Nested sync request shouldn't occur.");
                logger.error("Nested sync request: ", ex);
                throw ex;
            }

            BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
            busyDisplayer.start();

            syncsDepth++;
            try {
                RmiFuture<T> rmiFuture;
                if (direct) {
                    rmiFuture = createRmiFuture(request);
                    rmiExecutor.execute(rmiFuture);
                } else {
                    rmiFuture = execRmiRequestInternal(request);
                }

                while ((direct && !rmiFuture.isDone()) || (!direct && !rmiFutures.isEmpty())) {
                    waitOnEdtSyncBlocker(false);

                    ConnectionLostManager.blockIfHasFailed();
                    if (abandoned.get()) {
                        throw new RemoteAbandonedException();
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
        } else {
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
                    rmiFuture = createRmiFuture(request);
                    rmiExecutor.execute(rmiFuture);
                } else {
                    rmiFuture = execRmiRequestInternal(request);
                }

                while ((direct && !rmiFuture.isDone()) || (!direct && !isRmiFutureExecuted(rmiFuture))) { // ждём до тех пор, пока наш запрос не выполнится и не уйдёт из очереди.
                    long timeout = MainController.busyDialogTimeout - (System.currentTimeMillis() - start);

                    boolean flush = !direct;

                    if (timeout <= 0) { //секунда прошла, а запрос ещё выполняется
                        if(busyDisplayer == null) {
                            busyDisplayer = new BusyDialogDisplayer(serverMessageListProvider);
                            busyDisplayer.start();
                        }

                        busyRunning = true;

                        busyDisplayer.show(new Runnable() {
                            @Override
                            public void run() {
                                while ((direct && !rmiFuture.isDone()) || (!direct && !isRmiFutureDone() && !isRmiFutureExecuted(rmiFuture))) {
                                    try {
                                        waitOnEdtSyncBlocker(true);
                                    } catch (InterruptedException e) {
                                        logger.error(e);
                                    }
                                }
                            }
                        });//показываем диалог

                        busyRunning = false; //чтобы не выполнять обработку в EDT busyDialog (на всякий случай)
                        for(Runnable r : busyRunningRunnableList) {
                            SwingUtilities.invokeLater(r);
                        }
                        busyRunningRunnableList = new ArrayList<>();
                        if(pendingBusyFlush)
                            flush = true;
                        pendingBusyFlush = false;
                    } else
                        waitOnEdtSyncBlocker(timeout); //blocker выполнения запроса, но не более 1 секунды


                    //дождались, выполняем остальное
                    ConnectionLostManager.blockIfHasFailed();
                    if (abandoned.get()) {
                        throw new RemoteAbandonedException();
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
    }

    public <T> long asyncRequest(final RmiRequest<T> request) {
        asyncRequest(request, false);
        return request.getRequestIndex();
    }

    public <T> void asyncRequest(final RmiRequest<T> request, boolean preProceed) {
        if (logger.isDebugEnabled()) {
            logger.debug("Async request: " + request);
        }
        request.setTimeoutParams(new Pair<>(3, 20));

        execRmiRequestInternal(request, preProceed);

        request.onAsyncRequest();

        if (!asyncStarted) {
            asyncStarted = true;
            asyncListener.onAsyncStarted();
        }
    }

    public static void runAction(Runnable r) {
        runAction(r, false);
    }

    // синхронные события не должны вызываться через invokeLater(), иначе может появиться пауза для срабатывания ещё какого-либо 
    // события и нарушится синхронность. EDT должен блокироваться без задержек
    public static void runAction(Runnable r, boolean invokeLater) {
        if (invokeLater)
            SwingUtilities.invokeLater(r);
        else
            r.run();
    }

    private <T> RmiFuture<T> execRmiRequestInternal(RmiRequest<T> request) {
        return execRmiRequestInternal(request, false);
    }

    private <T> RmiFuture<T> execRmiRequestInternal(RmiRequest<T> request, boolean preProceed) {
        SwingUtils.assertDispatchThread();

        request.setRequestIndex(nextRmiRequestIndex++);
        request.setLastReceivedRequestIndex(lastReceivedRequestIndex);

        if (logger.isDebugEnabled()) {
            logger.debug("Executing request's thread: " + request);
        }

        RmiFuture<T> rmiFuture = createRmiFuture(request, preProceed);

        if (rmiFutures.isEmpty() && !dispatchingInProgress) {
            rmiFuture.setFirst(true);
        }
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

        //не обрабатываем результат (кроме с preProceeded), пока не закончится редактирование и не вызовется this.editingStopped()
        if(tableManager.isEditing()) {
            rmiFutures.forEach(future -> {
                if(future.isDone() && future.preProceeded != null && !future.preProceeded) {
                    try {
                        dispatchingStarted();
                        try {
                            future.execCallback();
                        } finally {
                            dispatchingEnded();
                        }
                        future.preProceeded = true;
                    } catch (Throwable t) {
                        ClientExceptionManager.handle(t, false);
                    }
                }
            });
        } else {
            flushCompletedRequestsNow(false);
        }
    }

    private void flushCompletedRequestsNow(boolean inSyncRequest) {
        assert !busyRunning;
        while (isRmiFutureDone()) {
            try {
                execNextFutureCallback();
            } catch (Throwable t) {
                // in sync call we need exception to be thrown up
                // => async call exceptions we handling immediately, and the last one throw up
                if (rmiFutures.isEmpty() && inSyncRequest)
                    throw Throwables.propagate(t);
                else // usually t instanceof RemoteHandledException, see RmiFuture.execCallback
                    ClientExceptionManager.handle(t, false);
            }
        }
    }

    // ожидаем только завершения всех запросов до данного синхронного и самого этого синхронного.
    // иногда возникает ситуация, что после синхронного успевает проскочить ещё какой-либо асинхронный запрос через
    // EDT BusyDialog'а (к примеру gainedFocus). тогда возникает dead lock. поэтому не ждём полного очищения очереди 
    private boolean isRmiFutureExecuted(RmiFuture future) {
        assert !rmiFutures.contains(future) == future.executed;
        return future.executed;
    }

    private boolean isRmiFutureDone() {
        return !rmiFutures.isEmpty() && rmiFutures.element().isDone();
    }

    public void editingStopped() {
        flushCompletedRequests();
    }

    public boolean isSyncStarted() {
        return syncsDepth != 0;
    }

    private void execNextFutureCallback() throws Exception {
        RmiFuture future = rmiFutures.remove();
        
        dispatchingStarted();
        
        try {
            if(future.preProceeded == null || !future.preProceeded) {
                future.execCallback();
            }
        } finally {
            dispatchingEnded();
            
            if (rmiFutures.isEmpty() && asyncStarted) {
                asyncStarted = false;
                asyncListener.onAsyncFinished();
            }
        }
    }

    private <T> RmiFuture<T> createRmiFuture(final RmiRequest<T> request) {
        return createRmiFuture(request, false);
    }

    private <T> RmiFuture<T> createRmiFuture(final RmiRequest<T> request, boolean preProceed) {
        RequestCallable<T> requestCallable = new RequestCallable<>(request, abandoned);
        RmiFuture<T> future = new RmiFuture<>(request, requestCallable, preProceed);
        requestCallable.setFutureInterface(future);
        return future;
    }


    private void setNextFutureFirst() {
        if (!rmiFutures.isEmpty()) {
            rmiFutures.element().setFirst(true);
        }
    }

    public void dispatchingStarted() {
        dispatchingInProgress = true;
    }

    public void postponeDispatchingEnded() {
        dispatchingPostponed = true;
    }

    public void dispatchingPostponedEnded(DispatcherInterface realDispatcher) {
        dispatchingEnded(realDispatcher);
    }

    @Override
    public void dispatchingEnded() {
        dispatchingEnded(null);
    }

    public void dispatchingEnded(DispatcherInterface realDispatcher) {
        if (realDispatcher != null) {
            assert dispatchingPostponed;
            dispatchingPostponed = false;
        } else {
            realDispatcher = dispatcher;
        }

        if (!realDispatcher.isDispatchingPaused() && !dispatchingPostponed) {
            dispatchingInProgress = false;
            setNextFutureFirst();
        }
    }

    public void setDispatcher(DispatcherInterface dispatcher) {
        this.dispatcher = dispatcher;
    }

    public class RmiFuture<T> extends FutureTask<T> implements RmiFutureInterface {
        private final RmiRequest<T> request;
        boolean executed;
        private boolean first = false;
        private Boolean preProceeded;

        public RmiFuture(final RmiRequest<T> request, final RequestCallable<T> requestCallable, boolean preProceed) {
            super(requestCallable);
            this.request = request;
            this.preProceeded = preProceed ? false : null;
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

            boolean failed = false;
            T result = null;
            try {
                try {
                    result = get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    throw cause instanceof Exception ? (Exception) cause : Throwables.propagate(cause);
                }
            } catch (Exception e) {
                if(e instanceof RemoteHandledException)
                    throw e;

                request.onResponseGetFailed(e);
                failed = true;
            }
            if(!failed)
                request.onResponse(result);
            executed = true;
        }

        @Override
        public boolean isFirst() {
            return first;
        }
        
        public void setFirst(boolean first) {
            this.first = first;
        }
    }

    private static class RequestCallable<T> implements Callable<T> {
        private final RmiRequest<T> request;
        private RmiFutureInterface futureInterface; // actually not null final
        private final AtomicBoolean abandoned;

        public RequestCallable(RmiRequest<T> request, AtomicBoolean abandoned) {
            this.request = request;
            this.abandoned = abandoned;
        }

        private void setFutureInterface(RmiFutureInterface futureInterface) {
            this.futureInterface = futureInterface;
        }

        @Override
        public T call() throws Exception {
            return runRetryableRequest(() -> request.doRequest(), abandoned, request.getTimeoutParams(), futureInterface);
        }
    }

    private interface RmiFutureInterface {
        boolean isFirst();
    }

    private long lastCompletedRequest = -1L;
    private Map<Long, ClientFormDockable> asyncForms = new HashMap<>();

    public AsyncFormController getAsyncFormController(long requestIndex) {
        return new AsyncFormController() {
            @Override
            public ClientFormDockable removeAsyncForm() {
                return asyncForms.remove(requestIndex);
            }

            @Override
            public void putAsyncForm(ClientFormDockable container) {
                asyncForms.put(requestIndex, container);
            }

            @Override
            public boolean checkNotCompleted() {
                return requestIndex > lastCompletedRequest;
            }

            @Override
            public boolean onServerInvocationResponse() {
                lastCompletedRequest = requestIndex;
                return asyncForms.containsKey(requestIndex);
            }
        };
    }
}
