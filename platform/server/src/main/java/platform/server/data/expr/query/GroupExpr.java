package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.Result;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.SFunctionSet;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.*;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.expr.where.pull.ExclExprPullWheres;
import platform.server.data.expr.where.pull.ExclPullWheres;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.AndContext;
import platform.server.data.query.CompileSource;
import platform.server.data.query.CompiledQuery;
import platform.server.data.query.SubQueryContext;
import platform.server.data.query.innerjoins.GroupStatType;
import platform.server.data.query.innerjoins.GroupStatWhere;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Collection;
import java.util.Iterator;

public class GroupExpr extends AggrExpr<Expr,GroupType,GroupExpr.Query,GroupJoin,GroupExpr,GroupExpr.QueryInnerContext> {

    @Override
    protected boolean hasDuplicateOuter() {
        return false;
    }
    
    public ImRevMap<Expr, BaseExpr> getRevGroup() {
        return group.toRevExclMap();
    }

    public static class Query extends AggrExpr.Query<GroupType, Query> implements AndContext<Query> {

        public Query(Expr expr, GroupType type) {
            this(ListFact.singleton(expr), MapFact.<Expr, Boolean>EMPTYORDER(), false, type);
            assert type.hasAdd();
        }

        public Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type) {
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

        public Query and(final Where where) { // вот тут надо быть аккуратнее, предполагается что первое выражение попадет в getWhere, см. AggrType.getWhere
            return new Query(exprs.mapListValues(new GetIndexValue<Expr, Expr>() {
                public Expr getMapValue(int i, Expr value) {
                    if(i==0)
                        value = value.and(where);
                    return value;
                }
            }), orders, ordersNotNull, type);
        }

        public String getSource(ImMap<Expr, String> fromPropertySelect, SQLSyntax syntax, Type resultType) {
            Result<ImSet<Expr>> orderExprsNotNull = new Result<ImSet<Expr>>();
            ImOrderMap<Expr, Boolean> packOrders = CompiledQuery.getOrdersNotNull(orders, orders.keys().toMap(), orderExprsNotNull);
            if(ordersNotNull) // если notNull, то все пометим
                orderExprsNotNull.set(packOrders.keys());
            return type.getSource(exprs.map(fromPropertySelect), packOrders.map(fromPropertySelect), orderExprsNotNull.result.map(fromPropertySelect).toSet(), resultType, syntax);
        }
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
    }

    public StatKeys<KeyExpr> getStatGroup() {
        Where where = getInner().getFullWhere();

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
        ImSet<KeyExpr> pushedKeys = BaseUtils.immutableCast(group.keys().filterFn(new SFunctionSet<Expr>() {
            public boolean contains(Expr element) {
                return element instanceof KeyExpr;
            }
        }));
        return where.getStatKeys(getInner().getInnerKeys().remove(pushedKeys));
    }
    private boolean checkNoKeys() {
        return getStatGroup().rows.lessEquals(new Stat(Long.MAX_VALUE));
    }
    protected GroupExpr(Query query, ImMap<Expr, BaseExpr> group) {
        super(query, group);
//        assert checkNoKeys();
    }

    protected GroupExpr createThis(Query query, ImMap<Expr, BaseExpr> group) {
        return new GroupExpr(query, group);
    }

    protected InnerExpr translate(MapTranslate translator) {
        return new GroupExpr(this, translator);
    }

