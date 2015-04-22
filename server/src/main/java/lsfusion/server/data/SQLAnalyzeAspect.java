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

    @Around("execution(* lsfusion.server.data.SQLSession.executeCommand(lsfusion.server.data.SQLCommand, lsfusion.server.data.query.DynamicExecuteEnvironment, lsfusion.server.data.OperationOwner, lsfusion.base.col.interfaces.immutable.ImMap, int," +
            "java.lang.Object)) && target(sql) && args(command, queryExecEnv, owner, paramObjects, transactTimeout, handler)")
    public Object executeCommand(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final SQLCommand command, DynamicExecuteEnvironment queryExecEnv, final OperationOwner owner, final ImMap<String, ParseInterface> paramObjects, final int transactTimeout, final Object handler) throws Throwable {
        if(!(command instanceof SQLAnalyze)) {
            boolean explain = sql.explainAnalyze();
            boolean noAnalyze = sql.explainNoAnalyze();
            if (command instanceof SQLDML && explain && !noAnalyze)
                return thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, false), queryExecEnv, owner, paramObjects, transactTimeout, handler});

            if (explain) {
                thisJoinPoint.proceed(new Object[]{sql, new SQLAnalyze(command, noAnalyze), queryExecEnv, owner, paramObjects, transactTimeout, new Result<Integer>()});
            }
        }

        return thisJoinPoint.proceed();
    }
}
