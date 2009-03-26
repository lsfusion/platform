package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.Pairs;
import platform.interop.Compare;
import platform.server.data.Source;
import platform.server.data.query.exprs.*;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.session.DataSession;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.*;

// запрос Join
public class JoinQuery<K,V> extends Source<K,V> {

    public JoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);

        mapKeys = new HashMap<K, KeyExpr>();
        for(K Key : keys)
            mapKeys.put(Key,new KeyExpr());
        properties = new HashMap<V, SourceExpr>();
    }
    public final Map<V, SourceExpr> properties;
    public Where where = Where.TRUE;

    final public Map<K,KeyExpr> mapKeys;

    public Collection<V> getProperties() {
        return properties.keySet();
    }

    public Type getType(V Property) {
        return properties.get(Property).getType();
    }

    private List<Join> joins = null;
    Map<ValueExpr, ValueExpr> values = null;
    
    private void fillJoins() {
        if(joins==null) {
            joins = new ArrayList<Join>();
            Set<ValueExpr> joinValues = new HashSet<ValueExpr>();
            for(SourceExpr property : properties.values())
                property.fillJoins(joins, joinValues);
            where.fillJoins(joins, joinValues);

            if(values ==null) {
                for(Join join : joins)
                    joinValues.addAll(join.source.getValues().keySet());
                values = BaseUtils.toMap(joinValues);
            }
        }
    }
    // скомпилированные св-ва
    List<Join> getJoins() {
        fillJoins();
        return joins;
    }
    public Map<ValueExpr, ValueExpr> getValues() {
        fillJoins();
        return values;
    }

    // перетранслирует
    public void parseJoin(Join<K, V> join, ExprTranslator translated, Collection<ParsedJoin> translatedJoins) {
        parse().parseJoin(join, translated, translatedJoins);
    }

    static <K> String stringOrder(List<K> sources, int offset, LinkedHashMap<K,Boolean> orders) {
        String orderString = "";
        for(Map.Entry<K,Boolean> order : orders.entrySet())
            orderString = (orderString.length()==0?"":orderString+",") + (sources.indexOf(order.getKey())+offset+1) + " " + (order.getValue()?"DESC":"ASC");
        return orderString;
    }

    public int hashProperty(V property) {
        return properties.get(property).hash();
    }

    public void and(Where addWhere) {
        where = where.and(addWhere);
    }

    public void putKeyWhere(Map<K,Integer> keyValues) {
        for(Map.Entry<K,Integer> mapKey : keyValues.entrySet())
            and(new CompareWhere(mapKeys.get(mapKey.getKey()),Type.object.getExpr(mapKey.getValue()), Compare.EQUALS));
    }

    public void fillJoinQueries(Set<JoinQuery> queries) {
        queries.add(this);
        for(Join join : getJoins())
            join.source.fillJoinQueries(queries);
    }

    private <CK,CV> ParsedQuery<CK,CV> cache(JoinQuery<CK,CV> query) {
        Map<CV,V> mapProps = new HashMap<CV,V>();
        for(Map<ValueExpr, ValueExpr> mapValues : new Pairs<ValueExpr, ValueExpr>(query.getValues().keySet(), getValues().keySet()))
            for(Map<CK, K> mapKeys : new Pairs<CK, K>(query.keys, keys))
                if(query.equals(this, mapKeys, mapProps, mapValues)) // нашли кэш
                    return new ParsedQuery<CK,CV>(parse,mapKeys,mapProps,mapValues);
        return null;
    }

    private static class ParseCaches extends ArrayList<JoinQuery> {
       <CK,CV> void cache(JoinQuery<CK,CV> query) {
            synchronized(this) {
                for(JoinQuery<?,?> cacheQuery : this) {
                    query.parse = cacheQuery.cache(query);
                    if(query.parse!=null) return;
                }
                query.parse = new ParsedQuery<CK,CV>(query);
                add(query);
            }
        }
    }
    
    final static Map<Integer, ParseCaches> cacheParse = new HashMap<Integer, ParseCaches>();
    
    private ParsedQuery<K,V> parse = null;
    ParsedQuery<K,V> parse() {
        if(parse!=null) return parse;

        ParseCaches hashCaches;
        synchronized(cacheParse) {
            hashCaches = cacheParse.get(hash());
            if(hashCaches==null) {
                hashCaches = new ParseCaches();
                cacheParse.put(hash(), hashCaches);
            }
        }
        hashCaches.cache(this);
        return parse;
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return parse().compileSelect(syntax);
    }
    CompiledQuery<K,V> compile(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int selectTop) {
        return parse().compileSelect(syntax,orders,selectTop);
    }

    <MK, MV> JoinQuery<K, Object> or(JoinQuery<MK, MV> toMergeQuery, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps) {

//        throw new RuntimeException("not supported");
        // результат
        JoinQuery<K,Object> mergeQuery = new JoinQuery<K,Object>(keys);

        // себя Join'им
        Join<K,Object> join = new Join<K,Object>((Source<K,Object>)this,mergeQuery);
        mergeQuery.properties.putAll(join.exprs);

        // Merge запрос Join'им, надо пересоздать объекты и построить карту
        Join<MK,MV> mergeJoin = new Join<MK,MV>(toMergeQuery,mergeQuery, mergeKeys);
        for(Map.Entry<MV, JoinExpr<MK,MV>> mergeExpr : mergeJoin.exprs.entrySet()) {
            Object mergeObject = new Object();
            mergeProps.put(mergeExpr.getKey(),mergeObject);
            mergeQuery.properties.put(mergeObject, mergeExpr.getValue());
        }

        // закинем их на Or
        mergeQuery.and(join.inJoin.or(mergeJoin.inJoin));

        Map<Object, SourceExpr> parsedProps = mergeQuery.parse().getPackedProperties();

        Collection<Object> removeProps = new ArrayList<Object>();
        // погнали сливать одинаковые (именно из раных)
        for(Map.Entry<MV,Object> mergeProperty : mergeProps.entrySet()) {
            SourceExpr mergeExpr = parsedProps.get(mergeProperty.getValue());
            for(V property : properties.keySet()) {
                if(parsedProps.get(property).equals(mergeExpr)) {
                    removeProps.add(mergeProperty.getValue());
                    mergeProperty.setValue(property);
                    break;
                }
            }
        }
        return new JoinQuery<K, Object>(mergeQuery,removeProps);
    }

    public static <V> LinkedHashMap<V,Boolean> reverseOrder(LinkedHashMap<V,Boolean> orders) {
        LinkedHashMap<V,Boolean> result = new LinkedHashMap<V, Boolean>();
        for(Map.Entry<V,Boolean> order : orders.entrySet())
            result.put(order.getKey(),!order.getValue());
        return result;
    }

    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession session) throws SQLException {
        return compile(session.syntax).executeSelect(session,false);
    }
    public LinkedHashMap<Map<K, Integer>, Map<V, Object>> executeSelect(DataSession session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        return compile(session.syntax,orders,selectTop).executeSelect(session,false);
    }

    public void outSelect(DataSession session) throws SQLException {
        compile(session.syntax).outSelect(session);
    }
    public void outSelect(DataSession session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        compile(session.syntax,orders,selectTop).outSelect(session);
    }

    String string = null;
    public String toString() {
/*        if(string==null) {
            try {
                string = compile(Main.Adapter).getSelect(Main.Adapter);
            } catch (Exception e) {
                string = e.getMessage();
            }
        }
        // временно
        return string;*/

        return "JQ";
    }

    // для кэша - возвращает выражения + where чтобы потом еще Orders сравнить
    <EK,EV> boolean equals(JoinQuery<EK, EV> query, Map<K, EK> equalKeys, Map<V, EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        if(this== query) {
            if(BaseUtils.identity(equalKeys) && BaseUtils.identity(mapValues)) {
                for(V property : properties.keySet())
                    mapProperties.put(property, (EV) property);
                return true;
            } else
                return false;
        }

        if(properties.size()!= query.properties.size()) return false;

        Map<ValueExpr,ValueExpr> mapJoinValues = BaseUtils.crossJoin(getValues(), query.getValues(), mapValues);
        if(mapJoinValues.values().contains(null)) return false;

        Map<KeyExpr,KeyExpr> mapJoinKeys = BaseUtils.crossJoin(mapKeys, query.mapKeys, equalKeys);

        // бежим по всем Join'ам, пытаемся промаппить их друг на друга
        List<Join> joins = getJoins();
        List<Join> queryJoins = query.getJoins();

        if(joins.size()!=queryJoins.size())
            return false;

        // строим hash
        Map<Integer,Collection<Join>> hashQueryJoins = new HashMap<Integer,Collection<Join>>();
        for(Join join : queryJoins) {
            Integer hash = join.hash();
            Collection<Join> hashJoins = hashQueryJoins.get(hash);
            if(hashJoins==null) {
                hashJoins = new ArrayList<Join>();
                hashQueryJoins.put(hash,hashJoins);
            }
            hashJoins.add(join);
        }

        // бежим
        MapJoinEquals mapJoinEquals = new MapJoinEquals();
        for(Join join : joins) {
            Collection<Join> hashJoins = hashQueryJoins.get(join.hash());
            if(hashJoins==null) return false;
            boolean atLeastOneEquals = false;
            for(Join hashJoin : hashJoins)
                atLeastOneEquals = join.equals(hashJoin, mapJoinValues, mapJoinKeys, mapJoinEquals) || atLeastOneEquals;
            if(!atLeastOneEquals) return false;
        }

        if(!where.equals(query.where, mapJoinValues, mapJoinKeys, mapJoinEquals)) return false;

        // свойства проверим
        Map<EV,V> queryMap = new HashMap<EV, V>();
        for(Map.Entry<V, SourceExpr> mapProperty : properties.entrySet()) {
            EV mapQuery = null;
            for(Map.Entry<EV, SourceExpr> mapQueryProperty : query.properties.entrySet())
                if(!queryMap.containsKey(mapQueryProperty.getKey()) &&
                    mapProperty.getValue().equals(mapQueryProperty.getValue(), mapJoinValues, mapJoinKeys, mapJoinEquals)) {
                    mapQuery = mapQueryProperty.getKey();
                    break;
                }
            if(mapQuery==null) return false;
            queryMap.put(mapQuery,mapProperty.getKey());
        }

        // составляем карту возврата
        for(Map.Entry<EV,V> mapQuery : queryMap.entrySet())
            mapProperties.put(mapQuery.getValue(),mapQuery.getKey());
        return true;
    }

    class EqualCache<EK,EV> {
        JoinQuery<EK,EV> query;
        Map<K,EK> mapKeys;
        Map<ValueExpr, ValueExpr> mapValues;

        EqualCache(JoinQuery<EK, EV> iQuery, Map<K, EK> iMapKeys, Map<ValueExpr, ValueExpr> iMapValues) {
            query = iQuery;
            mapKeys = iMapKeys;
            mapValues = iMapValues;
        }

        public boolean equals(Object o) {
            return this==o || (o instanceof EqualCache && query.equals(((EqualCache)o).query) && mapKeys.equals(((EqualCache)o).mapKeys) && mapValues.equals(((EqualCache)o).mapValues));
        }

        public int hashCode() {
            return mapValues.hashCode()*31*31+ mapKeys.hashCode()*31+ query.hashCode();
        }
    }
    
    // кэш для сравнений и метод собсно тоже для кэша
    Map<EqualCache,Map<V,?>> cacheEquals = new HashMap<EqualCache, Map<V,?>>();
    public <EK,EV> boolean equals(Source<EK,EV> source,Map<K,EK> equalKeys,Map<V,EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        if(!(source instanceof JoinQuery)) return false;

        JoinQuery<EK,EV> query = (JoinQuery<EK,EV>) source;
        
        EqualCache<EK,EV> equalCache = new EqualCache<EK,EV>(query, equalKeys, mapValues);
        Map<V,EV> equalMap = (Map<V,EV>) cacheEquals.get(equalCache);
        if(equalMap==null) {
            equalMap = new HashMap<V, EV>();
            if(!equals(query, equalKeys, equalMap, mapValues))
                return false;
            cacheEquals.put(equalCache,equalMap);
        }

        mapProperties.putAll(equalMap);
        return true;
    }

    protected int getHash() {
        // должны совпасть hash'и properties и hash'и wheres
        int hash = 0;
        for(SourceExpr property : properties.values())
            hash += property.hash();
        return where.hash()*31+hash;
    }

    // конструктор копирования
    public JoinQuery(JoinQuery<K,V> query) {
        super(query.keys);
        mapKeys = query.mapKeys;
        properties = new HashMap<V, SourceExpr>(query.properties);
        where = query.where;
        values = query.values;
    }

    // конструктор фиксации переменных
    JoinQuery(Map<? extends V, ValueExpr> propertyValues,JoinQuery<K,V> query) {
        this(query.keys);

        Join<K,V> sourceJoin = new Join<K,V>(query,this);
        where = sourceJoin.inJoin;

        // раскидываем по dumb'ам или на выход
        for(Map.Entry<V, JoinExpr<K,V>> property : sourceJoin.exprs.entrySet()) {
            ValueExpr propertyValue = propertyValues.get(property.getKey());
            if(propertyValue==null)
                properties.put(property.getKey(), property.getValue());
            else
                where = where.and(new CompareWhere(property.getValue(),propertyValue, Compare.EQUALS));
        }
    }

    // конструктор трансляции переменных
    JoinQuery(JoinQuery<K,V> query,Map<ValueExpr, ValueExpr> mapValues) {
        super(query.keys);
        mapKeys = query.mapKeys;

        properties = query.properties;
        where = query.where;
        values = BaseUtils.join(mapValues, query.getValues());

        if(query.parse !=null)
            parse = new ParsedQuery<K,V>(query.parse, mapValues);
    }

    // конструктор вырезания свойств
    JoinQuery(JoinQuery<K,V> query,Collection<V> removeProperties) {
        super(query.keys);
        mapKeys = query.mapKeys;

        properties = BaseUtils.removeKeys(query.properties,removeProperties);
        where = query.where;
        values = query.values;

        if(query.parse !=null)
            parse = new ParsedQuery<K,V>(query.parse, removeProperties);
    }
}

