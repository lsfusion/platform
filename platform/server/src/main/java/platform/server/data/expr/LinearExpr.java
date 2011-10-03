package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.CalculateJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.HashLazy;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.*;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать BaseExpr
@TranslateExprLazy
public class LinearExpr extends StaticClassExpr {

    final LinearOperandMap map;

    public LinearExpr(LinearOperandMap map) {
        this.map = map;
        assert (map.size()>0);
        assert !(map.size()==1 && BaseUtils.singleValue(map).equals(1));
    }

    public Type getType(KeyType keyType) {
        return getStaticClass();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return getStaticClass().getTypeStat();
    }

    // возвращает Where на notNull
    public Where calculateWhere() {
        return map.getWhere();
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return map.toString();
        else
            return map.getSource(compile);
    }

    public void enumDepends(ExprEnumerator enumerator) {
        map.enumerate(enumerator);
    }

    @Override
    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        map.fillJoinWheres(joins, andWhere);
    }

    // мы и так перегрузили fillJoinWheres
    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    @Override
    public boolean equals(Object obj) {
        if(map.size()==1) {
            Map.Entry<Expr, Integer> singleEntry = BaseUtils.singleEntry(map);
            if(singleEntry.getValue().equals(1)) return singleEntry.getKey().equals(obj);
        }
        return super.equals(obj);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return map.equals(((LinearExpr)obj).map);
    }

    @HashLazy
    public int hashOuter(HashContext hashContext) {
        return map.hashOuter(hashContext) * 5;
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        Expr result = null;
        for(Map.Entry<Expr, Integer> operand : map.entrySet()) {
            Expr transOperand = operand.getKey().translateQuery(translator).scale(operand.getValue());
            if(result==null)
                result = transOperand;
            else
                result = result.sum(transOperand);
        }
        return result;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    @ParamLazy
    public BaseExpr translateOuter(MapTranslate translator) {
        LinearOperandMap transMap = new LinearOperandMap();
        for(Map.Entry<Expr, Integer> operand : map.entrySet())
            transMap.put(operand.getKey().translateOuter(translator),operand.getValue());
        return new LinearExpr(transMap);
    }

    @Override
    public Expr packFollowFalse(Where where) {
        return map.packFollowFalse(where);
    }

    @IdentityLazy
    public IntegralClass getStaticClass() {
        return map.getType();
    }

    public long calculateComplexity() {
        return map.getComplexity();
    }

    public Stat getStatValue(KeyStat keyStat) {
        throw new RuntimeException("not supported yet"); //return getTypeStat(keyStat);
    }

    private static void fillOrderedExprs(BaseExpr baseExpr, BaseExpr fromExpr, OrderedMap<BaseExpr, Collection<BaseExpr>> orderedExprs) {
        Collection<BaseExpr> fromExprs = orderedExprs.get(baseExpr);
        if(fromExprs == null) {
            for(BaseExpr joinExpr : baseExpr.getBaseJoin().getJoins().values())
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
        for(Expr expr : map.keySet())
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

    @Override
    public boolean isOr() {
        return map.size() > 1;
    }
}