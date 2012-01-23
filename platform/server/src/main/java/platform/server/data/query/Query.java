package platform.server.data.query;

import org.apache.xpath.operations.Mult;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

// запрос JoinSelect
public class Query<K,V> extends AbstractInnerContext<Query<K,V>> implements MapKeysInterface<K> {

    public final Map<K,KeyExpr> mapKeys;
    public Map<V, Expr> properties;
    public Where where;

    public Query(Map<K,KeyExpr> mapKeys) {
        this(mapKeys, Where.TRUE);
    }

    public Query(Collection<K> keys) {
        this(KeyExpr.getMapKeys(keys));
    }

    public Query(Map<K,KeyExpr> mapKeys,Map<V, Expr> properties,Where where) {
        this.mapKeys = mapKeys;
        this.properties = properties;
        this.where = where;
    }

    public <MK,MV> Query(Query<MK, MV> query, Map<K, MK> mapK, Map<V, MV> mapV) {
        this(BaseUtils.join(mapK, query.mapKeys), BaseUtils.join(mapV, query.properties), query.where);
    }

    public Query(Map<K, KeyExpr> mapKeys, Map<V, Expr> properties) {
        this(mapKeys, properties, Expr.getOrWhere(properties.values()));
    }

    public Query(Map<K, KeyExpr> mapKeys, Expr property, V value) {
        this(mapKeys,Collections.singletonMap(value,property));
    }

    public Query(Map<K, KeyExpr> mapKeys, Expr property, V value, Where where) {
        this(mapKeys,Collections.singletonMap(value,property),where);
    }

    public Query(Map<K,KeyExpr> mapKeys,Where where) {
        this(mapKeys, new HashMap<V, Expr>(), where);
    }

    public Query(MapKeysInterface<K> mapInterface) {
        this(mapInterface.getMapKeys());
    }

    public Map<K, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public boolean isEmpty() {
        return where.isFalse();
    }

    public Type getKeyType(K key) {
        return mapKeys.get(key).getType(where);
    }

    public Type getPropertyType(V property) {
        return properties.get(property).getType(where);
    }

    public QuickSet<KeyExpr> getKeys() {
        return new QuickSet<KeyExpr>(mapKeys.values());
    }

    public QuickSet<Value> getValues() {
        return AbstractOuterContext.getOuterValues(properties.values()).merge(where.getOuterValues());
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return join(joinImplement, MapValuesTranslator.noTranslate);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) {
        assert joinImplement.size()==mapKeys.size();
        return parse().join(joinImplement, mapValues);
    }
    
    public Expr getExpr(V property) {
        return parse().getExpr(property);
    }

    public static <K> String stringOrder(List<K> sources, int offset, OrderedMap<K,Boolean> orders, SQLSyntax syntax) {
        OrderedMap<String, Boolean> orderSources = new OrderedMap<String, Boolean>();
        for(Map.Entry<K,Boolean> order : orders.entrySet())
            orderSources.put(((Integer)(sources.indexOf(order.getKey())+offset+1)).toString(),order.getValue());
        return stringOrder(orderSources, syntax);
    }

    public static String stringOrder(OrderedMap<String,Boolean> orders, SQLSyntax syntax) {
        String orderString = "";
        for(Map.Entry<String,Boolean> order : orders.entrySet())
            orderString = (orderString.length()==0?"":orderString+",") + order.getKey() + " " + syntax.getOrderDirection(order.getValue());
        return orderString;
    }

    public void and(Where addWhere) {
        where = where.and(addWhere);
    }

    public void putKeyWhere(Map<K, DataObject> keyValues) {
        for(Map.Entry<K,DataObject> mapKey : keyValues.entrySet())
            and(mapKeys.get(mapKey.getKey()).compare(mapKey.getValue(),Compare.EQUALS));
    }

    @Message("message.core.query.parse")
    public ParsedQuery<K,V> parse() { // именно ParsedQuery потому как aspect'ами корректируется
        return new ParsedJoinQuery<K,V>(this);
    }

