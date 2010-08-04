package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.ReversedHashMap;
import platform.base.ReversedMap;
import platform.server.caches.IdentityLazy;
import platform.server.caches.TwinLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.cases.*;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.query.innerjoins.InnerSelectJoin;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.interop.Compare;

import java.util.*;

public abstract class GroupExpr extends QueryExpr<BaseExpr,Expr,GroupJoin> {

    @TwinLazy
    public Where getJoinsWhere() {
        return getWhere(group);
    }

    @IdentityLazy
    public Where getKeysWhere() {
        return getWhere(group.keySet());
    }

    protected GroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr, ClassExprWhere packClassWhere) {
        super(expr, group);

        if(packClassWhere!=null)
            packNoChange.add(packClassWhere);
    }
    
    public abstract boolean isMax();

    // трансляция
    public GroupExpr(GroupExpr groupExpr, MapTranslate translator) {
        super(groupExpr, translator);
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
        return getFullWhere(group, query);
    }

    public Type getType(KeyType keyType) {
        return query.getType(getFullWhere());
    }

    public abstract class NotNull extends InnerExpr.NotNull {

        public int hashContext(HashContext hashContext) {
            return GroupExpr.this.hashContext(hashContext);
        }

        @Override
        public Where packFollowFalse(Where falseWhere) {
            return GroupExpr.this.packFollowFalse(falseWhere).getWhere();
        }

        public ObjectJoinSets groupObjectJoinSets() {
            return new ObjectJoinSets(GroupExpr.this.getGroupJoin(),this);
        }
        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create
            return getClassWhere(fullWhere).and(getWhere(group).getClassWhere());
        }

        protected abstract ClassExprWhere getClassWhere(Where fullWhere);
    }

    @IdentityLazy
    public GroupJoin getGroupJoin() {
        InnerSelectJoin innerJoin = BaseUtils.single(getInnerJoins(query, BaseUtils.reverse(group), isMax()));
        assert innerJoin.keyEqual.isEmpty();
        return new GroupJoin(getKeys(), getValues(), BaseUtils.single(getExprCases(query, isMax())).where,
                innerJoin.joins, group);
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

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,boolean max,Map<K,? extends Expr> implement) {
        return create(group,expr,where,max,implement,null);
    }

    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,Where where,boolean max,Map<K,? extends Expr> implement, PullExpr noPull) {
        return create(group,expr.and(where),max,implement,noPull);
    }
    
    private static Expr add(Expr op1, Expr op2, boolean max) {
        return max?op1.max(op2):op1.sum(op2);
    }

    // "определяет" вытаскивать case'ы или нет
    private static Collection<? extends Case<? extends Expr>> getExprCases(Expr expr,boolean max) {
        return max?expr.getCases():Collections.singleton(new Case<Expr>(Where.TRUE,expr));
    }

    private Collection<ClassExprWhere> packNoChange = new ArrayList<ClassExprWhere>();

    @Override
    public Expr packFollowFalse(Where falseWhere) {
        Map<BaseExpr, Expr> packGroup = packFollowFalse(group, falseWhere);

        Where outerWhere = falseWhere.not();
        if(BaseUtils.hashEquals(packGroup,group) && Collections.disjoint(group.values(), outerWhere.getExprValues().keySet())) { // если простой пак
            ReversedMap<BaseExpr, BaseExpr> innerOuter = new ReversedHashMap<BaseExpr, BaseExpr>(group);

            ClassExprWhere packClasses = getPackWhere(innerOuter, outerWhere);

            for(ClassExprWhere packed : packNoChange)
                if(packed.means(packClasses)) // если более общим пакуем
                    return this;

             Expr result = createPack(innerOuter, query, isMax(), outerWhere, Where.TRUE);
             if(BaseUtils.hashEquals(result,this)) {
                 packNoChange.add(packClasses);
                 return this;
             } else
                 return result;
        } else
            return createOuterGroupCases(packGroup, query, isMax(), outerWhere);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return createOuterGroupCases(translator.translate(group), query, isMax(), null);
    }

    private static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr,boolean max, Map<K, ? extends Expr> outer, PullExpr noPull) {
        Map<Object, Expr> pullInner = new HashMap<Object, Expr>(inner);
        Map<Object, Expr> pullOuter = new HashMap<Object, Expr>(outer);
        for(KeyExpr key : enumKeys(inner.values(),expr.getEnum()))
            if(key instanceof PullExpr && !inner.containsValue(key) && !key.equals(noPull)) {
                Object pullObject = new Object();
                pullInner.put(pullObject,key);
                pullOuter.put(pullObject,key);
            }

        return create(pullInner, expr, max, pullOuter);
    }
    
    // вытаскивает из outer Case'ы
    public static <K> Expr create(Map<K, ? extends Expr> inner, Expr expr,boolean max, Map<K, ? extends Expr> outer) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<K> caseOuter : CaseExpr.pullCases(outer))
            result.add(caseOuter.where, createOuterBase(inner, expr, max, caseOuter.data));
        return result.getExpr();
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupCases(Map<BaseExpr, ? extends Expr> innerOuter, Expr expr, boolean max, Where outerWhere) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<BaseExpr> caseInnerOuter : CaseExpr.pullCases(innerOuter))
            result.add(caseInnerOuter.where, createOuterGroupBase(caseInnerOuter.data, expr, max, outerWhere));
        return result.getExpr();
    }

    private static <T extends Expr> Where getEqualsWhere(List<Pair<T,T>> equals) {
        Where where = Where.TRUE;
        for(Pair<T, T> equal : equals)
            where = where.and(equal.first.compare(equal.second, Compare.EQUALS));
        return where;
    }

    // для использования в нижних 2-х методах, ищет EQUALS'ы, EXPRVALUES, VALUES, из Collection<BaseExpr,T> делает Map<BaseExpr,T> без values (в том числе с учетом доп. where)
    private static <T extends Expr> List<Pair<T, T>> groupMap(Iterable<Pair<BaseExpr, T>> group, Where groupWhere, Map<BaseExpr, T> grouped) {

        Map<BaseExpr, BaseExpr> exprValues = groupWhere==null?new HashMap<BaseExpr, BaseExpr>():groupWhere.getExprValues();
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

    private static <K, T extends Expr> Where groupMap(final Map<K, T> inner, final Map<K, BaseExpr> outer, Where groupWhere, Map<BaseExpr, T> outerInner) {
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
        }, groupWhere, outerInner));
    }

    private static List<Pair<BaseExpr, BaseExpr>> groupMap(final Map<BaseExpr,BaseExpr> map, Where groupWhere, ReversedMap<BaseExpr, BaseExpr> reversed) {
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
        return groupMap(iterable, groupWhere, reversed);
    }

    private static <K, I extends Expr> Expr createOuterBase(Map<K,I> inner, Expr expr,boolean max, Map<K,BaseExpr> outer) {
        Map<BaseExpr, I> outerInner = new HashMap<BaseExpr, I>();
        expr = expr.and(groupMap(inner, outer, null, outerInner));
        return createInner(outerInner, expr, max, null);
    }

    // если translate или packFollowFalse, на самом деле тоже самое что сверху, но иначе придется кучу generics'ов сделать
    private static Expr createOuterGroupBase(Map<BaseExpr, BaseExpr> innerOuter, Expr expr, boolean max, Where outerWhere) {
        ReversedMap<BaseExpr, BaseExpr> outerInner = new ReversedHashMap<BaseExpr, BaseExpr>();
        List<Pair<BaseExpr, BaseExpr>> equals = groupMap(innerOuter, outerWhere, outerInner);
        expr = expr.and(getEqualsWhere(equals));
        return createExprEquals(outerInner, expr, max, equals, outerWhere);
    }

    private static Expr createInner(Map<BaseExpr, ? extends Expr> outerInner, Expr expr,boolean max,Where outerWhere) {
        return createInnerCases(outerInner, expr, max, outerWhere);
    }

    private static Expr createInnerCases(Map<BaseExpr, ? extends Expr> outerInner, Expr expr,boolean max,Where outerWhere) {
        Expr result = NULL;
        Where upWhere = Where.FALSE;
        for(MapCase<BaseExpr> caseOuterInner : CaseExpr.pullCases(outerInner)) {
            result = add(result, createInnerExprCases(caseOuterInner.data, expr.and(caseOuterInner.where.and(upWhere.not())), max, outerWhere), max);
            upWhere = upWhere.or(caseOuterInner.where);
        }
        return result;
    }

    private static Expr createInnerExprCases(Map<BaseExpr, BaseExpr> outerInner, Expr expr,boolean max, Where outerWhere) {
        Expr result = NULL;
        Where upWhere = Where.FALSE;
        for(Case<? extends Expr> exprCase : getExprCases(expr,max)) {
            result = add(result, createInnerSplit(outerInner, exprCase.data.and(exprCase.where.and(upWhere.not())), max, outerWhere), max);
            upWhere = upWhere.or(exprCase.where);
        }
        return result;

    }

    private static Where getFullWhere(Map<BaseExpr,BaseExpr> innerOuter, Expr expr) {
        return expr.getWhere().and(getWhere(innerOuter.keySet()));
    }

    private static <K> Where getFullWhere(Expr expr, Map<K,? extends Expr> mapInner) {
        return expr.getWhere().and(getWhere(mapInner.values()));
    }
    
    // "определяет" разбивать на innerJoins или нет
    private static Collection<InnerSelectJoin> getInnerJoins(Expr expr, Map<BaseExpr, BaseExpr> outerInner, boolean max) {
        return getFullWhere(expr, outerInner).getKeyEquals().getInnerJoins(max);
    }
    
    private static Expr createInnerSplit(Map<BaseExpr, BaseExpr> outerInner, Expr expr,boolean max,Where outerWhere) {

        Expr result = CaseExpr.NULL;
        Iterator<InnerSelectJoin> it = getInnerJoins(expr, outerInner, max).iterator();
        while(it.hasNext()) {
            InnerSelectJoin innerJoin = it.next();
            Expr innerResult;
            if(!innerJoin.keyEqual.isEmpty()) { // translatе'им expr
                QueryTranslator equalTranslator = innerJoin.keyEqual.getTranslator();
                innerResult = createInner(equalTranslator.translate(outerInner), expr.translateQuery(equalTranslator).and(innerJoin.where), max, outerWhere);
            } else
                innerResult = createInnerBase(outerInner, expr.and(innerJoin.where), max, outerWhere);
            
            // берем keyEquals
            result = add(result, innerResult, max);
            if(!max) { // для max'а нельзя ни followFalse'ить ни тем более push'ать, потому как для этого множества может изменит выражение\группировки
                expr = expr.and(innerJoin.fullWhere.not());
                it = getInnerJoins(expr, outerInner, max).iterator(); // именно так потому как иначе нарушается assertion
            }
        }
        return result;
    }

    private static <K> Expr createInnerBase(Map<BaseExpr, BaseExpr> outerInner, Expr expr,boolean max, Where outerWhere) {
        Where fullWhere = getFullWhere(expr, outerInner);

        ReversedMap<BaseExpr, BaseExpr> innerOuter = new ReversedHashMap<BaseExpr, BaseExpr>();
        Where equalsWhere = getEqualsWhere(groupMap(outerInner, fullWhere, innerOuter));

        // вытащим
        Where notWhere = Where.TRUE;
        for(Map.Entry<BaseExpr,BaseExpr> exprValue : fullWhere.getNotExprValues().entrySet()) {
            BaseExpr notValue = innerOuter.get(exprValue.getKey());
            if(notValue!=null)
                notWhere = notWhere.and(EqualsWhere.create(notValue,exprValue.getValue()).not());
        }

        return createPack(innerOuter, expr, max, outerWhere, notWhere).and(equalsWhere.and(notWhere));
    }

    private static ClassExprWhere getPackWhere(ReversedMap<BaseExpr, BaseExpr> innerOuter, Where outerWhere) {
        return getWhere(innerOuter).and(outerWhere==null? Where.TRUE:outerWhere).getClassWhere().mapBack(innerOuter).
                                and(getWhere(innerOuter.keySet()).getClassWhere());
    }
    
    private static Expr createPack(ReversedMap<BaseExpr, BaseExpr> innerOuter, Expr expr, boolean max, Where outerWhere, Where notWhere) {
        // именно так потому как нужно обеспечить инвариант что в ClassWhere должны быть все следствия  
        ClassExprWhere packClassWhere = getPackWhere(innerOuter, outerWhere);
        Where packWhere = packClassWhere.getPackWhere().and(notWhere);

        Where fullWhere = getFullWhere(innerOuter, expr);
        Where keepWhere = Where.TRUE;
        for(KeyExpr key : enumKeys(fullWhere))
            keepWhere = keepWhere.and(key.isClass(fullWhere.getKeepClass(key)));
        if(outerWhere==null)
            expr = expr.followFalse(packWhere.not(), false).and(keepWhere);
        else { // запускаем "рекурсивный" pack
            Expr packExpr = expr.followFalse(packWhere.not(), true).and(keepWhere); // сначала pack'аем expr
            Map<BaseExpr, Expr> packOuterInner = new HashMap<BaseExpr, Expr>();
            for(Map.Entry<BaseExpr,BaseExpr> entry : innerOuter.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
                packOuterInner.put(entry.getValue(), entry.getKey().packFollowFalse(packExpr.getWhere().and(packWhere.or(entry.getKey().getWhere().not()))));

            if(!BaseUtils.hashEquals(packExpr,expr) || !BaseUtils.hashEquals(innerOuter.reverse(),packOuterInner)) // если изменилось погнали по кругу, или же один раз
                return createInner(packOuterInner, packExpr, max, null);
        }

        return createHandleKeys(innerOuter, expr, max, outerWhere==null?null:packClassWhere);
    }

    private static Expr createHandleKeys(ReversedMap<BaseExpr, BaseExpr> innerOuter, Expr expr,boolean max, ClassExprWhere packClassWhere) {

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
        GroupExpr groupExpr;
        if(max)
            groupExpr = new MaxGroupExpr(group,expr,packClassWhere);
        else
            groupExpr = new SumGroupExpr(group,expr,packClassWhere);
        return BaseExpr.create(groupExpr).and(freeWhere);
    }

    // уже упаковано выражение, но добавились еще equals'ы и возможно outerWhere для pack'а, increment'ый алгоритм
    private static Expr createExprEquals(ReversedMap<BaseExpr, BaseExpr> outerInner, Expr expr,boolean max,List<Pair<BaseExpr,BaseExpr>> equals, Where outerWhere) {
        // assert что EqualsWhere - это Collection<BaseExpr,BaseExpr>
        KeyEqual keyEqual = new KeyEqual();
        for(Pair<BaseExpr, BaseExpr> equal : equals)
            keyEqual = keyEqual.and(KeyEqual.getKeyEqual(equal.first, equal.second));
        if(!keyEqual.isEmpty()) { // translate'им и погнали
            QueryTranslator equalTranslator = keyEqual.getTranslator();
            return createInner(equalTranslator.translate(outerInner), expr.translateQuery(equalTranslator), max, outerWhere);
        }

        // не было keyEqual, не добавились inner'ы keyEquals, просто pack'уем
        return createPack(outerInner.reverse(), expr, max, outerWhere, Where.TRUE);
    }
}

