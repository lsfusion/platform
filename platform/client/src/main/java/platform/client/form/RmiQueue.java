package platform.client.form;

import com.google.common.base.Throwables;
import platform.base.Callback;
import platform.base.ERunnable;
import platform.base.Provider;
import platform.base.Result;
import platform.client.SwingUtils;
import platform.interop.DaemonThreadFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
        long startTime = System.currentTimeMillis();

        if (timeOut >= FOREVER) {
            syncRequest(request);
            return true;
        }

        execRmiRequestInternal(request, request);

        boolean timedOut = true;
        //если timeout <=0 то даже не ждём, сразу выходим, т.к. чисто асинхронный вызов
        if (timeOut > 0) {
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
        //todo: надо бы переделать эту логику, либо вообще убрать...
//        boolean screenBlock = false;
//        for (MethodInvocation invocation : invocations) {
//            screenBlock |= (blockedScreen != null) && (blockedScreen.containsKey(invocation.name) && invocation.args.length > 0 && invocation.args[0].toString().equals(blockedScreen.get(invocation.name)));
//        }

//        System.out.println("----Sync request # " + nextRmiRequestIndex);
        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();

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
        } finally {
            busyDisplayer.stop();
        }
    }

    private void forceProcessAllRequests() {
        try {
            while (!rmiFutures.isEmpty()) {
                execNextFutureCallback();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
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

        // если чегонить редактируем, то не обрабатываем результат пока не закончим
        if (tableManager.isEditing()) {
            final JTable currentTable = tableManager.getCurrentTable();
            currentTable.getModel().addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    flushCompletedRequests();
                    currentTable.getModel().removeTableModelListener(this);
                }
            });
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

            callback.done(get());
        }
    }
}
