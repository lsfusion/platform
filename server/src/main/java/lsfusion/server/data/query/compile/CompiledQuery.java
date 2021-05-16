package lsfusion.server.data.query.compile;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.lambda.set.NotFunctionSet;
import lsfusion.base.log.DebugInfoWriter;
import lsfusion.base.log.StringDebugInfoWriter;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityQuickLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.formula.ConcatenateExpr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.base.BaseJoin;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.query.*;
import lsfusion.server.data.expr.join.where.GroupJoinsWhere;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.expr.join.where.WhereJoins;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.expr.query.order.PartitionCalc;
import lsfusion.server.data.expr.query.order.PartitionToken;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.value.StaticValueNullableExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.query.LimitOptions;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.query.exec.*;
import lsfusion.server.data.query.result.ResultHandler;
import lsfusion.server.data.sql.SQLQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.table.Field;
import lsfusion.server.data.table.Table;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.parse.LogicalParseInterface;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.data.type.parse.StringParseInterface;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.CheckWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.classes.data.ArrayClass;
import lsfusion.server.logics.classes.data.OrderClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import static lsfusion.base.log.DebugInfoWriter.pushPrefix;

// нужен для Map'а ключей / значений
// Immutable/Thread Safe
public class CompiledQuery<K,V> extends ImmutableObject {
    final public String from;
    final public ImMap<K,String> keySelect;
    final public ImMap<V,String> propertySelect;
    final public ImCol<String> whereSelect;
    final public ImOrderSet<K> keyOrder; // чисто оптимизация, чтобы лишний SELECT в getInsertSelect не делать
    final public ImOrderSet<V> propertyOrder;

    public final DynamicExecuteEnvironment queryExecEnv; // assertion что при передаче куда-то subQueries сохраняются - ни больше не меньше
    
    public final String debugInfo;

    private TypeExecuteEnvironment getTypeExecEnv(SQLSessionContextProvider userProvider) {
        if(sql.getLength() <= Settings.get().getQueryLengthTimeout())
            return TypeExecuteEnvironment.NONE;

        Integer type = null;
        if(userProvider != null) {
            type = DynamicExecuteEnvironment.getUserExecEnv(userProvider);
        }
        if(type == null)
            type = Settings.get().getDefaultTypeExecuteEnvironment();

        return TypeExecuteEnvironment.get(type);
    }

    // тут немного специфичная оптимизация на уменьшения locks, с учетом того что почти у всех пользователей всегда будут одни и те же env'ы
    public DynamicExecuteEnvironment getQueryExecEnv(SQLSessionContextProvider userProvider) {
        TypeExecuteEnvironment typeEnv = getTypeExecEnv(userProvider);
        TypeExecuteEnvironment queryType = queryExecEnv.getType();
        if(queryType == null || queryType.equals(typeEnv))
            return queryExecEnv;
        return extraEnvs.getEnv(typeEnv, sql);
    }

    private static class ExtraEnvs {
        private MAddExclMap<TypeExecuteEnvironment, DynamicExecuteEnvironment> extraQueryExecEnvs;

        private synchronized DynamicExecuteEnvironment getEnv(TypeExecuteEnvironment type, SQLQuery query) {
            if(extraQueryExecEnvs == null)
                extraQueryExecEnvs = MapFact.mAddExclMap();

            DynamicExecuteEnvironment execEnv = extraQueryExecEnvs.get(type);
            if (execEnv == null) {
                execEnv = type.create(query);
                extraQueryExecEnvs.exclAdd(type, execEnv);
            }
            return execEnv;
        }
    }
    private ExtraEnvs extraEnvs;

    final public ImRevMap<K,String> keyNames;
    final public ImRevMap<V,String> propertyNames;
    final ImRevMap<ParseValue,String> params;

    final public ImSet<K> areKeyValues;
    final public ImSet<V> arePropValues;

    public ImMap<V, ClassReader> getMapPropertyReaders() { // пока не хочется generics лепить для ClassReader
        return (ImMap<V, ClassReader>) propertyNames.join(sql.propertyReaders);
    }

    final public SQLQuery sql;
    final public Stat rows;
    public final StaticExecuteEnvironment env;

    private boolean checkQuery() {
        return true;
    }

    // перемаппит другой CompiledQuery
    public <MK,MV> CompiledQuery(CompiledQuery<MK,MV> compile,ImRevMap<K,MK> mapKeys,ImRevMap<V,MV> mapProperties, final MapValuesTranslate mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = mapKeys.join(compile.keySelect);
        propertySelect = mapProperties.join(compile.propertySelect);

        keyNames = mapKeys.join(compile.keyNames);
        propertyNames = mapProperties.join(compile.propertyNames);

        sql = compile.sql;
        rows = compile.rows;
        queryExecEnv = compile.queryExecEnv;
        extraEnvs = compile.extraEnvs;

        ImRevMap<MK, K> reversedMapKeys = mapKeys.reverse();
        ImRevMap<MV, V> reversedMapProps = mapProperties.reverse();

        areKeyValues = compile.areKeyValues.mapSetValues(reversedMapKeys.fnGetValue());
        arePropValues = compile.arePropValues.mapSetValues(reversedMapProps.fnGetValue());

        params = compile.params.mapRevKeys((ParseValue value) -> {
            if (value instanceof Value)
                return mapValues.translate((Value) value);
            assert value instanceof StaticValueExpr;
            return value;
        });

        keyOrder = compile.keyOrder.mapOrder(reversedMapKeys);
        propertyOrder = compile.propertyOrder.mapOrder(reversedMapProps);

        env = compile.env;
        debugInfo = compile.debugInfo;

        assert checkQuery();
    }

    private static class FullSelect extends CompileSource {

        private FullSelect(KeyType keyType, Where fullWhere, ImRevMap<ParseValue, String> params, SQLSyntax syntax, MStaticExecuteEnvironment env, ImMap<KeyExpr, String> keySelect, ImMap<FJData, String> joinData) {
            super(keyType, fullWhere, params, syntax, env);
            this.keySelect = keySelect;
            this.joinData = joinData;
        }

        public final ImMap<KeyExpr,String> keySelect;
        public final ImMap<FJData,String> joinData;

        @Override
        public String getSource(KeyExpr key) {
            return keySelect.get(key);
        }

        public String getSource(Table.Join.Expr expr) {
            assert joinData.get(expr)!=null;
            return joinData.get(expr);
        }

        public String getSource(Table.Join.IsIn where) {
            assert joinData.get(where)!=null;
            return joinData.get(where);
        }

        public String getSource(QueryExpr queryExpr, boolean needValue) {
            assert joinData.get(queryExpr)!=null;
            return joinData.get(queryExpr);
        }

        public String getSource(IsClassExpr classExpr, boolean needValue) {
            assert joinData.get(classExpr)!=null;
            return joinData.get(classExpr);
        }
    }

    // многие субд сами не могут определить некоторые вещи, приходится им помогать
    public static <P> ImOrderMap<P, CompileOrder> getPackedCompileOrders(ImMap<P, Expr> orderExprs, Where where, ImOrderMap<P, Boolean> orders) {
        MOrderExclMap<P, CompileOrder> mResult = MapFact.mOrderExclMapMax(orders.size());
        MAddSet<KeyExpr> currentKeys = SetFact.mAddSet();
        orderExprs = PropertyChange.simplifyExprs(orderExprs, where); // чтобы KeyEquals еще учесть
        for(int i=0,size=orders.size();i<size;i++) {
            P order = orders.getKey(i);
            Expr orderExpr = orderExprs.get(order);
            if(!currentKeys.containsAll(BaseUtils.<ImSet<KeyExpr>>immutableCast(orderExpr.getOuterKeys()))) {
                boolean notNull = false; // ??? where.means orderExpr.getWhere
                if(orderExpr instanceof KeyExpr) {
                    notNull = true;
                    currentKeys.add((KeyExpr)orderExpr);
                }
                mResult.exclAdd(order, new CompileOrder(orders.getValue(i), where.isFalse() ? NullReader.instance : orderExpr.getReader(where), notNull));
            }
        }
        return mResult.immutableOrder();
    }

    public CompiledQuery(final Query<K, V> query, SQLSyntax syntax, ImOrderMap<V, Boolean> orders, LimitOptions limit, SubQueryContext subcontext, boolean noExclusive, boolean noInline, ImMap<V, Type> exCastTypes, boolean needDebugInfo) {

        Result<ImOrderSet<K>> resultKeyOrder = new Result<>(); Result<ImOrderSet<V>> resultPropertyOrder = new Result<>();

        keyNames = query.mapKeys.mapRevValues(new GenNameIndex("jkey", ""));
        propertyNames = query.properties.mapRevValues(new GenNameIndex("jprop",""));
        params = SetFact.addExclSet(query.getInnerValues(), query.getInnerStaticValues()).mapRevValues(new GenNameIndex("qwer", "ffd"));

        MStaticExecuteEnvironment mEnv = StaticExecuteEnvironmentImpl.mEnv();
        Result<Cost> mBaseCost = new Result<>(Cost.MIN);
        Result<Boolean> mOptAdjustLimit = new Result<>(false);
        Result<Stat> mRows = new Result<>(Stat.ONE);
        MExclMap<String, SQLQuery> mSubQueries = MapFact.mExclMap();
        
        StringDebugInfoWriter debugInfoWriter = null;
        if(needDebugInfo)
            debugInfoWriter = new StringDebugInfoWriter();

        String select;

        boolean distinctValues = limit.isDistinctValues();

        ImMap<K, ClassReader> keyReaders = query.mapKeys.mapValues(value -> query.where.isFalse() || distinctValues ? NullReader.instance : value.getType(query.where));

        ImMap<V, ClassReader> propertyReaders = query.properties.mapValues(value -> query.where.isFalse() ? NullReader.instance : value.getReader(query.where));

        ImOrderMap<V, CompileOrder> compileOrders = query.getPackedCompileOrders(orders);

        boolean useFJ = syntax.useFJ();
        noExclusive = noExclusive || distinctValues || Settings.get().isNoExclusiveCompile(); // we don't want exclusiveness for distinct values since we can't use UNION ALL in that case
        Result<Boolean> unionAll = new Result<>();
        ImCol<GroupJoinsWhere> queryJoins = query.getWhereJoins(!useFJ && !noExclusive, unionAll,
                                limit.hasLimit() && syntax.orderTopProblem() ? orders.keyOrderSet().mapList(query.properties).toOrderSet() : SetFact.EMPTYORDER());
        boolean union = queryJoins.size() >= 2 && ((!useFJ && (unionAll.result || !Settings.get().isUseFJInsteadOfUnion())) || distinctValues); // it doesn't make sense (and apparently is not possible) to use full join for distinct values
        if (union) { // сложный UNION запрос
            ImMap<V, Type> castTypes = BaseUtils.immutableCast(
                    propertyReaders.filterFnValues(element -> {
                        return element instanceof Type && !(element instanceof OrderClass); // так как для упорядочивания по выражению, оно должно быть в запросе - опасный хак, но собственно ORDER оператор и есть один большой хак
                    }));
            if(exCastTypes != null)
                castTypes = exCastTypes.override(castTypes);

            String fromString = "";
            for(GroupJoinsWhere queryJoin : queryJoins) {
                boolean orderUnion = syntax.orderUnion(); // нужно чтобы фигачило внутрь orders а то многие SQL сервера не видят индексы внутри union all
                fromString = (fromString.length()==0?"":fromString+" UNION " + (unionAll.result?"ALL ":"")) + "(" + getInnerSelect(query.mapKeys, queryJoin, query.properties, params, orderUnion?orders:MapFact.EMPTYORDER(), orderUnion? limit : LimitOptions.NOLIMIT, syntax, keyNames, propertyNames, resultKeyOrder, resultPropertyOrder, castTypes, subcontext, false, mEnv, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, pushPrefix(debugInfoWriter, "UNION", queryJoin)) + ")";
                if(!orderUnion) // собственно потому как union cast'ит к первому union'у (во всяком случае postgreSQL)
                    castTypes = null;
            }

            final String alias = "UALIAS";
            AddAlias addAlias = new AddAlias(alias);
            keySelect = keyNames.mapValues(addAlias);
            propertySelect = propertyNames.mapValues(addAlias);
            from = "(" + fromString + ") "+alias;
            whereSelect = SetFact.EMPTY();
            String topString = limit.getString();
            Result<Boolean> needSources = new Result<>();
            String orderBy = Query.stringOrder(resultPropertyOrder.result, query.mapKeys.size(), compileOrders, propertySelect, syntax, needSources);
            if(needSources.result)
                select = syntax.getSelect(from, "*",  "", orderBy, topString, false);
            else
                select = syntax.getUnionOrder(fromString, orderBy, topString);
            areKeyValues = SetFact.EMPTY(); arePropValues = SetFact.EMPTY();
        } else {
            if(queryJoins.size()==0) { // "пустой" запрос
                keySelect = query.mapKeys.mapValues(() -> SQLSyntax.NULL);
                propertySelect = query.properties.mapValues(() -> SQLSyntax.NULL);
                from = "empty";
                whereSelect = SetFact.EMPTY();                
                areKeyValues = query.mapKeys.keys(); arePropValues = query.properties.keys(); 
            } else {
                Result<ImMap<K, String>> resultKey = new Result<>(); Result<ImMap<V, String>> resultProperty = new Result<>();
                if(queryJoins.size()==1) { // "простой" запрос
                    Result<ImCol<String>> resultWhere = new Result<>();
                    Result<ImSet<K>> resultKeyValues = new Result<>(); Result<ImSet<V>> resultPropValues= new Result<>();
                    from = fillInnerSelect(query.mapKeys, queryJoins.single(), query.properties, resultKey, resultProperty, resultWhere, params, syntax, subcontext, limit, compileOrders.keyOrderSet(), mEnv, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, resultKeyValues, resultPropValues, debugInfoWriter);
                    whereSelect = resultWhere.result;
                    areKeyValues = resultKeyValues.result; arePropValues = resultPropValues.result;
                } else { // "сложный" запрос с full join'ами
                    from = fillFullSelect(query.mapKeys, queryJoins, query.where, query.properties, orders, limit, resultKey, resultProperty, params, syntax, subcontext, mEnv, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, debugInfoWriter);
                    whereSelect = SetFact.EMPTY();
                    areKeyValues = SetFact.EMPTY(); arePropValues = SetFact.EMPTY();
                }
                keySelect = resultKey.result; propertySelect = resultProperty.result;
            }

            select = getSelect(from, keySelect, keyNames, resultKeyOrder, propertySelect, propertyNames, resultPropertyOrder, whereSelect, syntax, compileOrders, limit, noInline);
        }
        
        env = mEnv.finish();
        sql = new SQLQuery(select, mBaseCost.result, mOptAdjustLimit.result, mSubQueries.immutable(), env, keyNames.crossJoin(keyReaders), propertyNames.crossJoin(propertyReaders), union, false);
        rows = mRows.result;
        queryExecEnv = getTypeExecEnv(null).create(sql);
        extraEnvs = new ExtraEnvs();
        keyOrder = resultKeyOrder.result; propertyOrder = resultPropertyOrder.result;
        debugInfo = debugInfoWriter != null ? debugInfoWriter.getString() : null;
        
        assert checkQuery();
    }

    // в общем случае получить CompileAndQuery под которые подходит Where
    private static ImSet<CompileAndQuery> getWhereSubSet(ImSet<CompileAndQuery> andWheres, Where where) {

        MSet<CompileAndQuery> result = SetFact.mSet();
        CheckWhere resultWhere = Where.FALSE();
        while(result.size()< andWheres.size()) {
            // ищем куда закинуть заодно считаем
            CompileAndQuery lastQuery = null;
            CheckWhere lastWhere = null;
            for(CompileAndQuery and : andWheres)
                if(!result.contains(and)) {
                    lastQuery = and;
                    lastWhere = resultWhere.orCheck(lastQuery.innerSelect.getFullWhere());
                    if(where.means(lastWhere)) {
                        result.add(lastQuery);

                        return result.immutable();
                    }
                }
            resultWhere = lastWhere;
            result.add(lastQuery);
        }
        return result.immutable();
    }

