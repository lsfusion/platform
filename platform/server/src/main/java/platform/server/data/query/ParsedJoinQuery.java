package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.Message;
import platform.server.caches.IdentityLazy;
import platform.server.caches.SynchronizedLazy;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.pull.ExclPullWheres;
import platform.server.data.query.innerjoins.GroupJoinsWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslator;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;

import java.util.*;

public class ParsedJoinQuery<K,V> extends AbstractJoin<V> implements ParsedQuery<K,V> {

    public final Map<K,KeyExpr> mapKeys;
    public final Map<V, Expr> properties;
    protected final Where where;

    protected final QuickSet<Value> values;

    public QuickSet<Value> getValues() {
        return values;
    }

    ParsedJoinQuery(Query<K,V> query) {
        mapKeys = query.mapKeys;
        values = query.getInnerValues();

        where = query.where.pack();
        properties = where.followTrue(query.properties, true);
    }

    @SynchronizedLazy
    @Message("message.core.query.compile")
    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax, OrderedMap<V, Boolean> orders, int top, String prefix) {
        return new CompiledQuery<K,V>(this, syntax, orders, top, prefix);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) {
        assert joinImplement.size()==mapKeys.size();
        assert mapValues.assertValuesEquals(values.getSet()); // все должны быть параметры

        Map<K,KeyExpr> joinKeys = new HashMap<K, KeyExpr>();
        for(Map.Entry<K,? extends Expr> joinExpr : joinImplement.entrySet()) {
            if(!(joinExpr.getValue() instanceof KeyExpr) || joinKeys.values().contains((KeyExpr)joinExpr.getValue()))
                return joinExprs(joinImplement, mapValues);
           joinKeys.put(joinExpr.getKey(), (KeyExpr) joinExpr.getValue());
        }
        return new DirectTranslateJoin<V>(new MapTranslator(BaseUtils.crossJoin(mapKeys, joinKeys), mapValues), this);
    }

    public Join<V> joinExprs(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) { // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся
        assert joinImplement.size()==mapKeys.size();

        Join<V> join = this;

        // сначала map'им значения
        join = new DirectTranslateJoin<V>(mapValues.mapKeys(), join);

        // затем делаем подстановку
        join = new QueryTranslateJoin<V>(new QueryTranslator(BaseUtils.crossJoin(mapKeys, joinImplement)), join);

         // затем закидываем Where что все implement не null
        join = join.and(Expr.getWhere(joinImplement));

        return join;
    }

    // для заданных свойств вытягивает условия на классы для них, assertion что K extends B и V extends B
    @IdentityLazy
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        return (ClassWhere<B>) getClassWhere(where, mapKeys, BaseUtils.filterKeys(properties, classProps));
    }

    private static <B, K extends B, V extends B> ClassWhere<B> getClassWhere(Where where, final Map<K, KeyExpr> mapKeys, Map<V, Expr> mapProps) {
        return new ExclPullWheres<ClassWhere<B>, V, Where>() {
            protected ClassWhere<B> initEmpty() {
                return ClassWhere.STATIC(false);
            }
            protected ClassWhere<B> proceedBase(Where data, Map<V, BaseExpr> map) {
                return (ClassWhere<B>)(ClassWhere<?>)getClassWhereBase(data, mapKeys, map);
            }
            protected ClassWhere<B> add(ClassWhere<B> op1, ClassWhere<B> op2) {
                return op1.or(op2);
            }
        }.proceed(where, mapProps);
    }

    private static <B, K extends B, V extends B> ClassWhere<B> getClassWhereBase(Where where, Map<K, KeyExpr> mapKeys, Map<V, BaseExpr> mapProps) {
        return where.and(Expr.getWhere(mapProps.values())).
                    getClassWhere().get(BaseUtils.<B, BaseExpr>forceMerge(mapProps, mapKeys));
    }

    @IdentityLazy
    public Expr getExpr(V property) {
        return properties.get(property).and(where);
    }

    public Collection<V> getProperties() {
        return properties.keySet();
    }

    public Where getWhere() {
        return where;
    }

    public QuickSet<KeyExpr> getKeys() {
        return new QuickSet<KeyExpr>(mapKeys.values());
    }

    public Collection<GroupJoinsWhere> getWhereJoins(boolean notExclusive) {
        return where.getWhereJoins(notExclusive, getKeys());
    }

    private static <K> void pullValues(Map<K, ? extends Expr> map, Where where, Map<K, Expr> result) {
        Map<BaseExpr, BaseExpr> exprValues = where.getExprValues();
        for(Map.Entry<K, ? extends Expr> entry : map.entrySet()) {
            Expr exprValue = exprValues.get(entry.getValue());
            if(exprValue==null && entry.getValue().isValue())
                exprValue = entry.getValue();
            if(exprValue!=null)
                result.put(entry.getKey(), exprValue);
        }
    }

    // жестковатая эвристика, но не страшно
    public Query<K,V> pullValues(Map<K, Expr> pullKeys, Map<V, Expr> pullProps) {
        pullValues(mapKeys, where, pullKeys);
        QueryTranslator keyTranslator = new PartialQueryTranslator(BaseUtils.rightCrossJoin(mapKeys, pullKeys));
        Where transWhere = where.translateQuery(keyTranslator);
        Map<V, Expr> transProps = keyTranslator.translate(properties);
        pullValues(transProps, transWhere, pullProps);
        return new Query<K,V>(BaseUtils.filterNotKeys(mapKeys, pullKeys.keySet()), BaseUtils.filterNotKeys(transProps, pullProps.keySet()), transWhere);
    }
}
