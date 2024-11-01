package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.AbstractInnerContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.compile.CompileOptions;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.query.compile.SubQueryContext;
import lsfusion.server.data.query.result.ReadAllResultHandler;
import lsfusion.server.data.query.result.ReadDistinctValuesHandler;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.query.translate.MapQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.translate.MapValuesTranslator;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.physics.admin.monitor.sql.SQLDebugInfo;

import java.sql.SQLException;
import java.util.Objects;

public abstract class IQuery<K,V> extends AbstractInnerContext<IQuery<K, V>> implements MapKeysInterface<K> {

    @Override
    public IQuery<K, V> translateRemoveValues(MapValuesTranslate translate) {
        return translateQuery(translate.mapKeys());
    }

    @Override
    public IQuery<K,V> translateValues(MapValuesTranslate translate) { // оптимизация
        return translateMap(translate);
    }

    protected IQuery<K, V> translate(MapTranslate translator) {
        if (translator.identityKeys(getInnerKeys()))
            return translateValues(translator.mapValues());
        else
            return translateQuery(translator);
    }

    public abstract MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate);
    public abstract IQuery<K, V> translateQuery(MapTranslate translate);

    public CompiledQuery<K,V> compile(CompileOptions<V> options) {
        return compile(MapFact.EMPTYORDER(), options);
    }
    public abstract CompiledQuery<K,V> compile(ImOrderMap<V, Boolean> orders, CompileOptions<V> options);

    public abstract ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders);

    public ImOrderSet<ImMap<V, Object>> executeDistinctValues(DataSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        ReadDistinctValuesHandler<K, V> result = new ReadDistinctValuesHandler<>();
        executeSQL(session.sql, orders, limitOffset, true, session.env, result);
        return result.terminate();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSQL(SQLSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset, QueryEnvironment env) throws SQLException, SQLHandledException {
        ReadAllResultHandler<K, V> result = new ReadAllResultHandler<>();
        executeSQL(session, orders, limitOffset, false, env, result);
        return result.terminate();
    }

    @StackMessage("{message.query.execute}")
    public void executeSQL(SQLSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset, boolean distinctValues, QueryEnvironment env, ResultHandler<K, V> result) throws SQLException, SQLHandledException {
        CompileOptions<V> options = new CompileOptions<>(session.syntax, LimitOptions.get(limitOffset, distinctValues), SubQueryContext.EMPTY);
        CompiledQuery<K, V> compile = compile(orders, options);

        SQLDebugInfo<K, V> debugInfo = new SQLDebugInfo<>(this, options);
        SQLDebugInfo prevDebugInfo = SQLDebugInfo.pushStack(debugInfo);
        try {
            compile.execute(session, env, limitOffset, result);
        } finally {
            SQLDebugInfo.popStack(debugInfo, prevDebugInfo);
        }
    }

    public abstract <B> ClassWhere<B> getClassWhere(ImSet<? extends V> classProps);

    public abstract Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> getClassQuery(final BaseClass baseClass);

    public Join<V> join(ImMap<K, ? extends Expr> joinImplement) {
        return join(joinImplement, MapValuesTranslator.noTranslate(getInnerValues()));
    }
    public abstract Join<V> join(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues); // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся
    public abstract Join<V> joinExprs(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues);


    public abstract ImSet<V> getProperties();
    public abstract Expr getExpr(V property);
    public abstract Where getWhere();    

    public boolean isEmpty() {
        return getWhere().isFalse();
    }

    public void outSelect(SQLSession session) throws SQLException, SQLHandledException {
        outSelect(session, DataSession.emptyEnv(OperationOwner.debug), false);
    }
    public void outSelect(SQLSession session, QueryEnvironment env, boolean uniqueViolation) throws SQLException, SQLHandledException {
        compile(new CompileOptions<>(session.syntax)).outSelect(session, env, uniqueViolation);
    }

    public String readSelect(SQLSession session) throws SQLException, SQLHandledException {
        return readSelect(session,  DataSession.emptyEnv(OperationOwner.unknown));
    }
    public String readSelect(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        return compile(new CompileOptions<>(session.syntax)).readSelect(session, env);
    }

    public abstract Query<K, V> getQuery(); // по сути protectedQ  GH  N
    public abstract <RMK, RMV> IQuery<RMK,RMV> map(ImRevMap<RMK, K> remapKeys, ImRevMap<RMV, V> remapProps, MapValuesTranslate translate);
    public <RMK, RMV> IQuery<RMK,RMV> map(ImRevMap<RMK, K> remapKeys, ImRevMap<RMV, V> remapProps) {
        return map(remapKeys, remapProps, MapValuesTranslator.noTranslate(getInnerValues()));
    }

    
    public static class PullValues<K, V> {
        public final IQuery<K, V> query;
        public final ImMap<K, Expr> pullKeys;
        public final ImMap<V, Expr> pullProps;

        public PullValues(IQuery<K, V> query) {
            this(query, MapFact.<K, Expr>EMPTY(), MapFact.<V, Expr>EMPTY());
        }

        public PullValues(IQuery<K, V> query, ImMap<K, Expr> pullKeys, ImMap<V, Expr> pullProps) {
            this.query = query;
            this.pullKeys = pullKeys;
            this.pullProps = pullProps;
        }
        
        public boolean isEmpty() {
            return pullKeys.isEmpty() && pullProps.isEmpty();
        }
        
        public <MK, MV> PullValues<MK, MV> map(ImRevMap<MK, K> mapKeys, ImRevMap<MV, V> mapProps, MapValuesTranslate mapValues) {
            return new PullValues<>(query.map(mapKeys.removeValuesRev(pullKeys.keys()), mapProps.removeValuesRev(pullProps.keys()), mapValues.filter(query.getInnerValues())),
                    mapKeys.rightJoin(mapValues.mapKeys().translate(pullKeys)),
                    mapProps.rightJoin(mapValues.mapKeys().translate(pullProps)));
        }

        // only for check caches
        public boolean equals(Object o) {
            return this == o || o instanceof PullValues && query.equals(((PullValues<?, ?>) o).query) && pullKeys.equals(((PullValues<?, ?>) o).pullKeys) && pullProps.equals(((PullValues<?, ?>) o).pullProps);
        }

        public int hashCode() {
            return Objects.hash(query, pullKeys, pullProps);
        }
    }
    public abstract PullValues<K, V> pullValues();
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(env);
    }
}