    static class InnerSelect extends CompileSource {

        public final Map<KeyExpr,String> keySelect;
        private Stack<MRevMap<String, String>> stackTranslate = new Stack<>();
        private Stack<MSet<KeyExpr>> stackUsedPendingKeys = new Stack<>();
        private Stack<Result<Boolean>> stackUsedOuterPendingJoins = new Stack<>();
        private Set<KeyExpr> pending;

        void usedJoin(JoinSelect join) {
            if(!stackUsedOuterPendingJoins.isEmpty() && mOuterPendingJoins!=null && mOuterPendingJoins.contains(join))
                stackUsedOuterPendingJoins.peek().set(true);
        }

        public String getSource(KeyExpr key) {
            String source = keySelect.get(key);
            if(source == null) {
                source = "qxas" + keySelect.size() + "nbv";
                keySelect.put(key, source);
                pending.add(key);
            }

            if(pending.contains(key)) {
                if(stackUsedPendingKeys.empty())
                    throw getIncorrectOperationException();
                else
                    stackUsedPendingKeys.peek().add(key);
            }

            return source;
        }

        final WhereJoins whereJoins;

        public InnerJoins getInnerJoins() {
            return whereJoins.getInnerJoins();
        }
        public static ImOrderSet<InnerJoin> getInnerJoinOrder(ImOrderSet<BaseJoin> whereJoinOrder) {
            return WhereJoins.getInnerJoinOrder(whereJoinOrder);
        }

        public boolean isInner(InnerJoin join) {
            return getInnerJoins().containsAll(join);
        }

        final UpWheres<WhereJoin> upWheres;

        final SubQueryContext subcontext;
        final KeyStat keyStat;
        private final ImSet<KeyExpr> keys;

        private final MExclMap<String, SQLQuery> mSubQueries;

        public InnerSelect(ImSet<KeyExpr> keys, KeyType keyType, KeyStat keyStat, Where fullWhere, WhereJoins whereJoins, UpWheres<WhereJoin> upWheres, SQLSyntax syntax, MExclMap<String, SQLQuery> mSubQueries, MStaticExecuteEnvironment env, ImRevMap<ParseValue, String> params, SubQueryContext subcontext) {
            super(keyType, fullWhere, params, syntax, env);

            this.keys = keys;
            this.keyStat = keyStat;
            this.subcontext = subcontext;
            this.whereJoins = whereJoins;
            this.upWheres = upWheres;
            this.keySelect = new HashMap<>(); // сложное рекурсивное заполнение
            this.pending = new HashSet<>();
            this.mSubQueries = mSubQueries;
        }

        int aliasNum=0;
        MList<JoinSelect> mJoins = ListFact.mList();
        ImList<JoinSelect> joins;
        MList<String> mExplicitWheres = ListFact.mList();
        MList<String> mImplicitJoins = ListFact.mList();
        MOrderExclSet<JoinSelect> mOuterPendingJoins = SetFact.mOrderExclSet();

        boolean whereCompiling;

        private abstract class JoinSelect<I extends InnerJoin> {

            public final String alias; // final
            public String join; // final
            public final I innerJoin;

            protected abstract ImMap<String, BaseExpr> initJoins(I innerJoin, SQLSyntax syntax);

            protected boolean isInner() {
                return InnerSelect.this.isInner(innerJoin);
            }

            protected JoinSelect(I innerJoin) {
                alias = subcontext.wrapAlias("t" + (aliasNum++));
                this.innerJoin = innerJoin;
                boolean inner = isInner();
                boolean outerPending = false;

                // здесь проблема что keySelect может рекурсивно использоваться 2 раза, поэтому сначала пробежим не по ключам
                String joinString = "";
                ImMap<String, BaseExpr> initJoins = initJoins(innerJoin, syntax);
                MExclMap<String,KeyExpr> mJoinKeys = MapFact.mExclMapMax(initJoins.size());
                for(int i=0,size=initJoins.size();i<size;i++) {
                    BaseExpr expr = initJoins.getValue(i);
                    String keySource = alias + "." + initJoins.getKey(i);
                    if(expr instanceof KeyExpr && inner)
                        mJoinKeys.exclAdd(keySource, (KeyExpr) expr);
                    else {
                        stackUsedPendingKeys.push(SetFact.mSet());
                        stackTranslate.push(MapFact.mRevMap());
                        stackUsedOuterPendingJoins.push(new Result<>());
                        String exprJoin = keySource + "=" + expr.getSource(InnerSelect.this);
                        ImSet<KeyExpr> usedPendingKeys = stackUsedPendingKeys.pop().immutable();
                        ImRevMap<String, String> translate = stackTranslate.pop().immutableRev(); // их надо перетранслировать
                        Result<Boolean> usedOuterPending = stackUsedOuterPendingJoins.pop();
                        exprJoin = translatePlainParam(exprJoin, translate);

                        boolean havePending = usedPendingKeys.size() > translate.size() || usedOuterPending.result != null;
                        if(inner && havePending) { // какие-то ключи еще зависли, придется в implicitJoins закидывать
                            assert usedPendingKeys.size() <= translate.size() || usedPendingKeys.intersect(SetFact.fromJavaSet(pending));
                            mImplicitJoins.add(exprJoin);
                        } else { // можно explicit join делать, перетранслировав usedPending
                            joinString = (joinString.length() == 0 ? "" : joinString + " AND ") + exprJoin;
                            if(havePending) // если использовался другой outerPending, нужно и этот join докидывать ы outerPendingJoins иначе он будет раньше и не найдется использованный outerPending
                                outerPending = true;
                        }
                    }
                }
                ImMap<String, KeyExpr> joinKeys = mJoinKeys.immutable();

                for(int i=0,size=joinKeys.size();i<size;i++) { // дозаполним ключи
                    String keyString = joinKeys.getKey(i); KeyExpr keyExpr = joinKeys.getValue(i);
                    String keySource = keySelect.get(keyExpr);
                    if(keySource==null || pending.remove(keyExpr)) {
                        if(keySource!=null) { // нашли pending ключ, проставляем во все implicit joins
                            if(!stackUsedPendingKeys.isEmpty() && stackUsedPendingKeys.peek().contains(keyExpr)) // если ключ был использован, ну очень редкий случай
                                stackTranslate.peek().revAdd(keySource, keyString);
                            for(int j=0,sizeJ=mImplicitJoins.size();j<sizeJ;j++)
                                mImplicitJoins.set(j, mImplicitJoins.get(j).replace(keySource, keyString));
                            for(int j=0,sizeJ=mExplicitWheres.size();j<sizeJ;j++)
                                mExplicitWheres.set(j, mExplicitWheres.get(j).replace(keySource, keyString));
                            for(int j=0,sizeJ=mOuterPendingJoins.size();j<sizeJ;j++) {
                                JoinSelect pendingJoin = mOuterPendingJoins.get(j);
                                pendingJoin.join = pendingJoin.join.replace(keySource, keyString);
                            }
                        }
                        keySelect.put(keyExpr, keyString);
                    } else
                        joinString = (joinString.length()==0?"":joinString+" AND ") + keyString + "=" + keySource;
                }
                join = joinString;

                if(outerPending)
                    mOuterPendingJoins.exclAdd(this);
                else
                    mJoins.add(this);
            }

            public abstract String getSource(DebugInfoWriter debugInfoWriter);

            protected abstract Where getInnerWhere(); // assert что isInner
        }

        private Stat baseStat;
        @IdentityQuickLazy
        private boolean isOptAntiJoin(InnerJoin innerJoin) {
            assert !isInner(innerJoin);
            StatType type = StatType.ANTIJOIN;
            if(baseStat == null)
                baseStat = whereJoins.getStatKeys(keys, keyStat, type).getRows();
            // тут есть 2 стратегии : оптимистичная и пессимистичная
            // оптимистичная - если статистика остальные предикатов <= статистики этого join'а, то расчитываем что СУБД так их и выполнит, а потом будет LEFT JOIN делать и тогда уменьшение статистики будет релевантным
            return whereJoins.and(new WhereJoins(innerJoin)).getStatKeys(keys, keyStat, type).getRows().less(baseStat);
            // пессимистичная - дополнительно смотреть что если статистика join'а маленькая, потому как СУБД никто не мешает взять один из больших предикатов и нарушить верхнее предположение
        }

        public void fillInnerJoins(Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MCol<String> whereSelect, LimitOptions limit, ImOrderSet<Expr> orders, DebugInfoWriter debugInfoWriter) { // заполним Inner Joins, чтобы keySelect'ы были
            Result<ImSet<BaseExpr>> usedNotNulls = new Result<>();
            Result<ImOrderSet<BaseJoin>> joinOrder = new Result<>();
            whereJoins.fillCompileInfo(mBaseCost, mRows, usedNotNulls, joinOrder, mOptAdjustLimit, keys, keyStat, limit, orders, debugInfoWriter);

            stackUsedPendingKeys.push(SetFact.mSet()); stackTranslate.push(MapFact.mRevMap()); stackUsedOuterPendingJoins.push(new Result<>());
            
            // заполняем inner joins, чтобы заполнить все ключи (плюс сделать это в правильном порядке)
            // порядок получается не идеально правильный, так в архитектуре компиляции функциональная, а не реляционная логика (то есть учитывается разделение ключи значения), соответствено значение всегда "раньше" ключа, но всякие join_collapse_limit такой подход позволяет существенно снизить по идее
            // вообще можно было бы от getInnerJoins избавиться (использовать полученный Set), но так как InnerJoins используется в ветке следствий в выражениях и т.п. оставим пока как было 
            for (InnerJoin where : getInnerJoinOrder(joinOrder.result)) {
                assert isInner(where);
                getJoinSelect(where);
            }

            MSet<KeyExpr> usedKeys = stackUsedPendingKeys.pop();
            MRevMap<String, String> translate = stackTranslate.pop();
            stackUsedOuterPendingJoins.pop();
            assert usedKeys.size() == translate.size();

            whereSelect.addAll(mExplicitWheres.immutableList().getCol());
            whereSelect.addAll(mImplicitJoins.immutableList().getCol());
            mJoins.addAll(mOuterPendingJoins.immutableOrder());
            assert pending.isEmpty(); // из-за висячего ключа может падать (аналогично EmptyStackException)
            mExplicitWheres = null;
            mImplicitJoins = null;
            mOuterPendingJoins = null;

            if(syntax.hasNotNullIndexProblem())
                for(BaseExpr notNull : usedNotNulls.result)
                    whereSelect.add(notNull.getNotNullSource(this));

        }

        // получает условия следующие из логики inner join'ов SQL
        private Where getInnerWhere() {
            Where result = Where.TRUE();
            for(InnerJoin innerJoin : getInnerJoins().it()) {
                JoinSelect joinSelect = getJoinSelect(innerJoin);
                if(joinSelect!=null)
                    result = result.and(joinSelect.getInnerWhere());
            }
            return result;
        }

        public String getFrom(Where where, MCol<String> whereSelect, DebugInfoWriter debugInfoWriter) {
            where.getSource(this);

            joins = mJoins.immutableList();
            mJoins = null;
            for(JoinSelect join : joins)
                if(join instanceof QuerySelect)
                    ((QuerySelect)join).finalIm();

            whereCompiling = true;
            whereSelect.add(where.followFalse(getInnerWhere().not()).getSource(this));
            whereCompiling = false;

            if(joins.isEmpty()) return "dumb";

            String from;
            Iterator<JoinSelect> ij = joins.iterator();
            JoinSelect first = ij.next();
            if(first.isInner()) {
                from = first.getSource(debugInfoWriter) + " " + first.alias;
                if(!(first.join.length()==0))
                    whereSelect.add(first.join);
            } else {
                from = "dumb";
                ij = joins.iterator();
            }

            while(ij.hasNext()) {
                JoinSelect join = ij.next();
                from = from + (join.isInner() ?"":" LEFT")+" JOIN " + join.getSource(debugInfoWriter) + " " + join.alias  + " ON " + (join.join.length()==0?Where.TRUE_STRING:join.join);
            }

            return from;
        }

        private class TableSelect extends JoinSelect<Table.Join> {
            protected ImMap<String, BaseExpr> initJoins(Table.Join table, final SQLSyntax syntax) {
                return table.joins.mapKeys(value -> value.getName(syntax));
            }

            TableSelect(Table.Join join) {
                super(join);
            }

            public String getSource(DebugInfoWriter debugInfoWriter) {
                RecursiveTable recursiveTable = innerJoin.getRecursiveTable();
                if(recursiveTable != null)
                    env.addNotMaterializable(recursiveTable);

                return innerJoin.getQuerySource(InnerSelect.this);
            }

            protected Where getInnerWhere() {
                return innerJoin.getWhere();
            }
        }

        final MAddExclMap<Table.Join, TableSelect> tables = MapFact.mAddExclMap();
        private String getAlias(Table.Join table) {
            return getTableSelect(table).alias;
        }

        public String getSource(Table.Join.Expr expr) {
            return getAlias(expr.getInnerJoin())+"."+expr.property.getName(syntax);
        }
        public String getSource(Table.Join.IsIn where) {
            return getAlias(where.getJoin()) + "." + where.getFirstKey(syntax) + " IS NOT NULL";
        }
        public String getSource(IsClassExpr classExpr, boolean needValue) {
            InnerExpr joinExpr = classExpr.getInnerJoinExpr();
            if(joinExpr instanceof Table.Join.Expr)
                return getSource((Table.Join.Expr)joinExpr);
            else
                return getSource((SubQueryExpr)joinExpr, needValue);
        }

        private abstract class QuerySelect<K extends Expr, I extends QueryExpr.Query<I>,J extends QueryJoin<K,?,?,?>,E extends QueryExpr<K,I,J,?,?>> extends JoinSelect<J> {
            ImRevMap<String, K> group;

            protected ImMap<String, BaseExpr> initJoins(J groupJoin, SQLSyntax syntax) {
                group = groupJoin.group.mapRevValues(new GenNameIndex("k", "")).reverse();
                return group.join(groupJoin.group);
            }

            QuerySelect(J groupJoin) {
                super(groupJoin);
            }

            private MRevMap<I,String> mQueries = MapFact.mRevMap();
            private MExclMap<I,E> mExprs = MapFact.mExclMap(); // нужен для innerWhere и классовой информации, query транслированный -> в общее выражение
            private MSet<I> mNeedValues = SetFact.mSet();

            public ImRevMap<String, I> queries;
            protected ImMap<I,E> exprs; // нужен для innerWhere и классовой информации, query транслированный -> в общее выражение
            protected ImSet<I> needValues;
            public void finalIm() {
                queries = mQueries.immutableRev().reverse();
                mQueries = null;
                exprs = mExprs.immutable();
                mExprs = null;
                needValues = mNeedValues.immutable();
                mNeedValues = null;
            }

            public String add(I query, E expr, boolean needValue) {
                if(mQueries!=null) { // из-за getInnerWhere во from'е
                    String name = mQueries.get(query);
                    if(name==null) {
                        name = "e"+ mQueries.size();
                        mQueries.revAdd(query, name);
                        mExprs.exclAdd(query, expr);
                    }
                    if(needValue)
                        mNeedValues.add(query);
                    return alias + "." + name;
                } else // в getInnerWhere needValues уже не нужен
                    return alias + "." + queries.reverse().get(query);
            }

            protected boolean isEmptySelect(Where groupWhere, ImSet<KeyExpr> keys) {
                return groupWhere.pack().getPackWhereJoins(!syntax.useFJ() && !Settings.get().isNoExclusiveCompile(), keys, SetFact.EMPTYORDER()).first.isEmpty();
            }

