package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.col.lru.LRUWVWSMap;
import lsfusion.base.lambda.Processor;
import lsfusion.server.base.caches.ContextTwin;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.AbstractInnerContext;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.join.where.GroupJoinsWhere;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.expr.where.pull.ExclNullPullWheres;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.pack.Pack;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.compile.CompileOptions;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.query.translate.MapJoin;
import lsfusion.server.data.query.translate.MapQuery;
import lsfusion.server.data.query.translate.QueryTranslateJoin;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.translate.*;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.OrderClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.function.Function;

// запрос JoinSelect
public class Query<K,V> extends IQuery<K,V> {

    public final ImRevMap<K, KeyExpr> mapKeys;
    public final ImMap<V, Expr> properties;
    public final Where where;

    public Query(ImRevMap<K,KeyExpr> mapKeys) {
        this(mapKeys, Where.TRUE());
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
        this(mapKeys, MapFact.singleton(value, property));
    }

    public Query(ImRevMap<K, KeyExpr> mapKeys, Expr property, V value, Where where) {
        this(mapKeys, MapFact.singleton(value, property),where);
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys,Where where) {
        this(mapKeys, MapFact.EMPTY(), where);
    }

    public Query(ImRevMap<K,KeyExpr> mapKeys,Where where, ImMap<K, DataObject> mapValues) {
        this(mapKeys, where, mapValues, MapFact.EMPTY());
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
    
    public ImMap<K, Type> getKeyTypes(final Type.Getter<K> typeGetter) {
        return mapKeys.keys().mapValues((K key) -> getKeyType(typeGetter, key));
    }

    public Type getKeyType(final Type.Getter<K> typeGetter, K key) {
        Type propertyType = getKeyType(key);
        if(propertyType != null)
            return propertyType;
        Type entityType = typeGetter.getType(key);
        if(entityType != null)
            return entityType;
        return AbstractType.getUnknownTypeNull();
    }

    public ImMap<V, Type> getPropertyTypes(final Type.Getter<V> typeGetter) {
        return getProperties().mapValues((V value) -> {
            Type propertyType = getPropertyType(value);
            if(propertyType != null)
                return propertyType;
            Type entityType = typeGetter.getType(value);
            if(entityType != null)
                return entityType;
            return AbstractType.getUnknownTypeNull();
        });        
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
            if(!(joinExpr instanceof KeyExpr) || joinExpr instanceof PullExpr || checked.contains((KeyExpr) joinExpr))
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
            return new MapJoin<>(new MapTranslator(BaseUtils.immutableCast(mapKeys.crossJoin(joinKeys)), mapValues), getJoin());
    }

    @ContextTwin
    public Join<V> joinExprs(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate mapValues) { // последний параметр = какой есть\какой нужно, joinImplement не translateOuter'ся
        assert joinImplement.size()== mapKeys.size();

        Join<V> join = getJoin();

        // сначала map'им значения
        join = new MapJoin<>(mapValues, join);

        // затем делаем подстановку
        join = new QueryTranslateJoin<>(new KeyExprTranslator(mapKeys.crossJoin(joinImplement)), join);

        // затем закидываем Where что все implement не null
        join = join.and(Expr.getWhere(joinImplement));

        return join;
    }

    public static <K> String stringOrder(final ImOrderSet<K> sourcesOrder, final int offset, ImOrderMap<K, CompileOrder> orders, final ImMap<K, String> sources, SQLSyntax syntax, final Result<Boolean> needSources) {
        needSources.set(false);

        ImRevMap<K, String> orderNumbers = orders.getMap().mapRevValues((key, value) -> {
            if (value.reader instanceof OrderClass) {
                needSources.set(true);
                return sources.get(key);
            } else
                return ((Integer) (sourcesOrder.indexOf(key) + offset + 1)).toString();
        });
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
    @StackMessage("{message.core.query.pack}")
    public IQuery<K,V> calculatePack() {
        return new Query<>(this, true);
    }

    protected long calculateComplexity(boolean outer) {
        return AbstractOuterContext.getComplexity(properties.values(), outer) + where.getComplexity(outer);
    }

    @IdentityLazy
    @Pack
    public <B> ClassWhere<B> getClassWhere(ImSet<? extends V> classProps) {
        return (ClassWhere<B>) getClassWhere(where, mapKeys, properties.filterIncl(classProps));
    }

    public static <B, K extends B, V extends B> ClassWhere<B> getClassWhere(Where where, final ImMap<K, ? extends BaseExpr> mapKeys, ImMap<V, ? extends Expr> mapProps) {
        return new ExclPullWheres<ClassWhere<B>, V, Where>() {
            protected ClassWhere<B> initEmpty() {
                return ClassWhere.FALSE();
            }
            protected ClassWhere<B> proceedBase(Where data, ImMap<V, BaseExpr> map) {
                return getClassWhereBase(data, mapKeys, map);
            }
            protected ClassWhere<B> add(ClassWhere<B> op1, ClassWhere<B> op2) {
                return op1.or(op2);
            }
        }.proceed(where, mapProps);
    }

    private static <B, K extends B, V extends B> ClassWhere<B> getClassWhereBase(Where where, ImMap<K, ? extends BaseExpr> mapKeys, ImMap<V, BaseExpr> mapProps) {
        return getClassWhereBase(where.and(Expr.getWhere(mapProps.values())), MapFact.addExcl(mapProps, mapKeys));
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
    @IdentityLazy
    @Pack
    public PullValues<K, V> pullValues() {
        ImMap<K, Expr> pullKeys = pullValues(mapKeys, where);

        ExprTranslator keyTranslator = new PartialKeyExprTranslator(mapKeys.rightCrossJoin(pullKeys), true);
        Where transWhere = where.translateExpr(keyTranslator);
        ImMap<V, Expr> transProps = keyTranslator.translate(properties);

        ImMap<V, Expr> pullProps = pullValues(transProps, transWhere);
        if(pullKeys.isEmpty() && pullProps.isEmpty())
            return new PullValues<>(this);

        return new PullValues<>(new Query<>(mapKeys.removeRev(pullKeys.keys()),
                transProps.remove(pullProps.keys()), transWhere), pullKeys, pullProps);
    }

    @IdentityInstanceLazy // otherwise we'll need a lot of extra equals for check caches, however later it should be done
    @Pack
    @StackMessage("{message.core.query.compile}")
    public CompiledQuery<K, V> compile(ImOrderMap<V, Boolean> orders, CompileOptions<V> options) {
        Result<Boolean> ordersSplit = new Result<>();
        CompiledQuery<K, V> pessOrderTopQuery = compileOrderTop(orders, options, ordersSplit);
        if(ordersSplit.result) {
            CompiledQuery<K, V> optOrderTopQuery = compileOrderTop(orders, options, null);
            if(optOrderTopQuery.sql.baseCost.lessEquals(pessOrderTopQuery.sql.baseCost)) // if we haven't reduced the cost - fallback to the query without splitting the orders
                return optOrderTopQuery;
        }

        return pessOrderTopQuery;
    }

    private CompiledQuery<K, V> compileOrderTop(ImOrderMap<V, Boolean> orders, CompileOptions<V> options, Result<Boolean> ordersSplit) {
        return new CompiledQuery<>(this, options.syntax, orders, ordersSplit, options.limit, options.subcontext, options.recursive, options.noInline, options.castTypes, options.needDebugInfo);
    }

    @IdentityLazy
    @Pack
    public ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders) {
        return getPackedCompileOrders(orders);
    }

    public ImOrderMap<V, CompileOrder> getPackedCompileOrders(ImOrderMap<V, Boolean> orders) {
        return CompiledQuery.getPackedCompileOrders(properties, where, orders);
    }

    private static class OrderWhereJoins {
        public final Pair<ImCol<GroupJoinsWhere>,Boolean> whereJoinsExcl;
        public final boolean split;

        public OrderWhereJoins(Pair<ImCol<GroupJoinsWhere>, Boolean> whereJoinsExcl, boolean split) {
            this.whereJoinsExcl = whereJoinsExcl;
            this.split = split;
        }
    }

    public ImCol<GroupJoinsWhere> getWhereJoins(boolean tryExclusive, Result<Boolean> isExclusive, ImOrderSet<Expr> orderTop, Result<Boolean> ordersSplit) {
        // when we have LIMIT ORDER BY query, RDBMS can build plans where it runs over the index and exits almost immediately
        // however if in expr there is a case (not base expr), RDBMS can't do that, so we're trying to avoid that
        // we force where to be split into more smaller wheres (by forcing their exclusiveness)
        // this optimization is important for GROUP LAST ORDER MATERIALIZED case (when everything is materialized and indexed and we have ORDER BY CASE WHEN t.date IS NOT NULL THEN t.date ELSE bigtable.date)

        // maybe it makes sense to add some optimization to pull wheres only when there are exprs that are in some indexes, or some order "collapsing" if the complexity is too high
        // however there is no such optimization for GROUP BY (where all case exprs are split to base exprs), so don't see why it should be done for the order

        Pair<ImCol<GroupJoinsWhere>,Boolean> whereJoinsExcl;
        if(ordersSplit == null || Settings.get().isNoOrderTopSplit()) {
            whereJoinsExcl = getWhereJoins(where, tryExclusive, orderTop);
            if(ordersSplit != null)
                ordersSplit.set(false);
        } else {
            OrderWhereJoins orderWhereJoins = new ExclNullPullWheres<OrderWhereJoins, Integer, Where>() {
                protected OrderWhereJoins proceedNullBase(Where data, ImMap<Integer, ? extends Expr> map) {
                    return new OrderWhereJoins(getWhereJoins(data, tryExclusive, (ImOrderSet<Expr>) ListFact.fromIndexedMap(map).toOrderSet()), false);
                }

                protected OrderWhereJoins add(OrderWhereJoins op1, OrderWhereJoins op2) {
                    return new OrderWhereJoins(new Pair<>(op1.whereJoinsExcl.first.mergeCol(op2.whereJoinsExcl.first), op1.whereJoinsExcl.second && op2.whereJoinsExcl.second), true);
                }
            }.proceed(where, orderTop.toIndexedMap());

            whereJoinsExcl = orderWhereJoins.whereJoinsExcl;
            ordersSplit.set(orderWhereJoins.split);
        }

        isExclusive.set(whereJoinsExcl.second);
        return whereJoinsExcl.first;
    }

    private Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(Where where, boolean tryExclusive, ImOrderSet<Expr> orderTop) {
        return where.getPackWhereJoins(tryExclusive, getKeys(), orderTop);
    }

    public static <V> ImOrderMap<V,Boolean> reverseOrder(ImOrderMap<V,Boolean> orders) {
        ImOrderValueMap<V,Boolean> result = orders.mapItOrderValues();
        for(int i=0,size=orders.size();i<size;i++)
            result.mapValue(i,!orders.getValue(i));
        return result.immutableValueOrder();
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        return execute(session, DataSession.emptyEnv(owner));
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session) throws SQLException, SQLHandledException {
        return execute(session, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context) throws SQLException, SQLHandledException {
        return execute(context.getEnv());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        DataSession session = env.getSession();
        return execute(session.sql, env.getQueryEnv());
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form) throws SQLException, SQLHandledException {
        return execute(form, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        return execute(session, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT, env);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return execute(context.getSession(), orders, limitOffset);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return execute(session.sql, orders, limitOffset, session.env);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return execute(form.session.sql, orders, limitOffset, form.getQueryEnv());
    }

    private ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeSingle(QueryEnvironment env) { // оптимизация
        if(where.isFalse())
            return MapFact.EMPTYORDER(); // иначе типы ключей не узнаем

        ImMap<BaseExpr, BaseExpr> exprValues = where.getOnlyExprValues();
        if(exprValues==null || exprValues.size()!= mapKeys.size())
            return null;

        ImValueMap<K, DataObject> mvKeyValues = mapKeys.mapItValues(); // return
        for(int i=0,size=mapKeys.size();i<size;i++) {
            BaseExpr keyValue = exprValues.get(mapKeys.getValue(i));
            ObjectValue objectValue;
            if(keyValue!=null && (objectValue = keyValue.getObjectValue(env)) instanceof DataObject)
                mvKeyValues.mapValue(i, (DataObject) objectValue);
            else
                return null;
        }
        ImValueMap<V, ObjectValue> mvPropValues = properties.mapItValues(); // return
        for(int i=0,size=properties.size();i<size;i++) {
            ObjectValue objectValue; BaseExpr propValue;
            Expr propExpr = properties.getValue(i);
            if((objectValue = propExpr.getObjectValue(env))!=null ||
                    ((propValue = exprValues.getObject(propExpr))!=null && (objectValue = propValue.getObjectValue(env))!=null))
                mvPropValues.mapValue(i, objectValue);
            else
                return null;
        }
        return MapFact.singletonOrder(mvKeyValues.immutableValue(), mvPropValues.immutableValue());
    }

    private final static Function<ImMap<Object, ObjectValue>, ImMap<Object, Object>> getMapDataObjectValues = ObjectValue::getMapValues;
    private static <K, D extends ObjectValue> Function<ImMap<K, D>, ImMap<K, Object>> getMapDataObjectValues() {
        return BaseUtils.immutableCast(getMapDataObjectValues);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset, QueryEnvironment env) throws SQLException, SQLHandledException {
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> singleResult = executeSingle(env); // оптимизация
        if(singleResult!=null)
            return singleResult.mapOrderKeyValues(Query.getMapDataObjectValues(), Query.getMapDataObjectValues());

        return executeSQL(session, orders, limitOffset, env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        return executeClasses(session, DataSession.emptyEnv(owner), baseClass);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session) throws SQLException, SQLHandledException {
        return executeClasses(session.sql, session.env, session.baseClass);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return executeClasses(session.sql, session.env, session.baseClass, limitOffset);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session, ImOrderMap<? extends V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return executeClasses(session.sql, orders, limitOffset, session.baseClass, session.env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        return executeClasses(session, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT, baseClass, env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return executeClasses(session, MapFact.EMPTYORDER(), limitOffset, baseClass, env);
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, ImOrderMap<? extends Expr, Boolean> orders) throws SQLException, SQLHandledException {
        if(orders.isEmpty())
            return executeClasses(session, env, baseClass);

        ImRevMap<Object, Expr> orderObjects = ((ImOrderMap<Expr, Boolean>)orders).keys().mapRevKeys(Object::new);

        Query<K, Object> orderQuery = new Query<>(mapKeys, MapFact.addExcl(properties, orderObjects), where);
        ImOrderMap<Object, Boolean> orderProperties = ((ImOrderMap<Expr, Boolean>)orders).map(orderObjects.reverse());

        return orderQuery.executeClasses(session, orderProperties, LimitOffset.NOLIMIT, baseClass, env).mapOrderValues((ImMap<Object, ObjectValue> value) -> value.filterIncl(properties.keys()));
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context) throws SQLException, SQLHandledException {
        return executeClasses(context, MapFact.EMPTYORDER());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context, ImOrderMap<? extends V, Boolean> orders) throws SQLException, SQLHandledException {
        return executeClasses(context.getEnv(), orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return executeClasses(env, MapFact.EMPTYORDER());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env, ImOrderMap<? extends V, Boolean> orders) throws SQLException, SQLHandledException {
        DataSession session = env.getSession();
        return executeClasses(session.sql, orders, LimitOffset.NOLIMIT, session.baseClass, env.getQueryEnv());
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ImOrderMap<? extends Expr, Boolean> orders, ExecutionEnvironment env) throws SQLException, SQLHandledException {
        DataSession session = env.getSession();
        return executeClasses(session.sql, env.getQueryEnv(), session.baseClass, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(FormInstance formInstance, BaseClass baseClass) throws SQLException, SQLHandledException {
        return executeClasses(formInstance.session.sql, MapFact.EMPTYORDER(), LimitOffset.NOLIMIT, formInstance.session.baseClass, formInstance.getQueryEnv());
    }

    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, LimitOffset limitOffset, final BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> singleResult = executeSingle(env); // оптимизация
        if(singleResult!=null)
            return singleResult;

        return executeSQLClasses(getClassQuery(baseClass), session, (ImOrderMap<Object, Boolean>) orders, limitOffset, baseClass, env);
    }

    private static <K, V> ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeSQLClasses(final Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> classQuery, SQLSession session, ImOrderMap<Object, Boolean> orders, LimitOffset limitOffset, final BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        ImOrderMap<ImMap<K, Object>, ImMap<Object, Object>> rows = classQuery.first.executeSQL(session, orders, limitOffset, env);

        // перемаппим
        final KeyType keyType = classQuery.first.getWhere();
        final ImRevMap<K, KeyExpr> mapKeys = classQuery.first.getMapKeys();
        final ImSet<V> props = (ImSet<V>) classQuery.first.getProperties().remove(classQuery.second.valuesSet());
        
        // оптимизация
        final ImMap<K, ClassReader> keyReaders = mapKeys.mapValues(value -> value.getReader(keyType));
        final ImMap<V, Pair<Expr, ClassReader>> propExprReaders = props.mapValues((V prop) -> {
            Expr expr = classQuery.first.getExpr(prop);
            return new Pair<>(expr, expr.getReader(keyType));
        });
        
        return rows.mapOrderKeyValues((rowKey, rowValue) -> {
            final ImMap<Expr, Object> exprValues = classQuery.second.join(rowValue);
            return mapKeys.mapValues((key, keyExpr) -> new DataObject(rowKey.get(key), keyReaders.get(key).readClass(keyExpr, exprValues, baseClass, keyType)));
        }, value -> {
            final ImMap<Expr, Object> exprValues = classQuery.second.join(value);
            return props.mapValues((V prop) -> {
                Pair<Expr, ClassReader> exprReader = propExprReaders.get(prop);
                return ObjectValue.getValue(value.get(prop), exprReader.second.readClass(exprReader.first, exprValues, baseClass, keyType));
            });});
    }

    // создаем запрос с IsClassExpr'ами
    @IdentityInstanceLazy
    @Pack
    public Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> getClassQuery(final BaseClass baseClass) {
        MSet<Expr> mReadExprs = SetFact.mSet();
        for(KeyExpr expr : mapKeys.valueIt()) {
            ClassReader reader = expr.getReader(where);
            if(reader == null)
                throw new RuntimeException(ThreadLocalContext.localize("{exceptions.mix.of.types.or.incorrect.set.operation}"));
            reader.prepareClassesQuery(expr, where, mReadExprs, baseClass);
        }
        for(Expr expr : properties.valueIt())
            expr.getReader(where).prepareClassesQuery(expr, where, mReadExprs, baseClass);
        ImSet<Expr> readExprs = mReadExprs.immutable();
        final ImRevMap<Expr, Object> objects = BaseUtils.generateObjects(readExprs);
        return new Pair<>(new Query<>(mapKeys, MapFact.addExcl(properties, objects.reverse().mapValues(value -> value.classExpr(baseClass))), where), objects);
    }

    public void outClassesSelect(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        outClassesSelect(session, baseClass, System.out::println);
    }

    public void outClassesSelect(SQLSession session, BaseClass baseClass, Processor<String> process) throws SQLException, SQLHandledException {
        // выведем на экран
        session.outStatement = true;
        ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> result;
        try {
            result = executeClasses(session, baseClass, OperationOwner.debug);
        } finally {
            session.outStatement = false;
        }

        for(int i=0,size=result.size();i<size;i++) {
            ImMap<K, DataObject> rowKey = result.getKey(i);
            for(int j=0,sizeJ=rowKey.size();j<sizeJ;j++) {
                process.proceed(rowKey.getKey(j) + "-" + rowKey.getValue(j));
            }
            process.proceed("---- ");
            ImMap<V, ObjectValue> rowValue = result.getValue(i);
            for(int j=0,sizeJ=rowValue.size();j<sizeJ;j++) {
                process.proceed(rowValue.getKey(j) + "-" + rowValue.getValue(j));
            }
        }
    }

    public String toString() {
        return "{" + properties + "," + where + "," + mapKeys + "}";
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

        public int hash(HashContext hash) {
            return thisObj.where.hashOuter(hash) * 31 + AbstractSourceJoin.hashOuter(thisObj.properties.values(), hash);
        }
        public boolean equalsInner(MultiParamsContext object) {
            return BaseUtils.hashEquals(thisObj.where,object.getQuery().where) && BaseUtils.hashEquals(thisObj.properties.values().multiSet(), object.getQuery().properties.values().multiSet());
        }
    }
    private MultiParamsContext<K,V> multiParamsContext;
    public MultiParamsContext<K,V> getMultiParamsContext() {
        if(multiParamsContext==null)
            multiParamsContext = new MultiParamsContext<>(this);
        return multiParamsContext;
    }

    public int hash(HashContext hashContext) {
        return 31 * (where.hashOuter(hashContext) * 31 + AbstractSourceJoin.hashOuter(properties, hashContext)) + AbstractSourceJoin.hashOuter(mapKeys, hashContext);
    }

    public MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate) {
        return new MapQuery<>(this, properties.keys().toRevMap(), mapKeys.keys().toRevMap(), translate);
    }
    public Query<K, V> translateQuery(MapTranslate translate) {
        return new Query<>(translate.translateRevValues(mapKeys), translate.translate(properties), where.translateOuter(translate));
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
        return new MapQuery<>(this, remapProps, remapKeys, translate);
    }
}

