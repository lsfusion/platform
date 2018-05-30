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
    
    public static final String allRemoteCalls = "execution(public * (lsfusion.interop.remote.PendingRemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)";

    // за исключением системных вызовов, так как иначе они будут учавствовать в getLastThread, а значит в interrupt (и в итоге могут interrupt'ся они)
    public static final String allUserRemoteCalls = "execution(public * (lsfusion.interop.remote.PendingRemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)" + 
            " && !execution(public * lsfusion.interop.remote.PendingRemoteInterface.*(..))";

    @Around(allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        ContextAwarePendingRemoteObject remoteObject = (ContextAwarePendingRemoteObject) target;

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
