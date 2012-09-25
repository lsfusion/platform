package platform.server.data.expr.query;

import platform.base.*;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.*;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.pull.*;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.*;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

public class GroupExpr extends AggrExpr<Expr,GroupType,GroupExpr.Query,GroupJoin,GroupExpr,GroupExpr.QueryInnerContext> {

    @Override
    protected boolean hasDuplicateOuter() {
        return false;
    }

    public static class Query extends AggrExpr.Query<GroupType, Query> implements AndContext<Query> {

        public Query(Expr expr, GroupType type) {
            this(Collections.singletonList(expr), new OrderedMap<Expr, Boolean>(), false, type);
            assert type.hasAdd();
        }

        public Query(List<Expr> exprs, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type) {
            super(exprs, orders, ordersNotNull, type);
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public Query translateQuery(QueryTranslator translator) {
            return new Query(translator.translate(exprs), translator.translate(orders), ordersNotNull, type);
        }

        public Type getType(Where groupWhere) {
            return type.getType(getMainExpr().getType(getWhere().and(groupWhere)));
        }

        public Expr getSingleExpr() {
            return type.getSingleExpr(exprs, orders);
        }

        public Query followFalse(Where falseWhere, boolean pack) {
            return new Query(falseWhere.followFalse(exprs, pack), falseWhere.followFalse(orders, pack), ordersNotNull, type);
        }

        public String toString() {
            return "GROUP(" + exprs + "," + orders + "," + type + ")";
        }

        public Query and(Where where) { // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. AggrType.getWhere
            List<Expr> andExprs = new ArrayList<Expr>();
            Iterator<Expr> it = exprs.iterator();
            andExprs.add(it.next().and(where));
            while(it.hasNext())
                andExprs.add(it.next());
            return new Query(andExprs, orders, ordersNotNull, type);
        }

        public String getSource(Map<Expr, String> fromPropertySelect, SQLSyntax syntax, Type resultType) {
            Set<Expr> orderExprsNotNull = new HashSet<Expr>();
            OrderedMap<Expr, Boolean> packOrders = CompiledQuery.getOrdersNotNull(orders, BaseUtils.toMap(orders.keySet()), orderExprsNotNull);
            if(ordersNotNull) // если notNull, то все пометим
                orderExprsNotNull = new HashSet<Expr>(packOrders.keySet());
            return type.getSource(BaseUtils.mapList(exprs, fromPropertySelect), BaseUtils.mapOrder(packOrders, fromPropertySelect), BaseUtils.mapSet(orderExprsNotNull,fromPropertySelect), resultType, syntax);
        }
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
    }

    public StatKeys<KeyExpr> getStatGroup() {
        Where where = getInner().getFullWhere();

        QuickSet<KeyExpr> pushedKeys = new QuickSet<KeyExpr>();
        for(Expr groupExpr : group.keySet()) {
            if(groupExpr instanceof KeyExpr)
                pushedKeys.add((KeyExpr)groupExpr);
/*            final QuickSet<KeyExpr> keepKeys = new QuickSet<KeyExpr>();
            groupExpr.enumerate(new ExprEnumerator() {
                public boolean enumerate(OuterContext join) {
                    if(join instanceof QueryExpr) {
                        keepKeys.addAll(join.getOuterKeys());
                        return false;
                    }
                    return true;
                }
            });
            pushedKeys.addAll(groupExpr.getOuterKeys().remove(keepKeys));*/
        }
        return where.getStatKeys(getInner().getInnerKeys().remove(pushedKeys));
    }
    private boolean checkNoKeys() {
        return getStatGroup().rows.lessEquals(new Stat(Long.MAX_VALUE));
    }
    protected GroupExpr(Query query, Map<Expr, BaseExpr> group) {
        super(query, group);
//        assert checkNoKeys();
    }

    protected GroupExpr createThis(Query query, Map<Expr, BaseExpr> group) {
        return new GroupExpr(query, group);
    }

    protected InnerExpr translate(MapTranslate translator) {
        return new GroupExpr(this, translator);
    }

    private static Where getGroupWhere(Map<Expr, BaseExpr> innerOuter) {
        return getWhere(innerOuter.keySet());
    }

    public static class QueryInnerContext extends AggrExpr.QueryInnerContext<Expr,GroupType,GroupExpr.Query,GroupJoin,GroupExpr,QueryInnerContext> {
        public QueryInnerContext(GroupExpr thisObj) {
            super(thisObj);
        }

