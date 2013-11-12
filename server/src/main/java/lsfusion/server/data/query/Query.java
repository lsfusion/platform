package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.col.lru.LRUWVWSMap;
import lsfusion.server.Message;
import lsfusion.server.caches.*;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.OrderClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.query.innerjoins.GroupJoinsWhere;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.*;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

// запрос JoinSelect
public class Query<K,V> extends IQuery<K,V> {

    public final ImRevMap<K,KeyExpr> mapKeys;
    public ImMap<V, Expr> properties;
    public Where where;

    public Query(ImRevMap<K,KeyExpr> mapKeys) {
        this(mapKeys, Where.TRUE);
    }

    public Query(ImSet<K> keys) {
        this(KeyExpr.getMapKeys(keys));
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys, Where where, ImMap<K, DataObject> mapValues, ImMap<V, Expr> properties) {
        this(mapKeys, properties, where.and(CompareWhere.compareValues(mapKeys.filterInclRev(mapValues.keys()), mapValues)));

        assert mapKeys.keys().containsAll(mapValues.keys());
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys,ImMap<V, Expr> properties,Where where) {
        this.mapKeys = mapKeys;
        this.properties = properties;
        this.where = where;
    }

    public <MK,MV> Query(Query<MK, MV> query, ImRevMap<K, MK> mapK, ImRevMap<V, MV> mapV) {
        this(mapK.join(query.mapKeys), mapV.join(query.properties), query.where);
    }

    public Query(ImRevMap<K, KeyExpr> mapKeys, ImMap<V, Expr> properties) {
        this(mapKeys, properties, Expr.getOrWhere(properties.values()));
    }

    public Query(ImRevMap<K, KeyExpr> mapKeys, Expr property, V value) {
        this(mapKeys, MapFact.<V, Expr>singleton(value, property));
    }

    public Query(ImRevMap<K, KeyExpr> mapKeys, Expr property, V value, Where where) {
        this(mapKeys, MapFact.<V, Expr>singleton(value, property),where);
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys,Where where) {
        this(mapKeys, MapFact.<V, Expr>EMPTY(), where);
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys,Where where, ImMap<K, DataObject> mapValues) {
        this(mapKeys, where, mapValues, MapFact.<V, Expr>EMPTY());
    }

    public ImRevMap<K, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public Expr getExpr(V property) {
        return properties.get(property);
    }

    public Type getKeyType(K key) {
        return mapKeys.get(key).getType(where);
    }

    public Type getPropertyType(V property) {
        return properties.get(property).getType(where);
    }

    public ImSet<ParamExpr> getKeys() {
        return BaseUtils.immutableCast(mapKeys.valuesSet());
    }

    public ImSet<Value> getValues() {
        return AbstractOuterContext.getOuterColValues(properties.values()).merge(where.getOuterValues());
    }

    public ImSet<StaticValueExpr> getInnerStaticValues() { // можно было бы вынести в общий интерфейс InnerContext, но нужен только для компиляции запросов
        return AbstractOuterContext.getOuterStaticValues(properties.values()).merge(where.getOuterStaticValues());
    }

    public Where getWhere() {
        return where;
    }

    public ImSet<V> getProperties() {
        return properties.keys();
    }

    private Join<V> join;
    private Join<V> getJoin() {
        if(join==null) {
            join = new AbstractJoin<V>() {
                public Expr getExpr(V property) {
                    return properties.get(property).and(where);
                }
                public Where getWhere() {
                    return where;
                }
                public ImSet<V> getProperties() {
                    return properties.keys();
                }
                public Join<V> translateRemoveValues(MapValuesTranslate translate) {
                    return ((Query<K,V>)Query.this.translateRemoveValues(translate)).getJoin();
                }
            };
        }
        return join;
    }

    public static <K> ImRevMap<K, KeyExpr> getMapKeys(ImMap<K, ? extends Expr> joinImplement) {
        MAddSet<KeyExpr> checked = SetFact.mAddSet();
        for(int i=0,size=joinImplement.size();i<size;i++) {
            Expr joinExpr = joinImplement.getValue(i);
            if(!(joinExpr instanceof KeyExpr && !(joinExpr instanceof PullExpr)) || checked.contains((KeyExpr) joinExpr))
                return null;
            checked.add((KeyExpr) joinExpr);
        }
        return (ImRevMap<K, KeyExpr>) joinImplement.toRevExclMap();
    }
    
