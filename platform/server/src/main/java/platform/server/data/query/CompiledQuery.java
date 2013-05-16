package platform.server.data.query;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.base.col.interfaces.mutable.mapvalue.*;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.SystemProperties;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.classes.IntegralClass;
import platform.server.data.*;
import platform.server.data.expr.*;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.expr.order.PartitionCalc;
import platform.server.data.expr.order.PartitionToken;
import platform.server.data.expr.query.*;
import platform.server.data.query.innerjoins.GroupJoinsWhere;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.*;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.Where;
import platform.server.logics.ServerResourceBundle;

import java.sql.SQLException;
import java.util.*;

// нужен для Map'а ключей / значений
// Immutable/Thread Safe
public class CompiledQuery<K,V> extends ImmutableObject {
    final public String from;
    final public ImMap<K,String> keySelect;
    final public ImMap<V,String> propertySelect;
    final public ImCol<String> whereSelect;

    final public String select;
    final public ImOrderSet<K> keyOrder;
    final public ImOrderSet<V> propertyOrder;
    final public ImRevMap<K,String> keyNames;
    final public ImMap<K, ClassReader> keyReaders;
    final public ImRevMap<V,String> propertyNames;
    final public ImMap<V, ClassReader> propertyReaders;

    public boolean union;

    final ImRevMap<Value,String> params;
    public final ExecuteEnvironment env;

    private boolean checkQuery() {
        return true;
    }

    // перемаппит другой CompiledQuery
    public <MK,MV> CompiledQuery(CompiledQuery<MK,MV> compile,ImRevMap<K,MK> mapKeys,ImRevMap<V,MV> mapProperties, MapValuesTranslate mapValues) {
        from = compile.from;
        whereSelect = compile.whereSelect;
        keySelect = mapKeys.join(compile.keySelect);
        propertySelect = mapProperties.join(compile.propertySelect);

        select = compile.select;
        keyOrder = compile.keyOrder.mapOrder(mapKeys.reverse());
        propertyOrder = compile.propertyOrder.mapOrder(mapProperties.reverse());
        keyNames = mapKeys.join(compile.keyNames);
        keyReaders = mapKeys.join(compile.keyReaders);
        propertyNames = mapProperties.join(compile.propertyNames);
        propertyReaders = mapProperties.join(compile.propertyReaders);
        union = compile.union;

        params = mapValues.translateValuesMapKeys(compile.params);

        env = compile.env.translateValues(mapValues);

        assert checkQuery();
    }

    private static class FullSelect extends CompileSource {

        private FullSelect(KeyType keyType, Where fullWhere, ImRevMap<Value, String> params, SQLSyntax syntax, ImMap<KeyExpr,String> keySelect, ImMap<JoinData, String> joinData) {
            super(keyType, fullWhere, params, syntax);
            this.keySelect = keySelect;
            this.joinData = joinData;
        }

        public final ImMap<KeyExpr,String> keySelect;
        public final ImMap<JoinData,String> joinData;

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

        public String getSource(QueryExpr queryExpr) {
            assert joinData.get(queryExpr)!=null;
            return joinData.get(queryExpr);
        }

        public String getSource(IsClassExpr classExpr) {
            assert joinData.get(classExpr)!=null;
            return joinData.get(classExpr);
        }
    }

    // многие субд сами не могут определить
    public static <V> ImOrderMap<V, Boolean> getOrdersNotNull(ImOrderMap<V, Boolean> orders, ImMap<V, Expr> orderExprs, Result<ImSet<V>> resultOrders) {
        MOrderFilterMap<V, Boolean> mResult = MapFact.mOrderFilter(orders);
        MOrderFilterSet<V> mOrdersNotNull = SetFact.mOrderFilter(orders);
        MAddSet<KeyExpr> currentKeys = SetFact.mAddSet();
        for(int i=0,size=orders.size();i<size;i++) {
             V order = orders.getKey(i);
             Expr orderExpr = orderExprs.get(order);
             if(!currentKeys.containsAll(BaseUtils.<ImSet<KeyExpr>>immutableCast(orderExpr.getOuterKeys()))) {
                 if(orderExpr instanceof KeyExpr) {
                     mOrdersNotNull.keep(order);
                     currentKeys.add((KeyExpr)orderExpr);
                 }
                 mResult.keep(order, orders.getValue(i));
             }
         }
        resultOrders.set(SetFact.imOrderFilter(mOrdersNotNull, orders).getSet());
        return MapFact.imOrderFilter(mResult, orders);
    }

    public CompiledQuery(final Query<K,V> query, SQLSyntax syntax, ImOrderMap<V,Boolean> orders, int top, SubQueryContext subcontext, boolean noExclusive) {

        Result<ImOrderSet<K>> resultKeyOrder = new Result<ImOrderSet<K>>(); Result<ImOrderSet<V>> resultPropertyOrder = new Result<ImOrderSet<V>>();

        keyNames = query.mapKeys.mapRevValues(new GenNameIndex("jkey", ""));
        propertyNames = query.properties.mapRevValues(new GenNameIndex("jprop",""));
        params = query.getInnerValues().mapRevValues(new GenNameIndex("qwer","ffd"));

        env = new ExecuteEnvironment();

        keyReaders = query.mapKeys.mapValues(new GetValue<ClassReader, KeyExpr>() {
            public ClassReader getMapValue(KeyExpr value) {
                return query.where.isFalse() ? NullReader.instance : value.getType(query.where);
            }
        });

        propertyReaders = query.properties.mapValues(new GetValue<ClassReader, Expr>() {
            public ClassReader getMapValue(Expr value) {
                return query.where.isFalse() ? NullReader.instance : value.getReader(query.where);
            }
        });

        boolean useFJ = syntax.useFJ();
        noExclusive = noExclusive || Settings.get().isNoExclusiveCompile();
        Result<Boolean> unionAll = new Result<Boolean>();
        ImCol<GroupJoinsWhere> queryJoins = query.getWhereJoins(!useFJ && !noExclusive, unionAll,
                                top > 0 && syntax.orderTopTrouble() ? orders.keyOrderSet().mapOrder(query.properties) : SetFact.<Expr>EMPTYORDER());
        union = !useFJ && queryJoins.size() >= 2 && (unionAll.result || !Settings.get().isUseFJInsteadOfUnion());
        if (union) { // сложный UNION запрос
            ImMap<V, Type> castTypes = BaseUtils.immutableCast(
                    propertyReaders.filterFnValues(new SFunctionSet<ClassReader>() {
                        public boolean contains(ClassReader element) {
                            return element instanceof Type;
                        }
                    }));

            String fromString = "";
            for(GroupJoinsWhere queryJoin : queryJoins) {
                boolean orderUnion = syntax.orderUnion(); // нужно чтобы фигачило внутрь orders а то многие SQL сервера не видят индексы внутри union all
                fromString = (fromString.length()==0?"":fromString+" UNION " + (unionAll.result?"ALL ":"")) + "(" + getInnerSelect(query.mapKeys, queryJoin, queryJoin.getFullWhere().followTrue(query.properties, !queryJoin.isComplex()), params, orderUnion?orders:MapFact.<V, Boolean>EMPTYORDER(), orderUnion?top:0, syntax, keyNames, propertyNames, resultKeyOrder, resultPropertyOrder, castTypes, subcontext, false, env) + ")";
                if(!orderUnion)
                    castTypes = null;
            }

            final String alias = "UALIAS";
            AddAlias addAlias = new AddAlias(alias);
            keySelect = keyNames.mapValues(addAlias);
            propertySelect = propertyNames.mapValues(addAlias);
            from = "(" + fromString + ") "+alias;
            whereSelect = SetFact.EMPTY();
            select = syntax.getUnionOrder(fromString, Query.stringOrder(resultPropertyOrder.result, query.mapKeys.size(), orders, SetFact.<V>EMPTY(), syntax), top ==0?"":String.valueOf(top));
        } else {
            ImSet<V> ordersNotNull = SetFact.<V>EMPTY();
            if(queryJoins.size()==0) { // "пустой" запрос
                keySelect = query.mapKeys.mapValues(new GetStaticValue<String>() {
                    public String getMapValue() {
                        return SQLSyntax.NULL;
                    }
                });
                propertySelect = query.properties.mapValues(new GetStaticValue<String>() {
                    public String getMapValue() {
                        return SQLSyntax.NULL;
                    }});
                from = "empty";
                whereSelect = SetFact.EMPTY();
            } else {
                Result<ImMap<K, String>> resultKey = new Result<ImMap<K, String>>(); Result<ImMap<V, String>> resultProperty = new Result<ImMap<V, String>>();
                if(queryJoins.size()==1) { // "простой" запрос
                    Result<ImCol<String>> resultWhere = new Result<ImCol<String>>(); Result<ImSet<V>> resultOrders = new Result<ImSet<V>>();
                    from = fillInnerSelect(query.mapKeys, queryJoins.single(), query.properties, resultKey, resultProperty, resultWhere, params, syntax, subcontext, env);
                    orders = getOrdersNotNull(orders, query.properties, resultOrders);
                    whereSelect = resultWhere.result; ordersNotNull = resultOrders.result;
                } else { // "сложный" запрос с full join'ами
                    from = fillFullSelect(query.mapKeys, queryJoins, query.where, query.properties, orders, top, resultKey, resultProperty, params, syntax, subcontext, env);
                    whereSelect = SetFact.EMPTY();
                }
                keySelect = resultKey.result; propertySelect = resultProperty.result;
            }

            select = getSelect(from, keySelect, keyNames, resultKeyOrder, propertySelect, propertyNames, resultPropertyOrder, whereSelect, syntax, orders, ordersNotNull, top, false);
        }

        keyOrder = resultKeyOrder.result; propertyOrder = resultPropertyOrder.result;

        assert checkQuery();
    }