            protected SQLQuery getEmptySelect(final Where groupWhere) {
                return getSQLQuery(null, Cost.MIN, MapFact.EMPTY(), StaticExecuteEnvironmentImpl.mEnv(), groupWhere, false);
            }

            protected SQLQuery getSQLQuery(String select, Cost baseCost, ImMap<String, SQLQuery> subQueries, final MStaticExecuteEnvironment mSubEnv, final Where innerWhere, boolean recursionFunction) {
                ImMap<String, Type> keyTypes = group.mapValues(value -> value.getType(innerWhere));
                ImMap<String, Type> propertyTypes = queries.mapValues(value -> exprs.get(value).getType());
                if(select == null) {
                    Function<Type, String> nullGetter = value -> value.getCast(SQLSyntax.NULL, syntax, mSubEnv);
                    ImMap<String, String> keySelect = keyTypes.mapValues(nullGetter);
                    ImMap<String, String> propertySelect = propertyTypes.mapValues(nullGetter);
                    select = "(" + SQLSession.getSelect(syntax, "empty", keySelect, propertySelect, SetFact.EMPTY()) + ")";
                }
                return new SQLQuery(select, baseCost, false, subQueries, mSubEnv.finish(), keyTypes, propertyTypes, false, recursionFunction);
            }

            protected Where pushWhere(Where groupWhere, ImSet<KeyExpr> keys, Result<SQLQuery> empty, DebugInfoWriter debugInfoWriter) {
                return pushWhere(groupWhere, keys, empty, null, debugInfoWriter);
            }
            protected Where pushWhere(Where groupWhere, ImSet<KeyExpr> keys, Result<SQLQuery> empty, Result<Pair<ImRevMap<K, KeyExpr>, Where>> pushJoinWhere, DebugInfoWriter debugInfoWriter) {
                Where fullWhere = groupWhere;
                Where pushWhere;
                Depth subQueryDepth = subcontext.getSubQueryDepth();
                if(subQueryDepth != Depth.INFINITE){
                    if ((pushWhere = whereJoins.getPushWhere(upWheres, innerJoin, subQueryDepth == Depth.LARGE, isInner(), keyStat, pushJoinWhere, debugInfoWriter)) != null) // проталкивание предиката
                        fullWhere = fullWhere.and(pushWhere);
                    if (isEmptySelect(fullWhere, keys)) { // может быть когда проталкивается верхнее условие, а внутри есть NOT оно же
                        // getKeyEquals - для надежности, так как идет перетранслирование ключей и условие может стать false, а это критично, так как в emptySelect есть cast'ы, а скажем в GroupSelect, может придти EMPTY, ключи NULL и "Class Cast'ы" будут
                        empty.set(getEmptySelect(groupWhere));
                        return null;
                    }
                } else
                    ServerLoggers.assertLog(false, "INFINITE PUSH DOWN");
                return fullWhere;
            }


            public String getSource(DebugInfoWriter debugInfoWriter) {
                SQLQuery query = getSQLQuery(debugInfoWriter);
               
                env.add(query.getEnv());
                if(Settings.get().isDisableCompiledSubQueries())
                    return query.getString();

                String sqName = subcontext.wrapSiblingSubQuery("jdfkjsd" + mSubQueries.size() + "ref");
                mSubQueries.exclAdd(sqName, query);
                return sqName;
            }

            protected abstract SQLQuery getSQLQuery(DebugInfoWriter debugInfoWriter);

        }

        private interface SubQueryExprSelect {
            String getSource(ImRevMap<ParseValue, String> subQueryParams, MStaticExecuteEnvironment mSubEnv, Result<ImMap<String, SQLQuery>> rSubQueries);
        }

        public static Where pushWhere(ImMap<KeyExpr, Expr> mapKeys, boolean pushLargeDepth, Where groupWhere, Cost costPerStat, Cost costMax, DebugInfoWriter debugInfoWriter) {
            if(mapKeys.isEmpty()) {
                if(costMax.equals(Cost.ONE))
                    return null;
                if(costPerStat.lessEquals(costMax)) // здесь, как и в общем случае действуем пессимистично - если cost * stat = costMax проталкиваем 
                    return Where.TRUE();
                else {
                    assert false;
                    return null;
                }
            }
            
            ImCol<GroupExprJoinsWhere<KeyExpr>> groupJoinsWheres = groupWhere.getGroupExprJoinsWheres(mapKeys, StatType.GROUP_SPLIT, Settings.get().isGroupStatExprWhereJoins());// не уверен, что не просто false последний параметр

            Where result = Where.FALSE();
            for(GroupExprJoinsWhere<KeyExpr> groupJoinsWhere : groupJoinsWheres) {
                LastJoin lastJoin = new LastJoin(costPerStat, costMax, groupJoinsWhere.mapExprs);  
                
                GroupJoinsWhere joinsWhere = groupJoinsWhere.joinsWhere;
                Where pushWhere = joinsWhere.getCostPushWhere(lastJoin, pushLargeDepth, StatType.PUSH_OUTER(), debugInfoWriter);
                if(pushWhere == null)
                    return null;
                result = result.or(pushWhere);
            }
            return result;
        }

        private class GroupSelect extends QuerySelect<Expr, GroupExpr.Query, GroupJoin,GroupExpr> {

            final ImSet<KeyExpr> keys;

            GroupSelect(GroupJoin groupJoin) {
                super(groupJoin);
                keys = BaseUtils.immutableCast(groupJoin.getInnerKeys());
            }
            
            public SubQueryExprSelect getLastExprSource(final GroupExpr.Query query, Where valueWhere, SubQueryContext subQueryContext, Result<Cost> rBaseCost, Result<Boolean> optAdjustLimit, final DebugInfoWriter debugInfoWriter) {
                
                final CompileSource source = InnerSelect.this;
                
                final Expr valueExpr = query.getMainExpr();
                Where where = query.getWhere();
                ImOrderMap<Expr, Boolean> orders = query.orders;
                if(query.type.isMaxMin() && needValues.contains(query)) { // MAX = LAST f(a) ORDER f(a) WHERE f(a) (но он и так в query.getWhere есть), если не нужно значение то и порядок не нужен  
                    assert orders.isEmpty();
                    orders = MapFact.singletonOrder(valueExpr, query.type == GroupType.MIN); // MAX - ASC, MIN - DESC
                }

                Query<KeyExpr,Expr> subQuery = new Query<>(keys.toRevMap(),
                        orders.keys().merge(valueExpr).toMap(), where.and(valueWhere));
                // непонятно надо ли использовать или нет, но пока методики проталкивания его нет query.ordersNotNull

                final CompiledQuery<KeyExpr, Expr> compiled = subQuery.compile(Query.reverseOrder(orders), new CompileOptions<>(source.syntax, LimitOptions.get(1), subQueryContext, debugInfoWriter != null));
                rBaseCost.set(rBaseCost.result.or(compiled.sql.baseCost));
                optAdjustLimit.set(optAdjustLimit.result || compiled.sql.optAdjustLimit);
                final String alias = subQueryContext.wrapAlias(subQueryContext.wrapSiblingSubQuery("LEALIAS")); // чтобы оставить одну колонку 

                return (subQueryParams, mSubEnv, rSubQueries) -> {
                    final Result<ImMap<Expr,String>> fromPropertyNames = new Result<>();
                    Result<ImMap<String, SQLQuery>> gSubQueries = new Result<>();
                    String select = compiled.getSelect(fromPropertyNames, gSubQueries, params.addExcl(subQueryParams), mSubEnv, 1, pushPrefix(debugInfoWriter, "EXPR LAST TOP", query)); // не rev, так как subQueryParams - идут expr'ы и они могут быть равны expression'ам
                    rSubQueries.set(rSubQueries.result.addExcl(gSubQueries.result));
                    return "(" + syntax.getSelect("(" + select+ ") " + alias, fromPropertyNames.result.get(valueExpr), "") + ")";
                };
            }

            private ImRevMap<String, SubQueryExprSelect> getLastExprSources(final StaticValueNullableExpr.Level level, Result<ImRevMap<Value, Expr>> rVirtParams, ImRevMap<Expr, KeyExpr> mapKeys, final ClassWhere<KeyExpr> keyClasses, Result<Cost> rBaseCost, Result<Boolean> optAdjustLimit, DebugInfoWriter debugInfoWriter) {
                SubQueryContext subQueryContext = subcontext;

                // генерим для всех Expr - "виртуальные" ValueExpr, and.им (хотя можно как в getExprSource просто в whereSelect докинуть, но будут проблемы с index'ами, union'ами и т.п.)
                ImSet<KeyExpr> keys = mapKeys.valuesSet();
                final ImRevMap<KeyExpr, String> keyNames = keys.mapRevValues(new GenNameIndex("pfda", "fr"));
                ImRevMap<Expr, Value> virtParams = mapKeys.mapRevValues((KeyExpr value) -> new StaticValueNullableExpr(keyClasses.getCommonClass(value), keyNames.get(value), level));
                subQueryContext = subQueryContext.pushSubQueryExprs().pushAlias(1); // чтобы не пересекались alias'ы с верхним контекстом (sibling'и могут)

                rVirtParams.set(virtParams.reverse());
                Where valueWhere = CompareWhere.compare(BaseUtils.<ImRevMap<Expr, Expr>>immutableCast(virtParams));

                ImRevValueMap<String, SubQueryExprSelect> mPropertySelect = queries.mapItRevValues();// последействие
                for(int i=0,size=queries.size();i<size;i++) {
                    GroupExpr.Query group = queries.getValue(i);

                    subQueryContext = subQueryContext.pushSiblingSubQuery(); // чтобы не пересекались alias'ы при использовании subqueryexpr

                    mPropertySelect.mapValue(i, getLastExprSource(group, valueWhere, subQueryContext, rBaseCost, optAdjustLimit, debugInfoWriter));
                }
                return mPropertySelect.immutableValueRev();
            }

            private SQLQuery getLastTopQuery(Where topWhere, ImRevMap<Expr, KeyExpr> mapKeys, final Result<ImMap<String, SQLQuery>> rSubQueries, StaticValueNullableExpr.Level level, Cost lastBaseCosts, ImRevMap<Value, Expr> virtParams, ImRevMap<String, SubQueryExprSelect> propertySelect, Where groupWhere, Cost costMax, DebugInfoWriter debugInfoWriter) {
                final Query<Expr, Object> query = new Query<>(mapKeys, topWhere);
                final CompiledQuery<Expr, Object> compiled = query.compile(new CompileOptions<>(syntax, subcontext.pushSubQuery(), debugInfoWriter != null));

                final MStaticExecuteEnvironment mSubEnv = StaticExecuteEnvironmentImpl.mEnv();
                mSubEnv.addNotMaterializable(level);

                final Result<ImMap<Expr, String>> resultKeys = new Result<>();
                final Result<ImMap<Object,String>> fromPropertySelect = new Result<>();
                final Result<ImCol<String>> whereSelect = new Result<>(); // проверить crossJoin
                String fromSelect = compiled.fillSelect(resultKeys, fromPropertySelect, whereSelect, rSubQueries, params, mSubEnv, pushPrefix(debugInfoWriter, "GROUP LAST TOP", innerJoin));

                final ImRevMap<ParseValue, String> exParams = BaseUtils.immutableCast(virtParams.mapRevValues((Expr value) -> resultKeys.result.get(value)));
                ImMap<String, String> propertySources = propertySelect.mapValues(value -> value.getSource(exParams, mSubEnv, rSubQueries));
                mSubEnv.removeNotMaterializable(level);
                
                Cost baseCost = LastJoin.calcCost(compiled.sql.baseCost, compiled.rows, lastBaseCosts, costMax);
                if(debugInfoWriter != null)
                    debugInfoWriter.addLines("CALC COST " + baseCost + " : cost top query - " + compiled.sql.baseCost + ", rows top query - " + compiled.rows + ", cost per row - " + lastBaseCosts + ", cost group - " + costMax);
                return getSQLQuery("(" + SQLSession.getSelect(syntax, fromSelect, group.join(resultKeys.result), propertySources, whereSelect.result) + ")", baseCost, rSubQueries.result, mSubEnv, groupWhere, false);
            }

            public SQLQuery getLastSQLQuery(int useGroupLastOpt, Pair<ImRevMap<Expr, KeyExpr>, Where> pushedIn, Where groupWhere, Cost baseExecCost, Result<Boolean> optAdjustLimit, DebugInfoWriter debugInfoWriter) {

                ImRevMap<Expr, KeyExpr> mapKeys;
                Where pushedInWhere = null;
                if(pushedIn != null) {
                    assert useGroupLastOpt != 3;
                    mapKeys = pushedIn.first;
                    assert mapKeys.size() == group.size();
                    pushedInWhere = pushedIn.second;
                } else {
                    if(useGroupLastOpt == 1) // если не все ключи сразу выходим
                        return null;
                    mapKeys = KeyExpr.getMapKeys(group.valuesSet());
                }
                
                final Result<ImMap<String, SQLQuery>> rSubQueries = new Result<>();

                final StaticValueNullableExpr.Level level = new StaticValueNullableExpr.Level(subcontext.getSubQueryExprs());

                Result<Cost> rLastBaseCosts = new Result<>(Cost.ONE);
                Result<ImRevMap<Value, Expr>> rVirtParams = new Result<>();
                ClassWhere<KeyExpr> keyClasses = pushedInWhere != null ? ClassWhere.get(mapKeys.valuesSet().toRevMap(), pushedInWhere) : ClassWhere.get(mapKeys.reverse(), groupWhere); // we have to use actual topWhere because classes (TC) there can be wider than in groupWhere (GC), and using groupWhere will lead to conditions like c IS GC will be eliminated
                ImRevMap<String, SubQueryExprSelect> propertySelect = getLastExprSources(level, rVirtParams, mapKeys, keyClasses, rLastBaseCosts, optAdjustLimit, debugInfoWriter);

                Where pushedOutWhere = null;
                if(useGroupLastOpt != 1 && pushedInWhere == null) // || recheck 
                    pushedOutWhere = InnerSelect.pushWhere(mapKeys.reverse(), subcontext.getSubQueryDepth() == Depth.LARGE, groupWhere, rLastBaseCosts.result, baseExecCost, pushPrefix(debugInfoWriter, "PUSH LAST TOP", innerJoin));
                
                Where topWhere = pushedInWhere != null ? pushedInWhere : pushedOutWhere;
                
                SQLQuery lastTopQuery = null;
                if(topWhere != null) {
                    lastTopQuery  = getLastTopQuery(topWhere, mapKeys, rSubQueries, level, rLastBaseCosts.result, rVirtParams.result, propertySelect, groupWhere, baseExecCost, debugInfoWriter);
                    if(pushedInWhere != null && baseExecCost.lessEquals(lastTopQuery.baseCost)) { // для pushedOutWhere проверка идет в самом алгоритме
                        pushedInWhere = null;
                        lastTopQuery = null;
                    }
//                    if(recheck && !BaseUtils.nullEquals(pushedInWhere, pushedOutWhere))
//                        pushedInWhere = pushedInWhere;
                } 
                return lastTopQuery;
            }

