package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Message;
import lsfusion.server.caches.AbstractInnerContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.MapValuesTranslator;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

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

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return compile(syntax, MapFact.<V, Boolean>EMPTYORDER(), 0, SubQueryContext.EMPTY, false);
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax,ImOrderMap<V,Boolean> orders,int selectTop) {
        return compile(syntax, orders, selectTop, SubQueryContext.EMPTY);
    }
    public CompiledQuery<K,V> compile(SQLSyntax syntax, ImOrderMap<V, Boolean> orders, Integer selectTop, SubQueryContext subcontext) {
        return compile(syntax, orders, selectTop, subcontext, false);
    }
    public abstract CompiledQuery<K,V> compile(SQLSyntax syntax, ImOrderMap<V, Boolean> orders, Integer top, SubQueryContext subcontext, boolean recursive);

    public abstract ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders);

    @Message("message.query.execute")
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> executeSQL(SQLSession session, ImOrderMap<V, Boolean> orders, int selectTop, QueryEnvironment env) throws SQLException, SQLHandledException {
        return compile(session.syntax, orders, selectTop).execute(session, env);
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
        outSelect(session, DataSession.emptyEnv(OperationOwner.debug));
    }
    public void outSelect(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        compile(session.syntax).outSelect(session, env);
    }

    public String readSelect(SQLSession session) throws SQLException, SQLHandledException {
        return readSelect(session,  DataSession.emptyEnv(OperationOwner.debug));
    }
    public String readSelect(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        return compile(session.syntax).readSelect(session, env);
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
            return new PullValues<MK, MV>(query.map(mapKeys.filterNotValuesRev(pullKeys.keys()), mapProps.filterNotValuesRev(pullProps.keys()), mapValues.filter(query.getInnerValues())),
                    mapKeys.rightJoin(mapValues.mapKeys().translate(pullKeys)),
                    mapProps.rightJoin(mapValues.mapKeys().translate(pullProps)));
        }
    }
    public abstract PullValues<K, V> pullValues();
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(env);
    }
}
