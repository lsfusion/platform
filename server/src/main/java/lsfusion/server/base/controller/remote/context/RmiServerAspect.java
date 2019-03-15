package lsfusion.server.base.controller.remote.context;

import lsfusion.server.base.controller.thread.EventThreadInfo;
import lsfusion.server.base.controller.thread.ThreadInfo;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.remote.RmiServer;
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
