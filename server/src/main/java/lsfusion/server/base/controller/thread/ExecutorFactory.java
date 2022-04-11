package lsfusion.server.base.controller.thread;

import lsfusion.base.DaemonThreadFactory;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.manager.MonitorServer;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.manager.RmiServer;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

// есть в принципе 2 подхода :
// wrapService, когда привязываемся к task'е
// ThreadFactory - когда привязываемся к потоку (этот подход чисто оптимизационный)
public class ExecutorFactory {

    // ГЛОБАЛЬНЫЕ СЕРВИСЫ (МОНИТОРИНГ)
    private static final boolean useThreadFactoryForContext = false; // вообще run вызывается при запуске потока, а не выполнении submit'ого задания 

    public static ExecutorService createMonitorThreadService(Integer threads, final MonitorServer monitorServer) {

        ThreadFactory threadFactory = createMonitorThreadFactory(monitorServer);

        ExecutorService executorService;
        if(threads == null)
            executorService = Executors.newCachedThreadPool(threadFactory);
        else if(threads > 1)
            executorService = Executors.newFixedThreadPool(threads, threadFactory);
        else
            executorService = Executors.newSingleThreadExecutor(threadFactory);

        if(useThreadFactoryForContext)
            return executorService;
        else
            return wrapService(executorService, new TaskAspect() {
                @Override
                public void aspectBeforeRun() {
                    ThreadLocalContext.aspectBeforeMonitor(monitorServer, ExecutorFactoryThreadInfo.instance);
                }

                @Override
                public void aspectAfterRun() {
                    ThreadLocalContext.aspectAfterMonitor(ExecutorFactoryThreadInfo.instance);
                }
            });
//
    }

    private static ThreadFactory createMonitorThreadFactory(MonitorServer monitorServer) {
        ThreadFactory threadFactory;
        if(useThreadFactoryForContext)
            threadFactory = new GlobalDaemonThreadFactory(monitorServer);
        else
            threadFactory = new ClosableDaemonThreadFactory(monitorServer.getLogicsInstance(), monitorServer.getEventName());
        return threadFactory;
    }

    // here we'll manage context manually (since we don't create this scheduled threadService)
    // actually we don't want to have limited pool of processes, but there no such ScheduledExecutorServices
    // so we'll use available process for approximate scaling (the other option is to use Timer/runLater but this will lead to a threads bloating)
    public static ScheduledExecutorService createCloseScheduledThreadService() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    public static ScheduledExecutorService createMonitorScheduledThreadService(Integer threads, final MonitorServer monitorServer) {

        ThreadFactory threadFactory = createMonitorThreadFactory(monitorServer);

        ScheduledExecutorService executorService;
        if(threads == null)
            executorService = Executors.newScheduledThreadPool(0, threadFactory);
        else if(threads > 1)
            executorService = Executors.newScheduledThreadPool(threads, threadFactory);
        else
            executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);