            public SQLQuery getSQLQuery(DebugInfoWriter debugInfoWriter) {

                boolean isLastOpt = Settings.get().getUseGroupLastOpt() != 0; // если хоть одна не оптимизируется то и остальные тоже не оптимизируем (все равно "бежать по всем" для одного из значений)
                
                Where exprWhere = Where.FALSE();
                MSet<Expr> mQueryExprs = SetFact.mSet(); // так как может одновременно и SUM и MAX нужен
                for(GroupExpr.Query query : queries.valueIt()) {
                    mQueryExprs.addAll(query.getExprs());
                    exprWhere = exprWhere.or(query.getWhere());

                    isLastOpt = isLastOpt && query.isLastOpt(needValues.contains(query)); // или можно применять last opt или не нужно значение
                }
                ImSet<Expr> queryExprs = group.values().toSet().merge(mQueryExprs.immutable());

                Where groupWhere = exprWhere.and(Expr.getWhere(group));
                
                Result<Pair<ImRevMap<Expr, KeyExpr>, Where>> pushGroupWhere = null;
                int useGroupLastOpt = 0;
                if(isLastOpt) {
                    useGroupLastOpt = Settings.get().getUseGroupLastOpt();
                    assert useGroupLastOpt != 0; 
                    if(useGroupLastOpt != 3) {
                        pushGroupWhere = new Result<>();
                        if (group.isEmpty())
                            pushGroupWhere.set(new Pair<>(MapFact.EMPTYREV(), Where.TRUE()));
                    }
                }

                Result<SQLQuery> empty = new Result<>();
                groupWhere = pushWhere(groupWhere, keys, empty, pushGroupWhere, pushPrefix(debugInfoWriter, "PUSH", innerJoin));
                if(groupWhere==null)
                    return empty.result;

                final MStaticExecuteEnvironment mSubEnv = StaticExecuteEnvironmentImpl.mEnv();
                final Result<ImCol<String>> whereSelect = new Result<>(); // проверить crossJoin
                final Result<ImMap<Expr,String>> fromPropertySelect = new Result<>();
                final Result<ImMap<String, SQLQuery>> subQueries = new Result<>();
                final Query<KeyExpr, Expr> query = new Query<>(keys.toRevMap(), queryExprs.toMap(), groupWhere);
                final CompiledQuery<KeyExpr, Expr> compiled = query.compile(new CompileOptions<>(syntax, subcontext.pushSubQuery(), debugInfoWriter != null));

                String fromSelect = compiled.fillSelect(new Result<>(), fromPropertySelect, whereSelect, subQueries, params, mSubEnv, pushPrefix(debugInfoWriter, "GROUP", innerJoin));

                ImMap<String, String> keySelect = group.join(fromPropertySelect.result);
                ImMap<String, String> propertySelect = queries.mapValues(value -> value.getSource(fromPropertySelect.result, compiled.getMapPropertyReaders(), query, syntax, mSubEnv, exprs.get(value).getType()));
                ImSet<String> areKeyValues = group.filterValues(compiled.arePropValues).keys();

                ImCol<String> havingSelect;
                if(isSingle(innerJoin))
                    havingSelect = SetFact.singleton(propertySelect.get(queries.singleKey()) + " IS NOT NULL");
                else
                    havingSelect = SetFact.EMPTY();
                SQLQuery result = getSQLQuery("(" + getGroupSelect(fromSelect, keySelect, propertySelect, whereSelect.result, havingSelect, areKeyValues) + ")", compiled.sql.baseCost, subQueries.result, mSubEnv, groupWhere, false);

                if(isLastOpt) {
                    Result<Boolean> optAdjustLimit = new Result<>(false); 
                    SQLQuery lastSQLQuery = getLastSQLQuery(useGroupLastOpt, pushGroupWhere != null ? pushGroupWhere.result : null, groupWhere, compiled.sql.baseCost, optAdjustLimit, debugInfoWriter);
                    if(lastSQLQuery != null) {
                        if(optAdjustLimit.result && !Settings.get().isDisablePessQueries())
                            lastSQLQuery.pessQuery = result;
                        return lastSQLQuery;
                    }
                }
                
                return result;
            }

            protected Where getInnerWhere() {
                // бежим по всем exprs'ам и проверяем что нет AggrType'а
                Where result = Where.TRUE();
                for(int i=0,size=exprs.size();i<size;i++) {
                    if(exprs.getKey(i).type.canBeNull())
                        return Where.TRUE();
                    result = result.or(exprs.getValue(i).getWhere());
                }
                return result;
            }
        }

        private String getGroupBy(ImCol<String> keySelect) {
            return BaseUtils.evl((syntax.supportGroupNumbers() ? ListFact.consecutiveList(keySelect.size()) : keySelect.toList()).toString(","), "3+2");
        }

        private class PartitionSelect extends QuerySelect<KeyExpr, PartitionExpr.Query, PartitionJoin,PartitionExpr> {

            final ImMap<KeyExpr,BaseExpr> mapKeys;
            private PartitionSelect(PartitionJoin partitionJoin) {
                super(partitionJoin);
                mapKeys = partitionJoin.group;
            }

            public SQLQuery getSQLQuery(DebugInfoWriter debugInfoWriter) {

                MSet<Expr> mQueryExprs = SetFact.mSet();
                for(PartitionExpr.Query query : queries.valueIt())
                    mQueryExprs.addAll(query.getExprs());
                ImSet<Expr> queryExprs = mQueryExprs.immutable();

                Where innerWhere = innerJoin.getWhere();

                Result<SQLQuery> empty = new Result<>();
                innerWhere = pushWhere(innerWhere, group.valuesSet(), empty, pushPrefix(debugInfoWriter, "PUSH", innerJoin));
                if(innerWhere == null)
                    return empty.result;

                MStaticExecuteEnvironment mSubEnv = StaticExecuteEnvironmentImpl.mEnv();
                Result<ImMap<String,String>> keySelect = new Result<>();
                Result<ImMap<Expr,String>> fromPropertySelect = new Result<>();
                Result<ImCol<String>> whereSelect = new Result<>(); // проверить crossJoin
                Result<ImMap<String, SQLQuery>> subQueries = new Result<>();
                Query<String, Expr> subQuery = new Query<>(group, queryExprs.toMap(), innerWhere);
                CompiledQuery<String, Expr> compiledSubQuery = subQuery.compile(new CompileOptions<>(syntax, subcontext.pushSubQuery(), debugInfoWriter != null));
                String fromSelect = compiledSubQuery.fillSelect(keySelect, fromPropertySelect, whereSelect, subQueries, params, mSubEnv, pushPrefix(debugInfoWriter, "PARTITION", innerJoin));

                // обработка multi-level order'ов
                MExclMap<PartitionToken, String> mTokens = MapFact.mExclMap();
                ImRevValueMap<String, PartitionCalc> mResultNames = queries.mapItRevValues();// последействие (mTokens)
                for(int i=0,size=queries.size();i<size;i++) {
                    PartitionExpr.Query query = queries.getValue(i);
                    PartitionCalc calc = query.type.createAggr(mTokens,
                            query.exprs.mapList(fromPropertySelect.result),
                            subQuery.getCompileOrders(query.orders).map(fromPropertySelect.result),
                            query.partitions.map(fromPropertySelect.result).toSet(), syntax, query.getType(), mSubEnv);
                    mResultNames.mapValue(i, calc);
                }
                final ImRevMap<PartitionCalc, String> resultNames = mResultNames.immutableValueRev().reverse();
                ImMap<PartitionToken, String> tokens = mTokens.immutable();

                for(int i=1;;i++) {
                    MSet<PartitionToken> mNext = SetFact.mSet();
                    boolean last = true;
                    for(int j=0,size=tokens.size();j<size;j++) {
                        PartitionToken token = tokens.getKey(j);
                        ImSet<PartitionCalc> tokenNext = token.getNext();
                        boolean neededUp = tokenNext.isEmpty(); // верхний, надо протаскивать
                        last = true;
                        for(PartitionCalc usedToken : tokenNext) {
                            if(usedToken.getLevel() >= i) {
                                if(usedToken.getLevel() == i) // если тот же уровень
                                    mNext.add(usedToken);
                                else
                                    neededUp = true;
                                last = false;
                            }
                        }
                        if(neededUp)
                            mNext.add(token);
                    }
                    ImSet<PartitionToken> next = mNext.immutable();
                    
                    if(last)
                        return getSQLQuery(fromSelect, compiledSubQuery.sql.baseCost, subQueries.result, mSubEnv, innerWhere, false);

                    ImRevMap<PartitionToken, String> nextTokens = next.mapRevValues((i1, token) -> {
                        return token.getNext().isEmpty() ? resultNames.get((PartitionCalc) token) : "ne" + i1; // если верхний то нужно с нормальным именем тащить
                    });
                    final ImMap<PartitionToken, String> ftokens = tokens;
                    ImMap<String, String> propertySelect = nextTokens.reverse().mapValues(value -> {
//                            Type resultType = null; String resultName;
//                            if(value instanceof PartitionCalc && (resultName = resultNames.get((PartitionCalc) value))!=null)
//                                resultType = queries.get(resultName).getType();
                        return value.getSource(ftokens, syntax);
                    });

                    fromSelect = "(" + SQLSession.getSelect(syntax, fromSelect + (i>1?" q":""), keySelect.result, propertySelect, (i>1?SetFact.EMPTY():whereSelect.result)) + ")";
                    keySelect.set(keySelect.result.keys().toMap()); // ключи просто превращаем в имена
                    tokens = nextTokens;
                }
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE();
                for(int i=0,size=exprs.size();i<size;i++)
                    if(!exprs.getKey(i).type.canBeNull())
                        result = result.and(exprs.getValue(i).getWhere());
                return result;
            }
        }

        private class SubQuerySelect extends QuerySelect<KeyExpr, SubQueryExpr.Query, SubQueryJoin,SubQueryExpr> {

            final ImMap<KeyExpr,BaseExpr> mapKeys;
            private SubQuerySelect(SubQueryJoin subQueryJoin) {
                super(subQueryJoin);
                mapKeys = subQueryJoin.group;
            }

            public SQLQuery getSQLQuery(DebugInfoWriter debugInfoWriter) {

                Where innerWhere = innerJoin.getWhere();

                Result<SQLQuery> empty = new Result<>();
                innerWhere = pushWhere(innerWhere, group.valuesSet(), empty, debugInfoWriter);
                if(innerWhere==null)
                    return empty.result;

                MStaticExecuteEnvironment mSubEnv = StaticExecuteEnvironmentImpl.mEnv();
                Result<ImMap<String, String>> keySelect = new Result<>();
                Result<ImMap<String, String>> propertySelect = new Result<>();
                Result<ImCol<String>> whereSelect = new Result<>();
                Result<ImMap<String, SQLQuery>> subQueries = new Result<>();
                CompiledQuery<String, String> compiledQuery = new Query<>(group, queries.mapValues(value -> value.expr), innerWhere).compile(new CompileOptions<>(syntax, subcontext.pushSubQuery(), debugInfoWriter != null));
                String fromSelect = compiledQuery.fillSelect(keySelect, propertySelect, whereSelect, subQueries, params, mSubEnv, pushPrefix(debugInfoWriter, "SUBQUERY", innerJoin));
                return getSQLQuery("(" + SQLSession.getSelect(syntax, fromSelect, keySelect.result, propertySelect.result, whereSelect.result) + ")", compiledQuery.sql.baseCost, subQueries.result, mSubEnv, innerWhere, false);
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE();
                for(int i=0,size=exprs.size();i<size;i++)
                    result = result.and(exprs.getValue(i).getWhere());
                return result;
            }
        }


