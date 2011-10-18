package platform.server.data.expr.query;

import platform.base.*;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.pull.*;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.AndContext;
import platform.server.data.query.CompileSource;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.*;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.MapStatKeys;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

public class GroupExpr extends QueryExpr<Expr,GroupExpr.Query,GroupJoin> {

    public static class Query extends AbstractOuterContext<Query> implements AndContext<Query> {
        public List<Expr> exprs;
        public OrderedMap<Expr, Boolean> orders;
        public GroupType type;

        public Query(Expr expr, GroupType type) {
            this(Collections.singletonList(expr), new OrderedMap<Expr, Boolean>(), type);
            assert type.hasAdd();
        }

        public Query(List<Expr> exprs, OrderedMap<Expr, Boolean> orders, GroupType type) {
            this.exprs = exprs;
            this.orders = orders;
            this.type = type;
        }

        @ParamLazy
        public Query translateOuter(MapTranslate translator) {
            return new Query(translator.translate(exprs), translator.translate(orders), type);
        }

        public Query translateQuery(QueryTranslator translator) {
            return new Query(translator.translate(exprs), translator.translate(orders), type);
        }

        public Type getType(Where groupWhere) {
            return type.getType(getMainExpr().getType(getWhere().and(groupWhere)));
        }

        public Expr getSingleExpr() {
            return type.getSingleExpr(exprs, orders);
        }

        public Expr getMainExpr() {
            return exprs.iterator().next();
        }

        public Query followFalse(Where falseWhere) {
            return new Query(falseWhere.followFalse(exprs), falseWhere.followFalse(orders), type);
        }

        public boolean twins(TwinImmutableInterface o) {
            return exprs.equals(((Query) o).exprs) && orders.equals(((Query) o).orders) && type.equals(((Query) o).type);
        }

        public int hashOuter(HashContext hashContext) {
            return (hashOuter(exprs, hashContext) * 31 + hashOuter(orders, hashContext)) * 31 + type.hashCode();
        }

        @IdentityLazy
        public Where getWhere() {
            return Expr.getWhere(exprs).and(Expr.getWhere(orders.keySet()));
        }

        public String toString() {
            return "GROUP(" + exprs + "," + orders + "," + type + ")";
        }

        public Query and(Where where) {
            List<Expr> andExprs = new ArrayList<Expr>();
            Iterator<Expr> it = exprs.iterator();
            andExprs.add(it.next().and(where));
            while(it.hasNext())
                andExprs.add(it.next());
            return new Query(andExprs, orders, type);
        }

        public SourceJoin[] getEnum() { // !!! Включим ValueExpr.TRUE потому как в OrderSelect.getSource - при проталкивании partition'а может создать TRUE
            Set<Expr> result = getExprs();
            return result.toArray(new Expr[result.size()]);
        }

        public Set<Expr> getExprs() {
            Set<Expr> result = new HashSet<Expr>();
            result.addAll(exprs);
            result.addAll(orders.keySet());
            return result;
        }

        public String getSource(Map<Expr, String> fromPropertySelect, SQLSyntax syntax) {
            return type.getSource(BaseUtils.mapList(exprs, fromPropertySelect), BaseUtils.mapOrder(orders, fromPropertySelect), syntax);
        }
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
    }

    protected GroupExpr(Query query, Map<Expr, BaseExpr> group) {
        super(query, group);
    }

    protected GroupExpr createThis(Query query, Map<Expr, BaseExpr> group) {
        return new GroupExpr(query, group);
    }

    public InnerExpr translateOuter(MapTranslate translator) {
        return new GroupExpr(this, translator);
    }

    private static Where getGroupWhere(Map<Expr, BaseExpr> innerOuter) {
        return getWhere(innerOuter.keySet());
    }
    @IdentityLazy
    public Where getGroupWhere() {
        return getGroupWhere(group);
    }

    @IdentityLazy
    public Where getFullWhere() {
        return query.getWhere().and(getGroupWhere());
    }

    @IdentityLazy
    public Type getType() {
        return query.getType(getGroupWhere());
    }