    @IdentityLazy
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> properties) {
        return parse().getClassWhere(properties);
    }

    public Query<K,V> pullValues(Map<K, Expr> pullKeys, Map<V, Expr> pullProps) throws SQLException {
        Query<K, V> result = parse().pullValues(pullKeys, pullProps);
        if(pullKeys.isEmpty() && pullProps.isEmpty())
            return this;
        else
            return result;
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return compile(syntax, new OrderedMap<V, Boolean>(), 0);
    }
    public CompiledQuery<K,V> compile(SQLSyntax syntax,OrderedMap<V,Boolean> orders,int selectTop) {
        return compile(syntax, orders, selectTop, "");
    }
    public CompiledQuery<K,V> compile(SQLSyntax syntax, String prefix) {
        return compile(syntax, new OrderedMap<V, Boolean>(), 0, prefix);
    }
    public CompiledQuery<K,V> compile(SQLSyntax syntax, OrderedMap<V, Boolean> orders, int selectTop, String prefix) {
        return parse().compileSelect(syntax,orders,selectTop,prefix);
    }

    public static <V> OrderedMap<V,Boolean> reverseOrder(OrderedMap<V,Boolean> orders) {
        OrderedMap<V,Boolean> result = new OrderedMap<V, Boolean>();
        for(Map.Entry<V,Boolean> order : orders.entrySet())
            result.put(order.getKey(),!order.getValue());
        return result;
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session) throws SQLException {
        return execute(session, QueryEnvironment.empty);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session, OrderedMap<V, Boolean> orders) throws SQLException {
        return execute(session, orders, 0, QueryEnvironment.empty);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(DataSession session) throws SQLException {
        return execute(session.sql, session.env);
    }

    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session, QueryEnvironment env) throws SQLException {
        return execute(session,new OrderedMap<V, Boolean>(),0, env);
    }

    @Message("message.query.execute")
    public OrderedMap<Map<K, Object>, Map<V, Object>> execute(SQLSession session, OrderedMap<V, Boolean> orders, int selectTop, QueryEnvironment env) throws SQLException {
        return compile(session.syntax, orders, selectTop).execute(session, env);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return executeClasses(session, new OrderedMap<V, Boolean>(), 0, baseClass, QueryEnvironment.empty);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(DataSession session) throws SQLException {
        return executeClasses(session, session.baseClass);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(DataSession session, BaseClass baseClass) throws SQLException {
        return executeClasses(session.sql, session.env, baseClass);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException {
        return executeClasses(session, new OrderedMap<V, Boolean>(), 0, baseClass, env);
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, OrderedMap<? extends Expr, Boolean> orders) throws SQLException {
        OrderedMap<Object, Boolean> orderProperties = new OrderedMap<Object, Boolean>();
        Query<K,Object> orderQuery = new Query<K,Object>((Query<K,Object>) this);
        for(Map.Entry<? extends Expr,Boolean> order : orders.entrySet()) {
            Object orderProperty = new Object();
            orderQuery.properties.put(orderProperty, order.getKey());
            orderProperties.put(orderProperty, order.getValue());
        }

        OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> result = new OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>>();
        for(Map.Entry<Map<K, DataObject>,Map<Object, ObjectValue>> orderRow : orderQuery.executeClasses(session, orderProperties, 0, baseClass, env).entrySet())
            result.put(orderRow.getKey(), BaseUtils.filterKeys(orderRow.getValue(), properties.keySet()));
        return result;
    }

    public OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> executeClasses(SQLSession session, OrderedMap<? extends V, Boolean> orders, int selectTop, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>> result = new OrderedMap<Map<K, DataObject>, Map<V, ObjectValue>>();

        if(where.isFalse()) return result; // иначе типы ключей не узнаем

        // создаем запрос с IsClassExpr'ами
        Query<K,Object> classQuery = new Query<K,Object>((Query<K,Object>) this);

        for(KeyExpr expr : mapKeys.values())
            expr.getReader(classQuery.where).prepareClassesQuery(expr, classQuery, baseClass);
        for(Expr expr : properties.values())
            expr.getReader(classQuery.where).prepareClassesQuery(expr, classQuery, baseClass);

        OrderedMap<Map<K, Object>, Map<Object, Object>> rows = classQuery.execute(session, (OrderedMap<Object,Boolean>) orders,selectTop, env);

        // перемаппим
        for(Map.Entry<Map<K,Object>,Map<Object,Object>> row : rows.entrySet()) {
            Map<K,DataObject> keyResult = new HashMap<K, DataObject>();
            for(Map.Entry<K,KeyExpr> mapKey : mapKeys.entrySet())
                keyResult.put(mapKey.getKey(), new DataObject(row.getKey().get(mapKey.getKey()),mapKey.getValue().getReader(classQuery.where).readClass(mapKey.getValue(), row.getValue(),baseClass,classQuery.where)));

            Map<V,ObjectValue> propResult = new HashMap<V, ObjectValue>();
            for(Map.Entry<V,Expr> property : properties.entrySet())
                propResult.put(property.getKey(),ObjectValue.getValue(row.getValue().get(property.getKey()),property.getValue().getReader(classQuery.where).readClass(property.getValue(), row.getValue(),baseClass,classQuery.where)));
            result.put(keyResult,propResult);
        }
        return result;
    }

    public void outSelect(SQLSession session) throws SQLException {
        outSelect(session, QueryEnvironment.empty);
    }
    public void outSelect(SQLSession session, QueryEnvironment env) throws SQLException {
        compile(session.syntax).outSelect(session, env);
    }
    public void outSelect(SQLSession session, QueryEnvironment env, OrderedMap<V,Boolean> orders,int selectTop) throws SQLException {
        compile(session.syntax,orders,selectTop).outSelect(session, env);
    }

    public String toString() {
        return "JQ";
    }

    // конструктор копирования
    public Query(Query<K, V> query) {
        this(query.mapKeys, new HashMap<V, Expr>(query.properties), query.where);
    }

    protected Query<K, V> translate(MapTranslate translate) {
        return new Query<K,V>(translate.translateKey(mapKeys), translate.translate(properties), where.translateOuter(translate));
    }
    public Query<K, V> translateInner(MapTranslate translate) {
        return (Query<K, V>) aspectTranslate(translate);
    }

    protected boolean isComplex() {
        return true;
    }
    public static class MultiParamsContext<K,V> extends AbstractInnerContext<MultiParamsContext<?,?>> {

        private final Query<K,V> thisObj;
        public MultiParamsContext(Query<K, V> thisObj) {
            this.thisObj = thisObj;
        }

        protected QuickSet<KeyExpr> getKeys() {
            return thisObj.getInnerKeys();
        }
        public QuickSet<Value> getValues() {
            return thisObj.getInnerValues();
        }
        protected MultiParamsContext translate(MapTranslate translator) {
            return thisObj.translateInner(translator).getMultiParamsContext();
        }
        public Query<K,V> getQuery() {
            return thisObj;
        }
        protected boolean isComplex() {
            return true;
        }

        protected int hash(HashContext hash) {
            return thisObj.where.hashOuter(hash) * 31 + AbstractSourceJoin.hashOuter(thisObj.properties.values(), hash);
        }
        public boolean equalsInner(MultiParamsContext object) {
            return BaseUtils.hashEquals(thisObj.where,object.getQuery().where) && BaseUtils.hashEquals(BaseUtils.multiSet(thisObj.properties.values()),BaseUtils.multiSet(object.getQuery().properties.values()));
        }
    }
    private MultiParamsContext<K,V> multiParamsContext;
    public MultiParamsContext<K,V> getMultiParamsContext() {
        if(multiParamsContext==null)
            multiParamsContext = new MultiParamsContext<K,V>(this);
        return multiParamsContext;
    }

    public int hash(HashContext hashContext) {
        return 31 * (where.hashOuter(hashContext) * 31 + AbstractSourceJoin.hashOuter(properties, hashContext)) + AbstractSourceJoin.hashOuter(mapKeys, hashContext);
    }

    public boolean equalsInner(Query<K, V> object) { // нужно проверить что совпадут
        return BaseUtils.hashEquals(where, object.where) && BaseUtils.hashEquals(properties, object.properties) && BaseUtils.hashEquals(mapKeys, object.mapKeys);
    }
}

