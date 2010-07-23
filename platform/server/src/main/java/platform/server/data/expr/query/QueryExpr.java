package platform.server.data.expr.query;

import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;
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
    protected QueryExpr(QueryExpr<K,I,J> queryExpr, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(queryExpr.getValues());

        if(mapValues.identity()) { // если все совпадает то и не перетранслируем внутри ничего
            query = queryExpr.query;
            group = translator.translateDirect(queryExpr.group);
        } else { // еще values перетранслируем
            MapTranslate valueTranslator = mapValues.mapKeys();
            query = queryExpr.query.translate(valueTranslator);
            group = new HashMap<K, BaseExpr>();
            for(Map.Entry<K, BaseExpr> keyJoin : queryExpr.group.entrySet())
                group.put((K)keyJoin.getKey().translate(valueTranslator),keyJoin.getValue().translate(translator));
        }

        assert checkExpr();        
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

    public void enumerate(ContextEnumerator enumerator) {
        enumerator.fill(group);
        for(ValueExpr value : getValues())
            enumerator.add(value);
    }

    public VariableExprSet getJoinFollows() {
        return InnerExpr.getExprFollows(group);
    }

    public boolean twins(AbstractSourceJoin obj) {
        QueryExpr<K,I,J> groupExpr = (QueryExpr)obj;

        assert hashCode()==groupExpr.hashCode();

        for(MapTranslate translator : new MapHashIterable(this, groupExpr, false))
            if(query.translate(translator).equals(groupExpr.query) &&
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
