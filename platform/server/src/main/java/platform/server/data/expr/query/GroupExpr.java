package platform.server.data.expr.query;

import platform.base.*;
import platform.interop.Compare;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.pull.*;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.*;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

public class GroupExpr extends QueryExpr<BaseExpr,GroupExpr.Query,GroupJoin> {

    public static class Query extends AbstractOuterContext<Query> {
        public Expr expr;
        public GroupType groupType;

        public Query(Expr expr, GroupType groupType) {
            this.expr = expr;
            this.groupType = groupType;
        }

        @ParamLazy
        public Query translateOuter(MapTranslate translator) {
            return new Query(expr.translateOuter(translator),groupType);
        }

        public boolean twins(TwinImmutableInterface o) {
            return expr.equals(((Query) o).expr) && groupType.equals(((Query) o).groupType);
        }

        public int hashOuter(HashContext hashContext) {
            return expr.hashOuter(hashContext) * 31 + groupType.hashCode();
        }

        public Where getWhere() {
            return expr.getWhere();
        }

        public Stat getTypeStat() {
            return expr.getTypeStat(getWhere());
        }

        public Type getType() {
            return expr.getType(getWhere());
        }

        public String toString() {
            return "GROUP(" + expr + "," + groupType + ")";
        }

        public SourceJoin[] getEnum() { // !!! Включим ValueExpr.TRUE потому как в OrderSelect.getSource - при проталкивании partition'а может создать TRUE
            return new SourceJoin[]{expr};
        }
    }

    @IdentityLazy
    public Where getJoinsWhere() {
        return getWhere(group);
    }

    protected GroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr, GroupType groupType) {
        super(new Query(expr, groupType), group);

//        assert checkInfinite(false);
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
    }

    protected GroupExpr(Query query, Map<BaseExpr, BaseExpr> group) {
        super(query, group);
    }

    protected QueryExpr<BaseExpr, Query, GroupJoin> createThis(Query query, Map<BaseExpr, BaseExpr> group) {
        return new GroupExpr(query, group);
    }

    public InnerExpr translateOuter(MapTranslate translator) {
        return new GroupExpr(this, translator);
    }

    @Override
    protected boolean checkExpr() {
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet()) {
            assert !(groupExpr.getValue() instanceof ValueExpr);
            assert !(groupExpr.getKey() instanceof ValueExpr);
        }

