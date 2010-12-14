package platform.server.remote;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.logging.Logger;

@Aspect
public class RemoteObjectLoggerAspect {
    private final static Logger logger = Logger.getLogger(RemoteObjectLoggerAspect.class.getName());

    @Around("(execution(* platform.interop.RemoteLogicsInterface.*(..))" +
            " || execution(* platform.interop.form.RemoteFormInterface.*(..))" +
            " || execution(* platform.interop.form.RemoteDialogInterface.*(..))" +
            " || execution(* platform.interop.navigator.RemoteNavigatorInterface.*(..)))" +
            " && !execution(* *.ping(..))")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = thisJoinPoint.proceed();
        long runTime = System.currentTimeMillis() - startTime;

        logger.info(String.format("Executing remote method (time: %1$d ms.): %2$s", runTime, thisJoinPoint.getSignature().getName()));

        return result;
    }
}
