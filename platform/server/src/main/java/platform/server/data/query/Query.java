package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.base.OrderedMap;
import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.Lazy;
import platform.server.caches.MapContext;
import platform.server.caches.HashContext;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.Type;
import platform.server.data.type.Reader;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.data.where.Where;

import java.sql.SQLException;
import java.util.*;

// запрос JoinSelect
@Immutable
public class Query<K,V> implements MapKeysInterface<K>, MapContext {

    public final Map<K,KeyExpr> mapKeys;
    public Map<V, Expr> properties;
    public Where where;

    public Query(Map<K,KeyExpr> mapKeys) {
        this.mapKeys = mapKeys;
        properties = new HashMap<V, Expr>();
        where = Where.TRUE;
    }

    public Query(Collection<K> keys) {
        this(KeyExpr.getMapKeys(keys));
    }

    public Query(Map<K,KeyExpr> mapKeys,Map<V, Expr> properties,Where where) {
        this.mapKeys = mapKeys;
        this.properties = properties;
        this.where = where;
    }

    public Query(Map<K, KeyExpr> mapKeys, Map<V, Expr> properties) {
        this.mapKeys = mapKeys;
        this.properties = properties;

        where = Where.FALSE;
        for(Map.Entry<V, Expr> property : properties.entrySet())
            where = where.or(property.getValue().getWhere());
    }

    public Query(Map<K, KeyExpr> mapKeys, Expr property, V value) {
        this(mapKeys,Collections.singletonMap(value,property));
    }

    public Query(Map<K, KeyExpr> mapKeys, Expr property, V value, Where where) {
        this(mapKeys,Collections.singletonMap(value,property),where);
    }

    public Query(Map<K,KeyExpr> mapKeys,Where where) {
        this.mapKeys = mapKeys;
        properties = new HashMap<V, Expr>();
        this.where = where;
    }

    public Query(MapKeysInterface<K> mapInterface) {
        this(mapInterface.getMapKeys());
    }

    public Map<K, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public Type getKeyType(K key) {
        return mapKeys.get(key).getType(where);
    }

    public Set<KeyExpr> getKeys() {
        return new HashSet<KeyExpr>(mapKeys.values());
    }

    @Lazy
    public Set<ValueExpr> getValues() {
        return AbstractSourceJoin.enumValues(properties.values(),where);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return join(joinImplement, BaseUtils.toMap(getValues())); //parse().join(joinImplement);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, Map<ValueExpr,ValueExpr> mapValues) {
        return parse().join(joinImplement, mapValues);
    }

    static <K> String stringOrder(List<K> sources, int offset, OrderedMap<K,Boolean> orders) {
        String orderString = "";
        for(Map.Entry<K,Boolean> order : orders.entrySet())
            orderString = (orderString.length()==0?"":orderString+",") + (sources.indexOf(order.getKey())+offset+1) + " " + (order.getValue()?"DESC NULLS LAST":"ASC NULLS FIRST");
        return orderString;
    }

    public void and(Where addWhere) {
        where = where.and(addWhere);
    }

    public void putKeyWhere(Map<K, DataObject> keyValues) {
        for(Map.Entry<K,DataObject> mapKey : keyValues.entrySet())
            and(mapKeys.get(mapKey.getKey()).compare(mapKey.getValue(),Compare.EQUALS));
    }

    public ParsedQuery<K,V> parse() { // именно ParsedQuery потому как aspect'ами корректируется
        return new ParsedJoinQuery<K,V>(this);
    }

