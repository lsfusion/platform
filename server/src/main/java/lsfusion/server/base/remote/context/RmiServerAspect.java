package lsfusion.server.base.remote.context;

import lsfusion.server.base.context.EventThreadInfo;
import lsfusion.server.base.context.ThreadInfo;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.base.remote.RmiServer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RmiServerAspect {


    @Around("execution(* (lsfusion.interop.server.RmiServerInterface+ && *..*Interface).*(..)) && target(server)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object server) throws Throwable {
        RmiServer rmiServer = (RmiServer) server;

        ThreadInfo threadInfo = EventThreadInfo.RMI(rmiServer);
        ThreadLocalContext.AspectState prevState = ThreadLocalContext.aspectBeforeRmi(rmiServer, false, threadInfo); // так как может быть explicit вызов - сервер вызывает другой сервер и т.п.
        try {
            return thisJoinPoint.proceed();
        } finally {
            ThreadLocalContext.aspectAfterRmi(prevState, false, threadInfo);
        }
    }
}
