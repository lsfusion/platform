package platform.server.data.expr;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.GroupJoin;
import platform.server.data.query.HashContext;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.InnerWhere;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.Case;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.classes.DataClass;

import java.util.*;

@Immutable
public abstract class GroupExpr extends MapExpr implements MapContext {

    public final Map<BaseExpr, BaseExpr> group;
    public final Expr expr;

    private static Set<KeyExpr> getKeys(Expr expr, Map<BaseExpr, BaseExpr> group) {
        return enumKeys(group.keySet(),expr);
    }

    @Lazy
    public Set<KeyExpr> getKeys() {
        return getKeys(expr, group);
    }

    @Lazy
    public Set<ValueExpr> getValues() {
        return enumValues(group.keySet(),expr);
    }

    @TwinLazy
    public Where getJoinsWhere() {
        return getJoinsWhere(group);
    }

    protected GroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        this.expr = expr;
        this.group = group;

        assert checkExpr();
    }
    
    // assertion когда не надо push'ать
    public boolean assertNoPush(Where<?> trueWhere) { // убрал trueWhere.and(getJoinsWhere())
        return Collections.disjoint(group.values(),trueWhere.getExprValues().keySet()) && getFullWhere().getClassWhere().means(trueWhere.getClassWhere().mapBack(group));
    }

    public abstract boolean isMax();

    @TwinLazy
    private Map<KeyExpr, Type> getContextTypeWhere() {
        Map<KeyExpr, Type> result = new HashMap<KeyExpr, Type>();
        Where fullWhere = getFullWhere();
        for(KeyExpr key : getKeys())
            result.put(key,key.getType(fullWhere));
        return result;
    }

    // проталкивает "верхний" where внутрь
    private static Where pushWhere(Map<BaseExpr, BaseExpr> group, Where trueWhere) {
        Where result = trueWhere.and(getJoinsWhere(group)).getClassWhere().mapBack(group).and(getWhere(group.keySet()).getClassWhere()).getMeansWhere();
        assert result.means(getWhere(group.keySet())); // надо assert'ить чтобы не and'ить
        return result;
    }
    
    private static Map<BaseExpr, BaseExpr> pushValues(Map<BaseExpr, BaseExpr> group,Where<?> trueWhere) {
        Map<BaseExpr,ValueExpr> exprValues = trueWhere.getExprValues();
        Map<BaseExpr, BaseExpr> result = new HashMap<BaseExpr, BaseExpr>(); ValueExpr pushValue; // проталкиваем values внутрь
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet())
            result.put(groupExpr.getKey(),((pushValue=exprValues.get(groupExpr.getValue()))==null?groupExpr.getValue():pushValue));
        return result;
    }

    @Override
    public BaseExpr packFollowFalse(Where falseWhere) {
        return (BaseExpr) createAnd(pushValues(group,falseWhere.not()), expr, isMax(), falseWhere.not(), getContextTypeWhere());
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr,KeyTranslator translator) {
        // надо еще транслировать "внутренние" values
        Map<ValueExpr, ValueExpr> mapValues = BaseUtils.filterKeys(translator.values, groupExpr.getValues());

        if(BaseUtils.identity(mapValues)) { // если все совпадает то и не перетранслируем внутри ничего 
            expr = groupExpr.expr;
            group = translator.translateDirect(groupExpr.group);
        } else { // еще values перетранслируем
            KeyTranslator valueTranslator = new KeyTranslator(BaseUtils.toMap(groupExpr.getKeys()), mapValues);
            expr = groupExpr.expr.translateDirect(valueTranslator);
            group = new HashMap<BaseExpr, BaseExpr>();
            for(Map.Entry<BaseExpr, BaseExpr> groupJoin : groupExpr.group.entrySet())
                group.put(groupJoin.getKey().translateDirect(valueTranslator),groupJoin.getValue().translateDirect(translator));
        }

        assert checkExpr();
    }

    private boolean checkExpr() {
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet())
            assert !(groupExpr.getValue() instanceof ValueExpr);