//        for(KeyExpr key : getKeys())
//            assert !(key instanceof PullExpr) || group.containsKey(key) || expr.equals(key);

        return true;
    }

    @IdentityLazy
    public Where getFullWhere() {
        return getFullWhere(group, query.expr);
    }

    public Type getType(KeyType keyType) {
        return query.expr.getType(getFullWhere());
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    public class NotNull extends InnerExpr.NotNull {

        public int hashOuter(HashContext hashContext) {
            return GroupExpr.this.hashOuter(hashContext);
        }

        @Override
        public Where packFollowFalse(Where falseWhere) {
            return GroupExpr.this.packFollowFalse(falseWhere).getWhere();
        }

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create
            return getClassWhere(fullWhere).and(getWhere(group).getClassWhere());
        }

        protected ClassExprWhere getClassWhere(Where fullWhere) {
            switch(query.groupType) {
                case SUM:
                    return fullWhere.getClassWhere().map(group).and(new ClassExprWhere(GroupExpr.this,(IntegralClass) query.expr.getType(fullWhere)));
                case MAX:
                case ANY:
                    return new ExclExprPullWheres<ClassExprWhere>() {
                        protected ClassExprWhere initEmpty() {
                            return ClassExprWhere.FALSE;
                        }
                        protected ClassExprWhere proceedBase(Where data, BaseExpr baseExpr) {
                            return data.getClassWhere().map(BaseUtils.merge(Collections.singletonMap(baseExpr, GroupExpr.this), group));
                        }
                        protected ClassExprWhere add(ClassExprWhere op1, ClassExprWhere op2) {
                            return op1.or(op2);
                        }
                    }.proceed(fullWhere, query.expr);
            }
            throw new RuntimeException("not supported");
        }
    }

    protected GroupExpr innerTranslate(MapTranslate translate) {
        return query.groupType.createExpr(translate.translateKeys(group), query.expr.translateOuter(translate));
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

    private void addNoChange(ClassExprWhere classes) {
        Iterator<ClassExprWhere> i = packNoChange.iterator();
        while(i.hasNext())
            if(classes.means(i.next()))
                i.remove();
        packNoChange.add(classes);
    }

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
        Map<BaseExpr, Expr> packGroup = packFollowFalse(group, falseWhere);

        Where outerWhere = falseWhere.not();
        Map<BaseExpr, BaseExpr> outerExprValues = outerWhere.getExprValues();
        if(!BaseUtils.hashEquals(packGroup,group) || !Collections.disjoint(group.values(), outerExprValues.keySet())) // если простой пак
            return createOuterGroupCases(packGroup, query.expr, query.groupType, outerExprValues);

        ClassExprWhere packClasses = getJoinsWhere().and(outerWhere).getClassWhere().mapBack(group).
                    and(getWhere(group.keySet()).getClassWhere());

        for(ClassExprWhere packed : packNoChange)
            if(packed.means(packClasses)) // если более общим пакуем
                return this;

        Expr packResult = packClassExprs.get(packClasses);
        if(packResult!=null)
            return packResult;

        Where packWhere = packClasses.getPackWhere();
        Expr packExpr = query.expr.followFalse(packWhere.not(), true).and(getKeepWhere(getFullWhere())); // сначала pack'аем expr
        Map<BaseExpr, Expr> packInnerGroup = new HashMap<BaseExpr, Expr>();
        for(Map.Entry<BaseExpr,BaseExpr> entry : group.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            packInnerGroup.put(entry.getValue(), entry.getKey().packFollowFalse(packExpr.getWhere().and(packWhere.or(entry.getKey().getWhere().not())).not()));

        if(BaseUtils.hashEquals(packExpr,query.expr) && BaseUtils.hashEquals(BaseUtils.reverse(group),packInnerGroup)) { // если изменилось погнали по кругу, или же один раз
            addNoChange(packClasses);
            return this;
        } else {
            Expr result = createInner(packInnerGroup, packExpr, query.groupType);
            packClassExprs.put(packClasses, result);
            return result;
        }
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return createOuterGroupCases(translator.translate(group), query.expr, query.groupType, new HashMap<BaseExpr, BaseExpr>());
    }

    private static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr, GroupType type, Map<K, ? extends Expr> outer, PullExpr noPull) {
        Map<Object, Expr> pullInner = new HashMap<Object, Expr>(inner);
        Map<Object, Expr> pullOuter = new HashMap<Object, Expr>(outer);
        for(KeyExpr key : enumKeys(inner.values(),expr.getEnum()))
            if(key instanceof PullExpr && !inner.containsValue(key) && !key.equals(noPull)) {
                Object pullObject = new Object();
                pullInner.put(pullObject,key);
                pullOuter.put(pullObject,key);
            }

        return create(pullInner, expr, type, pullOuter);
    }

    // вытаскивает из outer Case'ы
    public static <K> Expr create(final Map<K, ? extends Expr> group, final Expr expr, final GroupType type, Map<K, ? extends Expr> implement) {
        return new ExprPullWheres<K>() {
            protected Expr proceedBase(Map<K, BaseExpr> map) {
                return createOuterBase(group, expr, type, map);
            }
        }.proceed(implement);
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(Map<BaseExpr, ? extends Expr> innerOuter, final Expr expr, final GroupType type, final Map<BaseExpr,BaseExpr> outerExprValues) {
        return new ExprPullWheres<BaseExpr>() {
            protected Expr proceedBase(Map<BaseExpr, BaseExpr> map) {
                return createOuterGroupBase(map, expr, type, outerExprValues);
            }
        }.proceed(innerOuter);
    }

    private static <T extends Expr> Where getEqualsWhere(List<Pair<T,T>> equals) {
        Where where = Where.TRUE;
        for(Pair<T, T> equal : equals)
            where = where.and(equal.first.compare(equal.second, Compare.EQUALS));
        return where;
    }

    // для использования в нижних 2-х методах, ищет EQUALS'ы, EXPRVALUES, VALUES, из Collection<BaseExpr,T> делает Map<BaseExpr,T> без values (в том числе с учетом доп. where)
    private static <T extends Expr> List<Pair<T, T>> groupMap(Iterable<Pair<BaseExpr, T>> group, Map<BaseExpr, BaseExpr> exprValues, Map<BaseExpr, T> grouped) {

        List<Pair<T,T>> equals = new ArrayList<Pair<T,T>>();
        for(Pair<BaseExpr, T> outerExpr : group) {
            T reversedExpr = grouped.get(outerExpr.first); // ищем EQUALS'ы в outer
            if(reversedExpr==null) {
                BaseExpr exprValue;
                if(outerExpr.first.isValue()) // ищем VALUE
                    exprValue = outerExpr.first;
                else
                    exprValue = exprValues.get(outerExpr.first); // ищем EXPRVALUE
                if(exprValue!=null)
                    equals.add(new Pair<T,T>((T) exprValue, outerExpr.second));
                else
                    grouped.put(outerExpr.first, outerExpr.second);
            } else
                equals.add(new Pair<T,T>(reversedExpr,outerExpr.second));
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

    private static List<Pair<BaseExpr, BaseExpr>> groupMap(final Map<BaseExpr,BaseExpr> map, Map<BaseExpr,BaseExpr> exprValues, ReversedMap<BaseExpr, BaseExpr> reversed) {
        Iterable<Pair<BaseExpr, BaseExpr>> iterable = new Iterable<Pair<BaseExpr, BaseExpr>>() {
            public Iterator<Pair<BaseExpr, BaseExpr>> iterator() {
                return new Iterator<Pair<BaseExpr, BaseExpr>>() {

                    Iterator<Map.Entry<BaseExpr, BaseExpr>> entryIterator = map.entrySet().iterator();

                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    public Pair<BaseExpr, BaseExpr> next() {
                        Map.Entry<BaseExpr, BaseExpr> entry = entryIterator.next();
                        return new Pair<BaseExpr, BaseExpr>(entry.getValue(), entry.getKey());
                    }

                    public void remove() {
                        throw new RuntimeException("not supported");
                    }
                };
            }
        };
        return groupMap(iterable, exprValues, reversed);
    }

    private static <K, I extends Expr> Expr createOuterBase(Map<K,I> inner, Expr expr,GroupType type, Map<K,BaseExpr> outer) {
        Map<BaseExpr, I> outerInner = new HashMap<BaseExpr, I>();
        expr = expr.and(groupMap(inner, outer, outerInner));
        return createInner(outerInner, expr, type);
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(Map<BaseExpr, BaseExpr> innerOuter, Expr expr, GroupType type, Map<BaseExpr,BaseExpr> outerExprValues) {
        ReversedMap<BaseExpr, BaseExpr> outerInner = new ReversedHashMap<BaseExpr, BaseExpr>();
        List<Pair<BaseExpr, BaseExpr>> equals = groupMap(innerOuter, outerExprValues, outerInner);
        expr = expr.and(getEqualsWhere(equals));
        return createExprEquals(outerInner, expr, type, equals);
    }

    private static Expr createInner(Map<BaseExpr, ? extends Expr> outerInner, Expr expr,GroupType type) {
        return createInnerCases(outerInner, expr, type);
    }

    private static Expr createInnerCases(Map<BaseExpr, ? extends Expr> outerInner, Expr expr, final GroupType type) {
        return new ExclPullWheres<Expr, BaseExpr, Expr>() {
            protected Expr initEmpty() {
                return NULL;
            }
            protected Expr proceedBase(Expr data, Map<BaseExpr, BaseExpr> map) {
                return createInnerExprCases(map, data, type);
            }
            protected Expr add(Expr op1, Expr op2) {
                return type.add(op1, op2);
            }
        }.proceed(expr, outerInner);
    }

    private static Expr createInnerExprCases(final Map<BaseExpr, BaseExpr> outerInner, Expr expr, final GroupType type) {
        if (type.splitExprCases()) {
            return new ExclExprPullWheres<Expr>() {
                protected Expr initEmpty() {
                    return NULL;
                }
                protected Expr proceedBase(Where data, BaseExpr baseExpr) {
                    return createInnerSplit(outerInner, baseExpr.and(data), type);
                }
                protected Expr add(Expr op1, Expr op2) {
                    return type.add(op1, op2);
                }
            }.proceed(Where.TRUE, expr);
        } else {
            return createInnerSplit(outerInner, expr, type);
        }
    }

    private static Where getFullWhere(Map<BaseExpr,BaseExpr> innerOuter, Expr expr) {
        return expr.getWhere().and(getWhere(innerOuter.keySet()));
    }

    private static <K> Where getFullWhere(Expr expr, Map<K,? extends Expr> mapInner) {
        return expr.getWhere().and(getWhere(mapInner.values()));
    }

    // "определяет" разбивать на innerJoins или нет
    private static Collection<GroupStatWhere> getWhereJoins(Expr expr, Map<BaseExpr, BaseExpr> outerInner, GroupType type) {
        // если sum (не max) то exclusive
        Collection<GroupJoinsWhere> whereJoins = getFullWhere(expr, outerInner).getWhereJoins(type.noExclusive());

        if(type.splitInnerJoins()) // группируем по join'ам
            return BaseUtils.immutableCast(whereJoins);
        else // группируем по статистике
            return GroupJoinsWhere.groupStat(whereJoins, new HashSet<BaseExpr>(outerInner.values()));
    }

    private static Expr createInnerSplit(Map<BaseExpr, BaseExpr> outerInner, Expr expr,GroupType type) {

        Expr result = CaseExpr.NULL;
        for(GroupStatWhere innerWhere : getWhereJoins(expr, outerInner, type)) {
            Expr innerResult;
            if(!innerWhere.keyEqual.isEmpty()) { // translatе'им expr
                QueryTranslator equalTranslator = innerWhere.keyEqual.getTranslator();
                innerResult = createInner(equalTranslator.translate(outerInner), expr.translateQuery(equalTranslator).and(innerWhere.where), type);
            } else
                innerResult = createInnerBase(outerInner, expr.and(innerWhere.where), type);

            // берем keyEquals
            result = type.add(result, innerResult);
        }
        return result;
    }

    private static <K> Expr createInnerBase(Map<BaseExpr, BaseExpr> outerInner, Expr expr,GroupType type) {
        Where fullWhere = getFullWhere(expr, outerInner);

        ReversedMap<BaseExpr, BaseExpr> innerOuter = new ReversedHashMap<BaseExpr, BaseExpr>();
        Where equalsWhere = getEqualsWhere(groupMap(outerInner, fullWhere.getExprValues(), innerOuter));

        // вытащим not'ы
        Where notWhere = Where.TRUE;
        for(Map.Entry<BaseExpr,BaseExpr> exprValue : fullWhere.getNotExprValues().entrySet()) {
            BaseExpr notValue = innerOuter.get(exprValue.getKey());
            if(notValue!=null)
                notWhere = notWhere.and(EqualsWhere.create(notValue,exprValue.getValue()).not());
        }

        return createFollowExpr(innerOuter, expr, type, notWhere).and(equalsWhere.and(notWhere));
    }

    private static Expr createFollowExpr(ReversedMap<BaseExpr, BaseExpr> innerOuter, Expr expr, GroupType type, Where notWhere) {
        // именно так потому как нужно обеспечить инвариант что в ClassWhere должны быть все следствия

        return createHandleKeys(innerOuter, expr.followFalse(
                getWhere(innerOuter).getClassWhere().mapBack(innerOuter).and(getWhere(innerOuter.keySet()).getClassWhere()).getPackWhere().and(notWhere).not(), false).
                and(getKeepWhere(getFullWhere(innerOuter, expr))), type);
    }

    private static Where getKeepWhere(Where fullWhere) {

        Where keepWhere = Where.TRUE;
        for(KeyExpr key : enumKeys(fullWhere))
            keepWhere = keepWhere.and(fullWhere.getKeepWhere(key));
        return keepWhere;
    }

    private static Expr createHandleKeys(ReversedMap<BaseExpr, BaseExpr> innerOuter, Expr expr,GroupType type) {

        // NOGROUP - проверяем если по всем ключам группируется, значит это никакая не группировка
        Map<BaseExpr, BaseExpr> compares = new HashMap<BaseExpr, BaseExpr>();
        Set<KeyExpr> keys = getKeys(expr, innerOuter);
        Map<KeyExpr, BaseExpr> groupKeys = BaseUtils.splitKeys(innerOuter, keys, compares);
        if(groupKeys.size()==keys.size()) {
            QueryTranslator translator = new QueryTranslator(groupKeys);
            Where equalsWhere = Where.TRUE; // чтобы лишних проталкиваний не было
            for(Map.Entry<BaseExpr,BaseExpr> compare : compares.entrySet()) // оставшиеся
                equalsWhere = equalsWhere.and(compare.getKey().translateQuery(translator).compare(compare.getValue(), Compare.EQUALS));
            return expr.translateQuery(translator).and(equalsWhere);
        }

        // FREEKEYS - отрезаем свободные ключи (которые есть только в группировке) и создаем выражение
        Where freeWhere = Where.TRUE;
        Map<KeyExpr, BaseExpr> freeKeys = new HashMap<KeyExpr, BaseExpr>();
        Map<KeyExpr, BaseExpr> usedKeys = BaseUtils.splitKeys(groupKeys, getKeys(expr, compares), freeKeys);
        Map<BaseExpr, BaseExpr> group = innerOuter;
        if(freeKeys.size()>0) {
            for(Map.Entry<KeyExpr,BaseExpr> freeKey : freeKeys.entrySet())
                freeWhere = freeWhere.and(freeKey.getValue().getWhere());
            group = BaseUtils.merge(usedKeys,compares);
        }

        // CREATEBASE - создаем с createBase
        return BaseExpr.create(type.createExpr(group, expr)).and(freeWhere);
    }

    // уже упаковано выражение, но добавились еще equals'ы и возможно outerWhere для pack'а, increment'ый алгоритм
    private static Expr createExprEquals(ReversedMap<BaseExpr, BaseExpr> outerInner, Expr expr,GroupType type,List<Pair<BaseExpr,BaseExpr>> equals) {
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        KeyEqual keyEqual = new KeyEqual();
        for(Pair<BaseExpr, BaseExpr> equal : equals)
            keyEqual = keyEqual.and(KeyEqual.getKeyEqual(equal.first, equal.second));
        if(!keyEqual.isEmpty()) { // translate'им и погнали
            QueryTranslator equalTranslator = keyEqual.getTranslator();
            return createInner(equalTranslator.translate(outerInner), expr.translateQuery(equalTranslator), type);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createFollowExpr(outerInner.reverse(), expr, type, Where.TRUE);
    }

    public String getExprSource(CompileSource source, String prefix) {

        Set<Expr> queryExprs = BaseUtils.addSet(group.keySet(), query.expr); // так как может одновременно и SUM и MAX нужен

        Map<Expr,String> fromPropertySelect = new HashMap<Expr, String>();
        Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
        String fromSelect = new platform.server.data.query.Query<KeyExpr,Expr>(BaseUtils.toMap(getKeys()),BaseUtils.toMap(queryExprs), Expr.getWhere(queryExprs))
            .compile(source.syntax, prefix).fillSelect(new HashMap<KeyExpr, String>(), fromPropertySelect, whereSelect, source.params);
        for(Map.Entry<BaseExpr,BaseExpr> groupEntry : group.entrySet())
            whereSelect.add(fromPropertySelect.get(groupEntry.getKey())+"="+groupEntry.getValue().getSource(source));

        return "(" + source.syntax.getSelect(fromSelect, query.groupType.getString() + "(" + fromPropertySelect.get(query.expr) + ")",
                BaseUtils.toString(whereSelect, " AND "), "", "", "") + ")";
    }

    private Map<KeyExpr, Stat> getInnerKeyStats() {
        KeyStat contextStat = getFullWhere();
        Map<KeyExpr, Stat> keyStats = new HashMap<KeyExpr, Stat>();
        for(KeyExpr key : innerContext.getKeys())
            keyStats.put(key, contextStat.getKeyStat(key));
        return keyStats;
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
        GroupStatWhere innerWhere = BaseUtils.single(getWhereJoins(query.expr, BaseUtils.reverse(group), query.groupType));
        assert innerWhere.keyEqual.isEmpty();
        return new GroupJoin(getInnerKeyStats(), getInnerKeyTypes(), innerContext.getValues(), query.groupType.splitExprCases()?query.expr.getBaseWhere():Where.TRUE,
                innerWhere.joins, group);
    }

    @IdentityLazy
    public Stat getStatValue(KeyStat keyStat) {
        if(query.groupType.isSelect()) {
            // assert что expr учавствует в where
            return new StatPullWheres().proceed(getFullWhere(), query.expr);
        }
        return null;
    }

    public Stat getTypeStat(KeyStat keyStat) {
        return query.expr.getTypeStat(getFullWhere());
    }
}

