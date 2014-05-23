package lsfusion.server.data;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;

import static lsfusion.server.ServerLoggers.sqlLogger;

@Aspect
public class SQLSessionLoggerAspect {

    @Around("execution(* lsfusion.server.data.SQLSession.executeDDL(java.lang.String)) && args(queryString)")
    public Object executeSQL(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, queryString);
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeDML(java.lang.String, ..)) && args(queryString, ..)")
    public Object executeDML(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, queryString);
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeSelect(java.lang.String, ..)) && args(select, ..)")
    public Object executeSelect(ProceedingJoinPoint thisJoinPoint, String select) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, select);
    }

    @Around("execution(* lsfusion.server.data.SQLSession.insertBatchRecords(java.lang.String, lsfusion.base.col.interfaces.immutable.ImOrderSet, lsfusion.base.col.interfaces.immutable.ImMap, ..)) && args(table, keys, rows, ..)")
    public Object executeInsertBatch(ProceedingJoinPoint thisJoinPoint, String table, ImOrderSet keys, ImMap rows) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, "INSERT BATCH INTO " + table + " ROWS " + rows.size());
    }
    @Around("execution(* lsfusion.server.data.SQLSession.readSingleValues(lsfusion.server.data.SessionTable, ..)) && args(table, ..)")
    public Object executeReadSingleValues(ProceedingJoinPoint thisJoinPoint, SessionTable table) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, "READ SINGLE VALUES " + table);
    }

    private static long runningTotal = 0;
    private static long runningWarningTotal = 0;
    private static long runningCount = 0;
    public static int breakPointTime = 60;
    private static int breakPointLength = 10000;

    public Object executeMethodAndLogTime(ProceedingJoinPoint thisJoinPoint, String queryString) throws Throwable {
        boolean loggingEnabled = sqlLogger.isDebugEnabled();

        long startTime = 0;
        if (loggingEnabled)
            startTime = System.nanoTime();

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            long runTime = System.nanoTime() - startTime;
            if(runTime > breakPointTime * 1000000) {
                sqlLogger.debug("WARNING TIME");
                runningWarningTotal += runTime;
            }
            if(queryString.length() > breakPointLength)
                sqlLogger.debug("WARNING LENGTH");
            runningTotal += runTime;
            runningCount += 1;
            queryString = "[length " + queryString.length() + "] " + queryString;
            if(result instanceof ImOrderMap) // cheat, но чисто для логинга
                queryString = "[rows " + ((ImOrderMap)result).size() + "] " + queryString;
            if(result instanceof Integer) // cheat, но чисто для логинга
                queryString = "[rows " + result + "] " + queryString;
            sqlLogger.info(String.format("Executed query (time: %1$d ms., running total: %3$d, running warn: %4$d, running count: %5$d): %2$s", runTime/1000000, queryString, runningTotal/1000000, runningWarningTotal/1000000, runningCount));
        }

        return result;
    }
}
