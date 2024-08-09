package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSVSMap;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.*;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.query.GroupExprWhereJoins;
import lsfusion.server.data.expr.join.query.GroupJoin;
import lsfusion.server.data.expr.join.where.GroupStatType;
import lsfusion.server.data.expr.join.where.KeyEqual;
import lsfusion.server.data.expr.join.where.KeyEquals;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.classes.data.EqualsWhere;
import lsfusion.server.data.expr.where.pull.AndContext;
import lsfusion.server.data.expr.where.pull.ExclExprPullWheres;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.*;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.KeyExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.where.OrWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;

import java.util.Collection;
import java.util.Iterator;

public class GroupExpr extends AggrExpr<Expr,GroupType,GroupExpr.Query, GroupJoin,GroupExpr,GroupExpr.QueryInnerContext> {

    @Override
    protected boolean hasDuplicateOuter() {
        return false;
    }
    
    public ImRevMap<Expr, BaseExpr> getRevGroup() {
        return group.toRevExclMap();
    }

    public static class Query extends AggrExpr.Query<GroupType, Query> implements AndContext<Query> {

        public Query(Expr expr, GroupType type) {
            this(expr, type, false);
        }
        
        public Query(Expr expr, GroupType type, boolean noInnerFollows) {
            this(ListFact.singleton(expr), MapFact.EMPTYORDER(), false, type, noInnerFollows);
            assert type.hasAdd();
        }

        public Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type, boolean noInnerFollows) {
            super(exprs, orders, ordersNotNull, type, noInnerFollows);
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public Query translateExpr(ExprTranslator translator) {
            return new Query(translator.translate(exprs), translator.translate(orders), ordersNotNull, type, noInnerFollows);
        }

        public Type getGroupType(Where groupWhere) {
            return getType(getWhere().and(groupWhere));
        }

        public Expr getSingleExpr() {
            return type.getSingleExpr(exprs).and(getOrderWhere());
        }

        public Query followFalse(Where falseWhere, boolean pack) {
            return new Query(type.followFalse(falseWhere, exprs, pack), falseWhere.followFalse(orders, pack), ordersNotNull, type, noInnerFollows);
        }

        public String toString() {
            return "GROUP(" + exprs + "," + orders + "," + type + ")";
        }

        public Query and(final Where where) { // there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
            return new Query(exprs.mapListValues((i, value) -> {
                if(i==0)
                    value = value.and(where);
                return value;
            }), orders, ordersNotNull, type, noInnerFollows);
        }

        public String getSource(ImMap<Expr, String> fromPropertySelect, ImMap<Expr, ClassReader> propReaders, lsfusion.server.data.query.Query<KeyExpr, Expr> query, SQLSyntax syntax, TypeEnvironment typeEnv, Type resultType) {
            ImOrderMap<Expr, CompileOrder> compileOrders = query.getCompileOrders(orders);
            if(ordersNotNull) // если notNull, то все пометим
                compileOrders = CompileOrder.setNotNull(compileOrders);
            return type.getSource(exprs.mapList(fromPropertySelect), exprs.mapList(propReaders), compileOrders.map(fromPropertySelect), resultType, syntax, typeEnv);
        }

        public boolean isLastOpt(boolean needValue) {
            return type.isLastOpt(needValue, exprs, orders);
        }
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
    }