//        for(KeyExpr key : getKeys())
//            assert !(key instanceof PullExpr) || group.containsKey(key) || expr.equals(key); 

        return true;
    }

    // трансляция не прямая
    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<BaseExpr> mapCase : CaseExpr.pullCases(translator.translate(group)))
            result.add(mapCase.where,createAnd(mapCase.data, expr, isMax()));
        return result.getExpr();
    }

    private static Where getFullWhere(Map<BaseExpr,BaseExpr> group, Expr expr) {
        return expr.getWhere().and(getWhere(group.keySet()));
    }

    @Lazy
    public Where getFullWhere() {
        return getFullWhere(group,expr);
    }

    public Type getType(Where where) {
        return expr.getType(getFullWhere());
    }

    public abstract class NotNull extends MapExpr.NotNull {

        protected DataWhereSet getExprFollows() {
            return MapExpr.getExprFollows(group);
        }

        public InnerJoins getInnerJoins() {
            // здесь что-то типа GroupJoin'а быть должно
            return new InnerJoins(getGroupJoin(),this);
        }

        public int hashContext(HashContext hashContext) {
            return GroupExpr.this.hashContext(hashContext);
        }

        @Override
        public Where packFollowFalse(Where falseWhere) {
            return GroupExpr.this.packFollowFalse(falseWhere).getWhere();
        }

        public GroupJoin getGroupJoin() {
            return GroupExpr.this.getGroupJoin();
        }

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create
            return getClassWhere(fullWhere).and(getJoinsWhere(group).getClassWhere());
        }

        protected abstract ClassExprWhere getClassWhere(Where fullWhere);
    }

    // извращенное множественное наследование
    private GroupHashes hashes = new GroupHashes() {
        protected int hashValue(HashContext hashContext) {
            return expr.hashContext(hashContext);
        }
        protected Map<BaseExpr, BaseExpr> getGroup() {
            return group;
        }
    };
    public int hashContext(final HashContext hashContext) {
        return hashes.hashContext(hashContext);
    }
    public int hash(HashContext hashContext) {
        return hashes.hash(hashContext);
    }

    public boolean twins(AbstractSourceJoin obj) {
        GroupExpr groupExpr = (GroupExpr)obj;

        assert hashCode()==groupExpr.hashCode();

        for(KeyTranslator translator : new MapHashIterable(this, groupExpr, false))
            if(expr.translateDirect(translator).equals(groupExpr.expr) &&
                    translator.translateDirect(BaseUtils.reverse(group)).equals(BaseUtils.reverse(groupExpr.group)))
                return true;
        return false;
    }

    public InnerJoin getFJGroup() {
        return getGroupJoin();
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.fill(group);
        for(ValueExpr value : getValues())
            enumerator.add(value);
    }

    @Lazy
    public GroupJoin getGroupJoin() {
        return new GroupJoin(BaseUtils.single(getExprCases(expr, isMax())).where,
                BaseUtils.single(getInnerJoins(group, expr, isMax())).mean,
                group, getKeys(), getValues());
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
    
    public static <K> Expr create(Map<K,? extends Expr> group, Expr expr,boolean max,Map<K,? extends Expr> implement) {
        return create(group, expr, max, implement, null);
    }

    private static <K> Expr create(Map<K,? extends Expr> group, Expr expr,boolean max,Map<K,? extends Expr> implement,PullExpr noPull) {
        ExprCaseList result = new ExprCaseList();
        
        for(MapCase<K> mapOutCase : CaseExpr.pullCases(implement)) {
            Expr caseExpr = NULL;

            Where upWhere = Where.FALSE;            
            for(MapCase<K> mapInCase : CaseExpr.pullCases(group)) {
                Where groupWhere = mapInCase.where.and(upWhere.not());
                
                Map<BaseExpr, BaseExpr> groupAnd = BaseUtils.crossJoin(mapInCase.data, mapOutCase.data);

                Where upExprWhere = Where.FALSE;
                for(Case<? extends Expr> exprCase : getExprCases(expr,max)) {
                    caseExpr = pullExprs(caseExpr, max, groupAnd, exprCase.data.and(groupWhere.and(exprCase.where.and(upExprWhere.not()))), noPull);

                    upExprWhere = upExprWhere.or(exprCase.where);
                }

                upWhere = upWhere.or(mapInCase.where);
            }

            result.add(mapOutCase.where, caseExpr);
        }

        return result.getExpr();
    }

    // вытаскиваем pull Exprs
    private static Expr pullExprs(Expr result, boolean max, Map<BaseExpr, BaseExpr> group, Expr expr, PullExpr noPull) {
        Map<BaseExpr, BaseExpr> pullGroup = new HashMap<BaseExpr, BaseExpr>(group);
        for(KeyExpr key : getKeys(expr, group))
            if(key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull))
                pullGroup.put(key,key);
        group = pullGroup;

        return addInner(result, max, group, expr);
    }

    // "определяет" вытаскивать case'ы или нет
    private static Collection<? extends Case<? extends Expr>> getExprCases(Expr expr,boolean max) {
        return max?expr.getCases():Collections.singleton(new Case<Expr>(Where.TRUE,expr));
    }

    // "определяет" разбивать на innerJoins или нет
    private static Collection<InnerJoins.Entry> getInnerJoins(Map<BaseExpr,BaseExpr> group, Expr expr,boolean max) {
        return false?getFullWhere(group, expr).getInnerJoins().compileMeans():Collections.singleton(new InnerJoins.Entry(new InnerWhere(),Where.TRUE));
    }

    private static Expr addInner(Expr result, boolean max, Map<BaseExpr, BaseExpr> group, Expr expr) {
        
        Where innerUp = Where.FALSE;
        for(InnerJoins.Entry entry : getInnerJoins(group,expr,max)) {
            Where innerWhere = entry.where;
            if(!max) // для max'а нельзя ни followFalse'ить ни тем более push'ать, потому как для этого множества может изменит выражение\группировки
                innerWhere = innerWhere.and(innerUp.not());
            Expr innerExpr = createAnd(group, expr.and(innerWhere), max);
            if(max)
                result = result.max(innerExpr);
            else {
                result = result.sum(innerExpr);
                innerUp = innerUp.or(innerWhere);
            }
        }
        return result;
    }

    private static Expr createAnd(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max) {
        return createAnd(group, expr, max, Where.TRUE, new HashMap<KeyExpr, Type>());
    }

    private static Expr createAnd(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where upWhere, Map<KeyExpr,Type> keepTypes) {
        return pushValues(group, expr, max, pushWhere(group, upWhere), keepTypes);
    }

    // проталкиваем Values
    private static Expr pushValues(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where pushWhere, Map<KeyExpr,Type> keepTypes) {

        // PUSH VALUES
        Map<BaseExpr, BaseExpr> keepGroup = new HashMap<BaseExpr, BaseExpr>(); // проталкиваем values внутрь
        Where valueWhere = Where.TRUE; // чтобы лишних проталкиваний не было
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet())
            if (groupExpr.getValue() instanceof ValueExpr)
                valueWhere = valueWhere.and(EqualsWhere.create(groupExpr.getKey(), groupExpr.getValue()));
            else
                keepGroup.put(groupExpr.getKey(),groupExpr.getValue());

        return pack(keepGroup, expr.and(valueWhere), max, pushWhere, keepTypes);
    }

    // пакуем все что можно
    private static Expr pack(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where pushWhere, Map<KeyExpr,Type> keepTypes) {

        expr = expr.followFalse(pushWhere.not()); // сначала pack'аем expr
        Map<BaseExpr, BaseExpr> packGroup = new HashMap<BaseExpr, BaseExpr>();
        for(Map.Entry<BaseExpr, BaseExpr> entry : group.entrySet()) // собсно будем паковать "общим" where исключив, одновременно за or not'им себя, чтобы собой не пакнуться
            packGroup.put(entry.getKey().packFollowFalse(expr.getWhere().and(pushWhere.or(entry.getKey().getWhere().not()))),entry.getValue());

        return keepTypes(packGroup, expr, max, keepTypes);
    }

    // сохраняем типы для инварианта
    private static Expr keepTypes(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Map<KeyExpr,Type> keepTypes) {
        for(Map.Entry<KeyExpr,Type> keepType : BaseUtils.filterKeys(keepTypes,getKeys(expr,group)).entrySet()) // сохраним типы
            if(keepType.getValue() instanceof DataClass)
                expr = expr.and(keepType.getKey().isClass((DataClass)keepType.getValue()));

        return transKeyEquals(group, expr, max);
    }

    private static Expr transKeyEquals(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max) {

        // translate'им все key = выражение
        Map<KeyExpr, BaseExpr> keyExprs;
        if(!(keyExprs = expr.getWhere().getKeyExprs()).isEmpty()) {
            QueryTranslator translator = new QueryTranslator(keyExprs,new HashMap<ValueExpr, ValueExpr>(),false);
            Map<BaseExpr, BaseExpr> transGroup = new HashMap<BaseExpr, BaseExpr>();
            Where equalsWhere = Where.TRUE;
            for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet()) {
                Expr transExpr = groupExpr.getKey().translateQuery(translator);
                if(!(transExpr instanceof BaseExpr)) {
                    assert transExpr.equals(CaseExpr.NULL);
                    return CaseExpr.NULL;
                }
                BaseExpr baseExpr = (BaseExpr) transExpr;
                BaseExpr prevExpr = transGroup.get(baseExpr);
                if(prevExpr==null)
                    transGroup.put(baseExpr,groupExpr.getValue());
                else
                    equalsWhere = equalsWhere.and(EqualsWhere.create(groupExpr.getValue(),prevExpr));
            }
            // могла поменяться вся логика выражения - не можем гнать рекурсивно с pack, потому как не знаем что с pushWhere делать
            return transKeyEquals(transGroup,expr.translateQuery(translator),max).and(equalsWhere);
        }

        return handleKeys(group, expr, max);
    }

    private static Expr handleKeys(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max) {

        Map<BaseExpr, BaseExpr> compares = new HashMap<BaseExpr, BaseExpr>();
        Set<KeyExpr> keys = getKeys(expr, group);
        Map<KeyExpr, BaseExpr> groupKeys = BaseUtils.splitKeys(group, keys, compares);
        if(groupKeys.size()==keys.size()) {
            QueryTranslator translator = new QueryTranslator(groupKeys,new HashMap<ValueExpr, ValueExpr>(),true);
            Where equalsWhere = Where.TRUE; // чтобы лишних проталкиваний не было
            for(Map.Entry<BaseExpr, BaseExpr> compare : compares.entrySet()) // оставшиеся
                equalsWhere = equalsWhere.and(EqualsWhere.create((BaseExpr) compare.getKey().translateQuery(translator),compare.getValue()));
            return expr.translateQuery(translator).and(equalsWhere);
        }

        // отрезаем свободные ключи и создаем выражение
        Where freeWhere = Where.TRUE;
        Map<KeyExpr, BaseExpr> freeKeys = new HashMap<KeyExpr, BaseExpr>();
        Map<KeyExpr, BaseExpr> usedKeys = BaseUtils.splitKeys(groupKeys, getKeys(expr,compares), freeKeys);
        if(freeKeys.size()>0) {
            for(Map.Entry<KeyExpr, BaseExpr> groupExpr : freeKeys.entrySet())
                freeWhere = freeWhere.and(groupExpr.getValue().getWhere());
            group = BaseUtils.merge(usedKeys,compares);
        }
        return createGroupExpr(group,expr,max).and(freeWhere);
    }

    private static Expr createGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max) {
        GroupExpr groupExpr;
        if(max)
            groupExpr = new MaxGroupExpr(group,expr);
        else
            groupExpr = new SumGroupExpr(group,expr);
        return BaseExpr.create(groupExpr);
    }
}

