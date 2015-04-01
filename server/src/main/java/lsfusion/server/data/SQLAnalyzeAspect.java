package lsfusion.server.data;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class SQLAnalyzeAspect {

    @Around("execution(* lsfusion.server.data.SQLSession.executeCommand(lsfusion.server.data.SQLCommand, lsfusion.server.data.OperationOwner, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.DynamicExecuteEnvironment, int," +
            "java.lang.Object)) && target(sql) && args(command, owner, paramObjects, queryExecEnv, transactTimeout, handler)")
    public Object executeCommand(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final SQLCommand command, final OperationOwner owner, final ImMap<String, ParseInterface> paramObjects, DynamicExecuteEnvironment queryExecEnv, final int transactTimeout, final Object handler) throws Throwable {
        boolean explain = sql.explainAnalyze();
        boolean noAnalyze = sql.explainNoAnalyze();
        if(command instanceof SQLDML && explain && !noAnalyze)
            return thisJoinPoint.proceed(new Object[] {sql, new SQLAnalyze(command, false), owner, paramObjects, queryExecEnv, transactTimeout, handler});

        if (explain) {
            thisJoinPoint.proceed(new Object[] {sql, new SQLAnalyze(command, noAnalyze), owner, paramObjects, queryExecEnv, transactTimeout, new Result<Integer>()});
        }

        return thisJoinPoint.proceed();
    }
}