    public StatKeys<KeyExpr> getStatGroup() {
        Where where = getInner().getFullWhere();

/*            final QuickSet<KeyExpr> keepKeys = new QuickSet<KeyExpr>();
            groupExpr.enumerate(new ContextEnumerator() {
                public boolean enumerate(OuterContext join) {
                    if(join instanceof QueryExpr) {
                        keepKeys.addAll(join.getOuterKeys());
                        return false;
                    }
                    return true;
                }
            });
            pushedKeys.addAll(groupExpr.getOuterKeys().remove(keepKeys));*/
        ImSet<KeyExpr> pushedKeys = BaseUtils.immutableCast(group.keys().filterFn(element -> element instanceof KeyExpr));
        return where.getStatKeys(getInner().getQueryKeys().remove(pushedKeys), StatType.DEFAULT);
    }
    private boolean checkNoKeys() {
        return getStatGroup().getRows().lessEquals(new Stat(Long.MAX_VALUE));
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
        protected Where getFullWhere() {
            return thisObj.query.getWhere().and(getGroupWhere());
        }

        protected ImMap<KeyExpr, Type> getInnerKeyTypes() { // не делаем IdentityLazy так как только при конструировании InnerJoin используется
            final KeyType contextType = getFullWhere();
            return getQueryKeys().mapValues(contextType::getKeyType).removeNulls(); // могут быть nulls когда контекст сверху приходит
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

    public String getSource(CompileSource compile, boolean needValue) {
        return compile.getSource(this, needValue);
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
        return "G"+tobt+(SystemProperties.inDevMode ? "" : "-"+getInnerJoin());
    }

    @ParamLazy
    public Expr translate(ExprTranslator translator) {
        return createOuterGroupCases(translator.translate(group), query, MapFact.EMPTY(), false);
    }

    @IdentityInstanceLazy
    public GroupJoin getInnerJoin() {
        final Where queryWhere = query.getWhere();
        Settings settings = Settings.get();

        GroupExprWhereJoins<Expr> groupWhereJoins = queryWhere.getGroupExprWhereJoins(group.keys(), StatType.GROUP_SPLIT, settings.isGroupStatExprWhereJoins());

        ImSet<KeyExpr> innerKeys = getInner().getQueryKeys();

        Where groupWhere;
        if(query.type.nullsNotAllowed()) {
            groupWhere = queryWhere;
        } else {
            // не нужно, так как и так WhereJoins включаются
//            int groupJoinLevel = Settings.get().getGroupJoinLevel() - group.size();
//            if(groupJoinLevel >= 0) { // оптимизация
//                MSet<WhereJoin> mGroupJoins = SetFact.mSet();
//                for(GroupJoinsWhere queryJoin : queryWhere.getWhereJoins(false, innerKeys, StatType.GROUP_SPLIT, SetFact.<Expr>EMPTYORDER()).first)
//                    mGroupJoins.addAll(queryJoin.getLevelJoins(groupJoinLevel));
//                groupJoins = new WhereJoins(mGroupJoins.immutable());
//            } else
//                groupJoins = WhereJoins.EMPTY;
            groupWhere = (query.type.hasAdd() && query.type.splitExprCases() ? query.exprs.single().getBaseWhere() : Where.TRUE()).
                    and(query.getOrderWhere()); // тут особенность в том что в SQL даже если order null, он (в отличии от expr) может повлиять на результат, поэтому важно чтобы условия совпадали (конечно есть небольшой вопрос с and'ом getBaseWhere(), но эта ветка все равно пока не используется)
        }

        return new GroupJoin(innerKeys, getInner().getInnerValues(), getInner().getInnerKeyTypes(),
                getInner().getInnerFollows(), groupWhere, groupWhereJoins, group);
    }


    private Collection<ClassExprWhere> packNoChange = ListFact.mAddRemoveCol(); // потому как remove нужен
    private static final LRUWSVSMap<GroupExpr, ClassExprWhere, Expr> packClassExprs = new LRUWSVSMap<>(LRUUtil.L2);

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
        ImMap<Expr, Expr> packGroup = packPushFollowFalse(group, falseWhere);

        ImRevMap<Expr, BaseExpr> revGroup = getRevGroup();

        Where outerWhere = falseWhere.not();
        ImMap<BaseExpr, BaseExpr> outerExprValues = outerWhere.getExprValues();
        if(!BaseUtils.hashEquals(packGroup,group) || !revGroup.valuesSet().disjoint(outerExprValues.keys())) // если простой пак
            return createOuterGroupCases(packGroup, query, outerExprValues, true);

        synchronized (this) {
            return followFalse(outerWhere, Where.TRUE(), query, revGroup, this, true, null);
        }
    }

    private static Expr followFalse(Where outerWhere, Where innerWhere, Query query, ImRevMap<Expr, BaseExpr> innerOuter, GroupExpr thisExpr, final boolean pack, Query splitQuery) {
        ImRevMap<BaseExpr, Expr> outerInner = innerOuter.reverse();
        ClassExprWhere packClasses = ClassExprWhere.mapBack(outerWhere, outerInner);

        if(thisExpr!=null) {
            for(ClassExprWhere packed : thisExpr.packNoChange)
                if(packed.means(packClasses, OrWhere.implicitCast)) // если более общим пакуем
                    return thisExpr;

            Expr packResult = packClassExprs.get(thisExpr, packClasses);
            if(packResult!=null)
                return packResult;
        }

        final Where packWhere = packClasses.getPackWhere().and(innerWhere);

        // for keepWhere - need to avoid packing classes info
        Where groupWhere = thisExpr != null ? thisExpr.getInner().getGroupWhere() : getGroupWhere(innerOuter);
        Where fullWhere = thisExpr != null ? thisExpr.getInner().getFullWhere() : getFullWhere(groupWhere, query);
        Where keepWhere = getKeepWhere(fullWhere, query.noInnerFollows).followFalse(groupWhere.not(), pack);

        final Query packQuery = query.followFalse(packWhere.not(), pack).and(keepWhere); // сначала pack'аем expr
        ImMap<BaseExpr, Expr> packOuterInner = outerInner.mapValues(value -> { // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            return value.followFalse(andExprCheck(packQuery.getWhere(), orExprCheck(packWhere, value.getWhere().not())).not(), pack);
        });

        if(BaseUtils.hashEquals(packQuery,query) && BaseUtils.hashEquals(outerInner,packOuterInner)) { // если изменилось погнали по кругу, или же один раз
            if(thisExpr!=null) {
                Iterator<ClassExprWhere> i = thisExpr.packNoChange.iterator();
                while(i.hasNext())
                    if(packClasses.means(i.next(), OrWhere.implicitCast))
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
                packClassExprs.put(thisExpr, packClasses, result);
            return result;
        }
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> inner, Expr expr, GroupType type, ImMap<K, ? extends Expr> outer, PullExpr noPull) {
        return create(inner, new Query(expr, type), outer, noPull);
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> group, Where where, ImMap<K, ? extends Expr> implement, boolean top) {
        return create(group, top ? ValueExpr.COUNT.and(where) : ValueExpr.get(where), top ? GroupType.SUM : GroupType.LOGICAL(), implement, null); // boolean
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Where where, ImMap<K,? extends Expr> implement) {
        return create(group, ValueExpr.get(where), GroupType.LOGICAL(), implement, null); // boolean
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Expr expr,Where where,GroupType type,ImMap<K,? extends Expr> implement) {
        return create(group,expr,where,type,implement,null);
    }

    public static <K> Expr create(ImMap<K,? extends Expr> group, Expr expr,Where where,GroupType type,ImMap<K,? extends Expr> implement, PullExpr noPull) {
        return create(group,expr.and(where),type,implement,noPull);
    }

    private static <K> Expr create(final ImMap<K, ? extends Expr> inner, Query query, ImMap<K, ? extends Expr> outer, final PullExpr noPull) {
        ImMap<Object, ParamExpr> pullKeys = BaseUtils.<ImSet<ParamExpr>>immutableCast(getOuterColKeys(inner.values()).merge(query.getOuterKeys()))
                .filterFn(key -> key instanceof PullExpr && !((ImMap<K,Expr>)inner).containsValue(key) && !key.equals(noPull))
                .mapRevKeys(Object::new);
        return createTypeAdjust(MapFact.addExcl(inner, pullKeys), query, MapFact.addExcl(outer, pullKeys));
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> inner, Expr expr, GroupType type, ImMap<K, ? extends Expr> outer) {
        return create(inner, expr, type, outer, false);
    }
    
    public static <K> Expr create(ImMap<K, ? extends Expr> inner, Expr expr, GroupType type, ImMap<K, ? extends Expr> outer, boolean noInnerFollows) {
        return create(inner, new Query(expr, type, noInnerFollows), outer, null);
    }

    public static <K> Expr create(ImMap<K, ? extends Expr> group, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, GroupType type, ImMap<K, ? extends Expr> implement, boolean noInnerFollows) {
        return create(group, new Query(exprs, orders, ordersNotNull, type, noInnerFollows), implement, null);
    }
    
    private static <K> Type getType(ImMap<K, ? extends Expr> group, Query query) {
        return query.getGroupType(getWhere(group));
    }

    // вытаскивает из outer Case'ы
    public static <K> Expr createTypeAdjust(ImMap<K, ? extends Expr> group, Query query, ImMap<K, ? extends Expr> implement) {
        assert group.keys().equals(implement.keys());

        if(query.type.isSelect() && !query.type.isSelectNotInWhere() && getType(group, query) instanceof LogicalClass)
            query = new Query(query.exprs, query.orders, query.ordersNotNull, GroupType.LOGICAL(), query.noInnerFollows); // boolean
        return createOuterCases(group, query, implement);
    }

    public static <K> Expr createOuterCases(final ImMap<K, ? extends Expr> group, final Query query, ImMap<K, ? extends Expr> implement) {
        return new ExprPullWheres<K>() {
            protected Expr proceedBase(ImMap<K, BaseExpr> map) {
                return createOuterBase(group, query, map);
            }
        }.proceed(implement);
    }

    // если translateExpr или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(ImMap<Expr, ? extends Expr> innerOuter, final Query query, final ImMap<BaseExpr, BaseExpr> outerExprValues, final boolean pack) {
        return new ExprPullWheres<Expr>() {
            protected Expr proceedBase(ImMap<Expr, BaseExpr> map) {
                return createOuterGroupBase(map, query, outerExprValues, pack);
            }
        }.proceed(innerOuter);
    }

    // если translateOuter или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(ImMap<Expr, BaseExpr> innerOuter, Query query, ImMap<BaseExpr, BaseExpr> outerExprValues, boolean pack) {
        Result<ImRevMap<BaseExpr, Expr>> outerInner = new Result<>();
        ImList<Pair<Expr, Expr>> equals = groupMap(innerOuter, outerExprValues, outerInner);
        query = query.and(getEqualsWhere(equals));
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        if(query.type.hasAdd()) {
            if(query.type.splitInnerCases()) { // можно использовать
                KeyEqual keyEqual = KeyEqual.EMPTY;
                for(Pair<Expr, Expr> equal : equals)
                    keyEqual = keyEqual.and(KeyEqual.getKeyEqual((BaseExpr)equal.first, (BaseExpr)equal.second));
                if(!keyEqual.isEmpty()) { // translateOuter'им и погнали
                    ExprTranslator equalTranslator = keyEqual.getTranslator();
                    return createInner(equalTranslator.translate(outerInner.result), query.translateExpr(equalTranslator), pack);
                }
            } else
                if(equals.size() > 0) // могут появиться еще keyEquals, InnerJoin'ы и т.п.
                   return createInnerSplit(outerInner.result, query, pack);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createFollowExpr(outerInner.result.reverse(), query, Where.TRUE(), pack, null);
    }

    public static <A extends Expr, B extends Expr> Where getEqualsWhere(ImList<Pair<A,B>> equals) {
        Where where = Where.TRUE();
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
                    mEquals.add(new Pair<>(exprValue, outerExpr.second));
                else
                    mGrouped.exclAdd(outerExpr.first, outerExpr.second);
            } else
                mEquals.add(new Pair<>(reversedExpr, outerExpr.second));
        }
        grouped.set(mGrouped.immutable());
        return mEquals.immutableList();
    }

    private static <K, T extends Expr> Where groupMapValues(final ImMap<K, T> inner, final ImMap<K, BaseExpr> outer, Result<ImMap<BaseExpr, T>> outerInner) {
        return getEqualsWhere(groupMap(() -> new Iterator<Pair<BaseExpr, T>>() {
            int i=0;

            public boolean hasNext() {
                return i < inner.size();
            }

            public Pair<BaseExpr, T> next() {
                return new Pair<>(outer.get(inner.getKey(i)), inner.getValue(i++));
            }

            public void remove() {
                throw new RuntimeException("not supported");
            }
        }, MapFact.EMPTY(), outerInner));
    }

    // exprValues в "контексте" B - правой части map'а
    public static <A extends Expr, B extends Expr> ImList<Pair<Expr, A>> groupMap(final ImMap<A, B> map, ImMap<BaseExpr, BaseExpr> exprValues, Result<ImRevMap<B, A>> reversed) {
        Iterable<Pair<B, A>> iterable = () -> new Iterator<Pair<B, A>>() {

            int index = 0;

            public boolean hasNext() {
                return index < map.size();
            }

            public Pair<B, A> next() {
                return new Pair<>(map.getValue(index), map.getKey(index++));
            }

            public void remove() {
                throw new RuntimeException("not supported");
            }
        };
        Result<ImMap<B, A>> castReserved = new Result<>();
        ImList<Pair<Expr, A>> result = groupMap(iterable, exprValues, castReserved);
        reversed.set(castReserved.result.toRevExclMap()); // так, иначе пришлось бы сильно с классами мудится
        return result;
    }

    private static <K, I extends Expr> Expr createOuterBase(ImMap<K, I> inner, Query query, ImMap<K, BaseExpr> outer) {
        Result<ImMap<BaseExpr, I>> outerInner = new Result<>();
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
                    return Expr.NULL();
                }
                protected Expr proceedBase(Query data, ImMap<BaseExpr, BaseExpr> map) {
                    return createInnerExprCases(BaseUtils.immutableCast(map), data, pack);
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
                    return Expr.NULL();
                }
                protected Expr proceedBase(Where data, BaseExpr baseExpr) {
                    return createInnerSplit(outerInner, new Query(baseExpr.and(data), query.type), pack);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(Where.TRUE(), query.exprs.single());
        } else {
            return createInnerSplit(outerInner, query, pack);
        }
    }

    private static Where getFullWhere(Where groupWhere, Query query) {
        return query.getWhere().and(groupWhere);
    }

    protected static ImSet<KeyExpr> getKeys(Query query, ImMap<Expr, BaseExpr> group) {
        return BaseUtils.immutableCast(getOuterSetKeys(group.keys()).merge(query.getOuterKeys()));
    }

    private static <K> Where getFullWhere(Query query, ImMap<K, ? extends Expr> mapInner) {
        return query.getWhere().and(getWhere(mapInner.values()));
    }

    // "определяет" разбивать на innerJoins или нет
    private static ImCol<Pair<KeyEqual, Where>> getSplitJoins(final Where where, GroupType type, ImMap<BaseExpr, Expr> outerInner, boolean forceAll) {
        assert type.hasAdd();

        GroupStatType splitType = type.splitInnerJoins() ? GroupStatType.NONE : (Settings.get().isSplitGroupStatInnerJoins() ? GroupStatType.STAT : GroupStatType.ALL);
        if(forceAll)
            splitType = GroupStatType.ALL;
        
        boolean exclusive = type.exclusive();
        
        if(splitType.equals(GroupStatType.ALL)) { // оптимизация
            KeyEquals keyEquals = where.getKeyEquals();
            if(!exclusive || keyEquals.size() <= 1)
                return keyEquals.mapColValues(Pair::new);
        }
        return where.getSplitJoins(exclusive, outerInner.values().toSet(),
                StatType.GROUP_SPLIT, splitType).mapColSetValues(value -> new Pair<>(value.keyEqual, value.where));
    }

    private static Expr createInnerSplit(ImMap<BaseExpr, Expr> outerInner, Query query, boolean pack) {

        if(query.type.hasAdd()) {
            Expr result = Expr.NULL();
            ImCol<Pair<KeyEqual, Where>> splitJoins = getSplitJoins(query.getWhere(), query.type, outerInner, 
                                            query.type.isMaxMin() && !Settings.get().isSplitGroupStatMaxMinObjectType() && getType(outerInner, query) instanceof ObjectType);
            for(Pair<KeyEqual, Where> innerWhere : splitJoins) {
                Expr innerResult;
                if(!innerWhere.first.isEmpty()) { // translatе'им expr
                    ExprTranslator equalTranslator = innerWhere.first.getTranslator();
                    innerResult = createInner(equalTranslator.translate(outerInner), query.translateExpr(equalTranslator).and(innerWhere.second), pack);
                } else {
                    if(splitJoins.size() > 1) { // разрыв рекурсии + оптимизация
                        innerResult = createInnerBase(outerInner, query.and(innerWhere.second), pack, query);
                        if(innerResult==null) // нафиг такой split
                            return createInnerBase(outerInner, query, pack, null);
                    } else
                        return createInnerBase(outerInner, query, pack, null);
                }

                // берем keyEquals
                result = query.type.add(result, innerResult);
            }
            return result;
        } else {
            KeyEqual keyEqual = getFullWhere(query, outerInner).getKeyEquals().getSingle();
            if(!keyEqual.isEmpty()) {
                ExprTranslator equalTranslator = keyEqual.getTranslator();
                return createInner(equalTranslator.translate(outerInner), query.translateExpr(equalTranslator), pack);
            }
            return createInnerBase(outerInner, query, pack, null);
        }
    }

    private static Expr createInnerBase(ImMap<BaseExpr, Expr> outerInner, Query query, boolean pack, Query splitQuery) {
        Where fullWhere = getFullWhere(query, outerInner);

        Result<ImRevMap<Expr, BaseExpr>> innerOuter = new Result<>();
        Where equalsWhere = getEqualsWhere(groupMap(outerInner, fullWhere.getExprValues(), innerOuter));

        // вытащим not'ы
        Where notWhere = Where.TRUE();
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
        return followFalse(Where.TRUE(), notWhere, query, innerOuter, null, pack, splitQuery);
    }

    private static Where getKeepWhere(Where fullWhere, boolean noInnerFollows) {

        Where keepWhere = Where.TRUE();
        for(ParamExpr key : fullWhere.getOuterKeys())
            keepWhere = keepWhere.and(fullWhere.getKeepWhere((KeyExpr)key, noInnerFollows));
        return keepWhere;
    }

    private static Expr createHandleKeys(ImRevMap<Expr, BaseExpr> innerOuter, Query query) {

        // NOGROUP - проверяем если по всем ключам группируется, значит это никакая не группировка
        Result<ImRevMap<Expr, BaseExpr>> compares = new Result<>();
        ImSet<KeyExpr> keys = getKeys(query, innerOuter);
        ImRevMap<KeyExpr, BaseExpr> groupKeys = MapFact.splitRevKeys(innerOuter, keys, compares);
        if(groupKeys.size()==keys.size() && query.type.hasSingle()) {
            ExprTranslator translator = new KeyExprTranslator(groupKeys);
            Where equalsWhere = Where.TRUE(); // чтобы лишних проталкиваний не было
            for(int i=0,size=compares.result.size();i<size;i++) // оставшиеся
                equalsWhere = equalsWhere.and(compares.result.getKey(i).translateExpr(translator).compare(compares.result.getValue(i), Compare.EQUALS));
            return query.translateExpr(translator).and(equalsWhere).getSingleExpr();
        }

        // FREEKEYS - отрезаем свободные ключи (которые есть только в группировке) и создаем выражение
        Where freeWhere = Where.TRUE();
        Result<ImRevMap<KeyExpr, BaseExpr>> freeKeys = new Result<>();
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

