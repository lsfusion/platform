package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.Pairs;
import platform.server.data.DataSource;
import platform.server.data.Source;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.*;

public class GroupQuery<B,K extends B,V extends B,F> extends DataSource<K,V> {

    public void fillJoinQueries(Set<JoinQuery> queries) {
        from.fillJoinQueries(queries);
    }

    public String getSource(SQLSyntax syntax, Map<ValueExpr, String> params) {
        
        // сделаем запрос который поставит фильтр на ключи и на properties на или ???
        // ключи не колышат
        Map<B,String> fromPropertySelect = new HashMap<B, String>();
        Collection<String> whereSelect = new ArrayList<String>();
        String fromSelect = propertiesFrom.compile(syntax).fillSelect(new HashMap<F, String>(),fromPropertySelect, whereSelect, params);

        String groupBy = "";
        Map<K,String> keySelect = new HashMap<K, String>();
        Map<V,String> propertySelect = new HashMap<V, String>();
        for(K key : keys) {
            String keyExpr = fromPropertySelect.get(key);
            keySelect.put(key, keyExpr);
            groupBy = (groupBy.length()==0?"":groupBy+",") + keyExpr;
        }
        for(Map.Entry<V,Integer> property : properties.entrySet())
            propertySelect.put(property.getKey(),(property.getValue()==0?"MAX":"SUM")+"("+ fromPropertySelect.get(property.getKey()) +")");

        return "(" + syntax.getSelect(fromSelect,stringExpr(
                mapNames(keySelect, keyNames,new ArrayList<K>()),
                mapNames(propertySelect, propertyNames,new ArrayList<V>())),
                stringWhere(whereSelect),"",groupBy,"") + ")";
    }

