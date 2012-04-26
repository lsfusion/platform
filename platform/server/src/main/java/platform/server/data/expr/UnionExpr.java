package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.CalculateJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.type.Type;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

// выражение для оптимизации, разворачивание которого в case'ы даст экспоненту
public abstract class UnionExpr extends NotNullExpr implements StaticClassExprInterface {

    public abstract DataClass getStaticClass();

    protected abstract Collection<Expr> getParams();

    public Type getType(KeyType keyType) {
        return getStaticClass();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    public Where calculateOrWhere() {
        Where result = Where.FALSE;
        for(Expr operand : getParams())
            result = result.or(operand.getWhere());
        return result;
    }

    @Override
    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(getParams());
    }

    @Override
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(Expr operand : getParams()) // просто гоним по операндам
            operand.fillJoinWheres(joins, andWhere);
    }

    protected boolean isComplex() {
        return true;
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Stat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    private static void fillOrderedExprs(BaseExpr baseExpr, BaseExpr fromExpr, OrderedMap<BaseExpr, Collection<BaseExpr>> orderedExprs) {
        Collection<BaseExpr> fromExprs = orderedExprs.get(baseExpr);
        if(fromExprs == null) {
            for(BaseExpr joinExpr : baseExpr.getUsed())
                fillOrderedExprs(joinExpr, baseExpr, orderedExprs);
            fromExprs = new ArrayList<BaseExpr>();
            orderedExprs.put(baseExpr, fromExprs);
        }
        if(fromExpr!=null)
            fromExprs.add(fromExpr);
    }

    @IdentityLazy
    public List<BaseExpr> getCommonExprs() {

        Set<BaseExpr> baseExprs = new HashSet<BaseExpr>();
        for(Expr expr : getParams())
            baseExprs.addAll(expr.getBaseExprs());

        if(baseExprs.size()==1)
            return new ArrayList<BaseExpr>(baseExprs);

        Map<BaseExpr, Set<BaseExpr>> found = new HashMap<BaseExpr, Set<BaseExpr>>();
        OrderedMap<BaseExpr, Collection<BaseExpr>> orderedExprs = new OrderedMap<BaseExpr, Collection<BaseExpr>>();
        for(BaseExpr baseExpr : baseExprs)
            fillOrderedExprs(baseExpr, null, orderedExprs);

        List<BaseExpr> result = new ArrayList<BaseExpr>();
        for(BaseExpr baseExpr : BaseUtils.reverse(orderedExprs.keyList())) { // бежим с конца
            Set<BaseExpr> exprFound = new HashSet<BaseExpr>();
            for(BaseExpr depExpr : orderedExprs.get(baseExpr)) {
                Set<BaseExpr> prevSet = found.get(depExpr);
                if(prevSet==null) { // значит уже в result'е
                    exprFound = null;
                    break;
                }
                exprFound.addAll(prevSet);
            }
            if(baseExprs.contains(baseExpr))
                exprFound.add(baseExpr); // assert'ся что не может быть exprFound

            if(exprFound ==null || exprFound.size() == baseExprs.size()) { // все есть
                if(exprFound != null) // только что нашли
                   result.add(baseExpr);
            } else
                found.put(baseExpr, exprFound);
        }
        return result;
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return new CalculateJoin<Integer>(BaseUtils.toMap(getCommonExprs())); // тут надо было бы getTypeStat использовать, но пока не предполагается использование Linear в Join'ах
    }

    // множественное наследование StaticClassExpr
    @Override
    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return StaticClassExpr.getClassWhere(this, classes);
    }

    @Override
    public Expr classExpr(BaseClass baseClass) {
        return StaticClassExpr.classExpr(this, baseClass);
    }

    @Override
    public Where isClass(AndClassSet set) {
        return StaticClassExpr.isClass(this, set);
    }

    @Override
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        return StaticClassExpr.getAndClassSet(this, and);
    }

    @Override
    public boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add) {
        return StaticClassExpr.addAndClassSet(this, add);
    }
}