        @IdentityLazy
        public Where getGroupWhere() {
            return GroupExpr.getGroupWhere(thisObj.group);
        }

        @IdentityLazy
        public Type getType() {
            return thisObj.query.getType(getGroupWhere());
        }

        @IdentityLazy
        protected Where getFullWhere() {
            return thisObj.query.getWhere().and(getGroupWhere());
        }

        protected Map<KeyExpr, Type> getInnerKeyTypes() {
            KeyType contextType = getFullWhere();
            Map<KeyExpr, Type> keyStats = new HashMap<KeyExpr, Type>();
            for(KeyExpr key : getInnerKeys())
                keyStats.put(key, contextType.getKeyType(key));
            return keyStats;
        }
    }

    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    public class NotNull extends QueryExpr.NotNull {
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @Override
    public String toString() {
        int hash = hashCode();
        hash = hash>0?hash:-hash;
        int tobt = 0;
        for(int i=0;i<4;i++) {
            tobt += hash % 255;
            hash = hash/255;
        }
        return "G"+tobt;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return createOuterGroupCases(translator.translate(group), query, new HashMap<BaseExpr, BaseExpr>(), false);
    }

    public String getExprSource(CompileSource source, SubQueryContext subcontext) {

        Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
        Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
        String fromSelect = new platform.server.data.query.Query<KeyExpr,Expr>(getInner().getInnerKeys().toMap(),
                BaseUtils.toMap(BaseUtils.mergeSet(group.keySet(), query.getExprs())), getInner().getFullWhere())
            .compile(source.syntax, subcontext).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, source.params, null);
        for(Map.Entry<Expr, BaseExpr> groupEntry : group.entrySet())
            whereSelect.add(fromPropertySelect.get(groupEntry.getKey())+"="+groupEntry.getValue().getSource(source));

        return "(" + source.syntax.getSelect(fromSelect, query.getSource(fromPropertySelect, source.syntax, getType()),
                BaseUtils.toString(whereSelect, " AND "), "", "", "", "") + ")";
    }

    @IdentityLazy
    public GroupJoin getInnerJoin() {
        StatKeys<Expr> statKeys;
        if(query.type.hasAdd()) {
            statKeys = new StatKeys<Expr>(group.keySet());
            for(GroupStatWhere<Expr> join : getSplitJoins(query.getWhere(), query.type, BaseUtils.reverse(group), true))
                statKeys = statKeys.or(join.stats);
        } else
            statKeys = query.getWhere().getStatExprs(new QuickSet<Expr>(group.keySet()));
        return new GroupJoin(getInner().getInnerKeyTypes(), getInner().getInnerValues(),
                query.type.nullsNotAllowed() ? query.getWhere() :
                (query.type.hasAdd() && query.type.splitExprCases()?BaseUtils.single(query.exprs).getBaseWhere():Where.TRUE),
                statKeys, group);
    }


    private Collection<ClassExprWhere> packNoChange = new ArrayList<ClassExprWhere>();
    private Map<ClassExprWhere, Expr> packClassExprs = new HashMap<ClassExprWhere, Expr>();

    @ManualLazy
    @Override
    public Expr packFollowFalse(Where falseWhere) {
        // с рекурсией, помогает бывает, даже иногда в null
        Expr packInner = packInnerFollowFalse(falseWhere);
        if(packInner.getComplexity(false) < getComplexity(false)) // если изменился !BaseUtils.hashEquals(packInner,this)
            return packInner.followFalse(falseWhere, true);
        else
            return this;
    }

    // без рекурсии
    @TwinManualLazy
    public Expr packInnerFollowFalse(Where falseWhere) {
        Map<Expr, Expr> packGroup = packFollowFalse(group, falseWhere);

        Where outerWhere = falseWhere.not();
        Map<BaseExpr, BaseExpr> outerExprValues = outerWhere.getExprValues();
        if(!BaseUtils.hashEquals(packGroup,group) || !Collections.disjoint(group.values(), outerExprValues.keySet())) // если простой пак
            return createOuterGroupCases(packGroup, query, outerExprValues, true);

        return followFalse(outerWhere, Where.TRUE, query, group, this, true, null);
    }