    public GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,V property,int iOperator) {
        this(iKeys,iFrom, Collections.singletonMap(property,iOperator));
    }

    Map<K,String> keyNames = new HashMap<K, String>();
    Map<V,String> propertyNames = new HashMap<V, String>();

    public String getKeyName(K Key) {
        return keyNames.get(Key);
    }

    String string = null;
    public String toString() {
/*        if(string==null) {
            Map<ValueExpr,String> valueParams = new HashMap<ValueExpr,String>();
            for(Map.Entry<ValueExpr,ValueExpr> value : getValues().entrySet())
                valueParams.put(value.getKey(),value.getValue().getString(Main.Adapter));
            string = getSource(Main.Adapter,valueParams);
        }
        // временно
        return string;*/

        return "GQ";
    }

    public String getPropertyName(V property) {
        return propertyNames.get(property);
    }

    // с хоть одним property where
    JoinQuery<F,B> propertiesFrom;

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom, Map<V,Integer> iProperties) {
        super(iKeys);
        from = iFrom;
        properties = iProperties;

        // докидываем условия на NotNull ключей
        int keyCount = 0;
        for(K key : keys) {
            from.and(from.properties.get(key).getWhere());
            keyNames.put(key,"dkey"+(keyCount++));
        }
        int propertyCount = 0;
        for(V property : properties.keySet())
            propertyNames.put(property,"dprop"+(propertyCount++));

        propertiesFrom = new JoinQuery<F,B>(from.keys);
        Join<F,B> joinFrom = new Join<F,B>(from,propertiesFrom);
        propertiesFrom.properties.putAll(joinFrom.exprs);
        Where propertiesWhere = Where.FALSE;
        for(V property : properties.keySet())
            propertiesWhere = propertiesWhere.or(joinFrom.exprs.get(property).getWhere());
        propertiesFrom.and(propertiesWhere);
    }

    JoinQuery<F,B> from;
    Map<V,Integer> properties;

    // заменяет параметры на другие
    public DataSource<K, V> translateValues(Map<ValueExpr, ValueExpr> mapValues) {
        return new GroupQuery<B,K,V,F>(keys,new JoinQuery<F,B>(from, BaseUtils.filter(mapValues,getValues().keySet())), properties);
    }
    
    public void parseJoin(Join<K, V> join, ExprTranslator translated, Collection<ParsedJoin> translatedJoins) {
        if(propertiesFrom.parse().isEmpty()) {
            for(Map.Entry<V, JoinExpr<K,V>> mapExpr : join.exprs.entrySet())
                translated.put(mapExpr.getValue(),mapExpr.getValue().getType().getExpr(null));
            translated.put(join.inJoin, Where.FALSE);
        } else
            super.parseJoin(join, translated, translatedJoins);
    }

    public <MK, MV> DataSource<K, Object> merge(DataSource<MK, MV> merge, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps) {
        // если Merge'ся From'ы
        DataSource<K, Object> superMerge = super.merge(merge, mergeKeys, mergeProps);
        if(superMerge!=null) return superMerge;

        if(!(merge instanceof GroupQuery)) return null;

//        return null;
        return proceedGroupMerge((GroupQuery) merge, mergeKeys, mergeProps);
    }

    <MB,MK extends MB,MV extends MB,MF> DataSource<K, Object> proceedGroupMerge(GroupQuery<MB,MK,MV,MF> mergeGroup, Map<K,MK> mergeKeys, Map<MV, Object> mergeProps) {
            // если Merge'ся From'ы

        if(keys.size()!= mergeGroup.keys.size()) return null;

//        if(1==1) return null;

        // вроде проверено достаточно эффективно работает - ключи должны совпадать
        if(from.parse().where.hash()!=mergeGroup.from.parse().where.hash()) return null;

        // проверим вот что у этого и сливаемого возьмем from'ы с ключами на не null про pars'им 

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        for(Map<F, MF> mapKeys : new Pairs<F,MF>(from.keys, mergeGroup.from.keys)) {
            Map<MB,Object> fromMergeProps = new HashMap<MB, Object>();
            JoinQuery<F, Object> mergedFrom = from.or(mergeGroup.from, mapKeys, fromMergeProps);
            // проверим что ключи совпали
            boolean keyMatch = true;
            for(K key : keys)
                keyMatch = keyMatch && key.equals(fromMergeProps.get(mergeKeys.get(key)));

            if(keyMatch) {
                Map<Object,Integer> mergedProperties = new HashMap<Object, Integer>(properties);
                for(Map.Entry<MV,Integer> mergeProp : mergeGroup.properties.entrySet()) {
                    Object mapProp = fromMergeProps.get(mergeProp.getKey());
                    mergedProperties.put(mapProp,mergeProp.getValue());
                    mergeProps.put(mergeProp.getKey(),mapProp);
                }

                return new GroupQuery<Object, K, Object, F>(keys, mergedFrom, mergedProperties);
            }
        }

        return null;
    }

    public Collection<V> getProperties() {
        return properties.keySet();
    }

    public Type getType(V property) {
        return from.properties.get(property).getType();
    }

    DataSource<K, V> mergeKeyValue(Map<K, ValueExpr> mergeKeys, Collection<K> parseKeys) {
        return new GroupQuery<B, K, V, F>(parseKeys,new JoinQuery<F,B>(mergeKeys, from), properties);
    }

    public Map<ValueExpr, ValueExpr> getValues() {
        return from.getValues();
    }

    // для кэша
    public <EK, EV> boolean equals(Source<EK, EV> source, Map<K, EK> mapKeys, Map<V, EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        return source instanceof GroupQuery && equalsGroup((GroupQuery) source, mapKeys, mapProperties, mapValues);
    }

    <EB,EK extends EB,EV extends EB,EF> boolean equalsGroup(GroupQuery<EB,EK,EV,EF> query,Map<K,EK> mapKeys,Map<V,EV> mapProperties,Map<ValueExpr, ValueExpr> mapExprs) {

        for(Map<F, EF> mapGroupKeys : new Pairs<F,EF>(from.keys, query.from.keys)) {
            Map<B,EB> mapGroupProperties = new HashMap<B, EB>();
            if(from.equals(query.from, mapGroupKeys, mapGroupProperties, mapExprs)) {
                // проверим что ключи совпали
                boolean equal = true;
                for(Map.Entry<K,EK> mapKey : mapKeys.entrySet())
                    if(mapKey.getValue()!=mapGroupProperties.get(mapKey.getKey())) {equal=false; break;}
                if(!equal) continue;

                // проверим что операторы св-в совпали
                for(Map.Entry<V,Integer> property : properties.entrySet())
                    if(!property.getValue().equals(query.properties.get(mapGroupProperties.get(property.getKey())))) {equal=false; break;}
                if(!equal) continue;

                // уже точно true, заполним карту
                for(V property : properties.keySet())
                    mapProperties.put(property,(EV)mapGroupProperties.get(property));

                return true;
            }
        }

        return false;
    }

    protected int getHash() {
        // должны совпасть источники, количество Properties, типы, кол-во ключей
        return from.hash()+ properties.values().hashCode()*31+ keys.size()*31*31;
    }

    public int hashProperty(V property) {
        return from.hashProperty(property)*31 + properties.get(property).hashCode();
    }
}
