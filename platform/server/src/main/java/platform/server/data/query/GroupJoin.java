package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.MapContext;
import platform.server.caches.MapHashIterable;
import platform.server.caches.MapParamsIterable;
import platform.server.caches.Lazy;
import platform.server.data.expr.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Immutable
public class GroupJoin extends GroupHashes implements MapContext, InnerJoin {
    private final Where where;
    private final InnerWhere innerWhere;

    final Map<BaseExpr, BaseExpr> group;

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    final Set<KeyExpr> keys;
    final Set<ValueExpr> values;

    public DataWhereSet getJoinFollows() {
        return MapExpr.getExprFollows(group);
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, KeyTranslator translator) {
        // надо еще транслировать "внутренние" values
        Map<ValueExpr, ValueExpr> mapValues = BaseUtils.filterKeys(translator.values, join.getValues());

        if(BaseUtils.identity(mapValues)) { // если все совпадает то и не перетранслируем внутри ничего
            where = join.where;
            innerWhere = join.innerWhere;
            group = translator.translateDirect(join.group);
        } else { // еще values перетранслируем
            KeyTranslator valueTranslator = new KeyTranslator(BaseUtils.toMap(join.getKeys()), mapValues);
            where = join.where.translateDirect(valueTranslator);
            innerWhere = join.innerWhere.translateDirect(valueTranslator);
            group = new HashMap<BaseExpr, BaseExpr>();
            for(Map.Entry<BaseExpr, BaseExpr> groupJoin : join.group.entrySet())
                group.put(groupJoin.getKey().translateDirect(valueTranslator),groupJoin.getValue().translateDirect(translator));
        }
        keys = join.keys;
        values = BaseUtils.reverse(mapValues).keySet(); 
    }

    public InnerJoin translateDirect(KeyTranslator translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Where where, InnerWhere innerWhere, Map<BaseExpr, BaseExpr> group, Set<KeyExpr> keys, Set<ValueExpr> values) {
        this.where = where;
        this.innerWhere = innerWhere;

        this.group = group;

        this.keys = keys;
        this.values = values;
    }

    public Set<KeyExpr> getKeys() {
        return keys;
    }

    public Set<ValueExpr> getValues() {
        return values;
    }

    public int hashValue(HashContext hashContext) {
        int hash = 0;
        for(KeyExpr key : keys)
            hash += key.hashContext(hashContext);
        for(ValueExpr key : values)
            hash += key.hashContext(hashContext);
        return (innerWhere.hashContext(hashContext) * 31 + where.hashContext(hashContext)) * 31 + hash;
    }

    protected Map<BaseExpr, BaseExpr> getGroup() {
        return group;
    }

    public KeyTranslator merge(GroupJoin groupJoin) {
        if(hashCode()!=groupJoin.hashCode())
            return null;

        for(KeyTranslator translator : new MapHashIterable(this,groupJoin,false))
            if(where.translateDirect(translator).equals(groupJoin.where) &&
               innerWhere.translateDirect(translator).equals(groupJoin.innerWhere) &&
               translator.translateDirect(BaseUtils.reverse(group)).equals(BaseUtils.reverse(groupJoin.group)))
                    return translator;
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GroupJoin && merge((GroupJoin) o)!=null;
    }

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = MapParamsIterable.hash(this,false);
            hashCoded = true;
        }
        return hashCode;
    }
}
