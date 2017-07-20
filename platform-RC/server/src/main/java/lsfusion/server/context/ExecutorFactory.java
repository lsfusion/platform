package lsfusion.server.context;

import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.lifecycle.MonitorServer;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

// есть в принципе 2 подхода :
// wrapService, когда привязываемся к task'е
// ThreadFactory - когда привязываемся к потоку (этот подход чисто оптимизационный)
public class ExecutorFactory {

    // ГЛОБАЛЬНЫЕ СЕРВИСЫ (МОНИТОРИНГ)
    private static final boolean useThreadFactoryForContext = true;

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
                    ThreadLocalContext.aspectBeforeMonitor(monitorServer);
                }

                @Override
                public void aspectAfterRun() {
                    ThreadLocalContext.aspectAfterMonitor();
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
                    ThreadLocalContext.aspectBeforeMonitor(monitorServer);
                }

                @Override
                public void aspectAfterRun() {
                    ThreadLocalContext.aspectAfterMonitor();
                }
            });
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
                ThreadLocalContext.aspectBeforeRmi(object, true, type);
            }

            @Override
            public void aspectAfterRun() {
                ThreadLocalContext.aspectAfterRmi();
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
                ThreadLocalContext.aspectBeforeMonitor(monitor, type);
            }

            @Override
            public void aspectAfterRun() {
                ThreadLocalContext.aspectAfterMonitor();
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
                ThreadLocalContext.aspectBeforeLifecycle(logicsInstance, type);
            }

            @Override
            public void aspectAfterRun() {
                ThreadLocalContext.aspectAfterMonitor();
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
                ThreadLocalContext.aspectBeforeLifecycle(logicsInstance, type);
            }

            @Override
            public void aspectAfterRun() {
                ThreadLocalContext.aspectAfterLifecycle();
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

    public static ScheduledExecutorService createNewThreadService(ExecutionContext<PropertyInterface> context, Integer nThreads) {
        SyncType type = SyncType.NOSYNC;

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
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                aspect.aspectBeforeRun();
                try {
                    return callable.call();
                } finally {
                    aspect.aspectAfterRun();
                }
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
                        ThreadLocalContext.aspectBeforeMonitor(monitor);
                    super.run();
                } finally {
                    if (monitor != null)
                        ThreadLocalContext.aspectAfterMonitor();
                }
            }
        }
    }

    // ЛОКАЛЬНЫЕ СЕРВИСЫ

    private interface TaskInnerAspect<S> {
        S aspectSubmit(); // верхний поток \ стек

        void aspectBeforeRun(S submit); // внутренний поток \ стек
        void aspectAfterRun();
    }

    private static <T> Callable<T> wrapInnerTask(final Callable<T> callable, final TaskInnerAspect aspect, SyncType type) {
        final Object submit = aspect.aspectSubmit();

        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                aspect.aspectBeforeRun(submit);
                try {
                    return callable.call();
                } finally {
                    aspect.aspectAfterRun();
                }
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
                aspect.aspectAfterRun();
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
        return new TaskInnerAspect<Context>() {
            @Override
            public Context aspectSubmit() {
                return ThreadLocalContext.assureContext(context);
            }

            @Override
            public void aspectBeforeRun(Context submit) {
                ThreadLocalContext.aspectBeforeContext(submit, context, type);
            }

            @Override
            public void aspectAfterRun() {
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
                if(logicsInstance != null)
                    logicsInstance.getDbManager().closeThreadLocalSql();
            }
        }
    }

}