    private static Where getGroupWhere(ImMap<Expr, BaseExpr> innerOuter) {
        return getWhere(innerOuter.keys());
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

        protected ImMap<KeyExpr, Type> getInnerKeyTypes() {
            final KeyType contextType = getFullWhere();
            return getInnerKeys().mapValues(new GetValue<Type, KeyExpr>() {
                public Type getMapValue(KeyExpr value) {
                    return contextType.getKeyType(value);
                }
            });
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
        return createOuterGroupCases(translator.translate(group), query, MapFact.<BaseExpr, BaseExpr>EMPTY(), false);
    }

    public String getExprSource(final CompileSource source, SubQueryContext subcontext) {

        final Result<ImMap<Expr,String>> fromPropertySelect = new Result<ImMap<Expr, String>>();
        Result<ImCol<String>> fromWhereSelect = new Result<ImCol<String>>(); // проверить crossJoin
        String fromSelect = new platform.server.data.query.Query<KeyExpr,Expr>(getInner().getInnerKeys().toRevMap(),
                group.keys().addExcl(query.getExprs()).toMap(), getInner().getFullWhere())
            .compile(source.syntax, subcontext).fillSelect(new Result<ImMap<KeyExpr, String>>(), fromPropertySelect, fromWhereSelect, source.params, null);
        
        ImCol<String> whereSelect = fromWhereSelect.result.mergeCol(group.mapColValues(new GetKeyValue<String, Expr, BaseExpr>() {
            public String getMapValue(Expr key, BaseExpr value) {
                return fromPropertySelect.result.get(key)+"="+value.getSource(source);
            }}));

        return "(" + source.syntax.getSelect(fromSelect, query.getSource(fromPropertySelect.result, source.syntax, getType()),
                whereSelect.toString(" AND "), "", "", "", "") + ")";
    }

    @IdentityInstanceLazy
    public GroupJoin getInnerJoin() {
        StatKeys<Expr> statKeys;
        if(query.type.hasAdd()) {
            statKeys = new StatKeys<Expr>(group.keys());
            for(GroupStatWhere<Expr> join : getSplitJoins(query.getWhere(), query.type, getRevGroup().reverse(), true))
                statKeys = statKeys.or(join.stats);
        } else
            statKeys = query.getWhere().getStatExprs(group.keys());
        return new GroupJoin(getInner().getInnerKeyTypes(), getInner().getInnerValues(),
                query.type.nullsNotAllowed() ? query.getWhere() :
                (query.type.hasAdd() && query.type.splitExprCases()? query.exprs.single().getBaseWhere():Where.TRUE),
                statKeys, group);
    }


    private Collection<ClassExprWhere> packNoChange = ListFact.mAddRemoveCol(); // потому как remove нужен
    private MCacheMap<ClassExprWhere, Expr> packClassExprs = LRUCache.mSmall(LRUCache.EXP_RARE);

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
        ImMap<Expr, Expr> packGroup = packFollowFalse(group, falseWhere);

        ImRevMap<Expr, BaseExpr> revGroup = getRevGroup();

        Where outerWhere = falseWhere.not();
        ImMap<BaseExpr, BaseExpr> outerExprValues = outerWhere.getExprValues();
        if(!BaseUtils.hashEquals(packGroup,group) || !revGroup.valuesSet().disjoint(outerExprValues.keys())) // если простой пак
            return createOuterGroupCases(packGroup, query, outerExprValues, true);

        synchronized (this) {
            return followFalse(outerWhere, Where.TRUE, query, revGroup, this, true, null);
        }
    }

