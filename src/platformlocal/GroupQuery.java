package platformlocal;

import java.util.*;

class GroupQuery<B,K extends B,V extends B,F> extends DataSource<K,V> {

    void fillJoinQueries(Set<JoinQuery> Queries) {
        from.fillJoinQueries(Queries);
    }

    String getSource(SQLSyntax Syntax, Map<ValueExpr, String> Params) {
        
        // сделаем запрос который поставит фильтр на ключи и на properties на или ???
        JoinQuery<F,B> propertiesFrom = new JoinQuery<F,B>(keysFrom.keys);
        Join<F,B> joinFrom = new Join<F,B>(keysFrom,propertiesFrom);
        propertiesFrom.properties.putAll(joinFrom.exprs);
        Where propertiesWhere = Where.FALSE;
        for(V property : properties.keySet())
            propertiesWhere = propertiesWhere.or(joinFrom.exprs.get(property).getWhere());
        propertiesFrom.and(propertiesWhere);

        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap<B, String>();
        Collection<String> WhereSelect = new ArrayList<String>();
        String FromSelect = propertiesFrom.compile(Syntax).fillSelect(new HashMap<F, String>(),FromPropertySelect,WhereSelect,Params);

        String GroupBy = "";
        Map<K,String> KeySelect = new HashMap<K, String>();
        Map<V,String> PropertySelect = new HashMap<V, String>();
        for(K Key : keys) {
            String KeyExpr = FromPropertySelect.get(Key);
            KeySelect.put(Key,KeyExpr);
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
//            if(!(from.properties.get(Key) instanceof KeyExpr)) // Main.AllowNulls &&
//                WhereSelect.add(KeyExpr + " IS NOT NULL");
        }
        for(Map.Entry<V,Integer> Property : properties.entrySet())
            PropertySelect.put(Property.getKey(),(Property.getValue()==0?"MAX":"SUM")+"("+ FromPropertySelect.get(Property.getKey()) +")");

        return "(" + Syntax.getSelect(FromSelect,stringExpr(
                mapNames(KeySelect, keyNames,new ArrayList<K>()),
                mapNames(PropertySelect, propertyNames,new ArrayList<V>())),
                stringWhere(WhereSelect),"",GroupBy,"") + ")";
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,V Property,int iOperator) {
        this(iKeys,iFrom, Collections.singletonMap(Property,iOperator));
    }

    Map<K,String> keyNames = new HashMap<K, String>();
    Map<V,String> propertyNames = new HashMap<V, String>();

    String getKeyName(K Key) {
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

    String getPropertyName(V Property) {
        return propertyNames.get(Property);
    }

    // not null ключи
    JoinQuery<F,B> keysFrom;

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom, Map<V,Integer> iProperties) {
        super(iKeys);
        from = iFrom;
        properties = iProperties;

        keysFrom = new JoinQuery<F,B>(from.keys);
        Join<F,B> joinFrom = new Join<F,B>(from,keysFrom);
        keysFrom.properties.putAll(joinFrom.exprs);
        // докидываем условия на NotNull ключей
        int keyCount = 0;
        for(K key : keys) {
            keysFrom.and(joinFrom.exprs.get(key).getWhere());
            keyNames.put(key,"dkey"+(keyCount++));
        }
        int propertyCount = 0;
        for(V property : properties.keySet())
            propertyNames.put(property,"dprop"+(propertyCount++));
    }

    JoinQuery<F,B> from;
    Map<V,Integer> properties;