    private static Expr followFalse(Where outerWhere, Where innerWhere, Query query, Map<Expr, BaseExpr> innerOuter, GroupExpr thisExpr, boolean pack, Query splitQuery) {
        ClassExprWhere packClasses = ClassExprWhere.mapBack(outerWhere, innerOuter);

        if(thisExpr!=null) {
            for(ClassExprWhere packed : thisExpr.packNoChange)
                if(packed.means(packClasses)) // если более общим пакуем
                    return thisExpr;

            Expr packResult = thisExpr.packClassExprs.get(packClasses);
            if(packResult!=null)
                return packResult;
        }

        Where packWhere = packClasses.getPackWhere().and(innerWhere);
        Query packQuery = query.followFalse(packWhere.not(), pack).and(getKeepWhere(thisExpr!=null? thisExpr.getInner().getFullWhere() :getFullWhere(innerOuter, query))); // сначала pack'аем expr
        Map<BaseExpr, Expr> packOuterInner = new HashMap<BaseExpr, Expr>();
        for(Map.Entry<Expr,BaseExpr> entry : innerOuter.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            packOuterInner.put(entry.getValue(), entry.getKey().followFalse(packQuery.getWhere().and(packWhere.or(entry.getKey().getWhere().not())).not(), pack));

        if(BaseUtils.hashEquals(packQuery,query) && BaseUtils.hashEquals(BaseUtils.reverse(innerOuter),packOuterInner)) { // если изменилось погнали по кругу, или же один раз
            if(thisExpr!=null) {
                Iterator<ClassExprWhere> i = thisExpr.packNoChange.iterator();
                while(i.hasNext())
                    if(packClasses.means(i.next()))
                        i.remove();
                thisExpr.packNoChange.add(packClasses);

                return thisExpr;
            } else
                return createHandleKeys(innerOuter, packQuery);
        } else {
            if(splitQuery!=null && packQuery.getComplexity(false) >= splitQuery.getComplexity(false)) //BaseUtils.hashEquals(splitQuery, packQuery)
                return null;

            Expr result = createInner(packOuterInner, packQuery, pack);
            if(thisExpr!=null)
                thisExpr.packClassExprs.put(packClasses, result);
            return result;
        }
    }

    public static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr, GroupType type, Map<K, ? extends Expr> outer, PullExpr noPull) {
        return create(inner, new Query(expr, type), outer, noPull);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Where where, Map<K,? extends Expr> implement) {
        return create(group, ValueExpr.get(where), GroupType.ANY, implement, null);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,GroupType type,Map<K,? extends Expr> implement) {
        return create(group,expr,where,type,implement,null);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,GroupType type,Map<K,? extends Expr> implement, PullExpr noPull) {
        return create(group,expr.and(where),type,implement,noPull);
    }

    private static <K> Expr create(Map<K, ? extends Expr> inner, Query query, Map<K, ? extends Expr> outer, PullExpr noPull) {
        Map<Object, Expr> pullInner = new HashMap<Object, Expr>(inner);
        Map<Object, Expr> pullOuter = new HashMap<Object, Expr>(outer);
        for(KeyExpr key : getOuterKeys(inner.values()).merge(query.getOuterKeys()))
            if(key instanceof PullExpr && !inner.containsValue(key) && !key.equals(noPull)) {
                Object pullObject = new Object();
                pullInner.put(pullObject,key);
                pullOuter.put(pullObject,key);
            }

        return createTypeAdjust(pullInner, query, pullOuter);
    }

    public static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr, GroupType type, Map<K, ? extends Expr> outer) {
        return createTypeAdjust(inner, new Query(expr, type), outer);
    }

    public static <K> Expr create(Map<K, ? extends Expr> group, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type, Map<K, ? extends Expr> implement) {
        return createTypeAdjust(group, new Query(exprs, orders, ordersNotNull, type), implement);
    }

    // вытаскивает из outer Case'ы
    public static <K> Expr createTypeAdjust(Map<K, ? extends Expr> group, Query query, Map<K, ? extends Expr> implement) {
        assert group.keySet().equals(implement.keySet());

        if(query.type.isSelect() && !query.type.isSelectNotInWhere() && query.getType(getWhere(group)) instanceof LogicalClass)
            query = new Query(query.exprs, query.orders, query.ordersNotNull, GroupType.ANY);
        return createOuterCases(group, query, implement);
    }

