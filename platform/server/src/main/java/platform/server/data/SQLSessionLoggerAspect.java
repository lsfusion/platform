package platform.server.data;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static platform.server.ServerLoggers.sqlLogger;

@Aspect
public class SQLSessionLoggerAspect {

    @Around("execution(* platform.server.data.SQLSession.executeDDL(java.lang.String)) && args(queryString)")
    public Object executeSQL(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, queryString);
    }

    @Around("execution(* platform.server.data.SQLSession.executeDML(java.lang.String, ..)) && args(queryString, ..)")
    public Object executeDML(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, queryString);
    }

    @Around("execution(* platform.server.data.SQLSession.executeSelect(java.lang.String, ..)) && args(select, ..)")
    public Object executeSelect(ProceedingJoinPoint thisJoinPoint, String select) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, select);
    }

    private static long runningTotal = 0;

    private Object executeMethodAndLogTime(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        boolean loggingEnabled = sqlLogger.isDebugEnabled();

        long startTime = 0;
        if (loggingEnabled)
            startTime = System.currentTimeMillis();

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            long runTime = System.currentTimeMillis() - startTime;
            runningTotal += runTime;
            sqlLogger.debug(String.format("Executed query (time: %1$d ms., running total: %3$d): %2$s", runTime, queryString, runningTotal));
        }

        return result;
    }
}
