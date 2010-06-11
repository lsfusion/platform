package platform.server.data.expr.query;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.caches.*;
import platform.server.data.expr.query.GroupJoin;
import platform.server.data.query.CompileSource;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.InnerWhere;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.Case;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.expr.*;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.classes.DataClass;

import java.util.*;

@Immutable
public abstract class GroupExpr extends QueryExpr<BaseExpr,Expr,GroupJoin> implements MapContext {

    @TwinLazy
    public Where getJoinsWhere() {
        return getWhere(group);
    }

    @Lazy
    public Where getKeysWhere() {
        return getWhere(group.keySet());
    }

    protected GroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(expr, group);
    }
    
    // assertion когда не надо push'ать
    public boolean assertNoPush(Where trueWhere) { // убрал trueWhere.and(getJoinsWhere())
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

    // проталкиваем статичные значения внутрь
    @Override
    @ManualLazy
    public BaseExpr packFollowFalse(Where falseWhere) {
        ClassExprWhere classWhere;
        Map<BaseExpr, BaseExpr> pushedGroup = pushValues(group, falseWhere);
        if(pushedGroup==group) {
            classWhere = falseWhere.not().andMeans(getJoinsWhere()).getClassWhere().mapBack(group).and(getKeysWhere().getClassWhere());
            if(getFullWhere().getClassWhere().means(classWhere))
                return this;
        } else
            classWhere = falseWhere.not().andMeans(getWhere(pushedGroup)).getClassWhere().mapBack(group).and(getWhere(pushedGroup.keySet()).getClassWhere());
        Expr packed = pushValues(pushedGroup, query, isMax(), classWhere.getPackWhere(), getContextTypeWhere());
        if(packed instanceof BaseExpr)
            return (BaseExpr) packed;
        else {
//            assert false;
            return this;
        }
    }

    // трансляция
    public GroupExpr(GroupExpr groupExpr, DirectTranslator translator) {
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

    // трансляция не прямая
    protected Expr create(Map<BaseExpr, BaseExpr> group, Expr expr) {
        return createBase(group, expr, isMax());
    }

    private static Where getFullWhere(Map<BaseExpr,BaseExpr> group, Expr expr) {
        return expr.getWhere().and(getWhere(group.keySet()));
    }

    @Lazy
    public Where getFullWhere() {
        return getFullWhere(group, query);
    }

    public Type getType(Where where) {
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

        public InnerJoins groupInnerJoins() {
            return new InnerJoins(GroupExpr.this.getGroupJoin(),this);
        }
        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create
            return getClassWhere(fullWhere).and(getWhere(group).getClassWhere());
        }

        protected abstract ClassExprWhere getClassWhere(Where fullWhere);
    }

    @Lazy
    public GroupJoin getGroupJoin() {
        return new GroupJoin(getKeys(), getValues(), BaseUtils.single(getExprCases(query, isMax())).where,
                BaseUtils.single(getInnerJoins(group, query, isMax())).mean, group);
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

    private static Expr add(Expr op1, Expr op2, boolean max) {
        return max?op1.max(op2):op1.sum(op2);
    }

    // вытаскивает равные ключи, а также статичные значения из (group.keys)
    private static <K> Where pullOuter(Map<K,BaseExpr> group, Map<K,BaseExpr> implement, Map<BaseExpr,BaseExpr> result) {
        Where equalsWhere = Where.TRUE;
        for(Map.Entry<K,BaseExpr> groupKey : group.entrySet()) {
            BaseExpr groupImp = implement.get(groupKey.getKey());
            if(groupKey.getValue().isValue())
                equalsWhere = equalsWhere.and(EqualsWhere.create(groupImp,groupKey.getValue()));
            else {
                BaseExpr resultImp = result.get(groupKey.getValue());
                if(resultImp==null)
                    result.put(groupKey.getValue(),groupImp);
                else
                    equalsWhere = equalsWhere.and(EqualsWhere.create(groupImp,resultImp));
            }
        }
        return equalsWhere;
    }

    private static <K> Expr create(Map<K,? extends Expr> group, Expr expr,boolean max,Map<K,? extends Expr> implement,PullExpr noPull) {
        ExprCaseList result = new ExprCaseList();
        
        for(MapCase<K> mapOutCase : CaseExpr.pullCases(implement)) {
            Expr caseExpr = NULL;

            Where upWhere = Where.FALSE;            
            for(MapCase<K> mapInCase : CaseExpr.pullCases(group)) {
                Where inWhere = mapInCase.where.and(upWhere.not());

                Map<BaseExpr,BaseExpr> groupImp = new HashMap<BaseExpr, BaseExpr>();
                Where outerWhere = pullOuter(mapInCase.data, mapOutCase.data, groupImp);
                caseExpr = add(caseExpr, pullExprs(max, expr.and(inWhere), groupImp, noPull).and(outerWhere), max);

                upWhere = upWhere.or(mapInCase.where);
            }

            result.add(mapOutCase.where, caseExpr);
        }

        return result.getExpr();
    }

    // вытаскиваем pull Exprs
    private static Expr pullExprs(boolean max, Expr expr, Map<BaseExpr, BaseExpr> group, PullExpr noPull) {
        Map<BaseExpr, BaseExpr> pullGroup = new HashMap<BaseExpr, BaseExpr>(group);
        for(KeyExpr key : getKeys(expr, group))
            if(key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull))
                pullGroup.put(key,key);
        group = pullGroup;

        return splitExprCases(max, expr, group);
    }

    // вытягивает Case'ы из группировочного выражения если необходимо
    private static <K> Expr splitExprCases(boolean max, Expr expr, Map<BaseExpr, BaseExpr> groupAnd) {
        Expr result = CaseExpr.NULL;
        Where upWhere = Where.FALSE;
        for(Case<? extends Expr> exprCase : getExprCases(expr,max)) {
            result = add(result, splitInner(max, groupAnd, exprCase.data.and(exprCase.where.and(upWhere.not()))), max);
            upWhere = upWhere.or(exprCase.where);
        }
        return result;
    }

    // "определяет" вытаскивать case'ы или нет
    private static Collection<? extends Case<? extends Expr>> getExprCases(Expr expr,boolean max) {
        return max?expr.getCases():Collections.singleton(new Case<Expr>(Where.TRUE,expr));
    }

    // "определяет" разбивать на innerJoins или нет
    private static Collection<InnerJoins.Entry> getInnerJoins(Map<BaseExpr,BaseExpr> group, Expr expr,boolean max) {
        Where fullWhere = getFullWhere(group, expr);
        if(fullWhere.isFalse()) return new ArrayList<InnerJoins.Entry>();
        return max?fullWhere.getInnerJoins():Collections.singleton(new InnerJoins.Entry(new InnerWhere(),Where.TRUE));
    }

    private static Expr splitInner(boolean max, Map<BaseExpr, BaseExpr> group, Expr expr) {

        Expr result = CaseExpr.NULL;
        Iterator<InnerJoins.Entry> it = getInnerJoins(group, expr, max).iterator();
        while(it.hasNext()) {
            Where innerWhere = it.next().where;
            result = add(result, createBase(group, expr.and(innerWhere), max), max);
            if(!max) { // для max'а нельзя ни followFalse'ить ни тем более push'ать, потому как для этого множества может изменит выражение\группировки
                expr = expr.and(innerWhere.not());
                it = getInnerJoins(group, expr, max).iterator(); // именно так потому как иначе нарушается assertion
            }
        }
        return result;
    }

    // проверяет, что group reverse'able
    protected static Expr createBase(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max) {
        Map<BaseExpr,BaseExpr> reversedGroup = new HashMap<BaseExpr, BaseExpr>();
        for(Map.Entry<BaseExpr,BaseExpr> groupExpr : group.entrySet()) {
            BaseExpr groupKey = reversedGroup.get(groupExpr.getValue());
            if(groupKey==null)
                reversedGroup.put(groupExpr.getValue(),groupExpr.getKey());
            else
                expr = expr.and(EqualsWhere.create(groupExpr.getKey(),groupKey));
        }

        return pushValues(BaseUtils.reverse(reversedGroup), expr, max, getWhere(group).getClassWhere().mapBack(group).and(getWhere(group.keySet()).getClassWhere()).getPackWhere(), new HashMap<KeyExpr, Type>());
    }

    // проталкиваем Values, которые в группировке (group.values)
    private static Expr pushValues(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where pushWhere, Map<KeyExpr, Type> keepTypes) {

        // PUSH VALUES
        Map<BaseExpr, BaseExpr> keepGroup = new HashMap<BaseExpr, BaseExpr>(); // проталкиваем values внутрь
        Where valueWhere = Where.TRUE; // чтобы лишних проталкиваний не было
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet())
            if (groupExpr.getValue().isValue())
                valueWhere = valueWhere.and(EqualsWhere.create(groupExpr.getKey(), groupExpr.getValue()));
            else
                keepGroup.put(groupExpr.getKey(),groupExpr.getValue());

        return pullGroupEquals(keepGroup, expr.and(valueWhere), max, pushWhere, keepTypes);
    }

    // обратное pushValues - если видит равенство, убирает из группировки и дает равенство наверх, как в pullOuter   
    private static Expr pullGroupEquals(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where pushWhere, Map<KeyExpr,Type> keepTypes) {
        Where equalsWhere = Where.TRUE;
        Map<BaseExpr, BaseExpr> exprValues = expr.getWhere().getExprValues();
        Map<BaseExpr, BaseExpr> keepGroup = new HashMap<BaseExpr, BaseExpr>();
        for(Map.Entry<BaseExpr, BaseExpr> groupExpr : group.entrySet()) {
            BaseExpr groupValue = exprValues.get(groupExpr.getKey());
            if(groupValue!=null)
                equalsWhere = equalsWhere.and(EqualsWhere.create(groupExpr.getValue(),groupValue));
            else
                keepGroup.put(groupExpr.getKey(), groupExpr.getValue());
        }
        return pullGroupNotEquals(keepGroup, expr, max, pushWhere, keepTypes).and(equalsWhere);
    }

    // вытаскивает наверх условие на неравенство, одновременно убирая потом из общего условия push'ая (нужно в том числе чтобы в packFollowFalse assertion не нарушить)
    private static Expr pullGroupNotEquals(Map<BaseExpr, BaseExpr> group, Expr expr, boolean max, Where pushWhere, Map<KeyExpr,Type> keepTypes) {
        Where notWhere = Where.TRUE;
        for(Map.Entry<BaseExpr,BaseExpr> exprValue : expr.getWhere().getNotExprValues().entrySet()) {
            BaseExpr notValue = group.get(exprValue.getKey());
            if(notValue!=null) {
                pushWhere = pushWhere.and(EqualsWhere.create(exprValue.getKey(),exprValue.getValue()).not());
                notWhere = notWhere.and(EqualsWhere.create(notValue,exprValue.getValue()).not());
            }
        }
        return pack(group, expr, max, pushWhere, keepTypes).and(notWhere);
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
            QueryTranslator translator = new QueryTranslator(keyExprs,false);

            Map<BaseExpr,BaseExpr> transBase = new HashMap<BaseExpr, BaseExpr>();
            for(BaseExpr groupExpr : group.keySet()) {
                Expr transExpr = groupExpr.translateQuery(translator);
                if(!(transExpr instanceof BaseExpr)) {
                    assert transExpr.equals(CaseExpr.NULL);
                    return CaseExpr.NULL;
                }
                transBase.put(groupExpr, (BaseExpr) transExpr);
            }

            Map<BaseExpr,BaseExpr> transGroup = new HashMap<BaseExpr, BaseExpr>();
            Where equalsWhere = pullOuter(transBase,group,transGroup);
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
            QueryTranslator translator = new QueryTranslator(groupKeys,true);
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