        protected String getGroupSelect(String fromSelect, ImOrderMap<String, String> keySelect, ImOrderMap<String, String> propertySelect, ImCol<String> whereSelect, ImCol<String> havingSelect, final ImSet<String> areKeyValues) {
            String groupBy;
            boolean supportGroupNumbers = syntax.supportGroupNumbers();
            ImOrderMap<String, String> fixedKeySelect = keySelect;
            if(!areKeyValues.isEmpty() && !supportGroupNumbers) {
                keySelect = keySelect.mapOrderValues((key, value) -> {
                    if(areKeyValues.contains(key))
                        return syntax.getAnyValueFunc() + "(" + value + ")";
                    return value;
                });
                fixedKeySelect = keySelect.filterOrder(new NotFunctionSet<>(areKeyValues));
            }
            if(fixedKeySelect.isEmpty()) {
                if(syntax.supportGroupSingleValue())
                    groupBy = "3+2";
                else {
                    groupBy = "";
                    havingSelect = havingSelect.addCol("COUNT(*) > 0");
                }
            } else
                groupBy =  BaseUtils.evl((supportGroupNumbers ? ListFact.consecutiveList(keySelect.size()) : fixedKeySelect.values().toList()).toString(","), "3+2");
            return syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect, propertySelect), whereSelect.toString(" AND "), "", groupBy, havingSelect.toString(" AND "), "", false);
        }
        protected String getGroupSelect(String fromSelect, ImMap<String, String> keySelect, ImMap<String, String> propertySelect, ImCol<String> whereSelect, ImCol<String> havingSelect, ImSet<String> areKeyValues) {
            return getGroupSelect(fromSelect, keySelect.toOrderMap(), propertySelect.toOrderMap(), whereSelect, havingSelect, areKeyValues);
        }

        private class RecursiveSelect extends QuerySelect<KeyExpr,RecursiveExpr.Query, RecursiveJoin,RecursiveExpr> {
            private RecursiveSelect(RecursiveJoin recJoin) {
                super(recJoin);
            }

            private String getSelect(ImRevMap<String, KeyExpr> keys, ImMap<String, Expr> props, final ImMap<String, Type> columnTypes, Where where, Result<ImOrderSet<String>> keyOrder, Result<ImOrderSet<String>> propOrder, boolean useRecursionFunction, boolean recursive, ImRevMap<ParseValue, String> params, SubQueryContext subcontext, Result<Cost> baseCost, Result<ImMap<String, SQLQuery>> subQueries, final MStaticExecuteEnvironment env, DebugInfoWriter debugInfoWriter) {
                ImRevMap<String, KeyExpr> itKeys = innerJoin.getMapIterate().mapRevKeys(new GenNameIndex("pv_", ""));

                Result<ImMap<String, String>> keySelect = new Result<>();
                Result<ImMap<String, String>> propertySelect = new Result<>();
                Result<ImCol<String>> whereSelect = new Result<>();
                Result<ImMap<String, SQLQuery>> innerSubQueries = new Result<>();
                CompiledQuery<String, String> compiledQuery = new Query<>(keys.addRevExcl(itKeys), props, where).compile(new CompileOptions<>(syntax, subcontext, recursive && !useRecursionFunction, debugInfoWriter != null));
                String fromSelect = compiledQuery.fillSelect(keySelect, propertySelect, whereSelect, innerSubQueries, params, env, pushPrefix(debugInfoWriter, recursive ? "STEP" : "INITIAL", innerJoin));

                ImMap<String, SQLQuery> compiledSubQueries = innerSubQueries.result;
                if(subQueries.result != null) // по аналогии с subEnv
                    compiledSubQueries = compiledSubQueries.addExcl(subQueries.result);
                subQueries.set(compiledSubQueries);

                Cost compiledBaseCost = compiledQuery.sql.baseCost;
                if(baseCost.result != null)
                    compiledBaseCost = compiledBaseCost.or(baseCost.result);
                baseCost.set(compiledBaseCost);

                ImOrderMap<String, String> orderKeySelect = SQLSession.mapNames(keySelect.result.filterIncl(keys.keys()), keyOrder);
                ImOrderMap<String, String> orderPropertySelect = SQLSession.mapNames(propertySelect.result, propOrder);
                
                ImSet<String> areKeyValues = compiledQuery.areKeyValues.filter(keys.keys());
                if(useRecursionFunction) {
                    orderPropertySelect = orderPropertySelect.mapOrderValues((key, value) -> {
                        Type type = columnTypes.get(key);
                        return (type instanceof ArrayClass ? GroupType.AGGAR_SETADD : GroupType.SUM).getSource(
                                ListFact.singleton(value), ListFact.singleton(type), MapFact.EMPTYORDER(), type, syntax, env);
                    });
                }

                // у рекурсий есть проблемы с cast'ом (в WITH RECURSIVE странная ошибка столбец имеет тип int, а результат bigint);
                BiFunction<String, String, String> castTypes = (key, value) -> columnTypes.get(key).getCast(value, syntax, env);
                orderKeySelect = orderKeySelect.mapOrderValues(castTypes);
                orderPropertySelect = orderPropertySelect.mapOrderValues(castTypes);
                if(useRecursionFunction)
                    return getGroupSelect(fromSelect, orderKeySelect, orderPropertySelect, whereSelect.result, SetFact.EMPTY(), areKeyValues);
                else
                    return SQLSession.getSelect(syntax, fromSelect, orderKeySelect, orderPropertySelect, whereSelect.result);
            }
            
            public SQLQuery getParamSource(final boolean useRecursionFunction, final boolean wrapStep, DebugInfoWriter debugInfoWriter) {
                ImRevMap<KeyExpr, KeyExpr> mapIterate = innerJoin.getMapIterate();

                Where initialWhere = innerJoin.getInitialWhere();
                final Where baseInitialWhere = initialWhere;

                final boolean isLogical = innerJoin.isLogical();
                final boolean cyclePossible = innerJoin.isCyclePossible();

                boolean single = isSingle(innerJoin);

                String rowPath = "qwpather";

                ImMap<String, Type> propTypes;
                final MStaticExecuteEnvironment mSubEnv = StaticExecuteEnvironmentImpl.mEnv();

                ImMap<String, String> propertySelect;
                if(isLogical) {
                    propTypes = MapFact.EMPTY();
                    propertySelect = queries.mapValues(() -> syntax.getBitString(true));
                } else {
                    // тут возможно baseInitialWhere надо
                    propTypes = queries.mapValues(RecursiveExpr.Query::getType);
                    propertySelect = queries.mapValues((key, value) -> GroupType.SUM.getSource(ListFact.singleton(key), null, MapFact.<String, CompileOrder>EMPTYORDER(), value.getType(), syntax, mSubEnv));
                }

                Expr rowKeys = null; ArrayClass rowType = null; Expr rowSource = null;
                final boolean needRow = cyclePossible && (!isLogical || useRecursionFunction);
                if(needRow) {
                    rowKeys = ConcatenateExpr.create(mapIterate.keys().toOrderSet());
                    rowType = ArrayClass.get(rowKeys.getType(innerJoin.getClassWhere())); // classWhere а не initialWhere, чтобы общий тип был и не было проблем с cast'ом ConcatenateType'ов
                    propTypes = propTypes.addExcl(rowPath, rowType);

                    rowSource = FormulaExpr.createCustomFormula(syntax.getArrayConstructor("prm1", rowType, mSubEnv), rowType, rowKeys); // баг сервера, с какого-то бодуна ARRAY[char(8)] дает text[]
                }

                // проталкивание
                ImMap<KeyExpr, BaseExpr> staticGroup = innerJoin.getJoins().remove(mapIterate.keys());
                Result<SQLQuery> empty = new Result<>();
                initialWhere = pushWhere(initialWhere, staticGroup.keys(), empty, pushPrefix(debugInfoWriter, "PUSH", innerJoin));
                if(initialWhere==null)
                    return empty.result;

                // чтение params (outer / inner и типов)
                boolean noDynamicSQL = syntax.noDynamicSQL();

                String outerParams = null;
                ImRevMap<ParseValue, String> innerParams;
                ImList<FunctionType> types = null;
                if(useRecursionFunction) {
                    ImSet<OuterContext> outerContext = SetFact.merge(queries.valuesSet(), initialWhere);
                    ImSet<ParseValue> values = SetFact.addExclSet(AbstractOuterContext.getOuterColValues(outerContext), AbstractOuterContext.getOuterStaticValues(outerContext)); // не static values
                    outerParams = "";
                    ImRevValueMap<ParseValue, String> mvInnerParams = values.mapItRevValues(); // "совместная" обработка / последействие
                    MList<FunctionType> mParamTypes = ListFact.mListMax(values.size());
                    for(int i=0,size=values.size();i<size;i++) {
                        ParseValue value = values.get(i);
                        String paramValue = params.get(value);
                        // в данном случае опасно проверять на safeString, так как при перекомпиляции может быть другое значение не safe и будет падать ошибка, плюс те же вопросы с локализацией
                        if(!value.isAlwaysSafeString() || (noDynamicSQL && !(value instanceof StaticValueExpr))) {
                            outerParams = (outerParams.length() == 0 ? "" : outerParams + "," ) + paramValue;
                            mParamTypes.add(value.getFunctionType());
                            paramValue = syntax.getParamUsage(mParamTypes.size());
                        } else
                            mSubEnv.addNoPrepare();
                        mvInnerParams.mapValue(i, paramValue);
                    }
                    innerParams = mvInnerParams.immutableValueRev();
                    types = mParamTypes.immutableList();
                } else
                    innerParams = params;

                RecursiveJoin tableInnerJoin = innerJoin;
                if(!BaseUtils.hashEquals(initialWhere, baseInitialWhere)) // проверка на hashEquals - оптимизация, само такое проталкивание нужно чтобы у RecursiveTable - статистика была правильной
                    tableInnerJoin = new RecursiveJoin(innerJoin, initialWhere);

                ImRevMap<String, KeyExpr> recKeys = tableInnerJoin.genKeyNames();
                ImMap<String, Type> columnTypes = propTypes.addExcl(recKeys.mapValues((KeyExpr value) -> value.getType(baseInitialWhere)));

                SubQueryContext pushContext = subcontext.pushRecursion();// чтобы имена не пересекались
                pushContext = pushContext.pushSubQuery();
                
                Result<ImOrderSet<String>> keyOrder = new Result<>(); Result<ImOrderSet<String>> propOrder = new Result<>();
                Result<ImMap<String, SQLQuery>> rSubQueries = new Result<>();
                Result<Cost> baseCost = new Result<>();

                // INIT

                String initialSelect = getInitialSelect(initialWhere, recKeys, columnTypes, innerParams, useRecursionFunction, isLogical, pushContext, needRow, rowPath, rowSource, keyOrder, propOrder, mSubEnv, rSubQueries, baseCost, debugInfoWriter);

                // STEP

                if(!Settings.get().isDisableCompiledSubQueries())
                    pushContext = pushContext.pushSiblingSubQuery();

                String recName = subcontext.wrapRecursion("rectable");

                Result<ImMap<String, SQLQuery>> stepSubQueries = new Result<>();
                String stepSelect = getStepSelect(tableInnerJoin, wrapStep, null, recName, recKeys, propTypes, columnTypes, innerParams, useRecursionFunction, pushContext, isLogical, needRow, rowPath, rowKeys, rowType, rowSource, keyOrder, propOrder, mSubEnv, stepSubQueries, baseCost, debugInfoWriter);
                rSubQueries.set(rSubQueries.result.addExcl(stepSubQueries.result));

                int smallLimit = 0;
                String stepSmallSelect = "";
                if(useRecursionFunction) {
                    int adjustCount = Settings.get().getAdjustRecursionStat();
                    Stat adjustStat = new Stat(adjustCount);
                    if (adjustStat.less(tableInnerJoin.getStatKeys(StatType.ADJUST_RECURSION).getRows())) { // если статистика
                        // выполняем с тем же контекстом чтобы проверить протолкнется ли такой предикат или нет (одновременно с самим запросом не получилось бы из-за подзапросов)
                        Result<ImMap<String, SQLQuery>> smallSubQueries = new Result<>();
                        stepSmallSelect = getStepSelect(tableInnerJoin, wrapStep, adjustStat, recName, recKeys, propTypes, columnTypes, innerParams, useRecursionFunction, pushContext, isLogical, needRow, rowPath, rowKeys, rowType, rowSource, keyOrder, propOrder, StaticExecuteEnvironmentImpl.mEnv(), smallSubQueries, new Result<>(), null);
                        if(BaseUtils.hashEquals(stepSmallSelect, stepSelect) && BaseUtils.hashEquals(smallSubQueries.result, stepSubQueries.result)) { // в env'ы записываем только если протолкнулось
                            stepSmallSelect = "";
                        } else {
                            smallLimit = adjustCount;

                            if(!Settings.get().isDisableCompiledSubQueries())
                                pushContext = pushContext.pushSiblingSubQuery();

                            stepSmallSelect = getStepSelect(tableInnerJoin, wrapStep, adjustStat, recName, recKeys, propTypes, columnTypes, innerParams, useRecursionFunction, pushContext, isLogical, needRow, rowPath, rowKeys, rowType, rowSource, keyOrder, propOrder, mSubEnv, rSubQueries, baseCost, null);
                        }
                    }
                }

                // RESULT

                ImOrderSet<String> columnOrder = keyOrder.result.addOrderExcl(propOrder.result);
                ImMap<String, SQLQuery> subQueries = rSubQueries.result;

                ImCol<String> havingSelect;
                if(single)
                    havingSelect = SetFact.singleton(propertySelect.get(queries.singleKey()) + " IS NOT NULL");
                else
                    havingSelect = SetFact.EMPTY();

                ImMap<String, String> keySelect = group.crossValuesRev(recKeys);
                mSubEnv.addVolatileStats();
                String select;
                if(useRecursionFunction) {
                    mSubEnv.addNoReadOnly();
                    String fieldDeclare = Field.getDeclare(columnOrder.mapOrderMap(columnTypes), syntax, mSubEnv);
                    select = getGroupSelect(syntax.getRecursion(types, recName, initialSelect, stepSelect, stepSmallSelect, smallLimit, fieldDeclare, outerParams, mSubEnv),
                            keySelect, propertySelect, SetFact.EMPTY(), havingSelect, SetFact.EMPTY());
                } else {
                    if(SQLQuery.countMatches(stepSelect, recName, subQueries) > 1) // почти у всех SQL серверов ограничение что не больше 2-х раз CTE можно использовать
                        return null;
                    String recursiveWith = "WITH RECURSIVE " + recName + "(" + columnOrder.toString(",") + ") AS ((" + initialSelect +
                            ") UNION " + (isLogical && cyclePossible?"":"ALL ") + "(" + stepSelect + ")) ";
                    select = recursiveWith + (isLogical ? SQLSession.getSelect(syntax, recName, keySelect, propertySelect, SetFact.EMPTY())
                            : getGroupSelect(recName, keySelect, propertySelect, SetFact.EMPTY(), havingSelect, SetFact.EMPTY()));
                }
                return getSQLQuery("(" + select + ")", baseCost.result, subQueries, mSubEnv, baseInitialWhere, useRecursionFunction);
            }

            private String getInitialSelect(Where initialWhere, ImRevMap<String, KeyExpr> keyNames, ImMap<String, Type> columnTypes, ImRevMap<ParseValue, String> innerParams, boolean useRecursionFunction, boolean isLogical, SubQueryContext pushContext, boolean needRow, String rowPath, Expr rowSource, Result<ImOrderSet<String>> keyOrder, Result<ImOrderSet<String>> propOrder, MStaticExecuteEnvironment mSubEnv, Result<ImMap<String, SQLQuery>> subQueries, Result<Cost> baseCost, DebugInfoWriter debugInfoWriter) {
                ImRevMap<String, Expr> initialExprs;
                if(isLogical) {
                    initialExprs = MapFact.EMPTYREV();
                } else {
                    initialExprs = queries.mapRevValues((RecursiveExpr.Query value) -> value.initial);
                }

                if(needRow) {
                    initialExprs = initialExprs.addRevExcl(rowPath, rowSource); // заполняем начальный путь
                }

                assert initialExprs.addExcl(keyNames).mapValues(value -> value.getType(innerJoin.getInitialWhere())).equals(columnTypes);

                return getSelect(keyNames, initialExprs, columnTypes, initialWhere, keyOrder, propOrder, useRecursionFunction, false, innerParams, pushContext, baseCost, subQueries, mSubEnv, debugInfoWriter);
            }

            private String getStepSelect(RecursiveJoin tableJoin, final boolean wrapStep, Stat adjustStat, String tableName, ImRevMap<String, KeyExpr> keyNames, ImMap<String, Type> propTypes, ImMap<String, Type> columnTypes, ImRevMap<ParseValue, String> innerParams, boolean useRecursionFunction, SubQueryContext pushContext, boolean isLogical, boolean needRow, String rowPath, Expr rowKeys, ArrayClass rowType, Expr rowSource, Result<ImOrderSet<String>> keyOrder, Result<ImOrderSet<String>> propOrder, MStaticExecuteEnvironment mSubEnv, Result<ImMap<String, SQLQuery>> subQueries, Result<Cost> baseCost, DebugInfoWriter debugInfoWriter) {
                assert keyOrder.result != null && propOrder.result != null; // уже в initial должны быть заполнены

                Where stepWhere = innerJoin.getStepWhere();
                final Where wrapClassWhere = wrapStep ? tableJoin.getIsClassWhere() : null;
                if(wrapStep) // чтобы избавляться от проблем с 2-м использованием
                    stepWhere = SubQueryExpr.create(stepWhere.and(wrapClassWhere), false);

                Result<RecursiveTable> recursiveTable = new Result<>();
                final Join<String> recJoin = tableJoin.getRecJoin(propTypes, tableName, keyNames, adjustStat, recursiveTable);

                ImMap<String, Expr> stepExprs;
                if(isLogical) {
                    stepExprs = MapFact.EMPTY();
                } else {
                    stepExprs = queries.mapValues((key, value) -> {
                        Expr step = value.step;
                        if (wrapStep)
                            step = SubQueryExpr.create(step.and(wrapClassWhere), false);
                        return recJoin.getExpr(key).mult(step, (IntegralClass) value.getType());
                    });
                }

                Where recWhere;
                if(needRow) {
                    Expr prevPath = recJoin.getExpr(rowPath);

                    Where noNodeCycle = rowKeys.compare(prevPath, Compare.INARRAY).not();
                    if(isLogical)
                        recWhere = recJoin.getWhere().and(noNodeCycle);
                    else {
                        recWhere = Where.TRUE();
                        ImValueMap<String, Expr> mStepExprs = stepExprs.mapItValues(); // "совместное" заполнение
                        for(int i=0,size=stepExprs.size();i<size;i++) {
                            String key = stepExprs.getKey(i);
                            IntegralClass type = (IntegralClass)queries.get(key).getType();
                            Expr maxExpr = type.getStaticExpr(type.getSafeInfiniteValue());
                            mStepExprs.mapValue(i, stepExprs.getValue(i).ifElse(noNodeCycle, maxExpr)); // если цикл даем максимальное значение
                            recWhere = recWhere.and(recJoin.getExpr(key).compare(maxExpr, Compare.LESS)); // останавливаемся если количество значений становится очень большим
                        }
                        stepExprs = mStepExprs.immutableValue();
                    }

                    stepExprs = stepExprs.addExcl(rowPath, FormulaExpr.createCustomFormula(syntax.getArrayConcatenate(rowType, "prm1", "prm2", mSubEnv), rowType, prevPath, rowSource)); // добавляем тек. вершину
                } else
                    recWhere = recJoin.getWhere();

                MStaticExecuteEnvironment mSubSelectEnv = StaticExecuteEnvironmentImpl.mEnv();

                String result = getSelect(keyNames, stepExprs, columnTypes, stepWhere.and(recWhere), keyOrder, propOrder, useRecursionFunction, true, innerParams, pushContext, baseCost, subQueries, mSubSelectEnv, debugInfoWriter);

                mSubSelectEnv.removeNotMaterializable(recursiveTable.result);
                mSubEnv.add(mSubSelectEnv.finish());

                return result;
            }

            private SQLQuery getCTESource(boolean wrapExpr, DebugInfoWriter debugInfoWriter) {
                return getParamSource(false, wrapExpr, debugInfoWriter);
            }
            public SQLQuery getSQLQuery(DebugInfoWriter debugInfoWriter) {
                boolean isLogical = innerJoin.isLogical();
                boolean cyclePossible = innerJoin.isCyclePossible();

                // проверка на cyclePossible, потому как в противном случае количество записей в итерации (так как туда ключом попадет путь) будет расти экспоненциально
                if((isLogical || !cyclePossible) && syntax.enabledCTE()) { // если isLogical или !cyclePossible пытаемся обойтись рекурсивным CTE
                    SQLQuery cteSelect = getCTESource(false, debugInfoWriter);
                    if(cteSelect!=null)
                        return cteSelect;
                    cteSelect = getCTESource(true, debugInfoWriter);
                    if(cteSelect!=null)
                        return cteSelect;
                }
                return getParamSource(true, false, debugInfoWriter);
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE();
                for(RecursiveExpr expr : exprs.valueIt())
                    result = result.and(expr.getWhere());
                return result;
            }
        }

        // выделять отдельный Join, в общем то чтобы нормально использовался ANTI-JOIN в некоторых СУБД, (а он нужен в свою очередь чтобы A LEFT JOIN B WHERE B.e IS NULL)
        // не используется, потому a) как пока нет механизма выявления что идет именно ANTI-JOIN, в момент getSource
        // b) anti-join не сильно быстрее обычной стратегии с left join + filter
        private boolean isSingle(QueryJoin join) {
            return Settings.get().isUseSingleJoins() && (join instanceof GroupJoin || join instanceof RecursiveJoin) && !isInner(join);
        }

        private QuerySelect<?,?,?,?> getSingleSelect(QueryExpr queryExpr) {
            assert isSingle(queryExpr.getInnerJoin());
            for(int i=0,size=queries.size();i<size;i++) {
                QuerySelect select = queries.getValue(i);
                if(select.queries.size()==1)
                    if(BaseUtils.hashEquals(queryExpr, select.exprs.singleValue()))
                        return select;
            }
            return null;
        }
        public String getNullSource(InnerExpr innerExpr, String defaultSource) {
            InnerJoin<?, ?> innerJoin = innerExpr.getInnerJoin();
            if (innerExpr instanceof QueryExpr) {
                QueryExpr queryExpr = (QueryExpr) innerExpr;
                if (isSingle((QueryJoin) innerJoin)) {
                    QuerySelect singleSelect = getSingleSelect(queryExpr);
                    ImSet keys = singleSelect.group.keys();
                    if (!keys.isEmpty())
                        return singleSelect.alias + "." + keys.get(0) + " IS NULL";
                }
            }
            String result = super.getNullSource(innerExpr, defaultSource);
            // решает частично ту же проблему что и верхняя проверка
            if(syntax.hasNullWhereEstimateProblem() && whereCompiling && !Settings.get().isDisableAntiJoinOptimization() && !isInner(innerJoin) && isOptAntiJoin(innerJoin)) { // тут даже assert isInner возможно
                result = "(" + result + " OR " + syntax.getAdjustSelectivityPredicate() + ")";
            }
            return result;
        }

        final MAddExclMap<QueryJoin, QuerySelect> queries = MapFact.mAddExclMap();
        final MAddExclMap<GroupExpr, String> groupExprSources = MapFact.mAddExclMap();
        public String getSource(QueryExpr queryExpr, boolean needValue) {
            if(queryExpr instanceof GroupExpr) {
                GroupExpr groupExpr = (GroupExpr)queryExpr;
                if(Settings.get().getInnerGroupExprs() >0 && !isInner(groupExpr.getInnerJoin())) { // если left join
                    String groupExprSource = groupExprSources.get(groupExpr);
                    if(groupExprSource==null) {
                        groupExprSource = groupExpr.getExprSource(this, subcontext.pushAlias(groupExprSources.size()));
                        groupExprSources.exclAdd(groupExpr, groupExprSource);
                    }
                    return groupExprSource;
                }
            }

            QueryJoin exprJoin = queryExpr.getInnerJoin();

            QuerySelect select = null;
            Result<QueryExpr.Query> query = new Result<>(queryExpr.query);
            
            if(select == null && isSingle(exprJoin)) {
                select = getSingleSelect(queryExpr);
                usedJoin(select);
            }

            if(select == null)
                select = getQuerySelect(exprJoin, query);            
            return select.add(query.result,queryExpr, needValue);
        }
        private JoinSelect getJoinSelect(InnerJoin innerJoin) {
            if(!whereCompiling) { // поаналогии с QuerySelect.add до innerWhere и после
                if (innerJoin instanceof Table.Join)
                    return getTableSelect((Table.Join) innerJoin);
                if (innerJoin instanceof QueryJoin)
                    return getQuerySelect((QueryJoin) innerJoin, null);
            } else {
                if (innerJoin instanceof Table.Join)
                    return tables.get((Table.Join) innerJoin);
                if (innerJoin instanceof QueryJoin)
                    return queries.get((QueryJoin) innerJoin);
            }
            throw new RuntimeException("no matching class");
        }
        private TableSelect getTableSelect(Table.Join table) {
            TableSelect join = tables.get(table);
            if(join==null) {
                join = new TableSelect(table);
                tables.exclAdd(table,join);
            }
            usedJoin(join);
            return join;
        }
        private QuerySelect getQuerySelect(QueryJoin exprJoin, Result<QueryExpr.Query> query) {
            QuerySelect select = null;
            // группировка query / кэгирование
            for (int i = 0, size = queries.size(); i < size; i++) {
                MapTranslate translator;
                if ((translator = exprJoin.mapInnerIdentity(queries.getKey(i), false)) != null) {
                    select = queries.getValue(i);
                    if(query != null)
                        query.set((QueryExpr.Query<?>)query.result.translateOuter(translator));
                }
            }

            if(select == null) { // нету группы - создаем, чтобы не нарушать модульность сделаем без наследования
                if (exprJoin instanceof GroupJoin)
                    select = new GroupSelect((GroupJoin) exprJoin);
                else if (exprJoin instanceof PartitionJoin)
                    select = new PartitionSelect((PartitionJoin) exprJoin);
                else if (exprJoin instanceof RecursiveJoin)
                    select = new RecursiveSelect((RecursiveJoin) exprJoin);
                else
                    select = new SubQuerySelect((SubQueryJoin) exprJoin);

                queries.exclAdd(exprJoin, select);
            }
            usedJoin(select);
            return select;
        }
    }

    public static RuntimeException getIncorrectOperationException() {
        return new RuntimeException(ThreadLocalContext.localize("{exceptions.incorrect.set.operation}"));
    }

    private static <V> ImMap<V, String> castProperties(ImMap<V, String> propertySelect, final ImMap<V, Type> castTypes, final SQLSyntax syntax, final TypeEnvironment typeEnv) { // проставим Cast'ы для null'ов
        return propertySelect.mapValues((key, propertyString) -> {
            Type castType;
            // проблемы бывают когда NULL - автоматический cast к text'у, и когда результат blankPadded, а внутри могут быть нет
            if ((castType = castTypes.get(key)) != null && (propertyString.equals(SQLSyntax.NULL) || (castType instanceof StringClass && ((StringClass)castType).blankPadded)))
                propertyString = castType.getCast(propertyString, syntax, typeEnv);
            return propertyString;
        });
    }

    // castTypes параметр чисто для бага Postgre и может остальных
    private static <K,V> String getInnerSelect(ImRevMap<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, ImMap<V, Expr> compiledProps, ImRevMap<ParseValue, String> params, ImOrderMap<V, Boolean> orders, LimitOptions limit, SQLSyntax syntax, ImRevMap<K, String> keyNames, ImRevMap<V, String> propertyNames, Result<ImOrderSet<K>> keyOrder, Result<ImOrderSet<V>> propertyOrder, ImMap<V, Type> castTypes, SubQueryContext subcontext, boolean noInline, MStaticExecuteEnvironment env, Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MExclMap<String, SQLQuery> mSubQueries, DebugInfoWriter debugInfoWriter) {
        compiledProps = innerSelect.getFullWhere().followTrue(compiledProps, !innerSelect.isComplex());

        Result<ImMap<K,String>> andKeySelect = new Result<>(); Result<ImCol<String>> andWhereSelect = new Result<>(); Result<ImMap<V,String>> andPropertySelect = new Result<>();
        String andFrom = fillInnerCastSelect(mapKeys, innerSelect, compiledProps, andKeySelect, andPropertySelect, andWhereSelect, params, syntax, subcontext, limit, orders.keyOrderSet(), env, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, null, null, debugInfoWriter, castTypes);

        return getSelect(andFrom, andKeySelect.result, keyNames, keyOrder, andPropertySelect.result, propertyNames, propertyOrder, andWhereSelect.result, syntax, getPackedCompileOrders(compiledProps, innerSelect.getFullWhere(), orders), limit, noInline);
    }

    private static <K, V> String fillInnerCastSelect(ImRevMap<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, ImMap<V, Expr> compiledProps, Result<ImMap<K, String>> andKeySelect, Result<ImMap<V, String>> andPropertySelect, Result<ImCol<String>> andWhereSelect, ImRevMap<ParseValue, String> params, SQLSyntax syntax, SubQueryContext subcontext, LimitOptions limit, ImOrderSet<V> orders, MStaticExecuteEnvironment env, Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MExclMap<String, SQLQuery> mSubQueries, Result<ImSet<K>> resultKeyValues, Result<ImSet<V>> resultPropValues, DebugInfoWriter debugInfoWriter, ImMap<V, Type> castTypes) {
        String andFrom = fillInnerSelect(mapKeys, innerSelect, compiledProps, andKeySelect, andPropertySelect, andWhereSelect, params, syntax, subcontext, limit, orders, env, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, resultKeyValues, resultPropValues, debugInfoWriter);

        if(castTypes!=null)
            andPropertySelect.set(castProperties(andPropertySelect.result, castTypes, syntax, env));

        return andFrom;
    }

    private static <K,V> String getSelect(String from, ImMap<K, String> keySelect, ImRevMap<K, String> keyNames, Result<ImOrderSet<K>> keyOrder, ImMap<V, String> propertySelect, ImRevMap<V, String> propertyNames, Result<ImOrderSet<V>> propertyOrder, ImCol<String> whereSelect, SQLSyntax syntax, ImOrderMap<V, CompileOrder> orders, LimitOptions limit, boolean noInline) {
        boolean distinctValues = limit.isDistinctValues();
        ImOrderMap<String, String> keyOrderedNames = SQLSession.mapNames(keySelect, keyNames, keyOrder); // need to fill keyOrder
        return syntax.getSelect(from, SQLSession.stringExpr(distinctValues ? MapFact.EMPTYORDER() : keyOrderedNames,
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)) + (noInline && !distinctValues && syntax.inlineTrouble()?",random()":""),
                whereSelect.toString(" AND "), Query.stringOrder(propertyOrder.result, keySelect.size(), orders, propertySelect, syntax, new Result<>()), limit.getString(), distinctValues);
    }

    private static <K,AV> String fillSingleSelect(ImRevMap<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, ImMap<AV, Expr> compiledProps, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, ImRevMap<ParseValue, String> params, SQLSyntax syntax, SubQueryContext subcontext, MStaticExecuteEnvironment mEnv, Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MExclMap<String, SQLQuery> mSubQueries, DebugInfoWriter debugInfoWriter) {
        return fillFullSelect(mapKeys, SetFact.singleton(innerSelect), innerSelect.getFullWhere(), compiledProps, MapFact.EMPTYORDER(), LimitOptions.NOLIMIT, resultKey, resultProperty, params, syntax, subcontext, mEnv, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, debugInfoWriter);

/*        FullSelect FJSelect = new FullSelect(innerSelect.where, params,syntax); // для keyType'а берем первый where

        MapWhere<FJData> joinDatas = new MapWhere<FJData>();
        for(Map.Entry<AV, Expr> joinProp : compiledProps.entrySet())
            joinProp.getValue().fillJoinWheres(joinDatas, Where.TRUE());

        String innerAlias = subcontext+"inalias";
        Map<String, Expr> joinProps = new HashMap<String, Expr>();
        // затем все данные по JoinSelect'ам по вариантам
        for(FJData joinData : joinDatas.keys()) {
            String joinName = "join_" + joinProps.size();
            joinProps.put(joinName, joinData.getFJExpr());
            FJSelect.joinData.put(joinData,joinData.getFJString(innerAlias +'.'+joinName));
        }

        Map<K, String> keyNames = new HashMap<K, String>();
        for(K key : mapKeys.keySet()) {
            String keyName = "jkey" + keyNames.size();
            keyNames.put(key, keyName);
            keySelect.put(key, innerAlias +"."+ keyName);
            FJSelect.keySelect.put(mapKeys.get(key),innerAlias +"."+ keyName);
        }

        for(Map.Entry<AV, Expr> mapProp : compiledProps.entrySet())
            propertySelect.put(mapProp.getKey(), mapProp.getValue().getSource(FJSelect));

        return "(" + getInnerSelect(mapKeys, innerSelect, joinProps, params, new OrderedMap<String, Boolean>(),0 , syntax, keyNames, BaseUtils.toMap(joinProps.keySet()), new ArrayList<K>(), new ArrayList<String>(), null, subcontext, true) + ") " + innerAlias;*/
    }
    
    private static <K> void fillAreValues(ImMap<K, Expr> exprs, Result<ImSet<K>> result) {
        if(result != null) {
            result.set(exprs.filterFnValues(AbstractOuterContext::isValue).keys());
        }
    }

    private static <K,AV> String fillInnerSelect(ImRevMap<K, KeyExpr> mapKeys, final GroupJoinsWhere innerSelect, ImMap<AV, Expr> compiledProps, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, Result<ImCol<String>> resultWhere, ImRevMap<ParseValue, String> params, SQLSyntax syntax, SubQueryContext subcontext, LimitOptions limit, ImOrderSet<AV> orders, MStaticExecuteEnvironment env, Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MExclMap<String, SQLQuery> mSubQueries, Result<ImSet<K>> resultKeyValues, Result<ImSet<AV>> resultPropValues, DebugInfoWriter debugInfoWriter) {

        ImSet<KeyExpr> freeKeys = mapKeys.valuesSet().removeIncl(BaseUtils.<ImSet<KeyExpr>>immutableCast(innerSelect.keyEqual.keyExprs.keys()));
        final InnerSelect compile = new InnerSelect(freeKeys, innerSelect.where, innerSelect.where, innerSelect.where,innerSelect.joins,innerSelect.upWheres,syntax, mSubQueries, env, params, subcontext);

        if(Settings.get().getInnerGroupExprs() > 0) { // если не одни joinData
            final MAddSet<GroupExpr> groupExprs = SetFact.mAddSet(); final Result<Integer> repeats = new Result<>(0);
            for(Expr property : compiledProps.valueIt())
                property.enumerate(join -> {
                    if (join instanceof FJData) { // если FJData то что внутри не интересует
                        if (join instanceof GroupExpr && !compile.isInner(((GroupExpr) join).getInnerJoin()) && !groupExprs.add((GroupExpr) join))
                            repeats.set(repeats.result + 1);
                        return false;
                    }
                    return true;
                });
            if(repeats.result > Settings.get().getInnerGroupExprs())
                return fillSingleSelect(mapKeys, innerSelect, compiledProps, resultKey, resultProperty, params, syntax, subcontext, env, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, debugInfoWriter);
        }

        ExprTranslator keyEqualTranslator = innerSelect.keyEqual.getTranslator();
        compiledProps = keyEqualTranslator.translate(compiledProps);
        ImMap<K, Expr> compiledKeys = keyEqualTranslator.translate(mapKeys);

        MCol<String> mWhereSelect = ListFact.mCol();
        compile.fillInnerJoins(mBaseCost, mOptAdjustLimit, mRows, mWhereSelect, limit, orders.mapList(compiledProps).toOrderSet(), pushPrefix(debugInfoWriter, "STATS"));
        resultProperty.set(compiledProps.mapValues(compile.GETEXPRSOURCE()));
        resultKey.set(compiledKeys.mapValues(compile.GETEXPRSOURCE()));

        fillAreValues(compiledProps, resultPropValues);
        fillAreValues(compiledKeys, resultKeyValues);

        String from = compile.getFrom(innerSelect.where, mWhereSelect, pushPrefix(debugInfoWriter, "SUBQUERIES"));
        resultWhere.set(mWhereSelect.immutableCol());
        return from;
    }
    
    private final static class AddAlias implements Function<String, String> {
        private final String alias;
        private AddAlias(String alias) {
            this.alias = alias;
        }

        public String apply(String value) {
            return alias + "." + value;
        }
    }
    public final static class GenNameIndex implements IntFunction<String> {
        private final String prefix;
        private final String postfix;
        public GenNameIndex(String prefix, String postfix) {
            this.prefix = prefix;
            this.postfix = postfix;
        }

        public String apply(int index) {
            return prefix + index + postfix;
        }
    }
    private final static Function<String, String> coalesceValue = value -> "COALESCE(" + value + ")";

    private static <K,AV> String fillFullSelect(ImRevMap<K, KeyExpr> mapKeys, ImCol<GroupJoinsWhere> innerSelects, Where fullWhere, ImMap<AV, Expr> compiledProps, ImOrderMap<AV, Boolean> orders, LimitOptions limit, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, ImRevMap<ParseValue, String> params, SQLSyntax syntax, final SubQueryContext subcontext, MStaticExecuteEnvironment mEnv, Result<Cost> mBaseCost, Result<Boolean> mOptAdjustLimit, Result<Stat> mRows, MExclMap<String, SQLQuery> mSubQueries, DebugInfoWriter debugInfoWriter) {
        assert !limit.isDistinctValues(); // union check in CompiledQuery ensures that

        // создаем And подзапросыs
        final ImSet<CompileAndQuery> andProps = innerSelects.mapColSetValues((i, value) -> new CompileAndQuery(value, subcontext.wrapAlias("f" + i)));

        MMap<FJData, Where> mJoinDataWheres = MapFact.mMap(AbstractWhere.addOr());
        for(int i=0,size=compiledProps.size();i<size;i++)
            if(!orders.containsKey(compiledProps.getKey(i)))
                compiledProps.getValue(i).fillJoinWheres(mJoinDataWheres, Where.TRUE());
        ImMap<FJData, Where> joinDataWheres = mJoinDataWheres.immutable();

        // для JoinSelect'ов узнаем при каких условиях они нужны
        MMap<Object, Where> mJoinWheres = MapFact.mMapMax(joinDataWheres.size(), AbstractWhere.addOr());
        for(int i=0,size=joinDataWheres.size();i<size;i++)
            mJoinWheres.add(joinDataWheres.getKey(i).getFJGroup(),joinDataWheres.getValue(i));
        ImMap<Object, Where> joinWheres = mJoinWheres.immutable();

        // сначала распихиваем JoinSelect по And'ам
        ImMap<Object, ImSet<CompileAndQuery>> joinAnds = joinWheres.mapValues(value -> getWhereSubSet(andProps, value));

        // затем все данные по JoinSelect'ам по вариантам
        ImValueMap<FJData, String> mvJoinData = joinDataWheres.mapItValues(); // последействие есть
        for(int i=0;i<joinDataWheres.size();i++) {
            FJData joinData = joinDataWheres.getKey(i);
            String joinName = "join_" + i;
            Collection<CompileAndQuery> dataAnds = new ArrayList<>();
            for(CompileAndQuery and : getWhereSubSet(joinAnds.get(joinData.getFJGroup()), joinDataWheres.getValue(i))) {
                Expr joinExpr = joinData.getFJExpr();
                if(!and.innerSelect.getFullWhere().means(joinExpr.getWhere().not())) { // проверим что не всегда null
                    and.properties.exclAdd(joinName, joinExpr);
                    dataAnds.add(and);
                }
            }
            String joinSource = ""; // заполняем Source
            if(dataAnds.size()==0)
                throw new RuntimeException(ThreadLocalContext.localize("{data.query.should.not.be}"));
            else
            if(dataAnds.size()==1)
                joinSource = dataAnds.iterator().next().alias +'.'+joinName;
            else {
                for(CompileAndQuery and : dataAnds)
                    joinSource = (joinSource.length()==0?"":joinSource+",") + and.alias + '.' + joinName;
                joinSource = "COALESCE(" + joinSource + ")";
            }
            mvJoinData.mapValue(i, joinData.getFJString(joinSource));
        }
        ImMap<FJData, String> joinData = mvJoinData.immutableValue();

        // order'ы отдельно обрабатываем, они нужны в каждом запросе генерируем имена для Order'ов
        MOrderExclMap<String, Boolean> mOrderAnds = MapFact.mOrderExclMap(limit.hasLimit() ? orders.size() : 0);
        ImValueMap<AV, String> mvPropertySelect = compiledProps.mapItValues(); // сложный цикл
        for(int i=0,size=compiledProps.size();i<size;i++) {
            AV prop = compiledProps.getKey(i);
            Boolean dir = orders.get(prop);
            if(dir!=null) {
                String orderName = "order_" + i;
                String orderFJ = "";
                for(CompileAndQuery and : andProps) {
                    and.properties.exclAdd(orderName, compiledProps.get(prop));
                    orderFJ = (orderFJ.length()==0?"":orderFJ+",") + and.alias + "." + orderName;
                }
                if(limit.hasLimit()) // если все то не надо упорядочивать, потому как в частности MS SQL не поддерживает
                    mOrderAnds.exclAdd(orderName, dir);
                mvPropertySelect.mapValue(i, "COALESCE(" + orderFJ + ")");
            }
        }
        ImOrderMap<String, Boolean> orderAnds = mOrderAnds.immutableOrder();

        ImRevMap<K, String> keyNames = mapKeys.mapRevValues(new GenNameIndex("jkey",""));

        // бежим по всем And'ам делаем JoinSelect запросы, потом объединяем их FULL'ами
        String compileFrom = "";
        boolean first = true; // для COALESCE'ов
        ImMap<K, String> keySelect = null;
        for(CompileAndQuery and : andProps) {
            // закинем в And.Properties OrderBy, все равно какие порядки ключей и выражений
            ImMap<String, Expr> andProperties = and.properties.immutable();
            String andSelect = "(" + getInnerSelect(mapKeys, and.innerSelect, andProperties, params, orderAnds, limit, syntax, keyNames, andProperties.keys().toRevMap(), new Result<>(), new Result<>(), null, subcontext, innerSelects.size()==1, mEnv, mBaseCost, mOptAdjustLimit, mRows, mSubQueries, pushPrefix(debugInfoWriter, "FULL JOIN", and.innerSelect)) + ") " + and.alias;

            final ImRevMap<K, String> andKeySources = keyNames.mapRevValues(new AddAlias(and.alias));

            if(keySelect==null) {
                compileFrom = andSelect;
                keySelect = andKeySources;
            } else {
                String andJoin = andKeySources.crossJoin(first?keySelect:keySelect.mapValues(coalesceValue)).toString("=", " AND ");
                keySelect = keySelect.mapValues((key, value) -> value + "," + andKeySources.get(key));
                compileFrom = compileFrom + " FULL JOIN " + andSelect + " ON " + (andJoin.length()==0? Where.TRUE_STRING :andJoin);
                first = false;
            }
        }

        // полученные KeySelect'ы в Data
        if(innerSelects.size()>1)
            keySelect = keySelect.mapValues(coalesceValue);

        FullSelect FJSelect = new FullSelect(fullWhere, fullWhere, params, syntax, mEnv, mapKeys.crossJoin(keySelect), joinData); // для keyType'а берем первый where
        // закидываем PropertySelect'ы
        for(int i=0,size=compiledProps.size();i<size;i++) {
            AV prop = compiledProps.getKey(i);
            if(!orders.containsKey(prop)) // orders'ы уже обработаны
                mvPropertySelect.mapValue(i, compiledProps.getValue(i).getSource(FJSelect));
        }

        resultKey.set(keySelect);
        resultProperty.set(mvPropertySelect.immutableValue());
        return compileFrom;
    }

    // нерекурсивное транслирование параметров
    public static String translatePlainParam(String string,ImMap<String,String> paramValues) {
        for(int i=0,size=paramValues.size();i<size;i++)
            string = string.replace(paramValues.getKey(i), paramValues.getValue(i));
        return string;
    }

    // key - какие есть, value - которые должны быть
    public static String translateParam(String query,ImMap<String,String> paramValues) {
        // генерируем промежуточные имена, перетранслируем на них
        ImRevMap<String, String> preTranslate = paramValues.mapRevValues(new GenNameIndex("transp", "nt"));
        for(int i=0,size=preTranslate.size();i<size;i++)
            query = query.replace(preTranslate.getKey(i), preTranslate.getValue(i));

        // транслируем на те что должны быть
        ImMap<String, String> translateMap = preTranslate.crossJoin(paramValues);
        for(int i=0,size=translateMap.size();i<size;i++)
            query = query.replace(translateMap.getKey(i), translateMap.getValue(i));
        return query;
    }
    
    private static Function<ParseValue, ParseInterface> GETPARSE(final QueryEnvironment env, final EnsureTypeEnvironment typeEnv) {
        return value -> value.getParseInterface(env, typeEnv);
    }

    public ImMap<String, ParseInterface> getQueryParams(QueryEnvironment env) {
        return getQueryParams(env, 0);
    }
    public ImMap<String, ParseInterface> getQueryParams(QueryEnvironment env, final int limit) {
        MExclMap<String, ParseInterface> mMapValues = MapFact.mExclMap();
        if(limit > 0)
            mMapValues.exclAdd(SQLSession.limitParam, new StringParseInterface() {
                public String getString(SQLSyntax syntax, StringBuilder envString, boolean usedRecursion) {
                    return String.valueOf(limit);
                }
            });
        mMapValues.exclAdd(SQLSession.userParam, env.getSQLUser());
        mMapValues.exclAdd(SQLSession.authTokenParam, env.getSQLAuthToken());
        mMapValues.exclAdd(SQLSession.computerParam, env.getSQLComputer());
        mMapValues.exclAdd(SQLSession.formParam, env.getSQLForm());
        mMapValues.exclAdd(SQLSession.connectionParam, env.getSQLConnection());
        mMapValues.exclAdd(SQLSession.isServerRestartingParam, env.getIsServerRestarting());
        mMapValues.exclAdd(SQLSession.isDevParam, new LogicalParseInterface() {
            public boolean isTrue() {
                return SystemProperties.inDevMode;
            }
        });
        return mMapValues.immutable().addExcl(params.reverse().mapValues(GETPARSE(env, this.env.getEnsureTypes())));
    }

    private String fillSelect(final ImRevMap<String, String> params, Result<ImMap<K, String>> fillKeySelect, Result<ImMap<V, String>> fillPropertySelect, Result<ImCol<String>> fillWhereSelect, Result<ImMap<String, SQLQuery>> fillSubQueries, MStaticExecuteEnvironment fillEnv, DebugInfoWriter debugInfoWriter) {
        Function<String, String> transValue = value -> translateParam(value, params);

        fillKeySelect.set(keySelect.mapValues(transValue));
        fillPropertySelect.set(propertySelect.mapValues(transValue));
        fillWhereSelect.set(whereSelect.mapColValues(transValue));
        fillEnv.add(sql.getEnv());
        fillSubQueries.set(SQLQuery.translate(sql.subQueries, value -> translateParam(value, params)));
        if(debugInfoWriter != null)
            debugInfoWriter.addLines(translateParam(debugInfo, params));
        return translateParam(from, params);
    }
    private String getSelect(final ImMap<String, String> params, Result<ImMap<V, String>> fillPropertyNames, Result<ImMap<String, SQLQuery>> fillSubQueries, MStaticExecuteEnvironment fillEnv, DebugInfoWriter debugInfoWriter) {
        fillPropertyNames.set(propertyNames);
        fillEnv.add(sql.getEnv());
        fillSubQueries.set(SQLQuery.translate(sql.subQueries, value -> translateParam(value, params)));
        if(debugInfoWriter != null)
            debugInfoWriter.addLines(translateParam(debugInfo, params));
        return translateParam(sql.getString(), params);
    }

    private ImRevMap<String, String> getTranslate(ImRevMap<ParseValue, String> mapValues) {
        return params.crossJoin(mapValues);
    }
    private ImMap<String, String> getTranslate(ImMap<ParseValue, String> mapValues) {
        return params.crossJoin(mapValues);
    }

    // для подзапросов
    public String fillSelect(Result<ImMap<K, String>> fillKeySelect, Result<ImMap<V, String>> fillPropertySelect, Result<ImCol<String>> fillWhereSelect, Result<ImMap<String, SQLQuery>> fillSubQueries, ImRevMap<ParseValue, String> mapValues, MStaticExecuteEnvironment fillEnv, DebugInfoWriter debugInfoWriter) {
        return fillSelect(getTranslate(mapValues), fillKeySelect, fillPropertySelect, fillWhereSelect, fillSubQueries, fillEnv, debugInfoWriter);
    }
    public String getSelect(Result<ImMap<V, String>> fillPropertyNames, Result<ImMap<String, SQLQuery>> fillSubQueries, ImMap<ParseValue, String> mapValues, MStaticExecuteEnvironment fillEnv, int limit, DebugInfoWriter debugInfoWriter) {
        ImMap<String, String> translate = getTranslate(mapValues);
        if(limit > 0)
            translate = translate.addExcl(SQLSession.limitParam, String.valueOf(limit));
        return getSelect(translate, fillPropertyNames, fillSubQueries, fillEnv, debugInfoWriter);
    }

    public void execute(SQLSession session, QueryEnvironment queryEnv, int limit, ResultHandler<K, V> resultHandler) throws SQLException, SQLHandledException {
        session.executeSelect(sql, getQueryExecEnv(session.contextProvider), queryEnv.getOpOwner(), getQueryParams(queryEnv, limit), queryEnv.getTransactTimeout(), keyNames, propertyNames, resultHandler);
    }

    public void outSelect(SQLSession session, QueryEnvironment env, boolean uniqueViolation) throws SQLException, SQLHandledException {
        sql.outSelect(keyNames, propertyNames, session, getQueryExecEnv(session.contextProvider), null, getQueryParams(env), env.getTransactTimeout(), uniqueViolation, env.getOpOwner());
    }

    public String readSelect(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        return sql.readSelect(keyNames, propertyNames, session, getQueryExecEnv(session.contextProvider), null, getQueryParams(env), env.getTransactTimeout(), false, env.getOpOwner());
    }
}

