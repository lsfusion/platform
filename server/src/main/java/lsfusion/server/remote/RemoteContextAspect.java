package lsfusion.server.remote;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import lsfusion.server.context.ContextAwareThread;
import lsfusion.server.context.ThreadLocalContext;

@Aspect
public class RemoteContextAspect {
    private static final String executions =
            "(execution(* lsfusion.interop.RemoteLogicsInterface.*(..))" +
                    " || execution(* lsfusion.interop.form.RemoteFormInterface.*(..))" +
                    " || execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..))" +
                    " || execution(* lsfusion.interop.remote.PendingRemoteInterface.getRemoteActionMessage(..)))";

    private static final String remotePointCut =
            executions +
            " && !execution(* *.ping(..))" +
            " && !cflowbelow" + executions + "" +
            " && target(remoteObject)";

    @Before(remotePointCut)
    public void beforeCall(ContextAwarePendingRemoteObject remoteObject) {
        if (!(Thread.currentThread() instanceof ContextAwareThread)) {
            ThreadLocalContext.set(remoteObject.getContext());
        }
        remoteObject.addLinkedThread(Thread.currentThread());
    }

    @AfterReturning(remotePointCut)
    public void afterReturn(ContextAwarePendingRemoteObject remoteObject) {
        if (!(Thread.currentThread() instanceof ContextAwareThread)) {
            ThreadLocalContext.set(null);
        }
        remoteObject.removeLinkedThread(Thread.currentThread());
    }
}