    public Join<V> join(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) {
        assert joinImplement.size()== mapKeys.size();
        assert mapValues.assertValuesContains(getInnerValues()); // все должны быть параметры
        ImRevMap<K, KeyExpr> joinKeys = getMapKeys(joinImplement);
        if(joinKeys==null)
            return joinExprs(joinImplement, mapValues);
        else
            return new MapJoin<V>(new MapTranslator(BaseUtils.<ImRevMap<ParamExpr, ParamExpr>>immutableCast(mapKeys.crossJoin(joinKeys)), mapValues), getJoin());
    }

    @ContextTwin
    public Join<V> joinExprs(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) { // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся
        assert joinImplement.size()== mapKeys.size();

        Join<V> join = getJoin();

        // сначала map'им значения
        join = new MapJoin<V>(mapValues, join);

        // затем делаем подстановку
        join = new QueryTranslateJoin<V>(new QueryTranslator(mapKeys.crossJoin(joinImplement)), join);

        // затем закидываем Where что все implement не null
        join = join.and(Expr.getWhere(joinImplement));

        return join;
    }

    public static <K> String stringOrder(final ImOrderSet<K> sourcesOrder, final int offset, ImOrderMap<K, CompileOrder> orders, final ImMap<K, String> sources, SQLSyntax syntax, final Result<Boolean> needSources) {
        needSources.set(false);

        ImRevMap<K, String> orderNumbers = orders.getMap().mapRevValues(new GetKeyValue<String, K, CompileOrder>() {
            public String getMapValue(K key, CompileOrder value) {
                if (value.reader instanceof OrderClass) {
                    needSources.set(true);
                    return sources.get(key);
                } else
                    return ((Integer) (sourcesOrder.indexOf(key) + offset + 1)).toString();
            }});
        ImOrderMap<String, CompileOrder> orderSources = orders.map(orderNumbers);
        return stringOrder(orderSources, syntax);
    }

    private static ImOrderMap<String, CompileOrder> compileOrders(ImOrderMap<String,CompileOrder> orders) {
        MOrderExclMap<String, CompileOrder> mResult = MapFact.mOrderExclMap();
        for(int i=0,size=orders.size();i<size;i++) {
            String key = orders.getKey(i);
            CompileOrder compileOrder = orders.getValue(i);
            ImOrderMap<String, CompileOrder> compiledOrder;
            if(compileOrder.reader instanceof OrderClass && (compiledOrder = ((OrderClass)compileOrder.reader).getCompileOrders(key, compileOrder)) != null)
                mResult.exclAddAll(compiledOrder);
            else
                mResult.exclAdd(key, compileOrder);
        }
        return mResult.immutableOrder();
    }
    public static String stringOrder(ImOrderMap<String,CompileOrder> orders, SQLSyntax syntax) {
        orders = compileOrders(orders);

        String orderString = "";
        for(int i=0,size=orders.size();i<size;i++) {
            CompileOrder compileOrder = orders.getValue(i);
            orderString = (orderString.length()==0?"":orderString+",") + orders.getKey(i) + " " + syntax.getOrderDirection(compileOrder.desc, compileOrder.notNull);
        }
        return orderString;
    }

    public Query(Query<K,V> query, boolean pack) {
        mapKeys = query.mapKeys;

        where = query.where.pack();
        properties = where.followTrue(query.properties, true);
    }

    @ContextTwin
    public IQuery<K,V> calculatePack() {
        return new Query<K,V>(this, true);
    }

    protected long calculateComplexity(boolean outer) {
        return AbstractOuterContext.getComplexity(properties.values(), outer) + where.getComplexity(outer);
    }

    @IdentityLazy
    @Pack
    public <B> ClassWhere<B> getClassWhere(ImSet<? extends V> classProps) {
        return (ClassWhere<B>) getClassWhere(where, mapKeys, properties.filterIncl(classProps));
    }

