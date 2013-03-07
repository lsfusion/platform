package platform.server.remote;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.server.context.ContextAwareThread;
import platform.server.context.ThreadLocalContext;

@Aspect
public class RemoteContextAspect {
    private static final String executions =
            "(execution(* platform.interop.RemoteLogicsInterface.*(..))" +
                    " || execution(* platform.interop.form.RemoteFormInterface.*(..))" +
                    " || execution(* platform.interop.form.RemoteDialogInterface.*(..))" +
                    " || execution(* platform.interop.navigator.RemoteNavigatorInterface.*(..)))";

    private static final String remotePointCut =
            executions +
            " && !execution(* *.ping(..))" +
            " && !cflowbelow" + executions + "" +
            " && target(remoteObject)";

    @Before(remotePointCut)
    public void beforeCall(ContextAwarePendingRemoteObject remoteObject) {
        if (!(Thread.currentThread() instanceof ContextAwareThread)) {
            ThreadLocalContext.set(remoteObject.getContext()); // вообще должен быть null, но в силу thread pooling'а не всегда
            remoteObject.threads.add(Thread.currentThread());
        }
    }

    @AfterReturning(remotePointCut)
    public void afterReturn(ContextAwarePendingRemoteObject remoteObject) {
        remoteObject.threads.remove(Thread.currentThread());
    }
}
