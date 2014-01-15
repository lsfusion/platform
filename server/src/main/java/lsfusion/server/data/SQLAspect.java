package lsfusion.server.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.query.AdjustVolatileExecuteEnvironment;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.query.QueryExecuteEnvironment;
import lsfusion.server.data.type.Reader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;

@Aspect
public class SQLAspect {
    
    private static interface ProceedDefaultEnv {
        Object proceed() throws Throwable;
    } 
    @Around("execution(* lsfusion.server.data.SQLSession.executeDML(java.lang.String, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.ExecuteEnvironment, lsfusion.server.data.query.QueryExecuteEnvironment, int)) && target(sql) && args(queryString, paramObjects, env, queryExecEnv, transactTimeout)")
    public Object executeDML(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final String queryString, final ImMap paramObjects, final ExecuteEnvironment env, AdjustVolatileExecuteEnvironment queryExecEnv, final int transactTimeout) throws Throwable {
        return executeRepeatableStatement(thisJoinPoint, sql, queryExecEnv, new ProceedDefaultEnv() {
            public Object proceed() throws Throwable {
                return thisJoinPoint.proceed(new Object[] {sql, queryString, paramObjects, env, QueryExecuteEnvironment.DEFAULT, transactTimeout});
            }});
    }

    @Around("execution(* lsfusion.server.data.SQLSession.executeSelect(java.lang.String, lsfusion.server.data.query.ExecuteEnvironment, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.server.data.query.QueryExecuteEnvironment, int," +
            "lsfusion.base.col.interfaces.immutable.ImRevMap, lsfusion.base.col.interfaces.immutable.ImMap, lsfusion.base.col.interfaces.immutable.ImRevMap, lsfusion.base.col.interfaces.immutable.ImMap)) && target(sql) && args(select, env, paramObjects, queryExecEnv, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders)")
    public Object executeSelect(final ProceedingJoinPoint thisJoinPoint, final SQLSession sql, final String select, final ExecuteEnvironment env, final ImMap paramObjects, AdjustVolatileExecuteEnvironment queryExecEnv, final int transactTimeout, final ImRevMap keyNames, final ImMap keyReaders, final ImRevMap propertyNames, final ImMap propertyReaders) throws Throwable {
        return executeRepeatableStatement(thisJoinPoint, sql, queryExecEnv, new ProceedDefaultEnv() {
            public Object proceed() throws Throwable {
                return thisJoinPoint.proceed(new Object[] {sql, select, env, paramObjects, QueryExecuteEnvironment.DEFAULT, transactTimeout, keyNames, keyReaders, propertyNames, propertyReaders});
            }});
    }
    
    private Object executeRepeatableStatement(ProceedingJoinPoint thisJoinPoint, SQLSession session, AdjustVolatileExecuteEnvironment env, ProceedDefaultEnv proceedDefault) throws Throwable {
        
        if(env == null)
            return thisJoinPoint.proceed();

        Object result = null;
        Object state = env.before(session);
        if(state == null) // за null'им параметр
            return proceedDefault.proceed();
        
        try {
            result = thisJoinPoint.proceed();
            
            env.succeeded(state);
        } catch (SQLHandledException e) {
            env.failed(state);
            if(session.lockIsInTransaction()) // транзакция все равно прервана
                throw e;
        } finally {
            env.after(state, session);
        }
        
        if(result == null) { // повторяем
            try {
                result = ((MethodSignature) thisJoinPoint.getSignature()).getMethod().invoke(thisJoinPoint.getTarget(), thisJoinPoint.getArgs());
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        
        return result;
    }
}
