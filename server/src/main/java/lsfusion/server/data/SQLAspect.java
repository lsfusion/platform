package lsfusion.server.data;

import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.data.query.AdjustState;
import lsfusion.server.data.query.AdjustVolatileExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.query.DynamicExecuteEnvironment;
import lsfusion.server.data.type.ParseInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class SQLAspect {
    
    private static interface ProceedDefaultEnv {
        Object proceed() throws Throwable;
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeCommand(lsfusion.server.data.SQLCommand, lsfusion.server.data.OperationOwner, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.DynamicExecuteEnvironment, int," +
            "java.lang.Object)) && target(sql) && args(command, owner, paramObjects, queryExecEnv, transactTimeout, handler)")
    public Object executeCommand(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final SQLCommand command, final OperationOwner owner, final ImMap<String, ParseInterface> paramObjects, DynamicExecuteEnvironment queryExecEnv, final int transactTimeout, final Object handler) throws Throwable {
        return executeRepeatableStatement(thisJoinPoint, sql, owner, queryExecEnv, command, new ProceedDefaultEnv() {
            public Object proceed() throws Throwable {
                return thisJoinPoint.proceed(new Object[] {sql, command, owner, paramObjects, DynamicExecuteEnvironment.DEFAULT, transactTimeout, handler});
            }});
    }

    // проверка на closed
    private Object executeRepeatableStatement(ProceedingJoinPoint thisJoinPoint, SQLSession session, OperationOwner owner, DynamicExecuteEnvironment env, SQLCommand command, ProceedDefaultEnv proceedDefault) throws Throwable {
        if(command.command.length() > Settings.get().getQueryLengthLimit())
            throw new SQLTooLongQueryException(command.command);
        
        Object result = null;
        Result<Boolean> wasException = new Result<Boolean>();
        try {
            if(env instanceof AdjustVolatileExecuteEnvironment)
                result = executeRepeatableStatement(thisJoinPoint, session, owner, (AdjustVolatileExecuteEnvironment)env, proceedDefault, wasException);
            else 
                result = thisJoinPoint.proceed();
        } catch (SQLClosedException e) {
            if(e.isInTransaction() || !session.tryRestore(owner, e.connection, e.isPrivate))
                throw e;
            wasException.set(true);
        }

        if(wasException.result != null) // повторяем
            result = ReflectionUtils.invokeTransp(((MethodSignature) thisJoinPoint.getSignature()).getMethod(), thisJoinPoint.getTarget(), thisJoinPoint.getArgs());

        return result;
    }

    // проверка на timeout
    private Object executeRepeatableStatement(ProceedingJoinPoint thisJoinPoint, SQLSession session, OperationOwner owner, AdjustVolatileExecuteEnvironment env, ProceedDefaultEnv proceedDefault, Result<Boolean> wasException) throws Throwable {
        
        Object result = null;
        AdjustState state = env.before(session, owner);
        if(state == null) // за null'им параметр
            return proceedDefault.proceed();
        
        try {
            result = thisJoinPoint.proceed();
            
            env.succeeded(state);
        } catch (SQLHandledException e) {
            if (!(e instanceof SQLTimeoutException && !((SQLTimeoutException) e).isTransactTimeout))
                throw e;
            env.failed(state);
            if (e.isInTransaction()) // транзакция все равно прервана
                throw e;
            // вообще тут только timeout и closed могут быть.
            wasException.set(true);
        } finally {
            env.after(state, session, owner);
        }
        
        return result;
    }
}