    @Lazy
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> properties) {
        return parse().getClassWhere(properties);
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return compile(syntax, new OrderedMap<V, Boolean>(), 0);
    }
    CompiledQuery<K,V> compile(SQLSyntax syntax,OrderedMap<V,Boolean> orders,int selectTop) {
        return parse().compileSelect(syntax,orders,selectTop);
    }

    public static <V> OrderedMap<V,Boolean> reverseOrder(OrderedMap<V,Boolean> orders) {
        OrderedMap<V,Boolean> result = new OrderedMap<V, Boolean>();
        for(Map.Entry<V,Boolean> order : orders.entrySet())
            result.put(order.getKey(),!order.getValue());
        return result;
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session) throws SQLException {
        return execute(session,new OrderedMap<V, Boolean>(),0);
    }
    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session,OrderedMap<V,Boolean> orders,int selectTop) throws SQLException {
        return compile(session.syntax,orders,selectTop).execute(session,false);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return executeClasses(session, new OrderedMap<V, Boolean>(), 0, baseClass);
    }

    static class ReadClasses<T> {
        Map<T,DataClass> mapDataClasses = new HashMap<T, DataClass>();
        Map<T,Object> mapObjectClasses = new HashMap<T, Object>();
        Set<T> setNulls = new HashSet<T>();

        BaseClass baseClass;

        ReadClasses(Map<T,? extends Expr> map, Query<?,Object> query,BaseClass iBaseClass) {
            baseClass = iBaseClass;
            for(Map.Entry<T,? extends Expr> expr : map.entrySet()) {
                Reader reader = expr.getValue().getReader(query.where);
                if(reader instanceof Type) {
                    if(reader instanceof DataClass)
                        mapDataClasses.put(expr.getKey(),(DataClass)reader);
                    else {
                        Object propertyClass = new Object();
                        mapObjectClasses.put(expr.getKey(),propertyClass);
                        query.properties.put(propertyClass,expr.getValue().classExpr(baseClass));
                    }
                } else
                    setNulls.add(expr.getKey());
            }
        }

        ObjectValue read(T key,Object value,Map<Object,Object> classes) {
            if(setNulls.contains(key)) return NullValue.instance;
            ConcreteClass propertyClass = mapDataClasses.get(key);
            if(propertyClass==null) propertyClass = baseClass.findConcreteClassID((Integer) classes.get(mapObjectClasses.get(key)));
            return ObjectValue.getValue(value,propertyClass);
        }
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session,OrderedMap<V,Boolean> orders,int selectTop, BaseClass baseClass) throws SQLException {
        OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> result = new OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>>();

        if(where.isFalse()) return result; // иначе типы ключей не узнаем

        // создаем запрос с IsClassExpr'ами
        Query<K,Object> classQuery = new Query<K,Object>((Query<K,Object>) this);

        ReadClasses<K> keyClasses = new ReadClasses<K>(mapKeys,classQuery,baseClass);
        ReadClasses<V> propClasses = new ReadClasses<V>(properties,classQuery,baseClass);

        OrderedMap<Map<K, Object>, Map<Object, Object>> rows = classQuery.execute(session, (OrderedMap<Object,Boolean>) orders,selectTop);

        // перемаппим
        for(Map.Entry<Map<K,Object>,Map<Object,Object>> row : rows.entrySet()) {
            Map<K,DataObject> keyResult = new HashMap<K, DataObject>();
            for(Map.Entry<K,Object> keyRow : row.getKey().entrySet())
                keyResult.put(keyRow.getKey(), (DataObject) keyClasses.read(keyRow.getKey(),keyRow.getValue(),row.getValue()));
            Map<V,ObjectValue> propResult = new HashMap<V, ObjectValue>();
            for(V property : properties.keySet())
                propResult.put(property,propClasses.read(property,row.getValue().get(property),row.getValue()));
            result.put(keyResult,propResult);
        }
        return result;
    }

    public void outSelect(SQLSession session) throws SQLException {
        compile(session.syntax).outSelect(session);
    }
    public void outSelect(SQLSession session,OrderedMap<V,Boolean> orders,int selectTop) throws SQLException {
        compile(session.syntax,orders,selectTop).outSelect(session);
    }

    public String toString() {
        return "JQ";
    }

    // конструктор копирования
    public Query(Query<K, V> query) {
        mapKeys = query.mapKeys;
        properties = new HashMap<V, Expr>(query.properties);
        where = query.where;
    }

    public int hash(HashContext hashContext) {
        int hash = 0;
        for(Expr property : properties.values())
            hash += property.hashContext(hashContext);
        return where.hashContext(hashContext) * 31 + hash;
    }
}