/*
        // для работы с cross-column статистикой
        // не получается сделать, не выстраивая порядок JOIN'ов, а это уже перебор, уж за это SQL Server сам должен отвечать
        // вообще надо adjustSelectivity бороться, делая из SQL сервера пессимиста

        private static class RightJoins {
            public Map<Table.Join, Map<String, Field>> map = new HashMap<Table.Join, Map<String, Field>>();

            public void add(Table.Join join, String key, Field field) {
                Map<String, Field> joinFields = map.get(join);
                if(joinFields==null) {
                    joinFields = new HashMap<String, Field>();
                    map.put(join, joinFields);
                }
                joinFields.put(key, field);
            }

            public void addAll(RightJoins add) {
                for(Map.Entry<Table.Join, Map<String, Field>> addEntry : add.map.entrySet())
                    for(Map.Entry<String, Field> addField : addEntry.getValue().entrySet())
                        add(addEntry.getKey(), addField.getKey(), addField.getValue());
            }

            public void addAll(Map<String, Pair<Table.Join, KeyField>> add) {
                for(Map.Entry<String, Pair<Table.Join, KeyField>> addEntry : add.entrySet())
                    add(addEntry.getValue().first, addEntry.getKey(), addEntry.getValue().second);
            }
        }

        // для работы с cross-column статистикой

        // перебор индексов по соединяемым таблицам
        public static interface RecJoinTables {
            void proceed(Collection<String> freeFields);
        }
        private static void recJoinTables(final int i, final List<Table.Join> list, final Map<Table.Join, Map<String, Field>> joinTables, final Stack<List<List<String>>> current, final Collection<String> freeFields, final RecJoinTables result) {
            if(i>=list.size()) {
                result.proceed(freeFields);
                return;
            }
            Table.Join tableJoin = list.get(i);
            tableJoin.getTable().recIndexTuples(joinTables.get(tableJoin), current, new Table.RecIndexTuples<String>() {
                public void proceed(Map<String, ? extends Field> restFields) { // можно не учитывать restFields так как join'ы не пересекаются
                    recJoinTables(i + 1, list, joinTables, current, BaseUtils.merge(freeFields, restFields.keySet()), result);
                }
            });
        }

        private static class Coverage { // именно в таком приоритете
            private final int notIndexed;
            private final int leftRight;
            private final int tuples;
            private final int indexes;

            private Coverage(int notIndexed, int leftRight, int tuples, int indexes) {
                this.notIndexed = notIndexed;
                this.leftRight = leftRight;
                this.tuples = tuples;
                this.indexes = indexes;
            }

            boolean better(Coverage cov) {
                if(notIndexed < cov.notIndexed) // минимум не индексированных полей
                    return true;
                if(notIndexed > cov.notIndexed)
                    return false;
                if(leftRight > cov.leftRight) // максимум 2-сторонних индексов
                    return true;
                if(leftRight < cov.leftRight)
                    return false;
                if(tuples < cov.tuples) // минимум tuples (чтобы лучше статистика была)
                    return true;
                if(tuples > cov.tuples)
                    return false;
                if(indexes < cov.indexes) // минимум индексов
                    return true;
                if(tuples > cov.tuples)
                    return false;

                return false;
            }
        }

        // перебираем "правые" индексы
        private static void recJoinTables(Map<Table.Join, Map<String, Field>> joinTables, Stack<List<List<String>>> current, RecJoinTables result) {
            recJoinTables(0, new ArrayList<Table.Join>(joinTables.keySet()), joinTables, current, new ArrayList<String>(), result);
        }

        // перебор табличных join'ов, из которых брать ключи
        public static interface RecJoinKeyTables {
            void proceed(Map<String, Pair<Table.Join, KeyField>> map); // mutable
        }
        private static void recJoinKeyTables(final int i, final List<String> list, Map<String, KeyExpr> mapKeys, Map<KeyExpr, Collection<Pair<Table.Join, KeyField>>> keyTables, Map<String, Pair<Table.Join, KeyField>> current, RecJoinKeyTables result) {
            if(i>=list.size()) {
                result.proceed(current);
                return;
            }

            String key = list.get(i);
            for(Pair<Table.Join, KeyField> keyTable : keyTables.get(mapKeys.get(key))) {
                current.put(key, keyTable);
                recJoinKeyTables(i + 1, list, mapKeys, keyTables, current, result);
                current.remove(key);
            }
        }
        private static void recJoinKeyTables(Map<String, KeyExpr> mapKeys, Map<KeyExpr, Collection<Pair<Table.Join, KeyField>>> keyTables, RecJoinKeyTables result) {
            recJoinKeyTables(0, new ArrayList<String>(mapKeys.keySet()), mapKeys, keyTables, new HashMap<String, Pair<Table.Join, KeyField>>(), result);
        }


        // для работы с cross-column статистикой
        private Map<KeyExpr, Collection<Pair<Table.Join, KeyField>>> keyTables = new HashMap<KeyExpr, Collection<Pair<Table.Join, KeyField>>>();

        private abstract class JoinSelect<I extends InnerJoin> {

            final String alias; // final
            final String join; // final
            final I innerJoin;

            protected abstract Map<String, BaseExpr> initJoins(I innerJoin);

            protected boolean isInner() {
                return InnerSelect.this.isInner(innerJoin);
            }

            protected JoinSelect(final I innerJoin) {
                alias = subcontext.wrapAlias("t" + (aliasNum++));
                this.innerJoin = innerJoin;

                useTuples = true && isInner();

                // здесь проблема что keySelect может рекурсивно использоваться 2 раза, поэтому сначала пробежим не по ключам
                RightJoins joinTables = null;
                if(useTuples)
                    joinTables = new RightJoins();

                Map<String, String> joinSources = new HashMap<String, String>();
                Map<String,KeyExpr> joinKeys = new HashMap<String, KeyExpr>();
                for(Map.Entry<String, BaseExpr> keyJoin : initJoins(innerJoin).entrySet()) {
                    String keySource = alias + "." + keyJoin.getKey();
                    if(keyJoin.getValue() instanceof KeyExpr)
                        joinKeys.put(keySource,(KeyExpr)keyJoin.getValue());
                    else {
                        joinSources.put(keySource, keyJoin.getValue().getSource(InnerSelect.this));

                        if(useTuples && keyJoin instanceof Table.Join.Expr) {
                            Table.Join.Expr tableExpr = (Table.Join.Expr) keyJoin;
                            joinTables.add(tableExpr.getInnerJoin(), keySource, tableExpr.property);
                        }
                    }
                }
                for(Map.Entry<String,KeyExpr> keyJoin : joinKeys.entrySet()) { // дозаполним ключи
                    String keySource = keySelect.get(keyJoin.getValue());
                    if(keySource==null) {
                        assert isInner();
                        keySelect.put(keyJoin.getValue(),keyJoin.getKey());
                    } else
                        joinSources.put(keyJoin.getKey(), keySource);
                }

                if(useTuples) {
                    if(this instanceof TableSelect) { // записываем себя к ключам
                        Map<String, KeyField> mapKeys = ((TableSelect)this).initFields((Table.Join) innerJoin);
                        for(Map.Entry<String, KeyExpr> keyJoin : joinKeys.entrySet()) {
                            Collection<Pair<Table.Join, KeyField>> keyList = keyTables.get(keyJoin.getValue());
                            if(keyList==null) {
                                keyList = new ArrayList<Pair<Table.Join, KeyField>>();
                                keyTables.put(keyJoin.getValue(), keyList);
                            }
                            keyList.add(new Pair<Table.Join, KeyField>((Table.Join)innerJoin, mapKeys.get(keyJoin.getKey())));
                        }
                    }
                    join = "";
                    this.joinTables = joinTables; this.joinSources = joinSources; this.joinKeys = joinKeys;
                } else {
                    Collection<String> joinSelect = new ArrayList<String>();
                    for(Map.Entry<String, String> joinSource : joinSources.entrySet())
                        joinSelect.add(joinSource.getKey() + "=" + joinSource.getValue());
                    join = BaseUtils.toString(joinSelect, " AND ");
                }

                InnerSelect.this.joins.add(this);
            }

            // для работы с cross-column статистикой, не очень красиво, но других очевидных вариантов не видно
            private final boolean useTuples;
            private RightJoins joinTables; private Map<String, String> joinSources; private Map<String, KeyExpr> joinKeys;
            private void tupleJoin(Collection<String> joinSelect) {
                final Map<String, KeyExpr> joinKeyTables = BaseUtils.filterValues(joinKeys, keyTables.keySet()); // интересуют только, те для которых есть таблицы

                final Table table;
                final Map<String, KeyField> mapKeys;
                if(this instanceof TableSelect) {
                    table = ((Table.Join) innerJoin).getTable(); mapKeys = ((TableSelect)this).initFields((Table.Join) innerJoin);
                } else {
                    table = null; mapKeys = null;
                }

                final Result<Pair<Coverage, List<List<String>>>> bestCoverage = new Result<Pair<Coverage, List<List<String>>>>();
                final Result<Map<String, String>> bestKeySources = new Result<Map<String, String>>();

                // перебираем использование ключей в таблицах
                recJoinKeyTables(joinKeyTables, keyTables, new RecJoinKeyTables() {
                    public void proceed(final Map<String, Pair<Table.Join, KeyField>> mapKeyFields) {
                        final RightJoins recJoinTables = new RightJoins();
                        recJoinTables.addAll(mapKeyFields);
                        recJoinTables.addAll(joinTables);

                        final Stack<List<List<String>>> leftIndexes = new Stack<List<List<String>>>(); // здесь левые индексы
                        final Stack<List<List<String>>> rightIndexes = new Stack<List<List<String>>>(); // здесь правые индексы
                        final RecJoinTables finalResult = new RecJoinTables() {
                            public void proceed(Collection<String> freeFields) {
                                int keys = freeFields.size();
                                int leftRight = 0;
                                int tuples = 0;
                                int indexes = 0;

                                indexes += leftIndexes.size();
                                for(List<List<String>> leftIndex : leftIndexes)
                                    tuples += leftIndex.size();

                                indexes += rightIndexes.size();
                                if(table!=null) { // если таблица, считаем кол-во 2-сторонних индексов
                                    for(List<List<String>> rightIndex : rightIndexes) {
                                        tuples += rightIndex.size();
                                        int maxCommon = 0;
                                        for(List<List<Field>> leftIndex : table.indexes) {
                                            int cc = 0; int ckeys = 0; List<KeyField> commonTuple;
                                            while((cc < rightIndex.size() && cc<leftIndex.size()) && ((commonTuple = mapList(rightIndex.get(cc), mapKeys)).equals(leftIndex.get(cc)))) {
                                                ckeys += commonTuple.size(); cc++;
                                            }
                                            maxCommon = max(maxCommon, ckeys);
                                        }
                                        leftRight += maxCommon;
                                    }
                                }

                                Coverage coverage = new Coverage(keys, leftRight, tuples, indexes);
                                if(bestCoverage.result == null || coverage.better(bestCoverage.result.first)) {
                                    List<List<String>> resultIndexes = new ArrayList<List<String>>();
                                    for(List<List<String>> leftIndex : leftIndexes)
                                        resultIndexes.addAll(leftIndex);
                                    for(List<List<String>> rightIndex : rightIndexes)
                                        resultIndexes.addAll(rightIndex);
                                    for(String freeField : freeFields)
                                        resultIndexes.add(Collections.singletonList(freeField));
                                    bestCoverage.set(new Pair<Coverage, List<List<String>>>(coverage, resultIndexes));

                                    Map<String, String> keySources = new HashMap<String, String>();
                                    for(Map.Entry<String, Pair<Table.Join, KeyField>> mapKeyField : mapKeyFields.entrySet())
                                        keySources.put(mapKeyField.getKey(), getAlias(mapKeyField.getValue().first) + "." + mapKeyField.getValue().second);
                                    bestKeySources.set(keySources);
                                }
                            }
                        };

                        if(table!=null) // перебираем "левые" индексы
                            recJoinTables(recJoinTables.map, rightIndexes, new RecJoinTables() {
                                public void proceed(Collection<String> freeFields) {
                                    table.recIndexTuples(filterKeys(mapKeys, freeFields), leftIndexes, new Table.RecIndexTuples<String>() {
                                        public void proceed(Map<String, ? extends Field> restFields) {
                                            finalResult.proceed(restFields.keySet());
                                        }
                                    });
                                }
                            });
                        else
                            recJoinTables(recJoinTables.map, rightIndexes, finalResult);
                    }
                });

                joinSources.putAll(bestKeySources.result); // берем реально использованные ключи
                for(List<String> tuple : bestCoverage.result.second)
                    joinSelect.add(Table.getTuple(tuple) + " = " + Table.getTuple(mapList(tuple, joinSources)));

                this.joinTables = null; this.joinSources = null; this.joinKeys = null;
            }

            public abstract String getSource(StaticExecuteEnvironment env);

            protected abstract Where getInnerWhere(); // assert что isInner
        }
        public void fillInnerJoins(Collection<String> whereSelect) { // заполним Inner Joins, чтобы чтобы keySelect'ы были
            innerWhere = whereJoins.fillInnerJoins(upWheres, whereSelect, this);
        }

        private Where innerWhere;
        // получает условия следующие из логики inner join'ов SQL
        private Where getInnerWhere() {
            Where result = innerWhere;
            for(InnerJoin innerJoin : getInnerJoins()) {
                JoinSelect joinSelect = getJoinSelect(innerJoin);
                if(joinSelect!=null)
                    result = result.and(joinSelect.getInnerWhere());
            }
            return result;
        }

        public String getFrom(Where where, Collection<String> whereSelect, StaticExecuteEnvironment env) {
            where.getSource(this);
            whereSelect.add(where.followFalse(getInnerWhere().not()).getSource(this));

            if(joins.isEmpty()) return "dumb";

            String from;
            Iterator<JoinSelect> ij = joins.iterator();
            JoinSelect first = ij.next();
            if(first.isInner()) {
                from = first.getSource(env) + " " + first.alias;
                if(!(first.join.length()==0))
                    whereSelect.add(first.join);

                if(first.useTuples)
                    first.tupleJoin(whereSelect);
            } else {
                from = "dumb";
                ij = joins.iterator();
            }

            while(ij.hasNext()) {
                JoinSelect join = ij.next();
                from = from + (join.isInner() ?"":" LEFT")+" JOIN " + join.getSource(env) + " " + join.alias  + " ON " + (join.join.length()==0?Where.TRUE_STRING:join.join);

                if(join.useTuples)
                    join.tupleJoin(whereSelect);
            }

            return from;
        }


            protected Map<String, KeyField> initFields(Table.Join table) {
                Map<String, KeyField> result = new HashMap<String, KeyField>();
                for(KeyField key : table.joins.keySet())
                    result.put(key.toString(),key);
                return result;
            }

 */

