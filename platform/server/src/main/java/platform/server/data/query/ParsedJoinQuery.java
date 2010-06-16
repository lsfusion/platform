package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.Lazy;
import platform.server.caches.SynchronizedLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.*;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Immutable
class ParsedJoinQuery<K,V> extends Join<V> implements ParsedQuery<K,V> {

    public final Map<K,KeyExpr> mapKeys;
    public final Map<V, Expr> properties;
    protected final Where where;

    protected final Set<ValueExpr> values;

    public Set<ValueExpr> getValues() {
        return values;
    }

    ParsedJoinQuery(Query<K,V> query) {
        mapKeys = query.mapKeys;
        values = query.getValues();

        where = query.where.pack();
        properties = where.followTrue(query.properties);
    }

    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax) {
        return compileSelect(syntax,new OrderedMap<V, Boolean>(),0);
    }
    @SynchronizedLazy
    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax, OrderedMap<V,Boolean> orders,int top) {
        return new CompiledQuery<K,V>(this, syntax, orders, top);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        assert joinImplement.size()==mapKeys.size();
        return join(joinImplement, MapValuesTranslator.noTranslate);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) {
        assert joinImplement.size()==mapKeys.size();
        assert mapValues.assertValuesEquals(values); // все должны быть параметры

        Map<K,KeyExpr> joinKeys = new HashMap<K, KeyExpr>();
        for(Map.Entry<K,? extends Expr> joinExpr : joinImplement.entrySet()) {
            if(!(joinExpr.getValue() instanceof KeyExpr) || joinKeys.values().contains((KeyExpr)joinExpr.getValue()))
                return joinExprs(joinImplement, mapValues);
           joinKeys.put(joinExpr.getKey(), (KeyExpr) joinExpr.getValue());
        }
        return new DirectTranslateJoin<V>(new MapTranslator(BaseUtils.crossJoin(mapKeys, joinKeys), mapValues), this);
    }

    public Join<V> joinExprs(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) { // последний параметр = какой есть\какой нужно, joinImplement не translate'ся
        assert joinImplement.size()==mapKeys.size();

        Join<V> join = this;

        // сначала map'им значения
        join = new DirectTranslateJoin<V>(mapValues.mapKeys(), join);

        // затем делаем подстановку
        join = new QueryTranslateJoin<V>(new QueryTranslator(BaseUtils.crossJoin(mapKeys, joinImplement)), join);

         // затем закидываем Where что все implement не null
        join = new CaseJoin<V>(Expr.getWhere(joinImplement), join);

        return join;
    }

    // для заданных свойств вытягивает условия на классы для них
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        Where upWhere = where.not();

        ClassWhere<B> result = new ClassWhere<B>();
        for(MapCase<? extends V> mapCase : CaseExpr.pullCases(BaseUtils.filterKeys(properties,classProps))) {
            Where caseWhere = mapCase.where.and(upWhere.not());
            for(BaseExpr expr : mapCase.data.values()) // все следствия за and'им
                caseWhere = caseWhere.and(expr.getWhere());

            result = result.or(caseWhere.getClassWhere().get(BaseUtils.<B, BaseExpr>forceMerge(mapCase.data, mapKeys)));

            upWhere = upWhere.or(mapCase.where);
        }
        return result;
    }

    @Lazy
    public Expr getExpr(V property) {
        return properties.get(property).and(where);
    }

    public Collection<V> getProperties() {
        return properties.keySet();
    }

    public Where getWhere() {
        return where;
    }

}
