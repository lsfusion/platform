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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class RmiQueue {
    private RmiFuture currentDispatchingFuture;

    private final Queue<RmiFuture> rmiFutures = new ArrayDeque<RmiFuture>();
    private final ExecutorService rmiExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory("-client-dispatch-"));

    private final TableManager tableManager;
    private final Provider<String> serverMessageProvider;

    private long nextRmiRequestIndex = 0;

    public RmiQueue(TableManager tableManager, Provider<String> serverMessageProvider) {
        this.serverMessageProvider = serverMessageProvider;
        this.tableManager = tableManager;
    }

    public <T> T syncRequest(final RmiRequest<T> request) {
        //todo: надо бы переделать эту логику, либо вообще убрать...
//        boolean screenBlock = false;
//        for (MethodInvocation invocation : invocations) {
//            screenBlock |= (blockedScreen != null) && (blockedScreen.containsKey(invocation.name) && invocation.args.length > 0 && invocation.args[0].toString().equals(blockedScreen.get(invocation.name)));
//        }

        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();

        try {
            final Result<T> result = new Result<T>();
            asyncRequest(request, new Callback<T>() {
                @Override
                public void done(T r) throws Exception {
                    result.set(r);
                    request.done(r);
                }
            });

            while (!rmiFutures.isEmpty()) {
                execNextFutureCallback();
            }

            return result.result;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            busyDisplayer.stop();
        }
    }

    public <T> void asyncRequest(RmiRequest<T> request) {
        asyncRequest(request, request);
    }

    public <T> void asyncRequest(RmiRequest<T> request, Callback<T> callback) {
        SwingUtils.assertDispatchThread();

        request.setRequestIndex(nextRmiRequestIndex++);

        RmiFuture<T> rmiFuture = new RmiFuture<T>(request, callback);
        request.preRequest();

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
