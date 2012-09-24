package platform.server.data;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.server.session.ExecutionEnvironment;

import java.util.Map;

@Aspect
public class SQLSessionLoggerAspect {
    private final static Logger logger = Logger.getLogger(SQLSessionLoggerAspect.class);

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

    private Object executeMethodAndLogTime(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = thisJoinPoint.proceed();
        long runTime = System.currentTimeMillis() - startTime;

        logger.info(String.format("Executed query (time: %1$d ms.): %2$s", runTime, queryString));

        return result;
    }
}
