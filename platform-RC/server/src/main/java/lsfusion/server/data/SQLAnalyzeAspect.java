package lsfusion.server.data;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.Settings;
import lsfusion.server.data.query.DynamicExecEnvSnapshot;
import lsfusion.server.data.type.ParseInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SQLAnalyzeAspect {

    @Around("execution(* lsfusion.server.data.SQLSession.executeCommand(lsfusion.server.data.SQLCommand, lsfusion.server.data.query.DynamicExecEnvSnapshot, lsfusion.server.data.OperationOwner, lsfusion.base.col.interfaces.immutable.ImMap, " +
            "java.lang.Object)) && target(sql) && args(command, queryExecEnv, owner, paramObjects, handler)")
    public Object executeCommand(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final SQLCommand command, DynamicExecEnvSnapshot queryExecEnv, final OperationOwner owner, final ImMap<String, ParseInterface> paramObjects, final Object handler) throws Throwable {
        if (command instanceof SQLAnalyze)
            return thisJoinPoint.proceed();

        boolean explain = sql.explainAnalyze();
        boolean noAnalyze = sql.explainNoAnalyze();
        if (command instanceof SQLDML && explain && !noAnalyze)
            return thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, false), queryExecEnv, owner, paramObjects, handler});

        long started = System.currentTimeMillis();
        Object result = thisJoinPoint.proceed();

        if (explain && ( System.currentTimeMillis()-started) > Settings.get().getExplainThreshold()) {
            final DynamicExecEnvSnapshot analyzeEnv = queryExecEnv.forAnalyze();
            assert !analyzeEnv.hasRepeatCommand();
            thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, noAnalyze), analyzeEnv, owner, paramObjects, SQLDML.Handler.VOID});
        }

        return result;
    }
}