    // в общем случае получить AndJoinQuery под которые подходит Where
    private static ImSet<AndJoinQuery> getWhereSubSet(ImSet<AndJoinQuery> andWheres, Where where) {

        MSet<AndJoinQuery> result = SetFact.mSet();
        CheckWhere resultWhere = Where.FALSE;
        while(result.size()< andWheres.size()) {
            // ищем куда закинуть заодно считаем
            AndJoinQuery lastQuery = null;
            CheckWhere lastWhere = null;
            for(AndJoinQuery and : andWheres)
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
        private Stack<MRevMap<String, String>> stackTranslate = new Stack<MRevMap<String, String>>();
        private Stack<MSet<KeyExpr>> stackUsedPendingKeys = new Stack<MSet<KeyExpr>>();
        private Set<KeyExpr> pending;

        public String getSource(KeyExpr key) {
            String source = keySelect.get(key);
            if(source == null) {
                source = "qxas" + keySelect.size() + "nbv";
                keySelect.put(key, source);
                pending.add(key);
            }

            if(pending.contains(key))
                stackUsedPendingKeys.peek().add(key);

            return source;
        }

        final WhereJoins whereJoins;

        public InnerJoins getInnerJoins() {
            return whereJoins.getInnerJoins();
        }

        public boolean isInner(InnerJoin join) {
            return getInnerJoins().means(join);
        }

        final ImMap<WhereJoin, Where> upWheres;

        final SubQueryContext subcontext;
        final KeyStat keyStat;

        public InnerSelect(ImSet<KeyExpr> keys, KeyType keyType, KeyStat keyStat, Where fullWhere, WhereJoins whereJoins, ImMap<WhereJoin, Where> upWheres, SQLSyntax syntax, ImRevMap<Value, String> params, SubQueryContext subcontext) {
            super(keyType, fullWhere, params, syntax);

            this.keyStat = keyStat;
            this.subcontext = subcontext;
            this.whereJoins = whereJoins;
            this.upWheres = upWheres;
            this.keySelect = new HashMap<KeyExpr, String>(); // сложное рекурсивное заполнение
            this.pending = new HashSet<KeyExpr>();
        }

        int aliasNum=0;
        MList<JoinSelect> mJoins = ListFact.mList();
        ImList<JoinSelect> joins;
        MList<String> mImplicitJoins = ListFact.mList();
        MList<JoinSelect> mOuterPendingJoins = ListFact.mList();

        private abstract class JoinSelect<I extends InnerJoin> {

            final String alias; // final
            String join; // final
            final I innerJoin;

            protected abstract ImMap<String, BaseExpr> initJoins(I innerJoin);

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
                ImMap<String, BaseExpr> initJoins = initJoins(innerJoin);
                MExclMap<String,KeyExpr> mJoinKeys = MapFact.mExclMapMax(initJoins.size());
                for(int i=0,size=initJoins.size();i<size;i++) {
                    BaseExpr expr = initJoins.getValue(i);
                    String keySource = alias + "." + initJoins.getKey(i);
                    if(expr instanceof KeyExpr && inner)
                        mJoinKeys.exclAdd(keySource, (KeyExpr) expr);
                    else {
                        stackUsedPendingKeys.push(SetFact.<KeyExpr>mSet());
                        stackTranslate.push(MapFact.<String, String>mRevMap());
                        String exprJoin = keySource + "=" + expr.getSource(InnerSelect.this);
                        ImSet<KeyExpr> usedPendingKeys = stackUsedPendingKeys.pop().immutable();
                        ImRevMap<String, String> translate = stackTranslate.pop().immutableRev(); // их надо перетранслировать
                        exprJoin = translatePlainParam(exprJoin, translate);

                        boolean havePending = usedPendingKeys.size() > translate.size();
                        if(havePending && inner) { // какие-то ключи еще зависли, придется в implicitJoins закидывать
                            assert usedPendingKeys.intersect(SetFact.fromJavaSet(pending));
                            mImplicitJoins.add(exprJoin);
                        } else { // можно explicit join делать, перетранслировав usedPending
                            joinString = (joinString.length() == 0 ? "" : joinString + " AND ") + exprJoin;
                            if(havePending)
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
                    mOuterPendingJoins.add(this);
                else
                    mJoins.add(this);
            }

            public abstract String getSource(ExecuteEnvironment env);

            protected abstract Where getInnerWhere(); // assert что isInner
        }

        public void fillInnerJoins(MCol<String> whereSelect) { // заполним Inner Joins, чтобы keySelect'ы были
            innerWhere = whereJoins.fillInnerJoins(upWheres, whereSelect, this);
            whereSelect.addAll(mImplicitJoins.immutableList().getCol());
            mJoins.addAll(mOuterPendingJoins.immutableList());
            assert pending.isEmpty();
            mImplicitJoins = null;
            mOuterPendingJoins = null;
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

        public String getFrom(Where where, MCol<String> whereSelect, ExecuteEnvironment env) {
            where.getSource(this);

            joins = mJoins.immutableList();
            mJoins = null;
            for(JoinSelect join : joins)
                if(join instanceof QuerySelect)
                    ((QuerySelect)join).finalIm();
            
            whereSelect.add(where.followFalse(getInnerWhere().not()).getSource(this));

            if(joins.isEmpty()) return "dumb";

            String from;
            Iterator<JoinSelect> ij = joins.iterator();
            JoinSelect first = ij.next();
            if(first.isInner()) {
                from = first.getSource(env) + " " + first.alias;
                if(!(first.join.length()==0))
                    whereSelect.add(first.join);
            } else {
                from = "dumb";
                ij = joins.iterator();
            }

            while(ij.hasNext()) {
                JoinSelect join = ij.next();
                from = from + (join.isInner() ?"":" LEFT")+" JOIN " + join.getSource(env) + " " + join.alias  + " ON " + (join.join.length()==0?Where.TRUE_STRING:join.join);
            }

            return from;
        }

        private class TableSelect extends JoinSelect<Table.Join> {
            private String source;

            protected ImMap<String, BaseExpr> initJoins(Table.Join table) {
                return table.joins.mapKeys(new GetValue<String, KeyField>() {
                    public String getMapValue(KeyField value) {
                        return value.name;
                    }});
            }

            TableSelect(Table.Join join) {
                super(join);
                this.source = join.getQueryName(InnerSelect.this);
            }

            public String getSource(ExecuteEnvironment env) {
                return source;
            }

            protected Where getInnerWhere() {
                return innerJoin.getWhere();
            }
        }

        final MAddExclMap<Table.Join, TableSelect> tables = MapFact.mAddExclMap();
        private String getAlias(Table.Join table) {
            TableSelect join = tables.get(table);
            if(join==null) {
                join = new TableSelect(table);
                tables.exclAdd(table,join);
            }
            return join.alias;
        }

        public String getSource(Table.Join.Expr expr) {
            return getAlias(expr.getInnerJoin())+"."+expr.property;
        }
        public String getSource(Table.Join.IsIn where) {
            return getAlias(where.getJoin()) + "." + where.getFirstKey() + " IS NOT NULL";
        }
        public String getSource(IsClassExpr classExpr) {
            InnerExpr joinExpr = classExpr.getJoinExpr();
            if(joinExpr instanceof Table.Join.Expr)
                return getSource((Table.Join.Expr)joinExpr);
            else
                return getSource((SubQueryExpr)joinExpr);
        }

        private abstract class QuerySelect<K extends Expr, I extends OuterContext<I>,J extends QueryJoin<K,?,?,?>,E extends QueryExpr<K,I,J,?,?>> extends JoinSelect<J> {
            ImRevMap<String, K> group;

            protected ImMap<String, BaseExpr> initJoins(J groupJoin) {
                group = groupJoin.group.mapRevValues(new GenNameIndex("k", "")).reverse();
                return group.join(groupJoin.group);
            }

            QuerySelect(J groupJoin) {
                super(groupJoin);
            }

            private MRevMap<I,String> mQueries = MapFact.mRevMap();
            private MExclMap<I,E> mExprs = MapFact.mExclMap(); // нужен для innerWhere и классовой информации, query транслированный -> в общее выражение

            public ImRevMap<String, I> queries;
            protected ImMap<I,E> exprs; // нужен для innerWhere и классовой информации, query транслированный -> в общее выражение
            public void finalIm() {
                queries = mQueries.immutableRev().reverse();
                mQueries = null;
                exprs = mExprs.immutable();
                mExprs = null;
            }

            public String add(I query,E expr) {
                if(mQueries!=null) { // из-за getInnerWhere во from'е
                    String name = mQueries.get(query);
                    if(name==null) {
                        name = "e"+ mQueries.size();
                        mQueries.revAdd(query, name);
                        mExprs.exclAdd(query, expr);
                    }
                    return alias + "." + name;
                } else
                    return alias + "." + queries.reverse().get(query);
            }

            protected String getEmptySelect(final Where groupWhere) {
                ImMap<String, String> keySelect = group.mapValues(new GetValue<String, K>() {
                    public String getMapValue(K value) {
                        return value.getType(groupWhere).getCast(SQLSyntax.NULL, syntax, false);
                    }});

                ImMap<String, String> propertySelect = queries.mapValues(new GetValue<String, I>() {
                    public String getMapValue(I value) {
                        return exprs.get(value).getType().getCast(SQLSyntax.NULL, syntax, false);
                    }});
                return "(" + syntax.getSelect("empty", SQLSession.stringExpr(keySelect, propertySelect), "", "", "", "", "") + ")";
            }

            // чтобы разбить рекурсию
            protected boolean checkRecursivePush(Where fullWhere) {
                return false;
//                return fullWhere.getComplexity(false) > InnerSelect.this.fullWhere.getComplexity(false); // не проталкиваем если, полученная сложность больше сложности всего запроса
            }
            
            protected Where pushWhere(Where groupWhere, ImMap<K, BaseExpr> innerJoins, StatKeys<K> statKeys, Result<String> empty) {
                Where fullWhere = groupWhere;
                Where pushWhere = null;
                if(innerJoins.size() > 1 && (pushWhere = whereJoins.getGroupPushWhere(innerJoins, upWheres, innerJoin, keyStat, fullWhere.getStatRows(), statKeys.rows))!=null) // проталкивание по многим ключам
                    fullWhere = fullWhere.and(pushWhere);
                else
                    for(K key : innerJoins.keyIt()) // проталкивание по одному ключу
                        if((pushWhere = whereJoins.getGroupPushWhere(MapFact.singleton(key, innerJoin.group.get(key)), upWheres, innerJoin, keyStat, fullWhere.getStatRows(), statKeys.distinct.get(key)))!=null)
                            fullWhere = fullWhere.and(pushWhere);
                if(fullWhere.pack().isFalse()) { // может быть когда проталкивается верхнее условие, а внутри есть NOT оно же
                    empty.set(getEmptySelect(groupWhere));
                    return null;
                }
                if(pushWhere!=null && checkRecursivePush(fullWhere))
                    fullWhere = groupWhere;
                return fullWhere;
            }

        }

        private class GroupSelect extends QuerySelect<Expr, GroupExpr.Query,GroupJoin,GroupExpr> {

            final ImSet<KeyExpr> keys;

            GroupSelect(GroupJoin groupJoin) {
                super(groupJoin);
                keys = BaseUtils.immutableCast(groupJoin.getInnerKeys());
            }

            public String getSource(ExecuteEnvironment env) {

                Where exprWhere = Where.FALSE;
                MSet<Expr> mQueryExprs = SetFact.mSet(); // так как может одновременно и SUM и MAX нужен
                for(GroupExpr.Query query : queries.valueIt()) {
                    mQueryExprs.addAll(query.getExprs());
                    exprWhere = exprWhere.or(query.getWhere());
                }
                ImSet<Expr> queryExprs = group.values().toSet().merge(mQueryExprs.immutable());

                Where groupWhere = exprWhere.and(Expr.getWhere(group));

                Result<String> empty = new Result<String>();
                groupWhere = pushWhere(groupWhere, innerJoin.getJoins(), innerJoin.getStatKeys(keyStat), empty);
                if(groupWhere==null)
                    return empty.result;

                final Result<ImCol<String>> whereSelect = new Result<ImCol<String>>(); // проверить crossJoin
                final Result<ImMap<Expr,String>> fromPropertySelect = new Result<ImMap<Expr, String>>();
                String fromSelect = new Query<KeyExpr,Expr>(keys.toRevMap(), queryExprs.toMap(), groupWhere)
                    .compile(syntax, subcontext).fillSelect(new Result<ImMap<KeyExpr, String>>(), fromPropertySelect, whereSelect, params, env);

                ImMap<String, String> keySelect = group.join(fromPropertySelect.result);
                ImMap<String, String> propertySelect = queries.mapValues(new GetValue<String, GroupExpr.Query>() {
                    public String getMapValue(GroupExpr.Query value) {
                        return value.getSource(fromPropertySelect.result, syntax, exprs.get(value).getType());
                    }});

                ImCol<String> havingSelect;
                if(isSingle(innerJoin))
                    havingSelect = SetFact.singleton(propertySelect.get(queries.singleKey()) + " IS NOT NULL");
                else
                    havingSelect = SetFact.EMPTY();
                return "(" + getGroupSelect(fromSelect, keySelect, propertySelect, whereSelect.result, havingSelect) + ")";
            }

            protected Where getInnerWhere() {
                // бежим по всем exprs'ам и проверяем что нет AggrType'а
                Where result = Where.TRUE;
                for(int i=0,size=exprs.size();i<size;i++) {
                    if(exprs.getKey(i).type.canBeNull())
                        return Where.TRUE;
                    result = result.or(exprs.getValue(i).getWhere());
                }
                return result;
            }
        }

        private String getGroupBy(ImCol<String> keySelect) {
            return BaseUtils.evl(((ImList) (syntax.supportGroupNumbers() ? ListFact.consecutiveList(keySelect.size()) : keySelect.toList())).toString(","), "3+2");
        }

        private class PartitionSelect extends QuerySelect<KeyExpr, PartitionExpr.Query,PartitionJoin,PartitionExpr> {

            final ImMap<KeyExpr,BaseExpr> mapKeys;
            private PartitionSelect(PartitionJoin partitionJoin) {
                super(partitionJoin);
                mapKeys = partitionJoin.group;
            }

            public String getSource(ExecuteEnvironment env) {

                MSet<Expr> mQueryExprs = SetFact.mSet();
                for(PartitionExpr.Query query : queries.valueIt())
                    mQueryExprs.addAll(query.getExprs());
                ImSet<Expr> queryExprs = mQueryExprs.immutable();

                Where innerWhere = innerJoin.getWhere();

                Where fullWhere = innerWhere;
                if(Settings.get().isPushOrderWhere()) {
                    StatKeys<KeyExpr> statKeys = innerJoin.getStatKeys(keyStat); // определяем ключи которые надо протолкнуть
                    Where pushWhere;
                    if((pushWhere = whereJoins.getPartitionPushWhere(innerJoin.getJoins(), innerJoin.getPartitions(), upWheres, innerJoin, keyStat, fullWhere.getStatRows(), statKeys.rows))!=null) // проталкивание по многим ключам
                        fullWhere = fullWhere.and(pushWhere);
                    if(fullWhere.pack().isFalse()) // может быть когда проталкивается верхнее условие, а внутри есть NOT оно же
                        return getEmptySelect(innerWhere);
                    if(pushWhere!=null && checkRecursivePush(fullWhere))
                        fullWhere = innerWhere;
                }

                Result<ImMap<String,String>> keySelect = new Result<ImMap<String, String>>();
                Result<ImMap<Expr,String>> fromPropertySelect = new Result<ImMap<Expr, String>>();
                Result<ImCol<String>> whereSelect = new Result<ImCol<String>>(); // проверить crossJoin
                String fromSelect = new Query<String,Expr>(group,queryExprs.toMap(),fullWhere)
                    .compile(syntax, subcontext).fillSelect(keySelect, fromPropertySelect, whereSelect, params, env);

                // обработка multi-level order'ов
                MExclMap<PartitionToken, String> mTokens = MapFact.<PartitionToken, String>mExclMap();
                ImRevValueMap<String, PartitionCalc> mResultNames = queries.mapItRevValues();// последействие (mTokens)
                for(int i=0,size=queries.size();i<size;i++) {
                    PartitionExpr.Query query = queries.getValue(i);
                    PartitionCalc calc = query.type.createAggr(mTokens,
                            query.exprs.map(fromPropertySelect.result),
                            query.orders.map(fromPropertySelect.result),
                            query.partitions.map(fromPropertySelect.result).toSet());
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
                        return fromSelect;

                    ImRevMap<PartitionToken, String> nextTokens = next.mapRevValues(new GetIndexValue<String, PartitionToken>() {
                        public String getMapValue(int i, PartitionToken token) {
                            return token.getNext().isEmpty() ? resultNames.get((PartitionCalc) token) : "ne" + i; // если верхний то нужно с нормальным именем тащить
                        }});
                    final ImMap<PartitionToken, String> ftokens = tokens;
                    ImMap<String, String> propertySelect = nextTokens.reverse().mapValues(new GetValue<String, PartitionToken>() {
                        public String getMapValue(PartitionToken value) {
                            return value.getSource(ftokens, syntax);
                        }});

                    fromSelect = "(" + syntax.getSelect(fromSelect + (i>1?" q":""), SQLSession.stringExpr(keySelect.result,propertySelect),
                        (i>1?"":whereSelect.result.toString(" AND ")),"","","", "") + ")";
                    keySelect.set(keySelect.result.keys().toMap()); // ключи просто превращаем в имена
                    tokens = nextTokens;
                }
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(int i=0,size=exprs.size();i<size;i++)
                    if(!exprs.getKey(i).type.canBeNull())
                        result = result.and(exprs.getValue(i).getWhere());
                return result;
            }
        }

        private class SubQuerySelect extends QuerySelect<KeyExpr,Expr,SubQueryJoin,SubQueryExpr> {

            final ImMap<KeyExpr,BaseExpr> mapKeys;
            private SubQuerySelect(SubQueryJoin subQueryJoin) {
                super(subQueryJoin);
                mapKeys = subQueryJoin.group;
            }

            public String getSource(ExecuteEnvironment env) {

                Where innerWhere = innerJoin.getWhere();

                Result<String> empty = new Result<String>();
                innerWhere = pushWhere(innerWhere, innerJoin.getJoins(), innerJoin.getStatKeys(keyStat), empty);
                if(innerWhere==null)
                    return empty.result;

                Result<ImMap<String, String>> keySelect = new Result<ImMap<String, String>>();
                Result<ImMap<String, String>> propertySelect = new Result<ImMap<String, String>>();
                Result<ImCol<String>> whereSelect = new Result<ImCol<String>>();
                String fromSelect = new Query<String,String>(group, queries, innerWhere).compile(syntax, subcontext).fillSelect(keySelect, propertySelect, whereSelect, params, env);
                return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect.result,propertySelect.result),
                    whereSelect.result.toString(" AND "),"","","", "") + ")";

            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(int i=0,size=exprs.size();i<size;i++)
                    result = result.and(exprs.getValue(i).getWhere());
                return result;
            }
        }


        protected String getGroupSelect(String fromSelect, ImOrderMap<String, String> keySelect, ImOrderMap<String, String> propertySelect, ImCol<String> whereSelect, ImCol<String> havingSelect) {
            return syntax.getSelect(fromSelect, SQLSession.stringExpr(keySelect, propertySelect), whereSelect.toString(" AND "), "", getGroupBy(keySelect.values()), havingSelect.toString(" AND "), "");
        }
        protected String getGroupSelect(String fromSelect, ImMap<String, String> keySelect, ImMap<String, String> propertySelect, ImCol<String> whereSelect, ImCol<String> havingSelect) {
            return getGroupSelect(fromSelect, keySelect.toOrderMap(), propertySelect.toOrderMap(), whereSelect, havingSelect);
        }

        private class RecursiveSelect extends QuerySelect<KeyExpr,RecursiveExpr.Query,RecursiveJoin,RecursiveExpr> {
            private RecursiveSelect(RecursiveJoin recJoin) {
                super(recJoin);
            }

            private String getSelect(ImRevMap<String, KeyExpr> keys, ImMap<String, Expr> props, final ImMap<String, Type> columnTypes, Where where, Result<ImOrderSet<String>> keyOrder, Result<ImOrderSet<String>> propOrder, boolean useRecursionFunction, boolean recursive, ImRevMap<Value, String> params, SubQueryContext subcontext, ExecuteEnvironment env) {
                ImRevMap<String, KeyExpr> itKeys = innerJoin.getMapIterate().mapRevKeys(new GenNameIndex("pv_", ""));

                Result<ImMap<String, String>> keySelect = new Result<ImMap<String, String>>();
                Result<ImMap<String, String>> propertySelect = new Result<ImMap<String, String>>();
                Result<ImCol<String>> whereSelect = new Result<ImCol<String>>();
                String fromSelect = new Query<String,String>(keys.addRevExcl(itKeys), props, where).compile(syntax, subcontext, recursive && !useRecursionFunction).fillSelect(keySelect, propertySelect, whereSelect, params, env);

                ImOrderMap<String, String> orderKeySelect = SQLSession.mapNames(keySelect.result.filterIncl(keys.keys()), keyOrder);
                ImOrderMap<String, String> orderPropertySelect = SQLSession.mapNames(propertySelect.result, propOrder);

                if(useRecursionFunction) {
                    ImOrderMap<String, String> orderCastKeySelect = orderKeySelect.mapOrderValues(new GetKeyValue<String, String, String>() {
                        public String getMapValue(String key, String value) {
                            return columnTypes.get(key).getCast(value, syntax, false);
                        }});
                    ImOrderMap<String, String> orderGroupPropertySelect = orderPropertySelect.mapOrderValues(new GetKeyValue<String, String, String>() {
                        public String getMapValue(String key, String value) {
                            Type type = columnTypes.get(key);
                            return type.getCast((type instanceof ArrayClass ? GroupType.AGGAR_SETADD : GroupType.SUM).getSource(
                                    ListFact.<String>singleton(value), MapFact.<String, Boolean>EMPTYORDER(), SetFact.<String>EMPTY(), type, syntax), syntax, false);
                        }});
                    return "(" + getGroupSelect(fromSelect, orderCastKeySelect, orderGroupPropertySelect, whereSelect.result, SetFact.<String>EMPTY()) + ")";
                } else
                    return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(orderKeySelect, orderPropertySelect), whereSelect.result.toString(" AND "),"","","", "") + ")";
            }

            public String getParamSource(boolean useRecursionFunction, final boolean wrapStep, ExecuteEnvironment env) {
                ImRevMap<KeyExpr, KeyExpr> mapIterate = innerJoin.getMapIterate();

                Where initialWhere = innerJoin.getInitialWhere();
                final Where baseInitialWhere = initialWhere;
                Where stepWhere = innerJoin.getStepWhere();

                boolean isLogical = innerJoin.isLogical();
                boolean cyclePossible = innerJoin.isCyclePossible();

                boolean single = isSingle(innerJoin);

                String rowPath = "qwpather";

                ImMap<String, Type> props = queries.mapValues(new GetValue<Type, RecursiveExpr.Query>() {
                    public Type getMapValue(RecursiveExpr.Query value) {
                        return value.getType();
                    }});

                Expr concKeys = null; ArrayClass rowType = null;
                if(cyclePossible && (!isLogical || useRecursionFunction)) {
                    concKeys = ConcatenateExpr.create(mapIterate.keys().toOrderSet());
                    rowType = ArrayClass.get(concKeys.getType(baseInitialWhere));
                    props = props.addExcl(rowPath, rowType);
                }

                // проталкивание
                ImMap<KeyExpr, BaseExpr> staticGroup = innerJoin.getJoins().remove(mapIterate.keys());
                Result<String> empty = new Result<String>();
                initialWhere = pushWhere(initialWhere, staticGroup, initialWhere.getStatKeys(staticGroup.keys()), empty);
                if(initialWhere==null)
                    return empty.result;

                RecursiveJoin tableInnerJoin = innerJoin;
                if(!BaseUtils.hashEquals(initialWhere, baseInitialWhere)) // проверка на hashEquals - оптимизация, само такое проталкивание нужно чтобы у RecursiveTable - статистика была правильной
                    tableInnerJoin = new RecursiveJoin(innerJoin, initialWhere);

                String recName = subcontext.wrapRecursion("rectable"); Result<ImRevMap<String, KeyExpr>> recKeys = new Result<ImRevMap<String, KeyExpr>>();
                final Join<String> recJoin = tableInnerJoin.getRecJoin(props, recName, recKeys);
                
                final Where wrapClassWhere = wrapStep ? tableInnerJoin.getIsClassWhere() : null;

                ImRevMap<String, Expr> initialExprs;
                ImMap<String, Expr> stepExprs;
                ImMap<String, String> propertySelect;
                if(isLogical) {
                    propertySelect = queries.mapValues(new GetStaticValue<String>() {
                        public String getMapValue() {
                            return syntax.getBitString(true);
                        }});
                    stepExprs = MapFact.EMPTY();
                    initialExprs = MapFact.EMPTYREV();
                } else {
                    initialExprs = queries.mapRevValues(new GetValue<Expr, RecursiveExpr.Query>() {
                        public Expr getMapValue(RecursiveExpr.Query value) {
                            return value.initial;
                        }});
                    stepExprs = queries.mapValues(new GetKeyValue<Expr, String, RecursiveExpr.Query>() {
                        public Expr getMapValue(String key, RecursiveExpr.Query value) {
                            Expr step = value.step;
                            if (wrapStep)
                                step = SubQueryExpr.create(step.and(wrapClassWhere));
                            return recJoin.getExpr(key).mult(step, (IntegralClass) value.getType());
                        }});
                    propertySelect = queries.mapValues(new GetKeyValue<String, String, RecursiveExpr.Query>() {
                        public String getMapValue(String key, RecursiveExpr.Query value) {
                            return GroupType.SUM.getSource(ListFact.singleton(key), MapFact.<String, Boolean>EMPTYORDER(), SetFact.<String>EMPTY(), value.getType(), syntax);
                        }});
                }

                ImCol<String> havingSelect;
                if(single)
                    havingSelect = SetFact.singleton(propertySelect.get(queries.singleKey()) + " IS NOT NULL");
                else
                    havingSelect = SetFact.EMPTY();

                if(wrapStep) // чтобы избавляться от проблем с 2-м использованием
                    stepWhere = SubQueryExpr.create(stepWhere.and(wrapClassWhere));

                Where recWhere;
                if(cyclePossible && (!isLogical || useRecursionFunction)) {
                    Expr prevPath = recJoin.getExpr(rowPath);

                    Where noNodeCycle = concKeys.compare(prevPath, Compare.INARRAY).not();
                    if(isLogical)
                        recWhere = recJoin.getWhere().and(noNodeCycle);
                    else {
                        recWhere = Where.TRUE;
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

                    Expr rowSource = FormulaExpr.createCustomFormula(rowType.getCast("ARRAY[prm1]", syntax, false), rowType, concKeys); // баг сервера, с какого-то бодуна ARRAY[char(8)] дает text[]
                    initialExprs = initialExprs.addRevExcl(rowPath, rowSource); // заполняем начальный путь
                    stepExprs = stepExprs.addExcl(rowPath, FormulaExpr.createCustomFormula(rowType.getCast("(prm1 || prm2)", syntax, false), rowType, prevPath, rowSource)); // добавляем тек. вершину
                } else
                    recWhere = recJoin.getWhere();

                ImMap<String, Type> columnTypes = initialExprs.addExcl(recKeys.result).mapValues(new GetValue<Type, Expr>() {
                    public Type getMapValue(Expr value) {
                        return value.getType(baseInitialWhere);
                    }});

                String outerParams = null;
                ImRevMap<Value, String> innerParams;
                if(useRecursionFunction) {
                    ImSet<Value> values = AbstractOuterContext.getOuterValues(SetFact.merge(queries.valuesSet(), initialWhere));
                    outerParams = "";
                    ImRevValueMap<Value, String> mvInnerParams = values.mapItRevValues(); // "совместная" обработка / последействие
                    int iv = 1;
                    for(int i=0,size=values.size();i<size;i++) {
                        Value value = values.get(i);
                        String paramValue = params.get(value);
                        if(!value.getParseInterface().isSafeString()) {
                            Type type = ((ValueExpr)value).getType();
                            outerParams += "," + type.getBinaryCast(paramValue, syntax, false);
                            paramValue = type.getCast("$1["+(iv++)+"]",syntax, false);
                        } else
                            env.addNoPrepare();
                        mvInnerParams.mapValue(i, paramValue);
                    }
                    innerParams = mvInnerParams.immutableValueRev();
                    if(outerParams.isEmpty()) // забавный прикол СУБД (в частности postgre)
                        outerParams = ",CAST('x' AS char(1))";
                } else
                    innerParams = params;

                SubQueryContext pushContext = subcontext.pushRecursion();// чтобы имена не пересекались

                Result<ImOrderSet<String>> keyOrder = new Result<ImOrderSet<String>>(); Result<ImOrderSet<String>> propOrder = new Result<ImOrderSet<String>>();
                String initialSelect = getSelect(recKeys.result, initialExprs, columnTypes, initialWhere, keyOrder, propOrder, useRecursionFunction, false, innerParams, pushContext, env);
                String stepSelect = getSelect(recKeys.result, stepExprs, columnTypes, stepWhere.and(recWhere), keyOrder, propOrder, useRecursionFunction, true, innerParams, pushContext, env);
                ImOrderSet<String> fieldOrder = keyOrder.result.addOrderExcl(propOrder.result);

                ImMap<String, String> keySelect = group.crossValuesRev(recKeys.result);
                env.addVolatileStats();
                if(useRecursionFunction) {
                    env.addNoReadOnly();
                    return "(" + getGroupSelect("recursion('" + recName +"'," +
                            "'" + StringEscapeUtils.escapeSql(initialSelect)+"','" +StringEscapeUtils.escapeSql(stepSelect)+"'"+outerParams+") recursion ("
                            + Field.getDeclare(fieldOrder.mapOrderMap(columnTypes), syntax) + ")", keySelect, propertySelect, SetFact.<String>EMPTY(), havingSelect) + ")";
                } else {
                    if(StringUtils.countMatches(stepSelect, recName) > 1)
                        return null;
                    String recursiveWith = "WITH RECURSIVE " + recName + "(" + fieldOrder.toString(",") + ") AS (" + initialSelect +
                            " UNION " + (isLogical && cyclePossible?"":"ALL ") + stepSelect + ") ";
                    return "(" + recursiveWith + (isLogical ? syntax.getSelect(recName, SQLSession.stringExpr(keySelect, propertySelect), "", "", "", "", "")
                            : getGroupSelect(recName, keySelect, propertySelect, SetFact.<String>EMPTY(), havingSelect)) + ")";
                }
            }

            private String getCTESource(boolean wrapExpr, ExecuteEnvironment env) {
                ExecuteEnvironment thisEnv = new ExecuteEnvironment();
                String cteSelect = getParamSource(false, wrapExpr, thisEnv);
                if(cteSelect!=null)
                    env.add(thisEnv);
                return cteSelect;
            }
            public String getSource(ExecuteEnvironment env) {
                boolean isLogical = innerJoin.isLogical();
                boolean cyclePossible = innerJoin.isCyclePossible();

                if(isLogical || !cyclePossible) { // если isLogical или !cyclePossible пытаемся обойтись рекурсивным CTE
                    String cteSelect = getCTESource(false, env);
                    if(cteSelect!=null)
                        return cteSelect;
                    cteSelect = getCTESource(true, env);
                    if(cteSelect!=null)
                        return cteSelect;
                }
                return getParamSource(true, false, env);
            }

            protected Where getInnerWhere() {
                Where result = Where.TRUE;
                for(RecursiveExpr expr : exprs.valueIt())
                    result = result.and(expr.getWhere());
                return result;
            }
        }

        private boolean isSingle(QueryJoin join) { // в общем то чтобы нормально использовался ANTI-JOIN в некоторых СУБД, (а он нужен в свою очередь чтобы A LEFT JOIN B WHERE B.e IS NULL)
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
        public String getNullSource(QueryExpr queryExpr, boolean notNull) {
            String result = super.getNullSource(queryExpr, notNull);
            if(isSingle(queryExpr.getInnerJoin())) {
                QuerySelect singleSelect = getSingleSelect(queryExpr);
                ImSet keys = singleSelect.group.keys();
                if(!keys.isEmpty())
                    return singleSelect.alias + "." + keys.get(0) + " IS" + (notNull?" NOT":"") + " NULL";
            }
            return result;
        }

        final MAddExclMap<QueryJoin, QuerySelect> queries = MapFact.mAddExclMap();
        final MAddExclMap<GroupExpr, String> groupExprSources = MapFact.mAddExclMap();
        public String getSource(QueryExpr queryExpr) {
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
            if(isSingle(exprJoin)) {
                QuerySelect select = getSingleSelect(queryExpr);
                if(select!=null)
                    return select.add(queryExpr.query, queryExpr);
            } else
                for(int i=0,size=queries.size();i<size;i++) {
                    MapTranslate translator;
                    if((translator= exprJoin.mapInner(queries.getKey(i), false))!=null)
                        return queries.getValue(i).add(queryExpr.query.translateOuter(translator),queryExpr);
                }

            QuerySelect select;
            if(exprJoin instanceof GroupJoin) // нету группы - создаем, чтобы не нарушать модульность сделаем без наследования
                select = new GroupSelect((GroupJoin) exprJoin);
            else
            if(exprJoin instanceof PartitionJoin)
                select = new PartitionSelect((PartitionJoin) exprJoin);
            else
            if(exprJoin instanceof RecursiveJoin)
                select = new RecursiveSelect((RecursiveJoin)exprJoin);
            else
                select = new SubQuerySelect((SubQueryJoin)exprJoin);

            queries.exclAdd(exprJoin,select);
            return select.add(queryExpr.query,queryExpr);
        }
        private JoinSelect getJoinSelect(InnerJoin innerJoin) {
            if(innerJoin instanceof Table.Join)
                return tables.get((Table.Join)innerJoin);
            if(innerJoin instanceof QueryJoin)
                return queries.get((QueryJoin)innerJoin);
            throw new RuntimeException("no matching class");
        }
    }

    private static <V> ImMap<V, String> castProperties(ImMap<V,String> propertySelect, final ImMap<V,Type> castTypes, final SQLSyntax syntax) { // проставим Cast'ы для null'ов
        return propertySelect.mapValues(new GetKeyValue<String, V, String>() {
            public String getMapValue(V key, String propertyString) {
                Type castType;
                if (propertyString.equals(SQLSyntax.NULL) && (castType = castTypes.get(key)) != null) // кривовато с проверкой на null, но собсно и надо чтобы обойти баг
                    propertyString = castType.getCast(propertyString, syntax, false);
                return propertyString;
            }
        });
    }

    // castTypes параметр чисто для бага Postgre и может остальных
    private static <K,V> String getInnerSelect(ImRevMap<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, ImMap<V, Expr> compiledProps, ImRevMap<Value, String> params, ImOrderMap<V, Boolean> orders, int top, SQLSyntax syntax, ImRevMap<K, String> keyNames, ImRevMap<V, String> propertyNames, Result<ImOrderSet<K>> keyOrder, Result<ImOrderSet<V>> propertyOrder, ImMap<V, Type> castTypes, SubQueryContext subcontext, boolean noInline, ExecuteEnvironment env) {
        Result<ImMap<K,String>> andKeySelect = new Result<ImMap<K, String>>(); Result<ImCol<String>> andWhereSelect = new Result<ImCol<String>>(); Result<ImMap<V,String>> andPropertySelect = new Result<ImMap<V, String>>();
        String andFrom = fillInnerSelect(mapKeys, innerSelect, compiledProps, andKeySelect, andPropertySelect, andWhereSelect, params, syntax, subcontext, env);

        if(castTypes!=null)
            andPropertySelect.set(castProperties(andPropertySelect.result, castTypes, syntax));

        Result<ImSet<V>> ordersNotNull = new Result<ImSet<V>>();
        return getSelect(andFrom, andKeySelect.result, keyNames, keyOrder, andPropertySelect.result, propertyNames, propertyOrder, andWhereSelect.result, syntax, CompiledQuery.getOrdersNotNull(orders, compiledProps, ordersNotNull), ordersNotNull.result, top, noInline);
    }

    private static <K,V> String getSelect(String from, ImMap<K, String> keySelect, ImRevMap<K, String> keyNames, Result<ImOrderSet<K>> keyOrder, ImMap<V, String> propertySelect, ImRevMap<V, String> propertyNames, Result<ImOrderSet<V>> propertyOrder, ImCol<String> whereSelect, SQLSyntax syntax, ImOrderMap<V, Boolean> orders, ImSet<V> ordersNotNull, int top, boolean noInline) {
        return syntax.getSelect(from, SQLSession.stringExpr(SQLSession.mapNames(keySelect, keyNames, keyOrder),
                SQLSession.mapNames(propertySelect, propertyNames, propertyOrder)) + (noInline && syntax.inlineTrouble()?",random()":""),
                whereSelect.toString(" AND "), Query.stringOrder(propertyOrder.result, keySelect.size(), orders, ordersNotNull, syntax),"", "", top==0?"":String.valueOf(top));
    }

    private static <K,AV> String fillSingleSelect(ImRevMap<K, KeyExpr> mapKeys, GroupJoinsWhere innerSelect, ImMap<AV, Expr> compiledProps, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, ImRevMap<Value,String> params, SQLSyntax syntax, SubQueryContext subcontext, ExecuteEnvironment env) {
        return fillFullSelect(mapKeys, SetFact.singleton(innerSelect), innerSelect.getFullWhere(), compiledProps, MapFact.<AV, Boolean>EMPTYORDER(), 0, resultKey, resultProperty, params, syntax, subcontext, env);

/*        FullSelect FJSelect = new FullSelect(innerSelect.where, params,syntax); // для keyType'а берем первый where

        MapWhere<JoinData> joinDatas = new MapWhere<JoinData>();
        for(Map.Entry<AV, Expr> joinProp : compiledProps.entrySet())
            joinProp.getValue().fillJoinWheres(joinDatas, Where.TRUE);

        String innerAlias = subcontext+"inalias";
        Map<String, Expr> joinProps = new HashMap<String, Expr>();
        // затем все данные по JoinSelect'ам по вариантам
        for(JoinData joinData : joinDatas.keys()) {
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

    private static <K,AV> String fillInnerSelect(ImRevMap<K, KeyExpr> mapKeys, final GroupJoinsWhere innerSelect, ImMap<AV, Expr> compiledProps, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, Result<ImCol<String>> resultWhere, ImRevMap<Value, String> params, SQLSyntax syntax, SubQueryContext subcontext, ExecuteEnvironment env) {

        final InnerSelect compile = new InnerSelect(mapKeys.valuesSet(), innerSelect.where, innerSelect.where, innerSelect.where,innerSelect.joins,innerSelect.upWheres,syntax,params, subcontext);

        if(Settings.get().getInnerGroupExprs() > 0) { // если не одни joinData
            final MAddSet<GroupExpr> groupExprs = SetFact.mAddSet(); final Counter repeats = new Counter();
            for(Expr property : compiledProps.valueIt())
                property.enumerate(new ExprEnumerator() {
                    public Boolean enumerate(OuterContext join) {
                        if (join instanceof JoinData) { // если JoinData то что внутри не интересует
                            if (join instanceof GroupExpr && !compile.isInner(((GroupExpr) join).getInnerJoin()) && !groupExprs.add((GroupExpr) join))
                                repeats.add();
                            return false;
                        }
                        return true;
                    }
                });
            if(repeats.getValue() > Settings.get().getInnerGroupExprs())
                return fillSingleSelect(mapKeys, innerSelect, compiledProps, resultKey, resultProperty, params, syntax, subcontext, env);
        }

        MCol<String> mWhereSelect = ListFact.mCol();
        compile.fillInnerJoins(mWhereSelect);
        QueryTranslator keyEqualTranslator = innerSelect.keyEqual.getTranslator();

        resultProperty.set(keyEqualTranslator.translate(compiledProps).mapValues(compile.<Expr>GETSOURCE()));
        resultKey.set(keyEqualTranslator.translate(mapKeys).mapValues(compile.<Expr>GETSOURCE()));

        String from = compile.getFrom(innerSelect.where, mWhereSelect, env);
        resultWhere.set(mWhereSelect.immutableCol());
        return from;
    }
    
    private final static class AddAlias implements GetValue<String, String> {
        private final String alias;
        private AddAlias(String alias) {
            this.alias = alias;
        }

        public String getMapValue(String value) {
            return alias + "." + value;
        }
    }
    public final static class GenNameIndex implements GetIndex<String> {
        private final String prefix;
        private final String postfix;
        public GenNameIndex(String prefix, String postfix) {
            this.prefix = prefix;
            this.postfix = postfix;
        }

        public String getMapValue(int index) {
            return prefix + index + postfix;
        }
    }
    private final static GetValue<String, String> coalesceValue = new GetValue<String, String>() {
        public String getMapValue(String value) {
            return "COALESCE(" + value + ")";
        }
    };

    private static <K,AV> String fillFullSelect(ImRevMap<K, KeyExpr> mapKeys, ImCol<GroupJoinsWhere> innerSelects, Where fullWhere, ImMap<AV, Expr> compiledProps, ImOrderMap<AV, Boolean> orders, int top, Result<ImMap<K, String>> resultKey, Result<ImMap<AV, String>> resultProperty, ImRevMap<Value, String> params, SQLSyntax syntax, final SubQueryContext subcontext, ExecuteEnvironment env) {

        // создаем And подзапросыs
        final ImSet<AndJoinQuery> andProps = innerSelects.mapColSetValues(new GetIndexValue<AndJoinQuery, GroupJoinsWhere>() {
            public AndJoinQuery getMapValue(int i, GroupJoinsWhere value) {
                return new AndJoinQuery(value, subcontext.wrapAlias("f" + i));
            }
        });

        MMap<JoinData, Where> mJoinDataWheres = MapFact.mMap(AbstractWhere.<JoinData>addOr());
        for(int i=0,size=compiledProps.size();i<size;i++)
            if(!orders.containsKey(compiledProps.getKey(i)))
                compiledProps.getValue(i).fillJoinWheres(mJoinDataWheres, Where.TRUE);
        ImMap<JoinData, Where> joinDataWheres = mJoinDataWheres.immutable();

        // для JoinSelect'ов узнаем при каких условиях они нужны
        MMap<Object, Where> mJoinWheres = MapFact.mMapMax(joinDataWheres.size(), AbstractWhere.addOr());
        for(int i=0,size=joinDataWheres.size();i<size;i++)
            mJoinWheres.add(joinDataWheres.getKey(i).getFJGroup(),joinDataWheres.getValue(i));
        ImMap<Object, Where> joinWheres = mJoinWheres.immutable();

        // сначала распихиваем JoinSelect по And'ам
        ImMap<Object, ImSet<AndJoinQuery>> joinAnds = joinWheres.mapValues(new GetValue<ImSet<AndJoinQuery>, Where>() {
            public ImSet<AndJoinQuery> getMapValue(Where value) {
                return getWhereSubSet(andProps, value);
            }});

        // затем все данные по JoinSelect'ам по вариантам
        ImValueMap<JoinData, String> mvJoinData = joinDataWheres.mapItValues(); // последействие есть
        for(int i=0;i<joinDataWheres.size();i++) {
            JoinData joinData = joinDataWheres.getKey(i);
            String joinName = "join_" + i;
            Collection<AndJoinQuery> dataAnds = new ArrayList<AndJoinQuery>();
            for(AndJoinQuery and : getWhereSubSet(joinAnds.get(joinData.getFJGroup()), joinDataWheres.getValue(i))) {
                Expr joinExpr = joinData.getFJExpr();
                if(!and.innerSelect.getFullWhere().means(joinExpr.getWhere().not())) { // проверим что не всегда null
                    and.properties.exclAdd(joinName, joinExpr);
                    dataAnds.add(and);
                }
            }
            String joinSource = ""; // заполняем Source
            if(dataAnds.size()==0)
                throw new RuntimeException(ServerResourceBundle.getString("data.query.should.not.be"));
            else
            if(dataAnds.size()==1)
                joinSource = dataAnds.iterator().next().alias +'.'+joinName;
            else {
                for(AndJoinQuery and : dataAnds)
                    joinSource = (joinSource.length()==0?"":joinSource+",") + and.alias + '.' + joinName;
                joinSource = "COALESCE(" + joinSource + ")";
            }
            mvJoinData.mapValue(i, joinData.getFJString(joinSource));
        }
        ImMap<JoinData, String> joinData = mvJoinData.immutableValue();

        // order'ы отдельно обрабатываем, они нужны в каждом запросе генерируем имена для Order'ов
        MOrderExclMap<String, Boolean> mOrderAnds = MapFact.mOrderExclMap(!(top == 0) ? orders.size() : 0);
        ImValueMap<AV, String> mvPropertySelect = compiledProps.mapItValues(); // сложный цикл
        for(int i=0,size=compiledProps.size();i<size;i++) {
            AV prop = compiledProps.getKey(i);
            Boolean dir = orders.get(prop);
            if(dir!=null) {
                String orderName = "order_" + i;
                String orderFJ = "";
                for(AndJoinQuery and : andProps) {
                    and.properties.exclAdd(orderName, compiledProps.get(prop));
                    orderFJ = (orderFJ.length()==0?"":orderFJ+",") + and.alias + "." + orderName;
                }
                if(!(top==0)) // если все то не надо упорядочивать, потому как в частности MS SQL не поддерживает
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
        for(AndJoinQuery and : andProps) {
            // закинем в And.Properties OrderBy, все равно какие порядки ключей и выражений
            ImMap<String, Expr> andProperties = and.properties.immutable();
            String andSelect = "(" + getInnerSelect(mapKeys, and.innerSelect, andProperties, params, orderAnds, top, syntax, keyNames, andProperties.keys().toRevMap(), new Result<ImOrderSet<K>>(), new Result<ImOrderSet<String>>(), null, subcontext, innerSelects.size()==1, env) + ") " + and.alias;

            final ImRevMap<K, String> andKeySources = keyNames.mapRevValues(new AddAlias(and.alias));

            if(keySelect==null) {
                compileFrom = andSelect;
                keySelect = andKeySources;
            } else {
                String andJoin = andKeySources.crossJoin(first?keySelect:keySelect.mapValues(coalesceValue)).toString("=", " AND ");
                keySelect = keySelect.mapValues(new GetKeyValue<String, K, String>() {
                    public String getMapValue(K key, String value) {
                        return value + "," + andKeySources.get(key);
                    }});
                compileFrom = compileFrom + " FULL JOIN " + andSelect + " ON " + (andJoin.length()==0? Where.TRUE_STRING :andJoin);
                first = false;
            }
        }

        // полученные KeySelect'ы в Data
        if(innerSelects.size()>1)
            keySelect = keySelect.mapValues(coalesceValue);

        FullSelect FJSelect = new FullSelect(fullWhere, fullWhere, params, syntax, mapKeys.crossJoin(keySelect), joinData); // для keyType'а берем первый where
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
    private static String translateParam(String query,ImRevMap<String,String> paramValues) {
        // генерируем промежуточные имена, перетранслируем на них
        ImRevMap<String, String> preTranslate = paramValues.mapRevValues(new GenNameIndex("transp", "nt"));
        for(int i=0,size=preTranslate.size();i<size;i++)
            query = query.replace(preTranslate.getKey(i), preTranslate.getValue(i));

        // транслируем на те что должны быть
        ImRevMap<String, String> translateMap = preTranslate.crossJoin(paramValues);
        for(int i=0,size=translateMap.size();i<size;i++)
            query = query.replace(translateMap.getKey(i), translateMap.getValue(i));
        return query;
    }
    
    private final static GetValue<ParseInterface, Value> GETPARSE = new GetValue<ParseInterface, Value>() {
        public ParseInterface getMapValue(Value value) {
            return value.getParseInterface();
        }
    };
    public ImMap<String, ParseInterface> getQueryParams(QueryEnvironment env) {
        MExclMap<String, ParseInterface> mMapValues = MapFact.mExclMap();
        mMapValues.exclAdd(SQLSession.userParam, env.getSQLUser());
        mMapValues.exclAdd(SQLSession.computerParam, env.getSQLComputer());
        mMapValues.exclAdd(SQLSession.isServerRestartingParam, env.getIsServerRestarting());
        mMapValues.exclAdd(SQLSession.isFullClientParam, env.getIsFullClient());
        mMapValues.exclAdd(SQLSession.isDebugParam, new LogicalParseInterface() {
            public boolean isTrue() {
                return SystemProperties.isDebug;
            }
        });
        return mMapValues.immutable().addExcl(params.reverse().mapValues(GETPARSE));
    }

    private String fillSelect(final ImRevMap<String,String> params, Result<ImMap<K, String>> fillKeySelect, Result<ImMap<V, String>> fillPropertySelect, Result<ImCol<String>> fillWhereSelect, ExecuteEnvironment fillEnv) {
        GetValue<String, String> transValue = new GetValue<String, String>() {
            public String getMapValue(String value) {
                return translateParam(value, params);
            }};

        fillKeySelect.set(keySelect.mapValues(transValue));
        fillPropertySelect.set(propertySelect.mapValues(transValue));
        fillWhereSelect.set(whereSelect.mapColValues(transValue));
        fillEnv.add(env);
        return translateParam(from, params);
    }

    private ImRevMap<String, String> getTranslate(ImRevMap<Value, String> mapValues) {
        return params.crossJoin(mapValues);
    }

    // для подзапросов
    public String fillSelect(Result<ImMap<K, String>> fillKeySelect, Result<ImMap<V, String>> fillPropertySelect, Result<ImCol<String>> fillWhereSelect, ImRevMap<Value, String> mapValues, ExecuteEnvironment fillEnv) {
        return fillSelect(getTranslate(mapValues), fillKeySelect, fillPropertySelect, fillWhereSelect, fillEnv);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, QueryEnvironment queryEnv) throws SQLException {
        return session.executeSelect(select, env, getQueryParams(queryEnv), keyNames, keyReaders, propertyNames, propertyReaders);
    }

    public void outSelect(SQLSession session, QueryEnvironment env) throws SQLException {
        // выведем на экран
        ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> result = execute(session, env);

        for(int i=0,size=result.size();i<size;i++) {
            ImMap<K, Object> keyMap = result.getKey(i);
            for(int j=0,sizeJ=keyMap.size();j<sizeJ;j++) {
                System.out.println(keyMap.getKey(j)+"-"+keyMap.getValue(j));
            }
            System.out.println("---- ");
            ImMap<V, Object> rowMap = result.getValue(i);
            for(int j=0,sizeJ=rowMap.size();j<sizeJ;j++) {
                System.out.println(rowMap.getKey(j)+"-"+rowMap.getValue(j));
            }
        }
    }
}


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
