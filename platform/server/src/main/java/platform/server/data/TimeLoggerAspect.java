package platform.server.data;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static platform.server.ServerLoggers.sqlLogger;

@Aspect
public class TimeLoggerAspect {

    @Around("execution(@platform.server.data.LogTime * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        boolean loggingEnabled = sqlLogger.isDebugEnabled();

        long startTime = 0;
        if (loggingEnabled)
            startTime = System.currentTimeMillis();

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            long runTime = System.currentTimeMillis() - startTime;
            sqlLogger.debug(String.format("Executed method (time: %1$d ms.): %2$s", runTime, thisJoinPoint.getSignature().toString()));
        }

        return result;
    }
}
