package lsfusion.server.data;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.query.AdjustState;
import lsfusion.server.data.query.AdjustVolatileExecuteEnvironment;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.QueryExecuteEnvironment;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class SQLAspect {
    
    private static interface ProceedDefaultEnv {
        Object proceed() throws Throwable;
    } 
    @Around("execution(* lsfusion.server.data.SQLSession.executeDML(java.lang.String, lsfusion.server.data.OperationOwner, lsfusion.server.data.TableOwner, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.ExecuteEnvironment, lsfusion.server.data.query.QueryExecuteEnvironment, int)) && target(sql) && args(queryString, owner, tableOwner, paramObjects, env, queryExecEnv, transactTimeout)")
    public Object executeDML(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final String queryString, final OperationOwner owner, final TableOwner tableOwner, final ImMap paramObjects, final ExecuteEnvironment env, QueryExecuteEnvironment queryExecEnv, final int transactTimeout) throws Throwable {
        return executeRepeatableStatement(thisJoinPoint, sql, owner, queryExecEnv, new ProceedDefaultEnv() {
            public Object proceed() throws Throwable {
                return thisJoinPoint.proceed(new Object[] {sql, queryString, owner, tableOwner, paramObjects, env, QueryExecuteEnvironment.DEFAULT, transactTimeout});
            }});
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeSelect(java.lang.String, lsfusion.server.data.OperationOwner, lsfusion.server.data.query.ExecuteEnvironment, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.QueryExecuteEnvironment, int," +
            "lsfusion.base.col.interfaces.immutable.ImRevMap, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.base.col.interfaces.immutable.ImRevMap, lsfusion.base.col.interfaces.immutable.ImMap)) && target(sql) && args(select, owner, env, paramObjects, queryExecEnv, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders)")
    public Object executeSelect(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final String select, final OperationOwner owner, final ExecuteEnvironment env, final ImMap paramObjects, QueryExecuteEnvironment queryExecEnv, final int transactTimeout, final ImRevMap keyNames, final ImMap keyReaders, final ImRevMap propertyNames, final ImMap propertyReaders) throws Throwable {
        return executeRepeatableStatement(thisJoinPoint, sql, owner, queryExecEnv, new ProceedDefaultEnv() {
            public Object proceed() throws Throwable {
                return thisJoinPoint.proceed(new Object[] {sql, select, owner, env, paramObjects, QueryExecuteEnvironment.DEFAULT, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders});
            }});
    }

    // проверка на closed
    private Object executeRepeatableStatement(ProceedingJoinPoint thisJoinPoint, SQLSession session, OperationOwner owner, QueryExecuteEnvironment env, ProceedDefaultEnv proceedDefault) throws Throwable {
        Object result = null;
        try {
            if(env instanceof AdjustVolatileExecuteEnvironment)
                result = executeRepeatableStatement(thisJoinPoint, session, owner, (AdjustVolatileExecuteEnvironment)env, proceedDefault);
            else 
                result = thisJoinPoint.proceed();
        } catch (SQLClosedException e) {
            if(session.lockIsInTransaction(owner) || !session.tryRestore(owner, e.connection, e.isPrivate))
                throw e;
        }

        if(result == null) // повторяем
            result = ReflectionUtils.invokeTransp(((MethodSignature) thisJoinPoint.getSignature()).getMethod(), thisJoinPoint.getTarget(), thisJoinPoint.getArgs());

        return result;
    }

    // проверка на timeout
    private Object executeRepeatableStatement(ProceedingJoinPoint thisJoinPoint, SQLSession session, OperationOwner owner, AdjustVolatileExecuteEnvironment env, ProceedDefaultEnv proceedDefault) throws Throwable {
        
        Object result = null;
        AdjustState state = env.before(session, owner);
        if(state == null) // за null'им параметр
            return proceedDefault.proceed();
        
        try {
            result = thisJoinPoint.proceed();
            
            env.succeeded(state);
        } catch (SQLHandledException e) {
            if(e instanceof SQLClosedException || e instanceof SQLTooLargeQueryException)
                throw e;
            env.failed(state, e);
            if(session.lockIsInTransaction(owner)) // транзакция все равно прервана
                throw e;
            // вообще тут только timeout и closed могут быть.
        } finally {
            env.after(state, session, owner);
        }
        
        return result;
    }
}
