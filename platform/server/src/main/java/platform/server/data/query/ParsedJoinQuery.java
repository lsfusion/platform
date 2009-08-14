package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.Lazy;
import platform.server.caches.SynchronizedLazy;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Immutable
class ParsedJoinQuery<K,V> extends Join<V> implements ParsedQuery<K,V> {

    public final Map<K,KeyExpr> mapKeys;
    public final Map<V, SourceExpr> properties;
    protected final Where where;

    protected final Context context;

    ParsedJoinQuery(JoinQuery<K,V> query) {
        mapKeys = query.mapKeys;
        context = query.getContext();

        properties = query.where.followTrue(query.properties);
        where = query.where;
    }

    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax) {
        return compileSelect(syntax,new LinkedHashMap<V, Boolean>(),0);
    }
    @SynchronizedLazy
    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int top) {
        return new CompiledQuery<K,V>(this, syntax, orders, top);
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement) {
        assert joinImplement.size()==mapKeys.size();
        return join(joinImplement,BaseUtils.toMap(context.values));
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> mapValues) { // последний параметр = какой есть\какой нужно
        assert joinImplement.size()==mapKeys.size();

        Map<K,KeyExpr> joinKeys = new HashMap<K, KeyExpr>();
        for(Map.Entry<K,? extends SourceExpr> joinExpr : joinImplement.entrySet()) {
            if(!(joinExpr.getValue() instanceof KeyExpr) || joinKeys.values().contains((KeyExpr)joinExpr.getValue()))
                return joinExprs(joinImplement, mapValues);
           joinKeys.put(joinExpr.getKey(), (KeyExpr) joinExpr.getValue());
        }
        return new TranslateJoin<V>(new KeyTranslator(BaseUtils.crossJoin(mapKeys, joinKeys), mapValues), this);
    }

    public Join<V> joinExprs(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> mapValues) { // последний параметр = какой есть\какой нужно
        assert joinImplement.size()==mapKeys.size();

        Where joinWhere = Where.TRUE; // надо еще where join'ов закинуть
        for(SourceExpr joinExpr : joinImplement.values())
            joinWhere = joinWhere.and(joinExpr.getWhere());
        return new CaseJoin<V>(joinWhere, new TranslateJoin<V>(new QueryTranslator(BaseUtils.crossJoin(mapKeys, joinImplement), mapValues), this));
    }

    public ParsedJoinQuery(Context iContext,Map<K,KeyExpr> iMapKeys,Map<V,SourceExpr> iProperties, Where iWhere) { // для groupQuery full join'ов
        mapKeys = iMapKeys;

        properties = iProperties;
        where = iWhere;

        context = iContext;
    }

    // для заданных свойств вытягивает условия на классы для них
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        Where upWhere = where.not();

        ClassWhere<B> result = new ClassWhere<B>();
        for(MapCase<? extends V> mapCase : CaseExpr.pullCases(BaseUtils.filterKeys(properties,classProps))) {
            Where caseWhere = mapCase.where.and(upWhere.not());
            for(AndExpr expr : mapCase.data.values()) // все следствия за and'им
                caseWhere = caseWhere.and(expr.getWhere());

            result = result.or(caseWhere.getClassWhere().get(BaseUtils.<B, AndExpr>forceMerge(mapCase.data, mapKeys)));

            upWhere = upWhere.or(mapCase.where);
        }
        return result;
    }

    @Lazy
    public SourceExpr getExpr(V property) {
        return properties.get(property).and(where);
    }

    public Collection<V> getProperties() {
        return properties.keySet();
    }

    public Where getWhere() {
        return where;
    }

}