        if(useThreadFactoryForContext)
            return executorService;
        else
            return wrapService(executorService, new TaskAspect() {
                @Override
                public void aspectBeforeRun() {
                    ThreadLocalContext.aspectBeforeMonitor(monitorServer, ExecutorFactoryThreadInfo.instance);
                }

                @Override
                public void aspectAfterRun() {
                    ThreadLocalContext.aspectAfterMonitor(ExecutorFactoryThreadInfo.instance);
                }
            });
    }

    public static ExecutorService createRMIThreadService(Integer threads, final RmiServer rmiServer) {

        ThreadFactory threadFactory = createRMIThreadFactory(rmiServer);

        ExecutorService executorService;
        if(threads == null)
            executorService = Executors.newCachedThreadPool(threadFactory);
        else if(threads > 1)
            executorService = Executors.newFixedThreadPool(threads, threadFactory);
        else
            executorService = Executors.newSingleThreadExecutor(threadFactory);

        if(useThreadFactoryForContext)
            return executorService;
        else
            return wrapService(executorService, new TaskAspect() {
                @Override
                public void aspectBeforeRun() {
                    ThreadLocalContext.aspectBeforeRmi(rmiServer, false, ExecutorFactoryThreadInfo.instance);
                }

                @Override
                public void aspectAfterRun() {
                    ThreadLocalContext.aspectAfterRmi(ExecutorFactoryThreadInfo.instance);
                }
            });
//
    }

    private static ThreadFactory createRMIThreadFactory(RmiServer rmiServer) {
        ThreadFactory threadFactory;
        if(useThreadFactoryForContext)
            threadFactory = new GlobalDaemonThreadRmiServerFactory(rmiServer);
        else
            threadFactory = new ClosableDaemonThreadFactory(rmiServer.getLogicsInstance(), rmiServer.getEventName());
        return threadFactory;
    }

    // ЛОКАЛЬНЫЕ СЕРВИСЫ (когда есть верхний контекст \ стек)

    // создает синхронизированный клон потока, чтобы его можно было остановить и вернуть rmi ответ
    public static ExecutorService createRmiMirrorSyncService(final ContextAwarePendingRemoteObject object) {
        final SyncType type = SyncType.SYNC;

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(object.getContext().getLogicsInstance(), object.getSID() + "-pausable-daemon");

        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

        return wrapInnerService(executorService, type, new TaskInnerAspect() {
            @Override
            public Object aspectSubmit() {
                ThreadLocalContext.assureRmi(object);
                return null;
            }

            @Override
            public void aspectBeforeRun(Object submit) {
                ThreadLocalContext.aspectBeforeRmi(object, true, ExecutorFactoryThreadInfo.instance, type);
            }

            @Override
            public void aspectAfterRun(Object submit) {
                ThreadLocalContext.aspectAfterRmi(ExecutorFactoryThreadInfo.instance);
            }
        });
    }

    public static ExecutorService createMonitorMirrorSyncService(final MonitorServer monitor) {
        final SyncType type = SyncType.SYNC;

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(monitor.getLogicsInstance(), monitor.getEventName());

        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

        return wrapInnerService(executorService, type, new TaskInnerAspect() {
            @Override
            public Object aspectSubmit() {
                ThreadLocalContext.assureMonitor(monitor);
                return null;
            }

            @Override
            public void aspectBeforeRun(Object submit) {
                ThreadLocalContext.aspectBeforeMonitor(monitor, ExecutorFactoryThreadInfo.instance, type);
            }

            @Override
            public void aspectAfterRun(Object submit) {
                ThreadLocalContext.aspectAfterMonitor(ExecutorFactoryThreadInfo.instance);
            }
        });
    }

    public static ExecutorService createLifecycleMirrorSyncService() {
        final SyncType type = SyncType.SYNC;

        final LogicsInstance logicsInstance = ThreadLocalContext.getLogicsInstance();

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(logicsInstance, "lc-mirror");

        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

        return wrapInnerService(executorService, type, new TaskInnerAspect() {
            @Override
            public Object aspectSubmit() {
                ThreadLocalContext.assureLifecycle(logicsInstance);
                return null;
            }

            @Override
            public void aspectBeforeRun(Object submit) {
                ThreadLocalContext.aspectBeforeLifecycle(logicsInstance, ExecutorFactoryThreadInfo.instance, type);
            }

            @Override
            public void aspectAfterRun(Object submit) {
                ThreadLocalContext.aspectAfterMonitor(ExecutorFactoryThreadInfo.instance);
            }
        });
    }

    public static ExecutorService createTaskMirrorSyncService(ExecutionContext<PropertyInterface> context) {
        if(context == null)
            return createLifecycleMirrorSyncService();

        final SyncType type = SyncType.SYNC;

        final LogicsInstance logicsInstance = context.getLogicsInstance();

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(logicsInstance, "taskRunner-mirror");

        ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

        return wrapInnerService(executorService, type, createContextAspect(context, type));
    }

    public static ExecutorService createLifecycleMirrorMultiSyncService(int nThreads, BlockingQueue<Runnable> taskQueue) {
        final SyncType type = SyncType.MULTISYNC;

        final LogicsInstance logicsInstance = ThreadLocalContext.getLogicsInstance();

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(logicsInstance,"taskRunner-pool");

        ThreadPoolExecutor executorService = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                taskQueue,
                threadFactory);

        return wrapInnerAspectService(executorService, type, new TaskInnerAspect() {
            @Override
            public Object aspectSubmit() {
                ThreadLocalContext.assureLifecycle(logicsInstance);
                return null;
            }

            @Override
            public void aspectBeforeRun(Object submit) {
                ThreadLocalContext.aspectBeforeLifecycle(logicsInstance, ExecutorFactoryThreadInfo.instance, type);
            }

            @Override
            public void aspectAfterRun(Object submit) {
                ThreadLocalContext.aspectAfterLifecycle(ExecutorFactoryThreadInfo.instance);
            }
        });
    }

    // создает "несинхронизированный" клон потока, чтобы можно было продолжить выполнение
    // не забывать shutdown'ить
    public static ScheduledExecutorService createNewThreadService(ExecutionContext<PropertyInterface> context) {
        SyncType type = SyncType.NOSYNC;

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(context.getLogicsInstance(), "newthread-pool");

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory); // тут возможно имеет смысл кэшировать

        return wrapInnerService(executorService, type, createContextAspect(context, type));
    }

    public static ScheduledExecutorService createNewThreadService(ExecutionContext<PropertyInterface> context, Integer nThreads, boolean sync) {
        SyncType type = sync ? SyncType.MULTISYNC : SyncType.NOSYNC;

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(context.getLogicsInstance(), "executor-pool");

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(nThreads, threadFactory); // тут возможно имеет смысл кэшировать

        return wrapInnerService(executorService, type, createContextAspect(context, type));
    }

    // создает граф потоков синхронизированным с внешним контекстом, но внутри своя логика синхронизации
    // assert что submit'ся только AspectRunnable
    public static ExecutorService createTaskService(int nThreads, BlockingQueue<Runnable> taskQueue, ExecutionContext<PropertyInterface> context) {
        if(context == null)
            return createLifecycleMirrorMultiSyncService(nThreads, taskQueue);

        SyncType type = SyncType.MULTISYNC;

        final LogicsInstance logicsInstance = context.getLogicsInstance();

        ClosableDaemonThreadFactory threadFactory = new ClosableDaemonThreadFactory(logicsInstance,"taskRunner-pool");

        ThreadPoolExecutor executorService = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                taskQueue,
                threadFactory);

        return wrapInnerAspectService(executorService, type, createContextAspect(context, type));
    }



    // WRAPPER'ы

    private interface TaskAspect {
        void aspectBeforeRun();
        void aspectAfterRun();
    }

    private static <T> Callable<T> wrapTask(final Callable<T> callable, final TaskAspect aspect) {
        return () -> {
            aspect.aspectBeforeRun();
            try {
                return callable.call();
            } finally {
                aspect.aspectAfterRun();
            }
        };
    }

    private static ExecutorService wrapService(ExecutorService service, final TaskAspect aspect) {
        return new WrappingExecutorService(service) {
            @Override
            protected <T> Callable<T> wrapTask(final Callable<T> callable) {
                return ExecutorFactory.wrapTask(callable, aspect);
            }
        };
    }

    private static ScheduledExecutorService wrapService(ScheduledExecutorService service, final TaskAspect aspect) {
        return new WrappingScheduledExecutorService(service) {
            @Override
            protected <T> Callable<T> wrapTask(final Callable<T> callable) {
                return ExecutorFactory.wrapTask(callable, aspect);
            }
        };
    }

    // оптимизация своего рода, вместо того, чтобы делать wrapTask, проставим сразу контекст потоку
    public static class GlobalDaemonThreadFactory extends ClosableDaemonThreadFactory {

        protected final WeakReference<MonitorServer> wMonitor;

        public GlobalDaemonThreadFactory(MonitorServer monitor) {
            super(monitor.getLogicsInstance(), monitor.getEventName());

            wMonitor = new WeakReference<>(monitor);
        }

        protected Thread newThreadInstance(ThreadGroup group, Runnable r, String name, int stackSize) {
            return new ContextAwareThread(wLogicsInstance, wMonitor, group, r, name, stackSize);
        }

        public static class ContextAwareThread extends ClosableThread {

            protected final WeakReference<MonitorServer> wMonitor;

            private final String name;
            public ContextAwareThread(WeakReference<LogicsInstance> wLogicsInstance, WeakReference<MonitorServer> wMonitor, ThreadGroup group, Runnable target, String name, long stackSize) {
                super(wLogicsInstance, group, target, name, stackSize);
                this.name = name;
                this.wMonitor = wMonitor;
            }

            @Override
            public void run() {
                MonitorServer monitor = wMonitor.get();
                try {
                    if (monitor != null)
                        ThreadLocalContext.aspectBeforeMonitor(monitor, ExecutorFactoryThreadInfo.instance);
                    super.run();
                } finally {
                    if (monitor != null)
                        ThreadLocalContext.aspectAfterMonitor(ExecutorFactoryThreadInfo.instance);
                }
            }
        }
    }

    public static class GlobalDaemonThreadRmiServerFactory extends ClosableDaemonThreadFactory {

        protected final WeakReference<RmiServer> wRmi;

        public GlobalDaemonThreadRmiServerFactory(RmiServer rmi) {
            super(rmi.getLogicsInstance(), rmi.getEventName());

            wRmi = new WeakReference<>(rmi);
        }

        protected Thread newThreadInstance(ThreadGroup group, Runnable r, String name, int stackSize) {
            return new ContextAwareThread(wLogicsInstance, wRmi, group, r, name, stackSize);
        }

        public static class ContextAwareThread extends ClosableThread {

            protected final WeakReference<RmiServer> wRmi;

            private final String name;
            public ContextAwareThread(WeakReference<LogicsInstance> wLogicsInstance, WeakReference<RmiServer> wRmi, ThreadGroup group, Runnable target, String name, long stackSize) {
                super(wLogicsInstance, group, target, name, stackSize);
                this.name = name;
                this.wRmi = wRmi;
            }

            @Override
            public void run() {
                RmiServer rmi = wRmi.get();
                try {
                    if (rmi != null)
                        ThreadLocalContext.aspectBeforeRmi(rmi, false, ExecutorFactoryThreadInfo.instance);
                    super.run();
                } finally {
                    if (rmi != null)
                        ThreadLocalContext.aspectAfterRmi(ExecutorFactoryThreadInfo.instance);
                }
            }
        }
    }

    // ЛОКАЛЬНЫЕ СЕРВИСЫ

    private interface TaskInnerAspect<S> {
        S aspectSubmit(); // thread that creats new thread

        void aspectBeforeRun(S submit); // new thread start
        void aspectAfterRun(S submit); // new thread stop
    }

    private static <T> Callable<T> wrapInnerTask(final Callable<T> callable, final TaskInnerAspect aspect, SyncType type) {
        final Object submit = aspect.aspectSubmit();

        return () -> {
            aspect.aspectBeforeRun(submit);
            try {
                return callable.call();
            } finally {
                aspect.aspectAfterRun(submit);
            }
        };
    }


    private static ExecutorService wrapInnerService(ExecutorService service, final SyncType type, final TaskInnerAspect aspect) {
        return new WrappingExecutorService(service) {
            @Override
            protected <T> Callable<T> wrapTask(final Callable<T> callable) {
                return ExecutorFactory.wrapInnerTask(callable, aspect, type);
            }
        };
    }

    public static abstract class AspectRunnable implements Runnable {
        private TaskInnerAspect aspect;
        private Object submit;

        public void setAspect(TaskInnerAspect aspect) {
            assert this.aspect == null;
            this.aspect = aspect;
            submit = aspect.aspectSubmit();
        }

        protected abstract void aspectRun();

        @Override
        public void run() {
            aspect.aspectBeforeRun(submit);
            try {
                aspectRun();
            } finally {
                aspect.aspectAfterRun(submit);
            }
        }
    }

    // нужно чтобы не изменять ссылку, так как используется TaskBlockingQueue
    private static ExecutorService wrapInnerAspectService(ExecutorService service, final SyncType type, final TaskInnerAspect aspect) {
        return new WrappingExecutorService(service) {
            protected <T> Callable<T> wrapTask(final Callable<T> callable) {
                throw new RuntimeException();
            }

            protected Runnable wrapTask(Runnable command) {
                ((AspectRunnable)command).setAspect(aspect);
                return command;
            }
        };
    }

    private static ScheduledExecutorService wrapInnerService(ScheduledExecutorService service, final SyncType type, final TaskInnerAspect aspect) {
        return new WrappingScheduledExecutorService(service) {
            @Override
            protected <T> Callable<T> wrapTask(final Callable<T> callable) {
                return ExecutorFactory.wrapInnerTask(callable, aspect, type);
            }
        };
    }

    private static TaskInnerAspect createContextAspect(final ExecutionContext<PropertyInterface> context, final SyncType type) {
        return new TaskInnerAspect<Pair<Context, AbstractContext.MessageLogger>>() {
            @Override
            public Pair<Context, AbstractContext.MessageLogger> aspectSubmit() {
                return new Pair<>(ThreadLocalContext.assureContext(context), type != SyncType.NOSYNC ? ThreadLocalContext.get().getLogMessage() : null);
            }

            @Override
            public void aspectBeforeRun(Pair<Context, AbstractContext.MessageLogger> submit) {
                ThreadLocalContext.aspectBeforeContext(submit.first, context, type);
                if(submit.second != null)
                    ThreadLocalContext.pushLogMessage();
            }

            @Override
            public void aspectAfterRun(Pair<Context, AbstractContext.MessageLogger> submit) {
                if(submit.second != null) {
                    ImList<AbstractContext.LogMessage> logMessages = ThreadLocalContext.popLogMessage();
                    synchronized (submit.second) { // such synchronization is not very clean solution, since if the main thread will modify MessageLogger it will be not thread-safe, however for example delayUserInteraction is also not synchronized, so we'll assume that main thread should not do it
                        submit.second.addAll(logMessages);
                    }
                }
                ThreadLocalContext.aspectAfterContext();
            }
        };
    }

    public static class ClosableDaemonThreadFactory extends DaemonThreadFactory {
        protected final WeakReference<LogicsInstance> wLogicsInstance;

        public ClosableDaemonThreadFactory(LogicsInstance instance, String threadNamePrefix) {
            super(threadNamePrefix);
            wLogicsInstance = new WeakReference<>(instance);
        }

        protected Thread newThreadInstance(ThreadGroup group, Runnable r, String name, int stackSize) {
            return new ClosableThread(wLogicsInstance, group, r, name, stackSize);
        }

        public static class ClosableThread extends Thread {
            protected final WeakReference<LogicsInstance> wLogicsInstance;

            public ClosableThread(WeakReference<LogicsInstance> wLogicsInstance, ThreadGroup group, Runnable target, String name, long stackSize) {
                super(group, target, name, stackSize);
                this.wLogicsInstance = wLogicsInstance;
            }

            @Override
            public void run() {
                super.run();
                
                LogicsInstance logicsInstance = wLogicsInstance.get();
                if(logicsInstance != null) {
                    ThreadLocalContext.aspectBeforeLifecycle(logicsInstance, ExecutorFactoryThreadInfo.instance);
                    try {
                        logicsInstance.getDbManager().closeThreadLocalSql();
                    } finally {
                        ThreadLocalContext.aspectAfterLifecycle(ExecutorFactoryThreadInfo.instance);
                    }
                }
            }
        }
    }

}