/* !!!!! UNION ALL код            // в Properties закинем Orders,
            HashMap<Object,Query> UnionProps = new HashMap<Object, Query>(query.property);
            LinkedHashMap<String,Boolean> OrderNames = new LinkedHashMap<String, Boolean>();
            int io = 0;
            for(Map.Entry<Query,Boolean> Order : query.orders.entrySet()) {
                String OrderName = "order_"+io;
                UnionProps.put(OrderName,Order.getKey());
                OrderNames.put(OrderName,Order.getValue());
            }

            From = "";
            while(true) {
                Where AndWhere = QueryJoins.iterator().next();
                Map<K,String> AndKeySelect = new HashMap<K, String>();
                LinkedHashMap<Object,String> AndPropertySelect = new LinkedHashMap<Object, String>();
                Collection<String> AndWhereSelect = new ArrayList<String>();
                String AndFrom = fillAndSelect(Query.Keys,AndWhere,UnionProps,new LinkedHashMap<Query,Boolean>(),AndKeySelect,
                    AndPropertySelect,AndWhereSelect,QueryParams,new LinkedHashMap<String,Boolean>(), Syntax);

                LinkedHashMap<String,String> NamedProperties = new LinkedHashMap<String, String>();
                for(V Property : Query.Properties.keySet()) {
                    NamedProperties.put(PropertyNames.get(Property),AndPropertySelect.get(Property));
                    if(From.length()==0) PropertyOrder.add(Property);
                }
                for(String Order : OrderNames.keySet())
                    NamedProperties.put(Order,AndPropertySelect.get(Order));

                From = (From.length()==0?"":From+" UNION ALL ") +
                    Syntax.getSelect(AndFrom,Source.stringExpr(Source.mapNames(AndKeySelect,KeyNames,From.length()==0?KeyOrder:new ArrayList<K>()),NamedProperties),
                            Source.stringWhere(AndWhereSelect),"","","");

                OrWhere = OrWhere.andNot(AndWhere).getOr();
                if(OrWhere.isFalse()) break;
                break;
            }

            String Alias = "G";
            for(K Key : query.keys.keySet())
                keySelect.put(Key, Alias + "." + keyNames.get(Key));
            for(V Property : query.property.keySet())
                propertySelect.put(Property, Alias + "." + propertyNames.get(Property));

            from = "(" + from + ") "+Alias;

            select = syntax.getUnionOrder(from,Query.stringOrder(OrderNames), query.top ==0?"":String.valueOf(query.top));
            */