    public Type getType(KeyType keyType) {
        return getType();
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    public class NotNull extends InnerExpr.NotNull {

        @Override
        public Where packFollowFalse(Where falseWhere) {
            return GroupExpr.this.packFollowFalse(falseWhere).getWhere();
        }

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create

            ClassExprWhere result;
            if(query.type.isSelect())
                result = ClassExprWhere.map(fullWhere, BaseUtils.merge(Collections.singletonMap(query.getMainExpr(), GroupExpr.this), group));
            else
                result = ClassExprWhere.map(fullWhere,group).and(new ClassExprWhere(GroupExpr.this,(DataClass) getType()));
            return result.and(getWhere(group).getClassWhere());
        }
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

    public static <K> Expr create(Map<K,? extends Expr> group, Where where, Map<K,? extends Expr> implement) {
        return create(group, ValueExpr.TRUE, where, GroupType.ANY, implement);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,GroupType type,Map<K,? extends Expr> implement) {
        return create(group,expr,where,type,implement,null);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,GroupType type,Map<K,? extends Expr> implement, PullExpr noPull) {
        return create(group,expr.and(where),type,implement,noPull);
    }

    private Collection<ClassExprWhere> packNoChange = new ArrayList<ClassExprWhere>();
    private Map<ClassExprWhere, Expr> packClassExprs = new HashMap<ClassExprWhere, Expr>();

    @ManualLazy
    @Override
    public Expr packFollowFalse(Where falseWhere) {
        // с рекурсией, помогает бывает, даже иногда в null
        Expr packInner = packInnerFollowFalse(falseWhere);
        if(packInner.getComplexity() < getComplexity()) // если изменился !BaseUtils.hashEquals(packInner,this)
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
            return createOuterGroupCases(packGroup, query, outerExprValues);

        return packFollowFalse(outerWhere, Where.TRUE, query, group, this);
    }

    private static Expr packFollowFalse(Where outerWhere, Where innerWhere, Query query, Map<Expr, BaseExpr> innerOuter, GroupExpr thisExpr) {
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
        Query packQuery = query.followFalse(packWhere.not()).and(getKeepWhere(thisExpr!=null?thisExpr.getFullWhere():getFullWhere(innerOuter, query))); // сначала pack'аем expr
        Map<BaseExpr, Expr> packOuterInner = new HashMap<BaseExpr, Expr>();
        for(Map.Entry<Expr,BaseExpr> entry : innerOuter.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            packOuterInner.put(entry.getValue(), entry.getKey().followFalse(packQuery.getWhere().and(packWhere.or(entry.getKey().getWhere().not())).not(), true));

        if((thisExpr==null || BaseUtils.hashEquals(packQuery,query)) && BaseUtils.hashEquals(BaseUtils.reverse(innerOuter),packOuterInner)) { // если изменилось погнали по кругу, или же один раз
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
            Expr result = createInner(packOuterInner, packQuery);
            if(thisExpr!=null)
                thisExpr.packClassExprs.put(packClasses, result);
            return result;
        }
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return createOuterGroupCases(translator.translate(group), query, new HashMap<BaseExpr, BaseExpr>());
    }

    public static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr, GroupType type, Map<K, ? extends Expr> outer, PullExpr noPull) {
        return create(inner, new Query(expr, type), outer, noPull);
    }

    private static <K> Expr create(Map<K, ? extends Expr> inner, Query query, Map<K, ? extends Expr> outer, PullExpr noPull) {
        Map<Object, Expr> pullInner = new HashMap<Object, Expr>(inner);
        Map<Object, Expr> pullOuter = new HashMap<Object, Expr>(outer);
        for(KeyExpr key : enumKeys(inner.values(), query.getEnum()))
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

    public static <K> Expr create(Map<K, ? extends Expr> group, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, GroupType type, Map<K, ? extends Expr> implement) {
        return createTypeAdjust(group, new Query(exprs, orders, type), implement);
    }

    // вытаскивает из outer Case'ы
    public static <K> Expr createTypeAdjust(Map<K, ? extends Expr> group, Query query, Map<K, ? extends Expr> implement) {
        if(query.type.isSelect() && query.getType(getWhere(group)) instanceof LogicalClass)
            query = new Query(query.exprs, query.orders, GroupType.ANY);
        return createOuterCases(group, query, implement);
    }

    public static <K> Expr createOuterCases(final Map<K, ? extends Expr> group, final Query query, Map<K, ? extends Expr> implement) {
        return new ExprPullWheres<K>() {
            protected Expr proceedBase(Map<K, BaseExpr> map) {
                return createOuterBase(group, query, map);
            }
        }.proceed(implement);
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(Map<Expr, ? extends Expr> innerOuter, final Query query, final Map<BaseExpr,BaseExpr> outerExprValues) {
        return new ExprPullWheres<Expr>() {
            protected Expr proceedBase(Map<Expr, BaseExpr> map) {
                return createOuterGroupBase(map, query, outerExprValues);
            }
        }.proceed(innerOuter);
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(Map<Expr, BaseExpr> innerOuter, Query query, Map<BaseExpr,BaseExpr> outerExprValues) {
        ReversedMap<BaseExpr, Expr> outerInner = new ReversedHashMap<BaseExpr, Expr>();
        List<Pair<Expr, Expr>> equals = groupMap(innerOuter, outerExprValues, outerInner);
        query = query.and(getEqualsWhere(equals));
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        if(query.type.hasAdd()) {
            if(query.type.splitInnerCases()) { // можно использовать
                KeyEqual keyEqual = new KeyEqual();
                for(Pair<Expr, Expr> equal : equals)
                    keyEqual = keyEqual.and(KeyEqual.getKeyEqual((BaseExpr)equal.first, (BaseExpr)equal.second));
                if(!keyEqual.isEmpty()) { // translate'им и погнали
                    QueryTranslator equalTranslator = keyEqual.getTranslator();
                    return createInner(equalTranslator.translate(outerInner), query.translateQuery(equalTranslator));
                }
            } else
                if(equals.size() > 0) // могут появиться еще keyEquals, InnerJoin'ы и т.п.
                   return createInnerSplit(outerInner, query);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createFollowExpr(outerInner.reverse(), query, Where.TRUE);
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
        return createInner(outerInner, query);
    }

    private static Expr createInner(Map<BaseExpr,? extends Expr> outerInner, Query query) {
        return createInnerCases(outerInner, query);
    }

    private static Expr createInnerCases(Map<BaseExpr, ? extends Expr> outerInner, final Query query) {
        if(query.type.hasAdd() && query.type.splitInnerCases()) {
            return new ExclPullWheres<Expr, BaseExpr, Query>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Query data, Map<BaseExpr, BaseExpr> map) {
                    return createInnerExprCases(BaseUtils.<Map<BaseExpr, BaseExpr>, Map<BaseExpr, Expr>>immutableCast(map), data);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(query, outerInner);
        } else
            return createInnerExprCases((Map<BaseExpr,Expr>)outerInner, query);
    }

    private static Expr createInnerExprCases(final Map<BaseExpr, Expr> outerInner, final Query query) {
        if (query.type.hasAdd() && query.type.splitExprCases()) { // тут по идее можно assert'ить что query - simple, и обрабатывать как и было
            assert query.orders.isEmpty();
            return new ExclExprPullWheres<Expr>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Where data, BaseExpr baseExpr) {
                    return createInnerSplit(outerInner, new Query(baseExpr.and(data), query.type));
                }
                protected Expr add(Expr op1, Expr op2) {
                    return query.type.add(op1, op2);
                }
            }.proceed(Where.TRUE, BaseUtils.single(query.exprs));
        } else {
            return createInnerSplit(outerInner, query);
        }
    }

    private static Where getFullWhere(Map<Expr, BaseExpr> innerOuter, Query query) {
        return query.getWhere().and(getGroupWhere(innerOuter));
    }

    protected static Set<KeyExpr> getKeys(Query query, Map<Expr, BaseExpr> group) {
        return enumKeys(group.keySet(), query.getEnum());
    }

    private static <K> Where getFullWhere(Query query, Map<K, ? extends Expr> mapInner) {
        return query.getWhere().and(getWhere(mapInner.values()));
    }

    // "определяет" разбивать на innerJoins или нет
    private static Collection<GroupStatWhere<Expr>> getSplitJoins(final Query query, Map<BaseExpr, Expr> outerInner) {
        assert query.type.hasAdd();
        Collection<GroupStatWhere<Expr>> statJoins = query.getWhere().getStatJoins(query.type.noExclusive(), new HashSet<Expr>(outerInner.values()));

        if(query.type.splitInnerJoins()) // не группируем
            return statJoins;

        if (Settings.instance.isSplitGroupStatInnerJoins()) { // группируем по KeyEqual + статистике, or'им Where
            Collection<GroupStatWhere<Expr>> result = new ArrayList<GroupStatWhere<Expr>>();
            MapWhere<Pair<KeyEqual, StatKeys<Expr>>> mapWhere = new MapWhere<Pair<KeyEqual, StatKeys<Expr>>>();
            for(GroupStatWhere<Expr> statJoin : statJoins)
                mapWhere.add(new Pair<KeyEqual, StatKeys<Expr>>(statJoin.keyEqual,
                        statJoin.stats), statJoin.where);
            for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
                Pair<KeyEqual, StatKeys<Expr>> map = mapWhere.getKey(i);
                result.add(new GroupStatWhere<Expr>(map.first, map.second, mapWhere.getValue(i)));
            }
            return result;
        }

        // группируем по keyEqual, or'им StatKeys и Where
        Collection<GroupStatWhere<Expr>> result = new ArrayList<GroupStatWhere<Expr>>();
        MapWhere<KeyEqual> mapWhere = new MapWhere<KeyEqual>(); MapStatKeys<KeyEqual, Expr> mapStats = new MapStatKeys<KeyEqual, Expr>();
        for(GroupStatWhere<Expr> statJoin : statJoins) {
            mapWhere.add(statJoin.keyEqual, statJoin.where); mapStats.add(statJoin.keyEqual, statJoin.stats);
        }
        for(int i=0;i<mapWhere.size;i++) { // возвращаем результат
            KeyEqual keys = mapWhere.getKey(i);
            result.add(new GroupStatWhere<Expr>(keys, mapStats.get(keys), mapWhere.getValue(i)));
        }
        return result;
    }

    private static Expr createInnerSplit(Map<BaseExpr, Expr> outerInner, Query query) {

        if(query.type.hasAdd()) {
            Expr result = CaseExpr.NULL;
            for(GroupStatWhere innerWhere : getSplitJoins(query, outerInner)) {
                Expr innerResult;
                if(!innerWhere.keyEqual.isEmpty()) { // translatе'им expr
                    QueryTranslator equalTranslator = innerWhere.keyEqual.getTranslator();
                    innerResult = createInner(equalTranslator.translate(outerInner), query.translateQuery(equalTranslator));
                } else
                    innerResult = createInnerBase(outerInner, query.and(innerWhere.where));

                // берем keyEquals
                result = query.type.add(result, innerResult);
            }
            return result;
        } else
            return createInnerBase(outerInner, query);
    }

    private static Expr createInnerBase(Map<BaseExpr, Expr> outerInner, Query query) {
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

        return createFollowExpr(innerOuter, query, notWhere).and(equalsWhere.and(notWhere));
    }

    private static Expr createFollowExpr(ReversedMap<Expr, BaseExpr> innerOuter, Query query, Where notWhere) {
        // именно так потому как нужно обеспечить инвариант что в ClassWhere должны быть все следствия
        return packFollowFalse(Where.TRUE, notWhere, query, innerOuter, null);
    }

    private static Where getKeepWhere(Where fullWhere) {

        Where keepWhere = Where.TRUE;
        for(KeyExpr key : enumKeys(fullWhere))
            keepWhere = keepWhere.and(fullWhere.getKeepWhere(key));
        return keepWhere;
    }

    private static Expr createHandleKeys(Map<Expr, BaseExpr> innerOuter, Query query) {

        // NOGROUP - проверяем если по всем ключам группируется, значит это никакая не группировка
        Map<Expr, BaseExpr> compares = new HashMap<Expr, BaseExpr>();
        Set<KeyExpr> keys = getKeys(query, innerOuter);
        Map<KeyExpr, BaseExpr> groupKeys = BaseUtils.splitKeys(innerOuter, keys, compares);
        if(groupKeys.size()==keys.size()) {
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

    public String getExprSource(CompileSource source, String prefix) {

        Set<Expr> queryExprs = BaseUtils.mergeSet(group.keySet(), query.getExprs()); // так как может одновременно и SUM и MAX нужен

        Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
        Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
        String fromSelect = new platform.server.data.query.Query<KeyExpr,Expr>(BaseUtils.toMap(getKeys()),BaseUtils.toMap(queryExprs), Expr.getWhere(queryExprs))
            .compile(source.syntax, prefix).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, source.params);
        for(Map.Entry<Expr, BaseExpr> groupEntry : group.entrySet())
            whereSelect.add(fromPropertySelect.get(groupEntry.getKey())+"="+groupEntry.getValue().getSource(source));

        return "(" + source.syntax.getSelect(fromSelect, query.getSource(fromPropertySelect, source.syntax),
                BaseUtils.toString(whereSelect, " AND "), "", "", "") + ")";
    }

    private Map<KeyExpr, Type> getInnerKeyTypes() {
        KeyType contextType = getFullWhere();
        Map<KeyExpr, Type> keyStats = new HashMap<KeyExpr, Type>();
        for(KeyExpr key : innerContext.getKeys())
            keyStats.put(key, contextType.getKeyType(key));
        return keyStats;
    }

    @IdentityLazy
    public GroupJoin getInnerJoin() {
        StatKeys<Expr> statKeys;
        if(query.type.hasAdd()) {
            GroupStatWhere<Expr> innerWhere = BaseUtils.single(getSplitJoins(query, BaseUtils.reverse(group)));
            assert innerWhere.keyEqual.isEmpty();
            statKeys = innerWhere.stats;
        } else
            statKeys = query.getWhere().getStatExprs(group.keySet());
        return new GroupJoin(getInnerKeyTypes(), innerContext.getValues(), query.type.hasAdd() && query.type.splitExprCases()?BaseUtils.single(query.exprs).getBaseWhere():Where.TRUE,
                statKeys, group);
    }

    @IdentityLazy
    public Stat getStatValue(KeyStat keyStat) {
        if(query.type.isSelect()) {
            // assert что expr учавствует в where
            return new StatPullWheres().proceed(getFullWhere(), query.getMainExpr());
        }
        return null;
    }

    public Stat getTypeStat(KeyStat keyStat) {
        return query.getMainExpr().getTypeStat(getFullWhere());
    }
}

