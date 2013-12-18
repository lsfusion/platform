package lsfusion.server.remote;

import lsfusion.server.context.ContextAwareThread;
import lsfusion.server.context.ThreadLocalContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RemoteContextAspect {
    @Around("(execution(public * lsfusion.interop.RemoteLogicsInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.form.RemoteFormInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.remote.PendingRemoteInterface.getRemoteActionMessage(..)))" +
            " && !execution(public * lsfusion.interop.RemoteLogicsInterface.ping(..))" +
            " && target(ro)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object ro) throws Throwable {
        ContextAwarePendingRemoteObject remoteObject = (ContextAwarePendingRemoteObject) ro;
        if (!(Thread.currentThread() instanceof ContextAwareThread)) {
            ThreadLocalContext.set(remoteObject.getContext());
        }
        remoteObject.addLinkedThread(Thread.currentThread());

        try {
            return thisJoinPoint.proceed();
        } finally {
            if (!(Thread.currentThread() instanceof ContextAwareThread)) {
                ThreadLocalContext.set(null);
            }
            remoteObject.removeLinkedThread(Thread.currentThread());
        }
    }
}
