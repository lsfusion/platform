package platform.client.form;

import com.google.common.base.Throwables;
import platform.base.CallableCallback;
import platform.base.Callback;
import platform.base.ERunnable;
import platform.base.Provider;
import platform.client.SwingUtils;
import platform.interop.DaemonThreadFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class RmiQueue {
    private final Queue<RmiFuture> rmiFutures = new ArrayDeque<RmiFuture>();
    private final ExecutorService rmiExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory("-client-dispatch-"));

    private final TableManager tableManager;
    private final Provider<String> serverMessageProvider;

    public RmiQueue(TableManager tableManager, Provider<String> serverMessageProvider) {
        this.serverMessageProvider = serverMessageProvider;
        this.tableManager = tableManager;
    }

    public void syncVoidRequest(final ERunnable runnable) {
        syncRequest(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    public <T> void syncRequest(CallableCallback<T> request) throws Exception {
        syncRequest(request, request);
    }

    public <T> void syncRequest(Callable<T> request, Callback<T> callback) throws Exception {
        callback.done(syncRequest(request));
    }

    public <T> T syncRequest(Callable<T> request) {
        //todo: надо бы переделать эту логику, либо вообще убрать...
//        boolean screenBlock = false;
//        for (MethodInvocation invocation : invocations) {
//            screenBlock |= (blockedScreen != null) && (blockedScreen.containsKey(invocation.name) && invocation.args.length > 0 && invocation.args[0].toString().equals(blockedScreen.get(invocation.name)));
//        }

        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();

        try {
            while (!rmiFutures.isEmpty()) {
                rmiFutures.remove().execCallback();
            }

            return request.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            busyDisplayer.stop();
        }
    }

    public void asyncVoidRequest(final ERunnable runnable) {
        asyncRequest(new CallableCallback<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }

            @Override
            public void done(Void result) throws Exception {
            }
        });
    }

    public <T> void asyncRequest(CallableCallback<T> request) {
        SwingUtils.assertDispatchThread();

        RmiFuture<T> rmiFuture = new RmiFuture<T>(request);

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
                    rmiFutures.remove().execCallback();
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        }
    }

    public class RmiFuture<T> extends FutureTask<T> {
        private final CallableCallback<T> callback;

        public RmiFuture(CallableCallback<T> callback) {
            super(callback);
            this.callback = callback;
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
