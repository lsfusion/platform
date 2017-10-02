package lsfusion.server.remote;

import lsfusion.server.context.Context;
import lsfusion.server.context.ThreadLocalContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RmiServerAspect {


    @Around("execution(* (lsfusion.interop.remote.RmiServerInterface+ && *..*Interface).*(..)) && target(server)")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object server) throws Throwable {
        RmiServer rmiServer = (RmiServer) server;

        Context prevContext = ThreadLocalContext.aspectBeforeRmi(rmiServer, false); // так как может быть explicit вызов - сервер вызывает другой сервер и т.п.
        try {
            return thisJoinPoint.proceed();
        } finally {
            ThreadLocalContext.aspectAfterRmi(prevContext, false);
        }
    }
}
