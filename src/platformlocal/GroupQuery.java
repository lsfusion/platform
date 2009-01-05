package platformlocal;

import java.util.*;

class GroupQuery<B,K extends B,V extends B,F> extends DataSource<K,V> {

    String getSource(SQLSyntax Syntax) {
        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap<B, String>();
        Collection<String> WhereSelect = new ArrayList<String>();
        String FromSelect = From.fillSelect(new HashMap<F, String>(),FromPropertySelect,WhereSelect, Syntax);

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

        return "(" + Syntax.getSelect(FromSelect,stringExpr(KeySelect,new ArrayList<K>(),PropertySelect,new ArrayList<V>()),stringWhere(WhereSelect),"",GroupBy,0) + ")";
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom,V Property,int iOperator) {
        this(iKeys,iFrom, Collections.singletonMap(Property,iOperator));
    }

    GroupQuery(Collection<? extends K> iKeys,JoinQuery<F,B> iFrom, Map<V,Integer> iProperties) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;

        // докидываем условия на NotNull ключей
        for(K Key : Keys)
            From.and(From.Properties.get(Key).getWhere());
        for(V Property : Properties.keySet())
            From.and(From.Properties.get(Property).getWhere());
    }

    JoinQuery<F,B> From;
    Map<V,Integer> Properties;

    void compileJoin(Join<K, V> Join, ExprTranslator Translated, Collection<CompiledJoin> TranslatedJoins) {
        // если заведомо нету записей
        if(From.compile().isEmpty()) {
            for(Map.Entry<V,JoinExpr<K,V>> MapExpr : Join.Exprs.entrySet())
                Translated.put(MapExpr.getValue(),new ValueExpr(null,MapExpr.getValue().getType()));
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

    DataSource<K, ? extends Object> mergeKeyValue(Map<K, ValueExpr> MergeKeys, Collection<K> CompileKeys) {

        JoinQuery<F,B> Query = new JoinQuery<F,B>(From.Keys);

        Join<F,B> SourceJoin = new Join<F,B>(From,Query);
        Query.and(SourceJoin.InJoin);

        Map<B,SourceExpr> DumbMap = new HashMap();
        // раскидываем по dumb'ам или на выход
        for(Map.Entry<B,JoinExpr<F,B>> Property : SourceJoin.Exprs.entrySet()) {
            ValueExpr MergeValue = MergeKeys.get(Property.getKey());
            if(MergeValue==null)
                Query.Properties.put(Property.getKey(), Property.getValue());
            else
                Query.and(new CompareWhere(Property.getValue(),MergeValue,CompareWhere.EQUALS));
        }
        return new GroupQuery<B, K, V, F>(CompileKeys, Query, Properties);
    }

    // для кэша
    <EK, EV> boolean equals(Source<EK, EV> Source, Map<K, EK> MapKeys, Map<V, EV> MapProperties, Map<ObjectExpr, ObjectExpr> MapValues) {
        if(!(Source instanceof GroupQuery)) return false;

        return equalsGroup((GroupQuery)Source,MapKeys,MapProperties, MapValues);
    }

    <EB,EK extends EB,EV extends EB,EF> boolean equalsGroup(GroupQuery<EB,EK,EV,EF> Query,Map<K,EK> MapKeys,Map<V,EV> MapProperties,Map<ObjectExpr,ObjectExpr> MapExprs) {

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

    int hash() {
        // должны совпасть источники, количество Properties, типы, кол-во ключей
        return From.hash()+Properties.values().hashCode()*31+Keys.size()*31*31;
    }
}