    public static <B, K extends B, V extends B> ClassWhere<B> getClassWhere(Where where, final ImMap<K, ? extends BaseExpr> mapKeys, ImMap<V, Expr> mapProps) {
        return new ExclPullWheres<ClassWhere<B>, V, Where>() {
            protected ClassWhere<B> initEmpty() {
                return ClassWhere.FALSE();
            }
            protected ClassWhere<B> proceedBase(Where data, ImMap<V, BaseExpr> map) {
                return (ClassWhere<B>)(ClassWhere<?>)getClassWhereBase(data, mapKeys, map);
            }
            protected ClassWhere<B> add(ClassWhere<B> op1, ClassWhere<B> op2) {
                return op1.or(op2);
            }
        }.proceed(where, mapProps);
    }

    private static <B, K extends B, V extends B> ClassWhere<B> getClassWhereBase(Where where, ImMap<K, ? extends BaseExpr> mapKeys, ImMap<V, BaseExpr> mapProps) {
        return getClassWhereBase(where.and(Expr.getWhere(mapProps.values())), MapFact.<B, BaseExpr>addExcl(mapProps, mapKeys));
    }

    private static <B> ClassWhere<B> getClassWhereBase(Where where, ImMap<B, BaseExpr> mapExprs) {
        return where.getClassWhere().get(mapExprs);
    }

    private static <K> ImMap<K, Expr> pullValues(ImMap<K, ? extends Expr> map, Where where) {
        ImMap<BaseExpr, BaseExpr> exprValues = where.getExprValues();
        ImFilterValueMap<K, Expr> mResultMap = map.mapFilterValues();
        for(int i=0,size=map.size();i<size;i++) {
            Expr expr = map.getValue(i);
            Expr exprValue = exprValues.getObject(expr);
            if(exprValue==null && expr.isValue())
                exprValue = expr;
            if(exprValue!=null)
                mResultMap.mapValue(i, exprValue);
        }
        return mResultMap.immutableValue();
    }

    // жестковатая эвристика, но не страшно
    @SynchronizedLazy
    @Pack
    public PullValues<K, V> pullValues() {
        ImMap<K, Expr> pullKeys = pullValues(mapKeys, where);

        QueryTranslator keyTranslator = new PartialQueryTranslator(mapKeys.rightCrossJoin(pullKeys), true);
        Where transWhere = where.translateQuery(keyTranslator);
        ImMap<V, Expr> transProps = keyTranslator.translate(properties);

        ImMap<V, Expr> pullProps = pullValues(transProps, transWhere);
        if(pullKeys.isEmpty() && pullProps.isEmpty())
            return new PullValues<K, V>(this);

        return new PullValues<K, V>(new Query<K,V>(mapKeys.removeRev(pullKeys.keys()),
                transProps.remove(pullProps.keys()), transWhere), pullKeys, pullProps);
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax, SubQueryContext subcontext) {
        return compile(syntax, MapFact.<V, Boolean>EMPTYORDER(), 0, subcontext);
    }
    public CompiledQuery<K,V> compile(SQLSyntax syntax, SubQueryContext subcontext, boolean recursive) {
        return compile(syntax, MapFact.<V, Boolean>EMPTYORDER(), 0, subcontext, recursive);
    }
    @SynchronizedLazy
    @Pack
    @Message("message.core.query.compile")
    public CompiledQuery<K,V> compile(SQLSyntax syntax, ImOrderMap<V, Boolean> orders, Integer selectTop, SubQueryContext subcontext, boolean noExclusive) {
        return new CompiledQuery<K,V>(this, syntax, orders, selectTop, subcontext, noExclusive);
    }