    // заменяет параметры на другие
    DataSource<K, V> translateValues(Map<ValueExpr, ValueExpr> MapValues) {
        return new GroupQuery<B,K,V,F>(keys,new JoinQuery<F,B>(from, BaseUtils.filter(MapValues,getValues().keySet())), properties);
    }
    
    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        if(from.parse().isEmpty()) {
            for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.exprs.entrySet())
                Translated.put(MapExpr.getValue(),new NullExpr(MapExpr.getValue().getType()));
            Translated.put(Join.inJoin,Where.FALSE);
        } else
            super.compileJoin(Join, Translated, TranslatedJoins);
    }

    <MK, MV> DataSource<K, Object> merge(DataSource<MK, MV> Merge, Map<K, MK> MergeKeys, Map<MV, Object> MergeProps) {
        // если Merge'ся From'ы
        DataSource<K, Object> SuperMerge = super.merge(Merge, MergeKeys, MergeProps);
        if(SuperMerge!=null) return SuperMerge;

        if(!(Merge instanceof GroupQuery)) return null;

//        return null;
        return proceedGroupMerge((GroupQuery)Merge,MergeKeys,MergeProps);
    }

    <MB,MK extends MB,MV extends MB,MF> DataSource<K, Object> proceedGroupMerge(GroupQuery<MB,MK,MV,MF> mergeGroup, Map<K,MK> mergeKeys, Map<MV, Object> mergeProps) {
            // если Merge'ся From'ы

        if(keys.size()!= mergeGroup.keys.size()) return null;

//        if(1==1) return null;

        // вроде проверено достаточно эффективно работает - ключи должны совпадать
        if(keysFrom.parse().where.hash()!=mergeGroup.keysFrom.parse().where.hash()) return null;

        // проверим вот что у этого и сливаемого возьмем from'ы с ключами на не null про pars'им 

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<F, MF>> mapSet = MapBuilder.buildPairs(from.keys, mergeGroup.from.keys);
        if(mapSet==null) return null;

        for(Map<F, MF> mapKeys : mapSet) {
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

    Collection<V> getProperties() {
        return properties.keySet();
    }

    Type getType(V Property) {
        return from.properties.get(Property).getType();
    }

    DataSource<K, V> mergeKeyValue(Map<K, ValueExpr> MergeKeys, Collection<K> CompileKeys) {
        return new GroupQuery<B, K, V, F>(CompileKeys,new JoinQuery<F,B>(MergeKeys, from), properties);
    }

    Map<ValueExpr,ValueExpr> getValues() {
        return from.getValues();
    }

    // для кэша
    <EK, EV> boolean equals(Source<EK, EV> Source, Map<K, EK> MapKeys, Map<V, EV> MapProperties, Map<ValueExpr, ValueExpr> MapValues) {
        return Source instanceof GroupQuery && equalsGroup((GroupQuery)Source,MapKeys,MapProperties, MapValues);
    }

    <EB,EK extends EB,EV extends EB,EF> boolean equalsGroup(GroupQuery<EB,EK,EV,EF> Query,Map<K,EK> MapKeys,Map<V,EV> MapProperties,Map<ValueExpr,ValueExpr> MapExprs) {

        Collection<Map<F,EF>> MapSet = MapBuilder.buildPairs(from.keys, Query.from.keys);
        if(MapSet==null) return false;

        for(Map<F, EF> MapGroupKeys : MapSet) {
            Map<B,EB> MapGroupProperties = new HashMap<B, EB>();
            if(from.equals(Query.from,MapGroupKeys,MapGroupProperties,MapExprs)) {
                // проверим что ключи совпали
                boolean Equal = true;
                for(Map.Entry<K,EK> MapKey : MapKeys.entrySet())
                    if(MapKey.getValue()!=MapGroupProperties.get(MapKey.getKey())) {Equal=false; break;}
                if(!Equal) continue;

                // проверим что операторы св-в совпали
                for(Map.Entry<V,Integer> Property : properties.entrySet())
                    if(!Property.getValue().equals(Query.properties.get(MapGroupProperties.get(Property.getKey())))) {Equal=false; break;}
                if(!Equal) continue;

                // уже точно true, заполним карту
                for(V Property : properties.keySet())
                    MapProperties.put(Property,(EV)MapGroupProperties.get(Property));

                return true;
            }
        }

        return false;
    }

    int getHash() {
        // должны совпасть источники, количество Properties, типы, кол-во ключей
        return from.hash()+ properties.values().hashCode()*31+ keys.size()*31*31;
    }

    int hashProperty(V Property) {
        return from.hashProperty(Property)*31 + properties.get(Property).hashCode();
    }
}
