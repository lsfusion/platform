package platform.server.data.expr.query;

import platform.server.data.expr.*;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.query.HashContext;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.caches.MapContext;
import platform.server.caches.ParamLazy;
import platform.server.caches.MapHashIterable;
import platform.server.caches.Lazy;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public abstract class QueryExpr<K extends BaseExpr,I extends TranslateContext<I>,J extends QueryJoin> extends InnerExpr implements MapContext {

    public I query;
    Map<K, BaseExpr> group; // вообще гря не reverseable

    protected QueryExpr(I query, Map<K, BaseExpr> group) {
        this.query = query;
        this.group = group;

        assert checkExpr();
    }

    protected boolean checkExpr() {
        return true;
    }

    // трансляция
    protected QueryExpr(QueryExpr<K,I,J> queryExpr, KeyTranslator translator) {
        // надо еще транслировать "внутренние" values
        Map<ValueExpr, ValueExpr> mapValues = BaseUtils.filterKeys(translator.values, queryExpr.getValues());

        if(BaseUtils.identity(mapValues)) { // если все совпадает то и не перетранслируем внутри ничего
            query = queryExpr.query;
            group = translator.translateDirect(queryExpr.group);
        } else { // еще values перетранслируем
            KeyTranslator valueTranslator = new KeyTranslator(BaseUtils.toMap(queryExpr.getKeys()), mapValues);
            query = queryExpr.query.translateDirect(valueTranslator);
            group = new HashMap<K, BaseExpr>();
            for(Map.Entry<K, BaseExpr> keyJoin : queryExpr.group.entrySet())
                group.put((K)keyJoin.getKey().translateDirect(valueTranslator),keyJoin.getValue().translateDirect(translator));
        }

        assert checkExpr();        
    }

    protected abstract Expr create(Map<K,BaseExpr> group, I expr);

    // трансляция не прямая
    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<K> mapCase : CaseExpr.pullCases(translator.translate(group)))
            result.add(mapCase.where, create(mapCase.data, query));
        return result.getExpr();
    }

    // извращенное множественное наследование
    private QueryHashes<K> hashes = new QueryHashes<K>() {
        protected int hashValue(HashContext hashContext) {
            return query.hashContext(hashContext);
        }
        protected Map<K, BaseExpr> getGroup() {
            return group;
        }
    };
    public int hashContext(final HashContext hashContext) {
        return hashes.hashContext(hashContext);
    }
    public int hash(HashContext hashContext) {
        return hashes.hash(hashContext);
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.fill(group);
        for(ValueExpr value : getValues())
            enumerator.add(value);
    }

    public boolean twins(AbstractSourceJoin obj) {
        QueryExpr<K,I,J> groupExpr = (QueryExpr)obj;

        assert hashCode()==groupExpr.hashCode();

        for(KeyTranslator translator : new MapHashIterable(this, groupExpr, false))
            if(query.translateDirect(translator).equals(groupExpr.query) &&
                    translator.translateKeys(group).equals(groupExpr.group)) // нельзя reverse'ить
                return true;
        return false;
    }

    public abstract J getGroupJoin();

    public J getFJGroup() {
        return getGroupJoin();
    }

    protected static <I extends TranslateContext<I>,K extends BaseExpr> Set<KeyExpr> getKeys(I expr, Map<K, BaseExpr> group) {
        return enumKeys(group.keySet(),expr.getEnum());
    }

    @Lazy
    public Set<KeyExpr> getKeys() {
        return getKeys(query, group);
    }

    @Lazy
    public Set<ValueExpr> getValues() {
        return enumValues(group.keySet(), query.getEnum());
    }
}
