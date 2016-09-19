package lsfusion.server.data;

import lsfusion.base.LongCounter;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.Settings;
import lsfusion.server.data.query.DynamicExecEnvSnapshot;
import lsfusion.server.profiler.Profiler;
import lsfusion.server.stack.ExecutionStackAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static lsfusion.server.ServerLoggers.sqlLogger;

@Aspect
public class SQLSessionLoggerAspect {

    @Around("execution(* lsfusion.server.data.SQLSession.executeDDL(java.lang.String)) && target(session) && args(queryString)")
    public Object executeSQL(ProceedingJoinPoint thisJoinPoint, SQLSession session, String queryString) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, session, queryString);
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeCommand(lsfusion.server.data.SQLCommand, lsfusion.server.data.query.DynamicExecEnvSnapshot, ..)) && target(session) && args(command, snap, ..)")
    public Object executeSelect(ProceedingJoinPoint thisJoinPoint, SQLSession session, SQLCommand command, DynamicExecEnvSnapshot snap) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, session, command.toString());
    }

    @Around("execution(* lsfusion.server.data.SQLSession.insertBatchRecords(java.lang.String, lsfusion.base.col.interfaces.immutable.ImOrderSet, lsfusion.base.col.interfaces.immutable.ImMap, ..)) && target(session) && args(table, keys, rows, ..)")
    public Object executeInsertBatch(ProceedingJoinPoint thisJoinPoint, SQLSession session, String table, ImOrderSet keys, ImMap rows) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, session, "INSERT BATCH INTO " + table + " ROWS " + rows.size() + (rows.isEmpty() ? "" : " FIRST " + rows.getKey(0) + " - " + rows.getValue(0)));
    }
    @Around("execution(* lsfusion.server.data.SQLSession.readSingleValues(lsfusion.server.data.SessionTable, ..)) && target(session) && args(table, ..)")
    public Object executeReadSingleValues(ProceedingJoinPoint thisJoinPoint, SQLSession session, SessionTable table) throws Throwable {
        return executeMethodAndLogTime(thisJoinPoint, session, "READ SINGLE VALUES " + table);
    }

    private static long runningTotal = 0;
    private static long runningWarningTotal = 0;
    private static long runningCount = 0;
    public static int breakPointTime = 60;
    private static int breakPointLength = 10000;

    public Object executeMethodAndLogTime(ProceedingJoinPoint thisJoinPoint, SQLSession session, String queryString) throws Throwable {
        boolean loggingEnabled = session.isLoggerDebugEnabled();

        long startTime = 0;
        if (loggingEnabled || Profiler.PROFILER_ENABLED) {
            startTime = System.nanoTime();
        }

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            long runTime = System.nanoTime() - startTime;
            if(queryString.length() > breakPointLength)
                sqlLogger.debug("WARNING LENGTH");
            runningTotal += runTime;
            runningCount += 1;

            if(runTime > Settings.get().getLogTimeThreshold() * 1000000) {
                runningWarningTotal += runTime;

                queryString = "[length " + queryString.length() + "] " + queryString;
                if (result instanceof ImOrderMap) // cheat, но чисто для логинга
                    queryString = "[rows " + ((ImOrderMap) result).size() + "] " + queryString;
                if (result instanceof Integer) // cheat, но чисто для логинга
                    queryString = "[rows " + result + "] " + queryString;
                sqlLogger.info(String.format("Executed query (time: %1$d ms., running total: %3$d, running warn: %4$d, running count: %5$d): %2$s", runTime / 1000000, queryString, runningTotal / 1000000, runningWarningTotal / 1000000, runningCount));
            }
        }
        if (Profiler.PROFILER_ENABLED) {
            LongCounter counter = ExecutionStackAspect.sqlTime.get();
            if (counter != null) {
                counter.add(System.nanoTime() - startTime);
            }
        }

        return result;
    }
}
