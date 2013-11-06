package lsfusion.server.remote;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lsfusion.base.BaseUtils;
import lsfusion.server.ServerLoggers;

@Aspect
public class RemoteLoggerAspect {
    private final static Logger logger = ServerLoggers.remoteLogger;

    @Around("(execution(* lsfusion.interop.RemoteLogicsInterface.*(..))" +
            " || execution(* lsfusion.interop.form.RemoteFormInterface.*(..))" +
            " || execution(* lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)))" +
            " && !execution(* *.ping(..))" +
            "")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = thisJoinPoint.proceed();
        long runTime = System.currentTimeMillis() - startTime;

        if (logger.isDebugEnabled()) {
            logger.debug(
                    String.format(
                            "Executing remote method (time: %1$d ms.): %2$s(%3$s)",
                            runTime,
                            thisJoinPoint.getSignature().getName(),
                            BaseUtils.toString(", ", thisJoinPoint.getArgs())
                    )
            );
        }

        return result;
    }
}
