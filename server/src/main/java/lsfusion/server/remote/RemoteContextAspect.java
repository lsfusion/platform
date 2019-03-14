package lsfusion.server.remote;

import lsfusion.server.base.context.EventThreadInfo;
import lsfusion.server.base.context.ThreadInfo;
import lsfusion.server.base.context.ThreadLocalContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RemoteContextAspect {
    
    public static final String allRemoteCalls = "execution(public * (lsfusion.interop.PendingRemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)";

    // за исключением системных вызовов, так как иначе они будут учавствовать в getLastThread, а значит в interrupt (и в итоге могут interrupt'ся они)
    public static final String allUserRemoteCalls = "execution(public * (lsfusion.interop.PendingRemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)" + 
            " && !execution(public * lsfusion.interop.PendingRemoteInterface.*(..))";

    @Around(allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        ContextAwarePendingRemoteObject remoteObject = (ContextAwarePendingRemoteObject) target;

        ThreadInfo threadInfo = EventThreadInfo.RMI(remoteObject);
        ThreadLocalContext.AspectState prevState = ThreadLocalContext.aspectBeforeRmi(remoteObject, false, threadInfo); // because there can be explicit remote call (for example for Remote

        // because there can be explicit remote call (for example for Remote
        try {
            if(remoteObject.isLocal())
                return thisJoinPoint.proceed();

            remoteObject.addContextThread(Thread.currentThread());
            try {
                return thisJoinPoint.proceed();
            } finally {
                remoteObject.removeContextThread(Thread.currentThread());
            }
        } finally {
            ThreadLocalContext.aspectAfterRmi(prevState, false, threadInfo);
        }
    }
}