    public static <K> Expr createOuterCases(final Map<K, ? extends Expr> group, final Query query, Map<K, ? extends Expr> implement) {
        return new ExprPullWheres<K>() {
            protected Expr proceedBase(Map<K, BaseExpr> map) {
                return createOuterBase(group, query, map);
            }
        }.proceed(implement);
    }

    // если translateQuery или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(Map<Expr, ? extends Expr> innerOuter, final Query query, final Map<BaseExpr, BaseExpr> outerExprValues, final boolean pack) {
        return new ExprPullWheres<Expr>() {
            protected Expr proceedBase(Map<Expr, BaseExpr> map) {
                return createOuterGroupBase(map, query, outerExprValues, pack);
            }
        }.proceed(innerOuter);
    }

    // если translateOuter или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(Map<Expr, BaseExpr> innerOuter, Query query, Map<BaseExpr, BaseExpr> outerExprValues, boolean pack) {
        ReversedMap<BaseExpr, Expr> outerInner = new ReversedHashMap<BaseExpr, Expr>();
        List<Pair<Expr, Expr>> equals = groupMap(innerOuter, outerExprValues, outerInner);
        query = query.and(getEqualsWhere(equals));
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        if(query.type.hasAdd()) {
            if(query.type.splitInnerCases()) { // можно использовать
                KeyEqual keyEqual = new KeyEqual();
                for(Pair<Expr, Expr> equal : equals)
                    keyEqual = keyEqual.and(KeyEqual.getKeyEqual((BaseExpr)equal.first, (BaseExpr)equal.second));
                if(!keyEqual.isEmpty()) { // translateOuter'им и погнали
                    QueryTranslator equalTranslator = keyEqual.getTranslator();
                    return createInner(equalTranslator.translate(outerInner), query.translateQuery(equalTranslator), pack);
                }
            } else
                if(equals.size() > 0) // могут появиться еще keyEquals, InnerJoin'ы и т.п.
                   return createInnerSplit(outerInner, query, pack);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createFollowExpr(outerInner.reverse(), query, Where.TRUE, pack, null);
    }

    public static <A extends Expr, B extends Expr> Where getEqualsWhere(List<Pair<A,B>> equals) {
        Where where = Where.TRUE;
        for(Pair<A, B> equal : equals)
            where = where.and(equal.first.compare(equal.second, Compare.EQUALS));
        return where;
    }

    // для использования в нижних 2-х методах, ищет EQUALS'ы, EXPRVALUES, VALUES, из Collection<BaseExpr,T> делает Map<BaseExpr,T> без values (в том числе с учетом доп. where)
    private static <A extends Expr, B extends Expr> List<Pair<Expr, A>> groupMap(Iterable<Pair<B, A>> group, Map<BaseExpr, BaseExpr> exprValues, Map<B, A> grouped) {

        List<Pair<Expr,A>> equals = new ArrayList<Pair<Expr,A>>();
        for(Pair<B, A> outerExpr : group) {
            A reversedExpr = grouped.get(outerExpr.first); // ищем EQUALS'ы в outer
            if(reversedExpr==null) {
                Expr exprValue;
                if(outerExpr.first.isValue()) // ищем VALUE
                    exprValue = outerExpr.first;
                else
                    exprValue = exprValues.get(outerExpr.first); // ищем EXPRVALUE
                if(exprValue!=null)
                    equals.add(new Pair<Expr,A>(exprValue, outerExpr.second));
                else
                    grouped.put(outerExpr.first, outerExpr.second);
            } else
                equals.add(new Pair<Expr,A>(reversedExpr,outerExpr.second));
        }
        return equals;
    }

    private static <K, T extends Expr> Where groupMap(final Map<K, T> inner, final Map<K, BaseExpr> outer, Map<BaseExpr, T> outerInner) {
        return getEqualsWhere(groupMap(new Iterable<Pair<BaseExpr, T>>() {
                    public Iterator<Pair<BaseExpr, T>> iterator() {
                        return new Iterator<Pair<BaseExpr, T>>() {
                            Iterator<K> keyIterator = inner.keySet().iterator();

                            public boolean hasNext() {
                                return keyIterator.hasNext();
                            }

                            public Pair<BaseExpr, T> next() {
                                K key = keyIterator.next();
                                return new Pair<BaseExpr, T>(outer.get(key), inner.get(key));
                            }

                            public void remove() {
                                throw new RuntimeException("not supported");
                            }
                        };
                    }
                }, new HashMap<BaseExpr, BaseExpr>(), outerInner));
    }

    public static <A extends Expr, B extends Expr> List<Pair<Expr, A>> groupMap(final Map<A, B> map, Map<BaseExpr, BaseExpr> exprValues, ReversedMap<B, A> reversed) {
        Iterable<Pair<B, A>> iterable = new Iterable<Pair<B, A>>() {
            public Iterator<Pair<B, A>> iterator() {
                return new Iterator<Pair<B, A>>() {

                    Iterator<Map.Entry<A,B>> entryIterator = map.entrySet().iterator();

                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    public Pair<B, A> next() {
                        Map.Entry<A, B> entry = entryIterator.next();
                        return new Pair<B, A>(entry.getValue(), entry.getKey());
                    }

                    public void remove() {
                        throw new RuntimeException("not supported");
                    }
                };
            }
        };
        return groupMap(iterable, exprValues, reversed);
    }

    private static <K, I extends Expr> Expr createOuterBase(Map<K, I> inner, Query query, Map<K, BaseExpr> outer) {
        Map<BaseExpr, I> outerInner = new HashMap<BaseExpr, I>();
        query = query.and(groupMap(inner, outer, outerInner));
        return createInner(outerInner, query, false);
    }

    private static Expr createInner(Map<BaseExpr, ? extends Expr> outerInner, Query query, boolean pack) {
        return createInnerCases(outerInner, query, pack);
    }

    private static Expr createInnerCases(Map<BaseExpr, ? extends Expr> outerInner, final Query query, final boolean pack) {
        if(query.type.hasAdd() && query.type.splitInnerCases()) {
            return new ExclPullWheres<Expr, BaseExpr, Query>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Query data, Map<BaseExpr, BaseExpr> map) {
                    return createInnerExprCases(BaseUtils.<Map<BaseExpr, Expr>>immutableCast(map), data, pack);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(query, outerInner);
        } else
            return createInnerExprCases((Map<BaseExpr,Expr>)outerInner, query, pack);
    }

    private static Expr createInnerExprCases(final Map<BaseExpr, Expr> outerInner, final Query query, final boolean pack) {
        if (query.type.hasAdd() && query.type.splitExprCases()) { // тут по идее можно assert'ить что query - simple, и обрабатывать как и было
            assert query.orders.isEmpty();
            return new ExclExprPullWheres<Expr>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Where data, BaseExpr baseExpr) {
                    return createInnerSplit(outerInner, new Query(baseExpr.and(data), query.type), pack);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(Where.TRUE, BaseUtils.single(query.exprs));
        } else {
            return createInnerSplit(outerInner, query, pack);
        }
    }

    private static Where getFullWhere(Map<Expr, BaseExpr> innerOuter, Query query) {
        return query.getWhere().and(getGroupWhere(innerOuter));
    }

    protected static QuickSet<KeyExpr> getKeys(Query query, Map<Expr, BaseExpr> group) {
        return getOuterKeys(group.keySet()).merge(query.getOuterKeys());
    }

    private static <K> Where getFullWhere(Query query, Map<K, ? extends Expr> mapInner) {
        return query.getWhere().and(getWhere(mapInner.values()));
    }

    // "определяет" разбивать на innerJoins или нет
    private static Collection<GroupStatWhere<Expr>> getSplitJoins(final Where where, GroupType type, Map<BaseExpr, Expr> outerInner, boolean noWhere) {
        assert type.hasAdd();
        return where.getStatJoins(!noWhere && type.exclusive(), new QuickSet<Expr>(outerInner.values()),
                (type.splitInnerJoins()?GroupStatType.NONE:(Settings.instance.isSplitGroupStatInnerJoins()?GroupStatType.STAT:GroupStatType.ALL)), noWhere);
    }

    private static Expr createInnerSplit(Map<BaseExpr, Expr> outerInner, Query query, boolean pack) {

        if(query.type.hasAdd()) {
            Expr result = CaseExpr.NULL;
            Collection<GroupStatWhere<Expr>> splitJoins = getSplitJoins(query.getWhere(), query.type, outerInner, false);
            for(GroupStatWhere<Expr> innerWhere : splitJoins) {
                Expr innerResult;
                if(!innerWhere.keyEqual.isEmpty()) { // translatе'им expr
                    QueryTranslator equalTranslator = innerWhere.keyEqual.getTranslator();
                    innerResult = createInner(equalTranslator.translate(outerInner), query.translateQuery(equalTranslator).and(innerWhere.where), pack);
                } else {
                    if(splitJoins.size() > 1) { // разрыв рекурсии + оптимизация
                        innerResult = createInnerBase(outerInner, query.and(innerWhere.where), pack, query);
                        if(innerResult==null) // нафиг такой split
                            return createInnerBase(outerInner, query, pack, null);
                    } else
                        return createInnerBase(outerInner, query, pack, null);
                }

                // берем keyEquals
                result = query.type.add(result, innerResult);
            }
            return result;
        } else
            return createInnerBase(outerInner, query, pack, null);
    }

    private static Expr createInnerBase(Map<BaseExpr, Expr> outerInner, Query query, boolean pack, Query splitQuery) {
        Where fullWhere = getFullWhere(query, outerInner);

        ReversedMap<Expr, BaseExpr> innerOuter = new ReversedHashMap<Expr, BaseExpr>();
        Where equalsWhere = getEqualsWhere(groupMap(outerInner, fullWhere.getExprValues(), innerOuter));

        // вытащим not'ы
        Where notWhere = Where.TRUE;
        for(Map.Entry<BaseExpr,BaseExpr> exprValue : fullWhere.getNotExprValues().entrySet()) {
            BaseExpr notValue = innerOuter.get(exprValue.getKey());
            if(notValue!=null)
                notWhere = notWhere.and(EqualsWhere.create(notValue,exprValue.getValue()).not());
        }

        Expr followExpr = createFollowExpr(innerOuter, query, notWhere, pack, splitQuery);
        if(followExpr==null) // брейк рекурсии + оптимизация
            return null;

        return followExpr.and(equalsWhere.and(notWhere));
    }

    private static Expr createFollowExpr(ReversedMap<Expr, BaseExpr> innerOuter, Query query, Where notWhere, boolean pack, Query splitQuery) {
        // именно так потому как нужно обеспечить инвариант что в ClassWhere должны быть все следствия
        return followFalse(Where.TRUE, notWhere, query, innerOuter, null, pack, splitQuery);
    }

    private static Where getKeepWhere(Where fullWhere) {

        Where keepWhere = Where.TRUE;
        for(KeyExpr key : fullWhere.getOuterKeys())
            keepWhere = keepWhere.and(fullWhere.getKeepWhere(key));
        return keepWhere;
    }

    private static Expr createHandleKeys(Map<Expr, BaseExpr> innerOuter, Query query) {

        // NOGROUP - проверяем если по всем ключам группируется, значит это никакая не группировка
        Map<Expr, BaseExpr> compares = new HashMap<Expr, BaseExpr>();
        QuickSet<KeyExpr> keys = getKeys(query, innerOuter);
        Map<KeyExpr, BaseExpr> groupKeys = BaseUtils.splitKeys(innerOuter, keys, compares);
        if(groupKeys.size()==keys.size) {
            QueryTranslator translator = new QueryTranslator(groupKeys);
            Where equalsWhere = Where.TRUE; // чтобы лишних проталкиваний не было
            for(Map.Entry<Expr,BaseExpr> compare : compares.entrySet()) // оставшиеся
                equalsWhere = equalsWhere.and(compare.getKey().translateQuery(translator).compare(compare.getValue(), Compare.EQUALS));
            return query.translateQuery(translator).and(equalsWhere).getSingleExpr();
        }

        // FREEKEYS - отрезаем свободные ключи (которые есть только в группировке) и создаем выражение
        Where freeWhere = Where.TRUE;
        Map<KeyExpr, BaseExpr> freeKeys = new HashMap<KeyExpr, BaseExpr>();
        Map<KeyExpr, BaseExpr> usedKeys = BaseUtils.splitKeys(groupKeys, getKeys(query, compares), freeKeys);
        Map<Expr, BaseExpr> group = innerOuter;
        if(freeKeys.size()>0) {
            for(Map.Entry<KeyExpr,BaseExpr> freeKey : freeKeys.entrySet())
                freeWhere = freeWhere.and(freeKey.getValue().getWhere());
            group = BaseUtils.merge(usedKeys,compares);
        }

        // CREATEBASE - создаем с createBase
        return BaseExpr.create(new GroupExpr(query, group)).and(freeWhere);
    }
}