    private static Expr followFalse(Where outerWhere, Where innerWhere, Query query, ImRevMap<Expr, BaseExpr> innerOuter, GroupExpr thisExpr, final boolean pack, Query splitQuery) {
        ImRevMap<BaseExpr, Expr> outerInner = innerOuter.reverse();
        ClassExprWhere packClasses = ClassExprWhere.mapBack(outerWhere, outerInner);

        if(thisExpr!=null) {
            for(ClassExprWhere packed : thisExpr.packNoChange)
                if(packed.means(packClasses)) // если более общим пакуем
                    return thisExpr;

            Expr packResult = thisExpr.packClassExprs.get(packClasses);
            if(packResult!=null)
                return packResult;
        }

        final Where packWhere = packClasses.getPackWhere().and(innerWhere);
        final Query packQuery = query.followFalse(packWhere.not(), pack).and(getKeepWhere(thisExpr != null ? thisExpr.getInner().getFullWhere() : getFullWhere(innerOuter, query))); // сначала pack'аем expr
        ImMap<BaseExpr, Expr> packOuterInner = outerInner.mapValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) { // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
                return value.followFalse(andExprCheck(packQuery.getWhere(), orExprCheck(packWhere, value.getWhere().not())).not(), pack);
            }
        });

        if(BaseUtils.hashEquals(packQuery,query) && BaseUtils.hashEquals(outerInner,packOuterInner)) { // если изменилось погнали по кругу, или же один раз
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
                thisExpr.packClassExprs.exclAdd(packClasses, result);
            return result;
        }
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> inner, Expr expr, GroupType type, ImMap<K, ? extends Expr> outer, PullExpr noPull) {
        return create(inner, new Query(expr, type), outer, noPull);
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Where where, ImMap<K,? extends Expr> implement) {
        return create(group, ValueExpr.get(where), GroupType.ANY, implement, null);
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Expr expr,Where where,GroupType type,ImMap<K,? extends Expr> implement) {
        return create(group,expr,where,type,implement,null);
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Expr expr,Where where,GroupType type,ImMap<K,? extends Expr> implement, PullExpr noPull) {
        return create(group,expr.and(where),type,implement,noPull);
    }

    private static <K> Expr create(final ImMap<K, ? extends Expr> inner, Query query, ImMap<K, ? extends Expr> outer, final PullExpr noPull) {
        ImMap<Object, KeyExpr> pullKeys = getOuterKeys(inner.values()).merge(query.getOuterKeys()).filterFn(new SFunctionSet<KeyExpr>() {
            public boolean contains(KeyExpr key) {
                return key instanceof PullExpr && !((ImMap<K,Expr>)inner).containsValue(key) && !key.equals(noPull);
            }}).mapRevKeys(new GetStaticValue<Object>() {
            public Object getMapValue() {
                return new Object();
            }});
        return createTypeAdjust(MapFact.addExcl(inner, pullKeys), query, MapFact.addExcl(outer, pullKeys));
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> inner, Expr expr, GroupType type, ImMap<K, ? extends Expr> outer) {
        return createTypeAdjust(inner, new Query(expr, type), outer);
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> group, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type, ImMap<K, ? extends Expr> implement) {
        return createTypeAdjust(group, new Query(exprs, orders, ordersNotNull, type), implement);
    }

    // вытаскивает из outer Case'ы
    public static <K> Expr createTypeAdjust(ImMap<K, ? extends Expr> group, Query query, ImMap<K, ? extends Expr> implement) {
        assert group.keys().equals(implement.keys());

        if(query.type.isSelect() && !query.type.isSelectNotInWhere() && query.getType(getWhere(group)) instanceof LogicalClass)
            query = new Query(query.exprs, query.orders, query.ordersNotNull, GroupType.ANY);
        return createOuterCases(group, query, implement);
    }

    public static <K> Expr createOuterCases(final ImMap<K, ? extends Expr> group, final Query query, ImMap<K, ? extends Expr> implement) {
        return new ExprPullWheres<K>() {
            protected Expr proceedBase(ImMap<K, BaseExpr> map) {
                return createOuterBase(group, query, map);
            }
        }.proceed(implement);
    }

    // если translateQuery или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(ImMap<Expr, ? extends Expr> innerOuter, final Query query, final ImMap<BaseExpr, BaseExpr> outerExprValues, final boolean pack) {
        return new ExprPullWheres<Expr>() {
            protected Expr proceedBase(ImMap<Expr, BaseExpr> map) {
                return createOuterGroupBase(map, query, outerExprValues, pack);
            }
        }.proceed(innerOuter);
    }

    // если translateOuter или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(ImMap<Expr, BaseExpr> innerOuter, Query query, ImMap<BaseExpr, BaseExpr> outerExprValues, boolean pack) {
        Result<ImRevMap<BaseExpr, Expr>> outerInner = new Result<ImRevMap<BaseExpr, Expr>>();
        ImList<Pair<Expr, Expr>> equals = groupMap(innerOuter, outerExprValues, outerInner);
        query = query.and(getEqualsWhere(equals));
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        if(query.type.hasAdd()) {
            if(query.type.splitInnerCases()) { // можно использовать
                KeyEqual keyEqual = KeyEqual.EMPTY;
                for(Pair<Expr, Expr> equal : equals)
                    keyEqual = keyEqual.and(KeyEqual.getKeyEqual((BaseExpr)equal.first, (BaseExpr)equal.second));
                if(!keyEqual.isEmpty()) { // translateOuter'им и погнали
                    QueryTranslator equalTranslator = keyEqual.getTranslator();
                    return createInner(equalTranslator.translate(outerInner.result), query.translateQuery(equalTranslator), pack);
                }
            } else
                if(equals.size() > 0) // могут появиться еще keyEquals, InnerJoin'ы и т.п.
                   return createInnerSplit(outerInner.result, query, pack);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createFollowExpr(outerInner.result.reverse(), query, Where.TRUE, pack, null);
    }

    public static <A extends Expr, B extends Expr> Where getEqualsWhere(ImList<Pair<A,B>> equals) {
        Where where = Where.TRUE;
        for(Pair<A, B> equal : equals)
            where = where.and(equal.first.compare(equal.second, Compare.EQUALS));
        return where;
    }

    // для использования в нижних 2-х методах, ищет EQUALS'ы, EXPRVALUES, VALUES, из Collection<BaseExpr,T> делает Map<BaseExpr,T> без values (в том числе с учетом доп. where)
    private static <A extends Expr, B extends Expr> ImList<Pair<Expr, A>> groupMap(Iterable<Pair<B, A>> group, ImMap<BaseExpr, BaseExpr> exprValues, Result<ImMap<B, A>> grouped) {

        MList<Pair<Expr,A>> mEquals = ListFact.mList(); // вообще size есть но спрятан в Iterable
        MExclMap<B,A> mGrouped = MapFact.mExclMap();// вообще size есть но спрятан в Iterable
        for(Pair<B, A> outerExpr : group) {
            A reversedExpr = mGrouped.get(outerExpr.first); // ищем EQUALS'ы в outer
            if(reversedExpr==null) {
                Expr exprValue;
                if(outerExpr.first.isValue()) // ищем VALUE
                    exprValue = outerExpr.first;
                else
                    exprValue = exprValues.getObject(outerExpr.first); // ищем EXPRVALUE
                if(exprValue!=null)
                    mEquals.add(new Pair<Expr,A>(exprValue, outerExpr.second));
                else
                    mGrouped.exclAdd(outerExpr.first, outerExpr.second);
            } else
                mEquals.add(new Pair<Expr,A>(reversedExpr,outerExpr.second));
        }
        grouped.set(mGrouped.immutable());
        return mEquals.immutableList();
    }

    private static <K, T extends Expr> Where groupMapValues(final ImMap<K, T> inner, final ImMap<K, BaseExpr> outer, Result<ImMap<BaseExpr, T>> outerInner) {
        return getEqualsWhere(groupMap(new Iterable<Pair<BaseExpr, T>>() {
                    public Iterator<Pair<BaseExpr, T>> iterator() {
                        return new Iterator<Pair<BaseExpr, T>>() {
                            int i=0;

                            public boolean hasNext() {
                                return i < inner.size();
                            }

                            public Pair<BaseExpr, T> next() {
                                return new Pair<BaseExpr, T>(outer.get(inner.getKey(i)), inner.getValue(i++));
                            }

                            public void remove() {
                                throw new RuntimeException("not supported");
                            }
                        };
                    }
                }, MapFact.<BaseExpr, BaseExpr>EMPTY(), outerInner));
    }

    public static <A extends Expr, B extends Expr> ImList<Pair<Expr, A>> groupMap(final ImMap<A, B> map, ImMap<BaseExpr, BaseExpr> exprValues, Result<ImRevMap<B, A>> reversed) {
        Iterable<Pair<B, A>> iterable = new Iterable<Pair<B, A>>() {
            public Iterator<Pair<B, A>> iterator() {
                return new Iterator<Pair<B, A>>() {

                    int index = 0;

                    public boolean hasNext() {
                        return index < map.size();
                    }

                    public Pair<B, A> next() {
                        return new Pair<B, A>(map.getValue(index), map.getKey(index++));
                    }

                    public void remove() {
                        throw new RuntimeException("not supported");
                    }
                };
            }
        };
        Result<ImMap<B, A>> castReserved = new Result<ImMap<B, A>>();
        ImList<Pair<Expr, A>> result = groupMap(iterable, exprValues, castReserved);
        reversed.set(castReserved.result.toRevExclMap()); // так, иначе пришлось бы сильно с классами мудится
        return result;
    }

    private static <K, I extends Expr> Expr createOuterBase(ImMap<K, I> inner, Query query, ImMap<K, BaseExpr> outer) {
        Result<ImMap<BaseExpr, I>> outerInner = new Result<ImMap<BaseExpr, I>>();
        query = query.and(groupMapValues(inner, outer, outerInner));
        return createInner(outerInner.result, query, false);
    }

    private static Expr createInner(ImMap<BaseExpr, ? extends Expr> outerInner, Query query, boolean pack) {
        return createInnerCases(outerInner, query, pack);
    }

    private static Expr createInnerCases(ImMap<BaseExpr, ? extends Expr> outerInner, final Query query, final boolean pack) {
        if(query.type.hasAdd() && query.type.splitInnerCases()) {
            return new ExclPullWheres<Expr, BaseExpr, Query>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Query data, ImMap<BaseExpr, BaseExpr> map) {
                    return createInnerExprCases(BaseUtils.<ImMap<BaseExpr, Expr>>immutableCast(map), data, pack);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(query, outerInner);
        } else
            return createInnerExprCases((ImMap<BaseExpr,Expr>)outerInner, query, pack);
    }

    private static Expr createInnerExprCases(final ImMap<BaseExpr, Expr> outerInner, final Query query, final boolean pack) {
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
            }.proceed(Where.TRUE, query.exprs.single());
        } else {
            return createInnerSplit(outerInner, query, pack);
        }
    }

    private static Where getFullWhere(ImMap<Expr, BaseExpr> innerOuter, Query query) {
        return query.getWhere().and(getGroupWhere(innerOuter));
    }

    protected static ImSet<KeyExpr> getKeys(Query query, ImMap<Expr, BaseExpr> group) {
        return getOuterKeys(group.keys()).merge(query.getOuterKeys());
    }

    private static <K> Where getFullWhere(Query query, ImMap<K, ? extends Expr> mapInner) {
        return query.getWhere().and(getWhere(mapInner.values()));
    }

    // "определяет" разбивать на innerJoins или нет
    private static ImCol<GroupStatWhere<Expr>> getSplitJoins(final Where where, GroupType type, ImMap<BaseExpr, Expr> outerInner, boolean noWhere) {
        assert type.hasAdd();
        return where.getStatJoins(!noWhere && type.exclusive(), outerInner.values().toSet(),
                (type.splitInnerJoins()?GroupStatType.NONE:(Settings.get().isSplitGroupStatInnerJoins()?GroupStatType.STAT:GroupStatType.ALL)), noWhere);
    }

    private static Expr createInnerSplit(ImMap<BaseExpr, Expr> outerInner, Query query, boolean pack) {

        if(query.type.hasAdd()) {
            Expr result = CaseExpr.NULL;
            ImCol<GroupStatWhere<Expr>> splitJoins = getSplitJoins(query.getWhere(), query.type, outerInner, false);
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

    private static Expr createInnerBase(ImMap<BaseExpr, Expr> outerInner, Query query, boolean pack, Query splitQuery) {
        Where fullWhere = getFullWhere(query, outerInner);

        Result<ImRevMap<Expr, BaseExpr>> innerOuter = new Result<ImRevMap<Expr, BaseExpr>>();
        Where equalsWhere = getEqualsWhere(groupMap(outerInner, fullWhere.getExprValues(), innerOuter));

        // вытащим not'ы
        Where notWhere = Where.TRUE;
        ImMap<BaseExpr, BaseExpr> notExprValues = fullWhere.getNotExprValues();
        for(int i=0,size=notExprValues.size();i<size;i++) {
            BaseExpr notValue = innerOuter.result.get(notExprValues.getKey(i));
            if(notValue!=null)
                notWhere = notWhere.and(EqualsWhere.create(notValue,notExprValues.getValue(i)).not());
        }

        Expr followExpr = createFollowExpr(innerOuter.result, query, notWhere, pack, splitQuery);
        if(followExpr==null) // брейк рекурсии + оптимизация
            return null;

        return followExpr.and(equalsWhere.and(notWhere));
    }

    private static Expr createFollowExpr(ImRevMap<Expr, BaseExpr> innerOuter, Query query, Where notWhere, boolean pack, Query splitQuery) {
        // именно так потому как нужно обеспечить инвариант что в ClassWhere должны быть все следствия
        return followFalse(Where.TRUE, notWhere, query, innerOuter, null, pack, splitQuery);
    }

    private static Where getKeepWhere(Where fullWhere) {

        Where keepWhere = Where.TRUE;
        for(KeyExpr key : fullWhere.getOuterKeys())
            keepWhere = keepWhere.and(fullWhere.getKeepWhere(key));
        return keepWhere;
    }

    private static Expr createHandleKeys(ImRevMap<Expr, BaseExpr> innerOuter, Query query) {

        // NOGROUP - проверяем если по всем ключам группируется, значит это никакая не группировка
        Result<ImRevMap<Expr, BaseExpr>> compares = new Result<ImRevMap<Expr, BaseExpr>>();
        ImSet<KeyExpr> keys = getKeys(query, innerOuter);
        ImRevMap<KeyExpr, BaseExpr> groupKeys = MapFact.splitRevKeys(innerOuter, keys, compares);
        if(groupKeys.size()==keys.size()) {
            QueryTranslator translator = new QueryTranslator(groupKeys);
            Where equalsWhere = Where.TRUE; // чтобы лишних проталкиваний не было
            for(int i=0,size=compares.result.size();i<size;i++) // оставшиеся
                equalsWhere = equalsWhere.and(compares.result.getKey(i).translateQuery(translator).compare(compares.result.getValue(i), Compare.EQUALS));
            return query.translateQuery(translator).and(equalsWhere).getSingleExpr();
        }

        // FREEKEYS - отрезаем свободные ключи (которые есть только в группировке) и создаем выражение
        Where freeWhere = Where.TRUE;
        Result<ImRevMap<KeyExpr, BaseExpr>> freeKeys = new Result<ImRevMap<KeyExpr, BaseExpr>>();
        ImRevMap<KeyExpr, BaseExpr> usedKeys = groupKeys.splitRevKeys(getKeys(query, compares.result), freeKeys);
        ImRevMap<Expr, BaseExpr> group = innerOuter;
        if(freeKeys.result.size()>0) {
            for(BaseExpr freeKey : freeKeys.result.valueIt())
                freeWhere = freeWhere.and(freeKey.getWhere());
            group = MapFact.addRevExcl(usedKeys, compares.result);
        }

        // CREATEBASE - создаем с createBase
        return BaseExpr.create(new GroupExpr(query, group)).and(freeWhere);
    }
}

