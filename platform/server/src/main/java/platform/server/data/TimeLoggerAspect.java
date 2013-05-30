package platform.server.data;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static platform.server.ServerLoggers.sqlLogger;

@Aspect
public class TimeLoggerAspect {

    private static long runningTotal = 0;

    @Around("execution(@platform.server.data.LogTime * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        boolean loggingEnabled = sqlLogger.isDebugEnabled();

        long startTime = 0;
        if (loggingEnabled)
            startTime = System.nanoTime();

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            long runTime = System.nanoTime() - startTime;
            if(runTime > SQLSessionLoggerAspect.breakPointTime * 1000000)
                sqlLogger.debug("WARNING TIME");
            runningTotal += runTime;
            sqlLogger.debug(String.format("Executed method (time: %1$d ms., running total : %3$d ms.): %2$s object : %4$s", runTime/1000000, thisJoinPoint.getSignature().toString(), runningTotal/1000000, object.toString()));
        }

        return result;
    }
}
