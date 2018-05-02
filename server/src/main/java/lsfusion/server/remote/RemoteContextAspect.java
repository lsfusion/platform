package lsfusion.server.remote;

import lsfusion.server.context.Context;
import lsfusion.server.context.EventThreadInfo;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.context.ThreadInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RemoteContextAspect {
    @Around("(execution(public * (lsfusion.interop.RemoteLogicsInterface+ && *..*Interface).*(..)) ||" +
            "execution(public * lsfusion.interop.form.RemoteFormInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.remote.PendingRemoteInterface.getRemoteActionMessage(..)) ||" +
            "execution(public * lsfusion.interop.remote.PendingRemoteInterface.interrupt(..)) ||" +
            "execution(public * lsfusion.interop.remote.PendingRemoteInterface.getRemoteActionMessageList(..)))" +
            " && !execution(public * lsfusion.interop.RemoteLogicsInterface.ping(..))" +
            " && target(ro)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object ro) throws Throwable {
        ContextAwarePendingRemoteObject remoteObject = (ContextAwarePendingRemoteObject) ro;

        ThreadInfo threadInfo = EventThreadInfo.RMI(remoteObject);
        Context prevContext = ThreadLocalContext.aspectBeforeRmi(remoteObject, false, threadInfo); // так как может быть explicit remote call

        try {
            remoteObject.addLinkedThread(Thread.currentThread());
            try {
                return thisJoinPoint.proceed();
            } finally {
                remoteObject.removeLinkedThread(Thread.currentThread());
            }
        } finally {
            ThreadLocalContext.aspectAfterRmi(prevContext, false, threadInfo);
        }
    }
}
