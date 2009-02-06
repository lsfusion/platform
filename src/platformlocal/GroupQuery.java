package platformlocal;

import java.util.*;

class GroupQuery<B,K extends B,V extends B,F> extends DataSource<K,V> {

    void fillJoinQueries(Set<JoinQuery> Queries) {
        From.fillJoinQueries(Queries);
    }

    String getSource(SQLSyntax Syntax, Map<ValueExpr, String> Params) {
        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap<B, String>();
        Collection<String> WhereSelect = new ArrayList<String>();
        CompiledQuery<F,B> FromCompile = From.compile(Syntax);
        String FromSelect = FromCompile.fillSelect(new HashMap<F, String>(),FromPropertySelect,WhereSelect,Params);

        String GroupBy = "";
        Map<K,String> KeySelect = new HashMap<K, String>();
        Map<V,String> PropertySelect = new HashMap<V, String>();
        for(K Key : Keys) {
            String KeyExpr = FromPropertySelect.get(Key);
            KeySelect.put(Key,KeyExpr);
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
            if(!(From.Properties.get(Key) instanceof KeyExpr)) // Main.AllowNulls &&
                WhereSelect.add(KeyExpr + " IS NOT NULL");
        }
        for(Map.Entry<V,Integer> Property : Properties.entrySet())
            PropertySelect.put(Property.getKey(),(Property.getValue()==0?"MAX":"SUM")+"("+ FromPropertySelect.get(Property.getKey()) +")");

        return "(" + Syntax.getSelect(FromSelect,stringExpr(
                mapNames(KeySelect,KeyNames,new ArrayList<K>()),
                mapNames(PropertySelect,PropertyNames,new ArrayList<V>())),
                stringWhere(WhereSelect),"",GroupBy,"") + ")";
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,V Property,int iOperator) {
        this(iKeys,iFrom, Collections.singletonMap(Property,iOperator));
    }

    Map<K,String> KeyNames = new HashMap<K, String>();
    Map<V,String> PropertyNames = new HashMap<V, String>();

    String getKeyName(K Key) {
        return KeyNames.get(Key);
    }

    public String toString() {
        return "GQ";
    }

    String getPropertyName(V Property) {
        return PropertyNames.get(Property);
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom, Map<V,Integer> iProperties) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;

        // докидываем условия на NotNull ключей
        int KeyCount = 0;
        for(K Key : Keys) {
            From.and(From.Properties.get(Key).getWhere());
            KeyNames.put(Key,"dkey"+(KeyCount++));
        }
        int PropertyCount = 0;
        for(V Property : Properties.keySet()) {
            From.and(From.Properties.get(Property).getWhere());
            PropertyNames.put(Property,"dprop"+(PropertyCount++));
        }
    }

    JoinQuery<F,B> From;
    Map<V,Integer> Properties;

    // заменяет параметры на другие
    DataSource<K, V> translateValues(Map<ValueExpr, ValueExpr> MapValues) {
        return new GroupQuery<B,K,V,F>(Keys,new JoinQuery<F,B>(From, BaseUtils.filter(MapValues,getValues().keySet())),Properties);
    }
    
    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        if(From.parse().isEmpty()) {
            for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.Exprs.entrySet())
                Translated.put(MapExpr.getValue(),new NullExpr(MapExpr.getValue().getType()));
            Translated.put(Join.InJoin,new OrWhere());
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

    <MB,MK extends MB,MV extends MB,MF> DataSource<K, Object> proceedGroupMerge(GroupQuery<MB,MK,MV,MF> MergeGroup, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps) {
            // если Merge'ся From'ы

        if(Keys.size()!=MergeGroup.Keys.size()) return null;

        if(1==1) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<F, MF>> MapSet = MapBuilder.buildPairs(From.Keys, MergeGroup.From.Keys);
        if(MapSet==null) return null;

        for(Map<F, MF> MapKeys : MapSet) {
            Map<MB,Object> FromMergeProps = new HashMap<MB, Object>();
            JoinQuery<F, Object> MergedFrom = From.or(MergeGroup.From, MapKeys, FromMergeProps);
            // проверим что ключи совпали
            boolean KeyMatch = true;
            for(K Key : Keys)
                KeyMatch = KeyMatch && Key.equals(FromMergeProps.get(MergeKeys.get(Key)));

            if(KeyMatch) {
                Map<Object,Integer> MergedProperties = new HashMap<Object, Integer>(Properties);
                for(Map.Entry<MV,Integer> MergeProp : MergeGroup.Properties.entrySet()) {
                    Object MapProp = FromMergeProps.get(MergeProp.getKey());
                    MergedProperties.put(MapProp,MergeProp.getValue());
                    MergeProps.put(MergeProp.getKey(),MapProp);
                }

                return new GroupQuery<Object, K, Object, F>(Keys, MergedFrom, MergedProperties);
            }
        }

        return null;
    }

    Collection<V> getProperties() {
        return Properties.keySet();
    }

    Type getType(V Property) {
        return From.Properties.get(Property).getType();
    }

    DataSource<K, V> mergeKeyValue(Map<K, ValueExpr> MergeKeys, Collection<K> CompileKeys) {
        return new GroupQuery<B, K, V, F>(CompileKeys,new JoinQuery<F,B>(MergeKeys,From), Properties);
    }

    Map<ValueExpr,ValueExpr> getValues() {
        return From.getValues();
    }

    // для кэша
    <EK, EV> boolean equals(Source<EK, EV> Source, Map<K, EK> MapKeys, Map<V, EV> MapProperties, Map<ValueExpr, ValueExpr> MapValues) {
        return Source instanceof GroupQuery && equalsGroup((GroupQuery)Source,MapKeys,MapProperties, MapValues);
    }

    <EB,EK extends EB,EV extends EB,EF> boolean equalsGroup(GroupQuery<EB,EK,EV,EF> Query,Map<K,EK> MapKeys,Map<V,EV> MapProperties,Map<ValueExpr,ValueExpr> MapExprs) {

        Collection<Map<F,EF>> MapSet = MapBuilder.buildPairs(From.Keys, Query.From.Keys);
        if(MapSet==null) return false;

        for(Map<F, EF> MapGroupKeys : MapSet) {
            Map<B,EB> MapGroupProperties = new HashMap<B, EB>();
            if(From.equals(Query.From,MapGroupKeys,MapGroupProperties,MapExprs)) {
                // проверим что ключи совпали
                boolean Equal = true;
                for(Map.Entry<K,EK> MapKey : MapKeys.entrySet())
                    if(MapKey.getValue()!=MapGroupProperties.get(MapKey.getKey())) {Equal=false; break;}
                if(!Equal) continue;

                // проверим что операторы св-в совпали
                for(Map.Entry<V,Integer> Property : Properties.entrySet())
                    if(!Property.getValue().equals(Query.Properties.get(MapGroupProperties.get(Property.getKey())))) {Equal=false; break;}
                if(!Equal) continue;

                // уже точно true, заполним карту
                for(V Property : Properties.keySet())
                    MapProperties.put(Property,(EV)MapGroupProperties.get(Property));

                return true;
            }
        }

        return false;
    }

    int getHash() {
        // должны совпасть источники, количество Properties, типы, кол-во ключей
        return From.hash()+Properties.values().hashCode()*31+Keys.size()*31*31;
    }

    int hashProperty(V Property) {
        return From.hashProperty(Property)*31 + Properties.get(Property).hashCode();
    }
}
