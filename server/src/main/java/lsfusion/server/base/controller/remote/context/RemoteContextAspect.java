package lsfusion.server.base.controller.remote.context;

import lsfusion.server.base.controller.remote.manager.RmiServer;
import lsfusion.server.base.controller.thread.EventThreadInfo;
import lsfusion.server.base.controller.thread.ThreadInfo;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RemoteContextAspect {
    
    public static final String allRemoteCalls = "execution(public * (lsfusion.interop.base.remote.RemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.findClass(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)";

    // за исключением системных вызовов, так как иначе они будут учавствовать в getLastThread, а значит в interrupt (и в итоге могут interrupt'ся они)
    public static final String allUserRemoteCalls = "execution(public * (lsfusion.interop.base.remote.RemoteInterface+ && *..*Interface).*(..))" +
            " && !execution(public * *.ping(..))" +
            " && !execution(public * *.findClass(..))" +
            " && !execution(public * *.toString())" +
            " && target(target)" + 
            " && !execution(public * lsfusion.interop.base.remote.PendingRemoteInterface.*(..))";

    @Around(allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        ThreadLocalContext.AspectState prevState;
        ThreadInfo threadInfo;
        ContextAwarePendingRemoteObject remoteObject = null;
        if(target instanceof ContextAwarePendingRemoteObject) {
            remoteObject = (ContextAwarePendingRemoteObject) target;
            threadInfo = EventThreadInfo.RMI((ContextAwarePendingRemoteObject) target);

            prevState = ThreadLocalContext.aspectBeforeRmi(remoteObject, false, threadInfo); // because there can be explicit remote call
        } else {
            RmiServer rmiServer = (RmiServer) target;
            threadInfo = EventThreadInfo.RMI(rmiServer);

            prevState = ThreadLocalContext.aspectBeforeRmi(rmiServer, false, threadInfo);
        }

        try {
            if(remoteObject == null || remoteObject.isLocal())
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