    @SynchronizedLazy
    @Pack
    public ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders) {
        return getPackedCompileOrders(orders);
    }

    public ImOrderMap<V, CompileOrder> getPackedCompileOrders(ImOrderMap<V, Boolean> orders) {
        return CompiledQuery.getPackedCompileOrders(properties, where, orders);
    }

    public ImCol<GroupJoinsWhere> getWhereJoins(boolean tryExclusive, Result<Boolean> isExclusive, ImOrderSet<Expr> orderTop) {
        Pair<ImCol<GroupJoinsWhere>,Boolean> whereJoinsExcl = where.getPackWhereJoins(tryExclusive, getKeys(), orderTop);
        isExclusive.set(whereJoinsExcl.second);
        return whereJoinsExcl.first;
    }

    public static <V> ImOrderMap<V,Boolean> reverseOrder(ImOrderMap<V,Boolean> orders) {
        ImOrderValueMap<V,Boolean> result = orders.mapItOrderValues();
        for(int i=0,size=orders.size();i<size;i++)
            result.mapValue(i,!orders.getValue(i));
        return result.immutableValueOrder();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session) throws SQLException {
        return execute(session, QueryEnvironment.empty);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders) throws SQLException {
        return execute(session, orders, 0, QueryEnvironment.empty);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session) throws SQLException {
        return execute(session, MapFact.<V, Boolean>EMPTYORDER(), 0);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context) throws SQLException {
        return execute(context.getEnv());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionEnvironment env) throws SQLException {
        DataSession session = env.getSession();
        return execute(session.sql, env.getQueryEnv());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form) throws SQLException {
        return execute(form, MapFact.<V, Boolean>EMPTYORDER(), 0);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, QueryEnvironment env) throws SQLException {
        return execute(session, MapFact.<V, Boolean>EMPTYORDER(), 0, env);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException {
        return execute(session.sql, orders,selectTop, session.env);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException {
        return execute(form.session.sql, orders, selectTop, form.getQueryEnv());
    }

    private ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeSingle() { // оптимизация
        if(where.isFalse())
            return MapFact.EMPTYORDER(); // иначе типы ключей не узнаем

        ImMap<BaseExpr, BaseExpr> exprValues = where.getOnlyExprValues();
        if(exprValues==null || exprValues.size()!= mapKeys.size())
            return null;

        ImValueMap<K, DataObject> mvKeyValues = mapKeys.mapItValues(); // return
        for(int i=0,size=mapKeys.size();i<size;i++) {
            BaseExpr keyValue = exprValues.get(mapKeys.getValue(i));
            ObjectValue objectValue;
            if(keyValue!=null && (objectValue = keyValue.getObjectValue()) instanceof DataObject)
                mvKeyValues.mapValue(i, (DataObject) objectValue);
            else
                return null;
        }
        ImValueMap<V, ObjectValue> mvPropValues = properties.mapItValues(); // return
        for(int i=0,size=properties.size();i<size;i++) {
            ObjectValue objectValue; BaseExpr propValue;
            Expr propExpr = properties.getValue(i);
            if((objectValue = propExpr.getObjectValue())!=null ||
                    ((propValue = exprValues.getObject(propExpr))!=null && (objectValue = propValue.getObjectValue())!=null))
                mvPropValues.mapValue(i, objectValue);
            else
                return null;
        }
        return MapFact.singletonOrder(mvKeyValues.immutableValue(), mvPropValues.immutableValue());
    }

    private final static GetValue<ImMap<Object, Object>, ImMap<Object, ObjectValue>> getMapDataObjectValues = new GetValue<ImMap<Object, Object>, ImMap<Object, ObjectValue>>() {
        public ImMap<Object, Object> getMapValue(ImMap<Object, ObjectValue> key) {
            return ObjectValue.getMapValues(key);
        }};
    private static <K, D extends ObjectValue> GetValue<ImMap<K, Object>, ImMap<K, D>> getMapDataObjectValues() {
        return BaseUtils.immutableCast(getMapDataObjectValues);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders, int selectTop, QueryEnvironment env) throws SQLException {
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> singleResult = executeSingle(); // оптимизация
        if(singleResult!=null)
            return singleResult.mapOrderKeyValues(Query.<K, DataObject>getMapDataObjectValues(), Query.<V, ObjectValue>getMapDataObjectValues());

        return executeSQL(session, orders, selectTop, env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return executeClasses(session, QueryEnvironment.empty, baseClass);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session) throws SQLException {
        return executeClasses(session.sql, session.env, session.baseClass);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException {
        return executeClasses(session, MapFact.<V, Boolean>EMPTYORDER(), 0, baseClass, env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, ImOrderMap<? extends Expr, Boolean> orders) throws SQLException {
        if(orders.isEmpty())
            return executeClasses(session, env, baseClass);

        ImRevMap<Object, Expr> orderObjects = ((ImOrderMap<Expr, Boolean>)orders).keys().mapRevKeys(new GetStaticValue<Object>() {
            public Object getMapValue() {
                return new Object();
            }});

        Query<K, Object> orderQuery = new Query<K, Object>(mapKeys, MapFact.addExcl(properties, orderObjects), where);
        ImOrderMap<Object, Boolean> orderProperties = ((ImOrderMap<Expr, Boolean>)orders).map(orderObjects.reverse());

        return orderQuery.executeClasses(session, orderProperties, 0, baseClass, env).mapOrderValues(new GetValue<ImMap<V, ObjectValue>, ImMap<Object, ObjectValue>>() {
            public ImMap<V, ObjectValue> getMapValue(ImMap<Object, ObjectValue> value) {
                return value.filterIncl(properties.keys());
            }});
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context) throws SQLException {
        return executeClasses(context, MapFact.<V, Boolean>EMPTYORDER());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context, ImOrderMap<? extends V, Boolean> orders) throws SQLException {
        return executeClasses(context.getEnv(), orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException {
        return executeClasses(env, MapFact.<V, Boolean>EMPTYORDER());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env, ImOrderMap<? extends V, Boolean> orders) throws SQLException {
        DataSession session = env.getSession();
        return executeClasses(session.sql, orders, 0, session.baseClass, env.getQueryEnv());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ImOrderMap<? extends Expr, Boolean> orders, ExecutionEnvironment env) throws SQLException {
        DataSession session = env.getSession();
        return executeClasses(session.sql, env.getQueryEnv(), session.baseClass, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(FormInstance formInstance, BaseClass baseClass) throws SQLException {
        return executeClasses(formInstance.session.sql, MapFact.<V, Boolean>EMPTYORDER(), 0, formInstance.session.baseClass, formInstance.getQueryEnv());
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, int selectTop, final BaseClass baseClass, QueryEnvironment env) throws SQLException {
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> singleResult = executeSingle(); // оптимизация
        if(singleResult!=null)
            return singleResult;

        return executeSQLClasses(getClassQuery(baseClass), session, (ImOrderMap<Object, Boolean>) orders, selectTop, baseClass, env);
    }

    private static <K, V> ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeSQLClasses(final Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> classQuery, SQLSession session, ImOrderMap<Object, Boolean> orders, int selectTop, final BaseClass baseClass, QueryEnvironment env) throws SQLException {
        ImOrderMap<ImMap<K, Object>, ImMap<Object, Object>> rows = classQuery.first.executeSQL(session, orders, selectTop, env);

        // перемаппим
        final KeyType keyType = classQuery.first.getWhere();
        final ImRevMap<K, KeyExpr> mapKeys = classQuery.first.getMapKeys();
        final ImSet<V> props = (ImSet<V>) classQuery.first.getProperties().remove(classQuery.second.valuesSet());
        return rows.mapOrderKeyValues(new GetKeyValue<ImMap<K, DataObject>, ImMap<K, Object>, ImMap<Object, Object>>() {
            public ImMap<K, DataObject> getMapValue(final ImMap<K, Object> rowKey, final ImMap<Object, Object> rowValue) {
                final ImMap<Expr, Object> exprValues = classQuery.second.join(rowValue);
                return mapKeys.mapValues(new GetKeyValue<DataObject, K, KeyExpr>() {
                    public DataObject getMapValue(K key, KeyExpr keyExpr) {
                        return new DataObject(rowKey.get(key), keyExpr.getReader(keyType).readClass(keyExpr, exprValues, baseClass, keyType));
                    }
                });
            }}, new GetValue<ImMap<V, ObjectValue>, ImMap<Object, Object>>() {
            public ImMap<V, ObjectValue> getMapValue(final ImMap<Object, Object> value) {
                final ImMap<Expr, Object> exprValues = classQuery.second.join(value);
                return props.mapValues(new GetValue<ObjectValue, V>() {
                    public ObjectValue getMapValue(V prop) {
                        Expr expr = classQuery.first.getExpr(prop);
                        return ObjectValue.getValue(value.get(prop), expr.getReader(keyType).readClass(expr, exprValues, baseClass, keyType));
                    }
                });}});
    }

    // создаем запрос с IsClassExpr'ами
    @IdentityLazy
    @Pack
    public Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> getClassQuery(final BaseClass baseClass) {
        MSet<Expr> mReadExprs = SetFact.mSet();
        for(KeyExpr expr : mapKeys.valueIt())
            expr.getReader(where).prepareClassesQuery(expr, where, mReadExprs, baseClass);
        for(Expr expr : properties.valueIt())
            expr.getReader(where).prepareClassesQuery(expr, where, mReadExprs, baseClass);
        ImSet<Expr> readExprs = mReadExprs.immutable();
        final ImRevMap<Expr, Object> objects = BaseUtils.generateObjects(readExprs);
        return new Pair<IQuery<K, Object>, ImRevMap<Expr, Object>>(new Query<K, Object>(mapKeys, MapFact.addExcl(properties, objects.reverse().mapValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.classExpr(baseClass);
            }
        })), where), objects);
    }

    public void outClassesSelect(SQLSession session, BaseClass baseClass) throws SQLException {
        // выведем на экран
        session.outStatement = true;
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> result = executeClasses(session, baseClass);
        session.outStatement = false;

        for(int i=0,size=result.size();i<size;i++) {
            ImMap<K, DataObject> rowKey = result.getKey(i);
            for(int j=0,sizeJ=rowKey.size();j<sizeJ;j++) {
                System.out.println(rowKey.getKey(j)+"-"+rowKey.getValue(j));
            }
            System.out.println("---- ");
            ImMap<V, ObjectValue> rowValue = result.getValue(i);
            for(int j=0,sizeJ=rowValue.size();j<sizeJ;j++) {
                System.out.println(rowValue.getKey(j)+"-"+rowValue.getValue(j));
            }
        }
    }

    public String toString() {
        return "JQ";
    }

    protected boolean isComplex() {
        return true;
    }
    public static class MultiParamsContext<K,V> extends AbstractInnerContext<MultiParamsContext<?,?>> {

//        public MultiParamsContext<?, ?> getFrom() {
//            super.getFrom();
//
//            Query<K, V> from = (Query<K, V>) thisObj.getFrom();
//            if(from!=null)
//                return from.getMultiParamsContext();
//            return null;
//        }
//
//        public MapTranslate getTranslator() {
//            super.getTranslator();
//
//            return thisObj.getTranslator();
//        }
        
        
        public LRUWVWSMap.Value<MapTranslate, MultiParamsContext<?, ?>> getFromValue() {
            final LRUWVWSMap.Value<MapTranslate, Query<K, V>> from = BaseUtils.immutableCast(thisObj.getFromValue());
            return new LRUWVWSMap.Value<MapTranslate, MultiParamsContext<?, ?>>() {
                @Override
                public MapTranslate getLRUKey() {
                    return from.getLRUKey();
                }

                @Override
                public MultiParamsContext<?, ?> getLRUValue() {
                    return from.getLRUValue().getMultiParamsContext();
                }
            };
        }

        private final Query<K,V> thisObj;
        public MultiParamsContext(Query<K, V> thisObj) {
            this.thisObj = thisObj;
        }

        protected ImSet<ParamExpr> getKeys() {
            return thisObj.getInnerKeys();
        }
        public ImSet<Value> getValues() {
            return thisObj.getInnerValues();
        }
        protected MultiParamsContext translate(MapTranslate translator) {
            return thisObj.translateInner(translator).getQuery().getMultiParamsContext();
        }
        @Override
        public MultiParamsContext<?, ?> translateRemoveValues(MapValuesTranslate translate) {
            return thisObj.translateRemoveValues(translate).getQuery().getMultiParamsContext();
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
            return BaseUtils.hashEquals(thisObj.where,object.getQuery().where) && BaseUtils.hashEquals(thisObj.properties.values().multiSet(), object.getQuery().properties.values().multiSet());
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

    public MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate) {
        return new MapQuery<K,V,K,V>(this, properties.keys().toRevMap(), mapKeys.keys().toRevMap(), translate);
    }
    public Query<K, V> translateQuery(MapTranslate translate) {
        return new Query<K,V>(translate.translateRevValues(mapKeys), translate.translate(properties), where.translateOuter(translate));
    }

    public boolean equalsInner(Query<K, V> object) { // нужно проверить что совпадут
        return BaseUtils.hashEquals(where, object.where) && BaseUtils.hashEquals(properties, object.properties) && BaseUtils.hashEquals(mapKeys, object.mapKeys);
    }

    public boolean equalsInner(IQuery<K, V> object) { // нужно проверить что совпадут
        return equalsInner(object.getQuery());
    }

    public Query<K, V> getQuery() {
        return this;
    }

    public <RMK, RMV> IQuery<RMK, RMV> map(ImRevMap<RMK, K> remapKeys, ImRevMap<RMV, V> remapProps, MapValuesTranslate translate) {
        return new MapQuery<RMK, RMV, K, V>(this, remapProps, remapKeys, translate);
    }
}

