package lsfusion.server.physics.admin.profiler.sql;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.query.exec.DynamicExecEnvSnapshot;
import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLDML;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Aspect
public class SQLAnalyzeAspect {

    // Single-threaded scheduler keeps log order deterministic and fires deferred logs when a query hangs.
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sql-explain-scheduler");
                t.setDaemon(true);
                return t;
            });

    private static void logNoAnalyze(SQLNoAnalyze noAnalyze) {
        for (String line : noAnalyze.getLines())
            ServerLoggers.explainLogger.info(line);
        for (String line : noAnalyze.getCompileLines())
            ServerLoggers.explainCompileLogger.info(line);
    }

    @Around("execution(* lsfusion.server.data.sql.SQLSession.executeCommand(lsfusion.server.data.sql.SQLCommand, lsfusion.server.data.query.exec.DynamicExecEnvSnapshot, lsfusion.server.data.OperationOwner, lsfusion.base.col.interfaces.immutable.ImMap, " +
            "java.lang.Object)) && target(sql) && args(command, queryExecEnv, owner, paramObjects, handler)")
    public Object executeCommand(final ProceedingJoinPoint thisJoinPoint,
                                 final SQLSession sql,
                                 final SQLCommand command,
                                 DynamicExecEnvSnapshot queryExecEnv,
                                 final OperationOwner owner,
                                 final ImMap<String, ParseInterface> paramObjects,
                                 final Object handler) throws Throwable {

        if (command instanceof SQLAnalyze)
            return thisJoinPoint.proceed();

        final boolean explain = sql.explainAnalyze();
        final boolean noAnalyze = sql.explainNoAnalyze();
        final int thresholdMs = Settings.get().getExplainThreshold();
        final int noAnalyzeThresholdMs = Settings.get().getExplainNoAnalyzeThreshold();

        // Run EXPLAIN (VERBOSE, COSTS) before execution if the estimated cost exceeds the noAnalyze threshold.
        // Covers both noAnalyze and SQLDML-analyze paths so the plan is available even if the query hangs.
        SQLNoAnalyze noAnalyzeCommand;
        if (explain && command.baseCost.getDefaultTimeout() > noAnalyzeThresholdMs) {
            noAnalyzeCommand = new SQLNoAnalyze(command);
            thisJoinPoint.proceed(new Object[]{sql, noAnalyzeCommand, queryExecEnv.forAnalyze(), owner, paramObjects, SQLDML.Handler.VOID});
        } else
            noAnalyzeCommand = null;

        // Schedule deferred logging of the pre-explain plan in case the query hangs.
        ScheduledFuture<?> scheduledLog = null;
        if (noAnalyzeCommand != null)
            scheduledLog = scheduler.schedule(
                    () -> logNoAnalyze(noAnalyzeCommand),
                    noAnalyzeThresholdMs, TimeUnit.MILLISECONDS);

        final long started = System.currentTimeMillis();

        // Execute the actual command, wrapping DML with EXPLAIN ANALYZE when needed.
        final boolean ranExplain = command instanceof SQLDML && explain && !noAnalyze;
        final Object result = ranExplain
                ? thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, false), queryExecEnv, owner, paramObjects, handler})
                : thisJoinPoint.proceed();

        final long elapsedMs = System.currentTimeMillis() - started;

        if (noAnalyzeCommand != null && noAnalyze) {
            // cancel(false) returns true only if the task had not yet started (query finished
            // before the scheduled delay).  In that case log if the query was still slow.
            // If cancel returns false the scheduled task already ran and logged the plan.
            if (scheduledLog.cancel(false) && elapsedMs >= thresholdMs)
                logNoAnalyze(noAnalyzeCommand);
        } else if (!ranExplain && explain && elapsedMs >= thresholdMs) {
            // No pre-explain and no inline EXPLAIN ANALYZE: fallback to post-explain for slow queries.
            DynamicExecEnvSnapshot analyzeEnv = queryExecEnv.forAnalyze();
            assert !analyzeEnv.hasRepeatCommand();
            thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, noAnalyze), analyzeEnv, owner, paramObjects, SQLDML.Handler.VOID});
        }

        return result;
    }
}
